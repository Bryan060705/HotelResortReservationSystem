// Author: Bong Xin Yee
// Main controller for the housekeeping module
// Uses ArrayStack to handle the undo/rollback feature
package control;

import adt.ArrayStack;
import adt.StackInterface;
import entity.CleaningStatus;
import entity.CleaningTask;
import entity.HotelDataStore;
import entity.HousekeepingRecord;
import entity.Room;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import utility.SearchAndSortUtility;

public final class HousekeepingController {

    private static final int DEFAULT_CAPACITY = 10;
    private static final int FIRST_TASK_ID = 1042;

    // Sort rooms by cleaning status first, then by room number
    private static final Comparator<HousekeepingRecord> STATUS_PRIORITY_COMPARATOR
            = Comparator.comparingInt((HousekeepingRecord record) -> record.getStatus().ordinal())
                    .thenComparing(HousekeepingRecord::getRoomNumber);

    // Sort staff by the number of rooms they are handling
    private static final Comparator<StaffWorkload> STAFF_WORKLOAD_COMPARATOR
            = Comparator.comparingInt((StaffWorkload workload) -> workload.totalRoomsAssigned())
                    .reversed()
                    .thenComparing(StaffWorkload::staffName, String.CASE_INSENSITIVE_ORDER);

    private final HotelDataStore dataStore;
    private final Clock clock;

    // Stack used to keep track of status changes for rollback
    private final StackInterface<CleaningTask> undoStack;

    // Store the current status of each room
    private HousekeepingRecord[] roomRecords;
    private int roomRecordCount;

    // Keep a separate copy of all task logs for the reports
    private CleaningTask[] auditHistory;
    private int auditHistoryCount;

    private int nextTaskId;

    public HousekeepingController(HotelDataStore dataStore) {
        this(dataStore, Clock.systemDefaultZone());
    }

    HousekeepingController(HotelDataStore dataStore, Clock clock) {
        this.dataStore = Objects.requireNonNull(dataStore, "DataStore cannot be null");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
        this.roomRecords = new HousekeepingRecord[DEFAULT_CAPACITY];
        this.roomRecordCount = 0;
        this.auditHistory = new CleaningTask[DEFAULT_CAPACITY];
        this.auditHistoryCount = 0;
        this.undoStack = new ArrayStack<>();
        this.nextTaskId = FIRST_TASK_ID;
        syncNewRooms();
    }

    // Add any new rooms from the shared data store
    // New rooms start with DIRTY status
    private void syncNewRooms() {
        Room[] sharedRooms = dataStore.getRoomsSnapshot();

        for (Room room : sharedRooms) {
            if (findRecordInternal(room.getRoomNumber()) == null) {
                ensureRoomCapacity();

                roomRecords[roomRecordCount++] = new HousekeepingRecord(
                        room.getRoomNumber(),
                        room.getRoomType(),
                        CleaningStatus.DIRTY,
                        now()
                );
            }
        }
    }

    public HousekeepingRecord findRecord(String roomNumber) {
        syncNewRooms();
        return findRecordInternal(roomNumber);
    }

    private HousekeepingRecord findRecordInternal(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }

        for (int i = 0; i < roomRecordCount; i++) {
            if (roomRecords[i].getRoomNumber().equalsIgnoreCase(roomNumber.trim())) {
                return roomRecords[i];
            }
        }

