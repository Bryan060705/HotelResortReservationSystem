/*
 * Author: Bong Xin Yee
 * Controls housekeeping operations, including room status,
 * staff assignment, and rollback history.
 */
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
    
    // Sort records by cleaning status order, then room number
    private static final Comparator<HousekeepingRecord> STATUS_PRIORITY_COMPARATOR
            = Comparator.comparingInt(
                    (HousekeepingRecord record) -> record.getStatus().ordinal())
                    .thenComparing(HousekeepingRecord::getRoomNumber);

    private final HotelDataStore dataStore;
    private final Clock clock;
    private final StackInterface<CleaningTask> undoStack;
    private HousekeepingRecord[] records;
    private int recordCount;
    private int nextTaskId;

    public HousekeepingController(HotelDataStore dataStore) {
        this(dataStore, Clock.systemDefaultZone());
    }

    HousekeepingController(HotelDataStore dataStore, Clock clock) {
        this.dataStore = Objects.requireNonNull(
                dataStore, "Shared hotel data is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        records = new HousekeepingRecord[DEFAULT_CAPACITY];
        undoStack = new ArrayStack<>();
        nextTaskId = FIRST_TASK_ID;
        syncNewRooms();
    }

    // Sync untracked rooms from the data store with default DIRTY status
    private void syncNewRooms() {
        Room[] sharedRooms = dataStore.getRoomsSnapshot();
        for (Room room : sharedRooms) {
            if (findRecord(room.getRoomNumber()) == null) {
                ensureCapacity();
                records[recordCount++] = new HousekeepingRecord(
                        room.getRoomNumber(), room.getRoomType(),
                        CleaningStatus.DIRTY, now());
            }
        }
    }

    // Find record by room number using linear search
    public HousekeepingRecord findRecord(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }
        for (int index = 0; index < recordCount; index++) {
            if (records[index].getRoomNumber()
                    .equalsIgnoreCase(roomNumber.trim())) {
                return records[index];
            }
        }
        return null;
    }

    // Get sorted room statuses snapshot for display
    public RoomStatusView[] getAllRoomStatusesSnapshot() {
        syncNewRooms();
        HousekeepingRecord[] copy = new HousekeepingRecord[recordCount];
        System.arraycopy(records, 0, copy, 0, recordCount);
        SearchAndSortUtility.mergeSort(copy, STATUS_PRIORITY_COMPARATOR);

        RoomStatusView[] views = new RoomStatusView[copy.length];
        for (int index = 0; index < copy.length; index++) {
            views[index] = toView(copy[index]);
        }
        return views;
    }

    // Convert entity record to view DTO
    private RoomStatusView toView(HousekeepingRecord record) {
        long turnaround = record.getTurnaroundMinutes();
        String turnaroundText = turnaround < 0 ? "--" : turnaround + " mins";
        return new RoomStatusView(record.getRoomNumber(),
                record.getRoomType().getDisplayName(),
                record.getStatus().getDisplayName(),
                record.getAssignedStaff(), turnaroundText);
    }

    // Assign a staff member to a room
    public TaskActionResult assignCleaningTask(String roomNumber, String staffName) {
        HousekeepingRecord record = findRecord(roomNumber);
        if (record == null) {
            return new TaskActionResult(false,
                    "Room " + roomNumber + " is not tracked by housekeeping.",
                    roomNumber, staffName, null, null, -1, undoStack.size());
        }
        if (staffName == null || staffName.isBlank()) {
            return new TaskActionResult(false, "Staff name is required.",
                    roomNumber, staffName, record.getStatus().getDisplayName(),
                    record.getStatus().getDisplayName(), -1, undoStack.size());
        }

        record.assignStaff(staffName, now());
        return new TaskActionResult(true,
                "Room " + record.getRoomNumber() + " assigned to "
                        + record.getAssignedStaff() + ".",
                record.getRoomNumber(), record.getAssignedStaff(),
                record.getStatus().getDisplayName(),
                record.getStatus().getDisplayName(), -1, undoStack.size());
    }

    // Update status and push old state to undo stack
    public TaskActionResult updateRoomStatus(String roomNumber, int statusMenuChoice) {
        HousekeepingRecord record = findRecord(roomNumber);
        if (record == null) {
            return new TaskActionResult(false,
                    "Room " + roomNumber + " is not tracked by housekeeping.",
                    roomNumber, null, null, null, -1, undoStack.size());
        }

        CleaningStatus newStatus;
        try {
            newStatus = CleaningStatus.fromMenuChoice(statusMenuChoice);
        } catch (IllegalArgumentException exception) {
            return new TaskActionResult(false, exception.getMessage(),
                    roomNumber, record.getAssignedStaff(),
                    record.getStatus().getDisplayName(), null, -1,
                    undoStack.size());
        }

        CleaningStatus previousStatus = record.getStatus();
        LocalDateTime timestamp = now();
        record.changeStatus(newStatus, timestamp);

        int taskId = nextTaskId++;
        CleaningTask task = new CleaningTask(taskId, record.getRoomNumber(),
                record.getAssignedStaff(), previousStatus, newStatus, timestamp);
        undoStack.push(task);

        return new TaskActionResult(true,
                "Room " + record.getRoomNumber() + " updated to '"
                        + newStatus.getDisplayName() + "'.",
                record.getRoomNumber(), record.getAssignedStaff(),
                previousStatus.getDisplayName(), newStatus.getDisplayName(),
                taskId, undoStack.size());
    }

    // Pop the latest task from the stack and revert status
    public RollbackResult rollbackLastTask() {
        if (undoStack.isEmpty()) {
            return new RollbackResult(false,
                    "The rollback stack is empty; no task to undo.",
                    -1, null, null, 0);
        }

        CleaningTask task = undoStack.pop();
        HousekeepingRecord record = findRecord(task.getRoomNumber());
        if (record != null) {
            record.revertStatus(task.getPreviousStatus(), now());
        }

        return new RollbackResult(true,
                "Rollback complete! Room " + task.getRoomNumber()
                        + " reverted to '"
                        + task.getPreviousStatus().getDisplayName() + "'.",
                task.getTaskId(), task.getRoomNumber(),
                task.getPreviousStatus().getDisplayName(), undoStack.size());
    }

    public CleaningTask peekLastTask() {
        return undoStack.isEmpty() ? null : undoStack.peek();
    }

    public int getUndoStackSize() {
        return undoStack.size();
    }

    // Options for update menu (skip DIRTY)
    public StatusOption[] getUpdatableStatusOptions() {
        CleaningStatus[] values = CleaningStatus.values();
        StatusOption[] options = new StatusOption[values.length - 1];
        int index = 0;
        for (CleaningStatus status : values) {
            if (status == CleaningStatus.DIRTY) {
                continue;
            }
            options[index++] = new StatusOption(
                    status.getMenuChoice(), status.getMenuLabel());
        }
        return options;
    }

    // Sort staff by total rooms assigned descending, then alphabetically by name
    private static final Comparator<StaffWorkload> STAFF_WORKLOAD_COMPARATOR
            = Comparator.comparingInt(
                    (StaffWorkload workload) -> workload.totalRoomsAssigned())
                    .reversed()
                    .thenComparing(StaffWorkload::staffName,
                            String.CASE_INSENSITIVE_ORDER);

    // Calculate room counts, workload breakdown, and turnaround metrics
    public HousekeepingSummaryReport generateSummaryReport() {
        syncNewRooms();

        int statusValueCount = CleaningStatus.values().length;
        int[] countsByStatusOrdinal = new int[statusValueCount];

        // Parallel arrays to tally workload per staff member
        String[] staffNames = new String[Math.max(recordCount, 1)];
        int[] staffTotalRooms = new int[Math.max(recordCount, 1)];
        int[] staffInProgressRooms = new int[Math.max(recordCount, 1)];
        int[] staffCompletedRooms = new int[Math.max(recordCount, 1)];
        int staffTally = 0;

        int unassignedRooms = 0;
        long turnaroundSumMinutes = 0;
        int turnaroundSampleCount = 0;

        for (int index = 0; index < recordCount; index++) {
            HousekeepingRecord record = records[index];
            if (record == null) {
                continue;
            }

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

        StatusCount[] statusBreakdown = new StatusCount[statusValueCount];
        for (CleaningStatus status : CleaningStatus.values()) {
            int count = countsByStatusOrdinal[status.ordinal()];
            double percentage = recordCount == 0
                    ? 0.0 : (count * 100.0) / recordCount;
            statusBreakdown[status.ordinal()]
                    = new StatusCount(status.getDisplayName(), count, percentage);
        }

        StaffWorkload[] staffWorkload = new StaffWorkload[staffTally];
        for (int index = 0; index < staffTally; index++) {
            staffWorkload[index] = new StaffWorkload(staffNames[index],
                    staffTotalRooms[index], staffInProgressRooms[index],
                    staffCompletedRooms[index]);
        }
        SearchAndSortUtility.mergeSort(staffWorkload, STAFF_WORKLOAD_COMPARATOR);

        String busiestStaffName = staffWorkload.length == 0
                ? "N/A" : staffWorkload[0].staffName();
        int busiestStaffRoomCount = staffWorkload.length == 0
                ? 0 : staffWorkload[0].totalRoomsAssigned();

        double averageTurnaroundMinutes = turnaroundSampleCount == 0
                ? 0.0 : (double) turnaroundSumMinutes / turnaroundSampleCount;

        return new HousekeepingSummaryReport(now(), recordCount, statusBreakdown,
                unassignedRooms, recordCount - unassignedRooms, staffWorkload,
                busiestStaffName, busiestStaffRoomCount,
                averageTurnaroundMinutes, undoStack.size());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // Expand records array by doubling its capacity
    private void ensureCapacity() {
        if (recordCount == records.length) {
            HousekeepingRecord[] expanded
                    = new HousekeepingRecord[records.length * 2];
            System.arraycopy(records, 0, expanded, 0, records.length);
            records = expanded;
        }
    }

    public record RoomStatusView(String roomNumber, String roomType,
            String status, String assignedStaff, String turnaround) {
    }

    public record TaskActionResult(boolean success, String message,
            String roomNumber, String staffName, String previousStatus,
            String newStatus, int taskId, int undoStackSize) {
    }

    public record RollbackResult(boolean success, String message, int taskId,
            String roomNumber, String revertedStatus, int remainingStackSize) {
    }

    public record StatusOption(int value, String label) {
    }

    public record StatusCount(String statusLabel, int roomCount,
            double percentageOfTotal) {
        public StatusCount {
            statusLabel = statusLabel == null ? "Unknown" : statusLabel;
            roomCount = Math.max(0, roomCount);
            percentageOfTotal = Double.isFinite(percentageOfTotal)
                    ? Math.max(0.0, percentageOfTotal) : 0.0;
        }
    }

    public record StaffWorkload(String staffName, int totalRoomsAssigned,
            int roomsInProgress, int roomsCompleted) {
        public StaffWorkload {
            staffName = (staffName == null || staffName.isBlank())
                    ? "Unassigned" : staffName.trim();
            totalRoomsAssigned = Math.max(0, totalRoomsAssigned);
            roomsInProgress = Math.max(0, roomsInProgress);
            roomsCompleted = Math.max(0, roomsCompleted);
        }
    }

    public record HousekeepingSummaryReport(LocalDateTime generatedAt,
            int totalRoomsTracked, StatusCount[] statusBreakdown,
            int unassignedRooms, int assignedRooms,
            StaffWorkload[] staffWorkload, String busiestStaffName,
            int busiestStaffRoomCount, double averageTurnaroundMinutes,
            int pendingRollbackTasks) {

        public HousekeepingSummaryReport {
            generatedAt = generatedAt == null ? LocalDateTime.now() : generatedAt;
            totalRoomsTracked = Math.max(0, totalRoomsTracked);
            statusBreakdown = statusBreakdown == null
                    ? new StatusCount[0] : statusBreakdown.clone();
            unassignedRooms = Math.max(0, unassignedRooms);
            assignedRooms = Math.max(0, assignedRooms);
            staffWorkload = staffWorkload == null
                    ? new StaffWorkload[0] : staffWorkload.clone();
            busiestStaffName = (busiestStaffName == null || busiestStaffName.isBlank())
                    ? "N/A" : busiestStaffName;
            busiestStaffRoomCount = Math.max(0, busiestStaffRoomCount);
            averageTurnaroundMinutes = Double.isFinite(averageTurnaroundMinutes)
                    ? Math.max(0.0, averageTurnaroundMinutes) : 0.0;
            pendingRollbackTasks = Math.max(0, pendingRollbackTasks);
        }

        // Return defensive copies of arrays to maintain immutability
        @Override
        public StatusCount[] statusBreakdown() {
            return statusBreakdown.clone();
        }

        @Override
        public StaffWorkload[] staffWorkload() {
            return staffWorkload.clone();
        }
    }
}