        return null;
    }

    // Get a copy of all room records and sort them for display
    public HousekeepingRecord[] getAllRecordsSnapshot() {
        syncNewRooms();

        HousekeepingRecord[] copy = new HousekeepingRecord[roomRecordCount];
        System.arraycopy(roomRecords, 0, copy, 0, roomRecordCount);

        SearchAndSortUtility.mergeSort(copy, STATUS_PRIORITY_COMPARATOR);

        return copy;
    }

    public RoomStatusView[] getAllRoomStatusesSnapshot() {
        HousekeepingRecord[] sortedRecords = getAllRecordsSnapshot();
        RoomStatusView[] views = new RoomStatusView[sortedRecords.length];

        for (int i = 0; i < sortedRecords.length; i++) {
            views[i] = toView(sortedRecords[i]);
        }

        return views;
    }

    // Get a copy of the audit history for the report
    public CleaningTask[] getAuditHistorySnapshot() {
        CleaningTask[] copy = new CleaningTask[auditHistoryCount];
        System.arraycopy(auditHistory, 0, copy, 0, auditHistoryCount);

        return copy;
    }

    private RoomStatusView toView(HousekeepingRecord record) {
        String turnaroundText;

        // Only show duration if assigned AND active cleaning has actually begun
        if (!record.isAssigned() || record.getStatus() == CleaningStatus.DIRTY) {
            turnaroundText = "--";
        } else {
            long turnaround = record.getTurnaroundMinutes();
            if (turnaround < 0) {
                turnaroundText = "--";
            } else if (turnaround == 0) {
                turnaroundText = "< 1 min";
            } else {
                turnaroundText = turnaround + " mins";
            }
        }

        return new RoomStatusView(
                record.getRoomNumber(),
                record.getRoomType().getDisplayName(),
                record.getStatus().getDisplayName(),
                record.getAssignedStaff(),
                turnaroundText
        );
    }

    // Assign a staff member to the selected room
    public TaskActionResult assignCleaningTask(String roomNumber, String staffName) {
        HousekeepingRecord record = findRecord(roomNumber);

        if (record == null) {
            return new TaskActionResult(
                    false,
                    "Room " + roomNumber + " is not tracked by housekeeping.",
                    roomNumber,
                    staffName,
                    null,
                    null,
                    -1,
                    undoStack.size()
            );
        }

        if (staffName == null || staffName.isBlank()) {
            return new TaskActionResult(
                    false,
                    "Staff name is required.",
                    roomNumber,
                    staffName,
                    record.getStatus().getDisplayName(),
                    record.getStatus().getDisplayName(),
                    -1,
                    undoStack.size()
            );
        }

        record.assignStaff(staffName.trim(), now());

        return new TaskActionResult(
                true,
                "Room " + record.getRoomNumber() + " assigned to " + record.getAssignedStaff() + ".",
                record.getRoomNumber(),
                record.getAssignedStaff(),
                record.getStatus().getDisplayName(),
                record.getStatus().getDisplayName(),
                -1,
                undoStack.size()
        );
    }

    // Change the room status and save the change to the undo stack
    public TaskActionResult updateRoomStatus(String roomNumber, int statusMenuChoice) {
        HousekeepingRecord record = findRecord(roomNumber);

        if (record == null) {
            return new TaskActionResult(
                    false,
                    "Room " + roomNumber + " is not tracked by housekeeping.",
                    roomNumber,
                    null,
                    null,
                    null,
                    -1,
                    undoStack.size()
            );
        }

        CleaningStatus newStatus;

        try {
            newStatus = CleaningStatus.fromMenuChoice(statusMenuChoice);
        } catch (IllegalArgumentException ex) {
            return new TaskActionResult(
                    false,
                    ex.getMessage(),
                    roomNumber,
                    record.getAssignedStaff(),
                    record.getStatus().getDisplayName(),
                    null,
                    -1,
                    undoStack.size()
            );
        }

        // Avoid duplicate status update
        if (record.getStatus() == newStatus) {
            return new TaskActionResult(
                    false,
                    "Room " + roomNumber + " is already in status '" + newStatus.getDisplayName() + "'. No change made.",
                    roomNumber,
                    record.getAssignedStaff(),
                    record.getStatus().getDisplayName(),
                    null,
                    -1,
                    undoStack.size()
            );
        }

        // Avoid updating already completed rooms via normal update
        if (record.getStatus() == CleaningStatus.READY) {
            return new TaskActionResult(
                    false,
                    "Room " + roomNumber + " is already READY. Use Rollback (Option 4) to revert.",
                    roomNumber,
                    record.getAssignedStaff(),
                    record.getStatus().getDisplayName(),
                    null,
                    -1,
                    undoStack.size()
            );
        }

        CleaningStatus previousStatus = record.getStatus();
        LocalDateTime timestamp = now();

        record.changeStatus(newStatus, timestamp);
        
        // Sync with shared data store
        Room sharedRoom = dataStore.findRoom(record.getRoomNumber());
        if (sharedRoom != null && newStatus == CleaningStatus.READY) {
            sharedRoom.release(); // Marks AVAILABLE for Booking / VIP
        }

        int taskId = nextTaskId++;

        CleaningTask task = new CleaningTask(
                taskId,
                record.getRoomNumber(),
                record.getAssignedStaff(),
                previousStatus,
                newStatus,
                timestamp
        );

        // Save the change in the stack so the latest change can be undone first
        undoStack.push(task);

        // Keep the same task in the audit history for reporting
        ensureAuditCapacity();
        auditHistory[auditHistoryCount++] = task;

        return new TaskActionResult(
                true,
                "Room " + record.getRoomNumber()
                        + " updated to '" + newStatus.getDisplayName() + "'.",
                record.getRoomNumber(),
                record.getAssignedStaff(),
                previousStatus.getDisplayName(),
                newStatus.getDisplayName(),
                taskId,
                undoStack.size()
        );
    }

// Remove the latest task from the stack and restore the previous status
    public RollbackResult rollbackLastTask() {
        if (undoStack.isEmpty()) {
            return new RollbackResult(
                    false,
                    "The rollback stack is empty; no task to undo.",
                    -1,
                    null,
                    null,
                    0
            );
        }

        CleaningTask topTask = undoStack.peek();

        // Integrity Check: ONLY block rollback if this task made the room READY and a guest has since occupied it
        Room sharedRoom = dataStore.findRoom(topTask.getRoomNumber());
        if (topTask.getNewStatus() == CleaningStatus.READY 
                && sharedRoom != null 
                && sharedRoom.getStatus() == entity.RoomStatus.OCCUPIED) {
            return new RollbackResult(
                    false,
                    "Integrity Violation: Room " + topTask.getRoomNumber()
                            + " has already been ALLOCATED to a checked-in guest. Rollback rejected.",
                    topTask.getTaskId(),
                    topTask.getRoomNumber(),
                    null,
                    undoStack.size()
            );
        }

        CleaningTask task = undoStack.pop();
        HousekeepingRecord record = findRecord(task.getRoomNumber());

        if (record != null) {
            record.revertStatus(task.getPreviousStatus(), now());

            // If we are rolling back away from READY, lock the room back to not available
            Room sharedRoomToRevert = dataStore.findRoom(task.getRoomNumber());
            if (sharedRoomToRevert != null) {
                if (task.getPreviousStatus() == CleaningStatus.READY) {
                    sharedRoomToRevert.release(); // Reverted back into READY
                } else if (task.getNewStatus() == CleaningStatus.READY) {
                    sharedRoomToRevert.setStatus(entity.RoomStatus.OCCUPIED); // Reverted away from READY
                }
            }
        }

       return new RollbackResult(
                true,
                "Rollback complete! Room " + task.getRoomNumber()
                        + " reverted to '"
                        + task.getPreviousStatus().getDisplayName()
                        + "'.",
                task.getTaskId(),
                task.getRoomNumber(),
                task.getPreviousStatus().getDisplayName(),
                undoStack.size()
        );
    }

    // Look at the latest task without removing it
    public CleaningTask peekLastTask() {
        return undoStack.isEmpty() ? null : undoStack.peek();
    }

    // Return the number of tasks currently waiting to be undone
    public int getUndoStackSize() {
        return undoStack.size();
    }
    
    // Returns only valid forward transitions (excluding DIRTY, identical current status, and regression)
    public StatusOption[] getAvailableStatusTransitions(CleaningStatus currentStatus) {
        CleaningStatus[] allStatuses = CleaningStatus.values();
        int count = 0;
        for (CleaningStatus status : allStatuses) {
            if (status != CleaningStatus.DIRTY && status != currentStatus) {
                if (currentStatus == CleaningStatus.INSPECTED && status == CleaningStatus.CLEANING) {
                    continue;
                }
                count++;
            }
        }

        StatusOption[] options = new StatusOption[count];
        int index = 0;
        for (CleaningStatus status : allStatuses) {
            if (status != CleaningStatus.DIRTY && status != currentStatus) {
                if (currentStatus == CleaningStatus.INSPECTED && status == CleaningStatus.CLEANING) {
                    continue;
                }
                options[index++] = new StatusOption(
                        status.getMenuChoice(),
                        status.getMenuLabel()
                );
            }
        }

        return options;
    }

    // Get the status options that can be selected
    // DIRTY is left out because it is the starting status
    public StatusOption[] getUpdatableStatusOptions() {
        CleaningStatus[] values = CleaningStatus.values();
        StatusOption[] options = new StatusOption[values.length - 1];

        int index = 0;

        for (CleaningStatus status : values) {
            if (status == CleaningStatus.DIRTY) {
                continue;
            }

            options[index++] = new StatusOption(
                    status.getMenuChoice(),
                    status.getMenuLabel()
            );
        }

        return options;
    }
    
    // Registered duty staff
    private static final String[] DUTY_STAFF = {
        "Bong Xin Yee",
        "Tang Hong Yi",
        "Bryan Won Chu Ming",
        "Carret Chong Kar Loke",
        "Siti Aminah",
        "Muhammad Faiz",
        "Kavitha Rajan"
    };

    public String[] getDutyStaff() {
        String[] copy = new String[DUTY_STAFF.length];
        System.arraycopy(DUTY_STAFF, 0, copy, 0, DUTY_STAFF.length);
        return copy;
    }

    // Calculate the room and staff information needed for the summary report
    public HousekeepingSummaryReport generateSummaryReport() {
        syncNewRooms();

        int statusValueCount = CleaningStatus.values().length;
        int[] countsByStatusOrdinal = new int[statusValueCount];

        String[] staffNames = new String[Math.max(roomRecordCount, 1)];
        int[] staffTotalRooms = new int[Math.max(roomRecordCount, 1)];
        int[] staffInProgressRooms = new int[Math.max(roomRecordCount, 1)];
        int[] staffCompletedRooms = new int[Math.max(roomRecordCount, 1)];

        int staffTally = 0;
        int unassignedRooms = 0;
        long turnaroundSumMinutes = 0;
        int turnaroundSampleCount = 0;

        for (int i = 0; i < roomRecordCount; i++) {
            HousekeepingRecord record = roomRecords[i];
            CleaningStatus status = record.getStatus();

            if (status != null) {
                countsByStatusOrdinal[status.ordinal()]++;
            }

            if (!record.isAssigned()) {
                unassignedRooms++;
                continue;
            }

            long turnaround = record.getTurnaroundMinutes();

            if (turnaround >= 0) {
                turnaroundSumMinutes += turnaround;
                turnaroundSampleCount++;
            }

            String staffName = record.getAssignedStaff();

            if (staffName == null || staffName.isBlank()) {
                continue;
            }

            int staffIndex = -1;

            for (int existing = 0; existing < staffTally; existing++) {
                if (staffNames[existing].equalsIgnoreCase(staffName)) {
                    staffIndex = existing;
                    break;
                }
            }

            if (staffIndex == -1) {
                staffIndex = staffTally++;
                staffNames[staffIndex] = staffName;
            }

            staffTotalRooms[staffIndex]++;

            if (status == CleaningStatus.READY) {
                staffCompletedRooms[staffIndex]++;
            } else {
                staffInProgressRooms[staffIndex]++;
            }
        }

        // Calculate the percentage for each cleaning status
        StatusCount[] statusBreakdown = new StatusCount[statusValueCount];

        for (CleaningStatus status : CleaningStatus.values()) {
            int count = countsByStatusOrdinal[status.ordinal()];

            double percentage = roomRecordCount == 0
                    ? 0.0
                    : (count * 100.0) / roomRecordCount;

            statusBreakdown[status.ordinal()] = new StatusCount(
                    status.getDisplayName(),
                    count,
                    percentage
            );
        }

        // Prepare the workload information for each staff member
        StaffWorkload[] staffWorkload = new StaffWorkload[staffTally];

        for (int index = 0; index < staffTally; index++) {
            staffWorkload[index] = new StaffWorkload(
                    staffNames[index],
                    staffTotalRooms[index],
                    staffInProgressRooms[index],
                    staffCompletedRooms[index]
            );
        }

        SearchAndSortUtility.mergeSort(
                staffWorkload,
                STAFF_WORKLOAD_COMPARATOR
        );

        String busiestStaffName = staffWorkload.length == 0
                ? "N/A"
                : staffWorkload[0].staffName();

        int busiestStaffRoomCount = staffWorkload.length == 0
                ? 0
                : staffWorkload[0].totalRoomsAssigned();

        double averageTurnaroundMinutes = turnaroundSampleCount == 0
                ? 0.0
                : (double) turnaroundSumMinutes / turnaroundSampleCount;

        return new HousekeepingSummaryReport(
                now(),
                roomRecordCount,
                statusBreakdown,
                unassignedRooms,
                roomRecordCount - unassignedRooms,
                staffWorkload,
                busiestStaffName,
                busiestStaffRoomCount,
                averageTurnaroundMinutes,
                undoStack.size()
        );
    }

    // Increase the room array size when there is no more space
    private void ensureRoomCapacity() {
        if (roomRecordCount == roomRecords.length) {
            HousekeepingRecord[] expanded =
                    new HousekeepingRecord[roomRecords.length * 2];

            System.arraycopy(
                    roomRecords,
                    0,
                    expanded,
                    0,
                    roomRecords.length
            );

            roomRecords = expanded;
        }
    }

    // Increase the audit log array size when there is no more space
    private void ensureAuditCapacity() {
        if (auditHistoryCount == auditHistory.length) {
            CleaningTask[] expanded =
                    new CleaningTask[auditHistory.length * 2];

            System.arraycopy(
                    auditHistory,
                    0,
                    expanded,
                    0,
                    auditHistory.length
            );

            auditHistory = expanded;
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record RoomStatusView(
            String roomNumber,
            String roomType,
            String status,
            String assignedStaff,
            String turnaround
    ) {}

    public record TaskActionResult(
            boolean success,
            String message,
            String roomNumber,
            String staffName,
            String previousStatus,
            String newStatus,
            int taskId,
            int undoStackSize
    ) {}

    public record RollbackResult(
            boolean success,
            String message,
            int taskId,
            String roomNumber,
            String revertedStatus,
            int remainingStackSize
    ) {}

    public record StatusOption(
            int value,
            String label
    ) {}

    public record StatusCount(
            String statusLabel,
            int roomCount,
            double percentageOfTotal
    ) {}

    public record StaffWorkload(
            String staffName,
            int totalRoomsAssigned,
            int roomsInProgress,
            int roomsCompleted
    ) {}

    public record HousekeepingSummaryReport(
            LocalDateTime generatedAt,
            int totalRoomsTracked,
            StatusCount[] statusBreakdown,
            int unassignedRooms,
            int assignedRooms,
            StaffWorkload[] staffWorkload,
            String busiestStaffName,
            int busiestStaffRoomCount,
            double averageTurnaroundMinutes,
            int pendingRollbackTasks
    ) {}
}