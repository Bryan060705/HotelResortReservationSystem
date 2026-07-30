/*
 * Author: Bong Xin Yee
 * Orchestrates room cleaning status tracking, staff task assignment, and
 * the rollback/undo history for housekeeping status changes.
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

    // Receives the shared hotel data used by all integrated modules.
    public HousekeepingController(HotelDataStore dataStore) {
        this(dataStore, Clock.systemDefaultZone());
    }

    // Creates a controller with a selected clock; this is used by tests.
    HousekeepingController(HotelDataStore dataStore, Clock clock) {
        this.dataStore = Objects.requireNonNull(
                dataStore, "Shared hotel data is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        records = new HousekeepingRecord[DEFAULT_CAPACITY];
        undoStack = new ArrayStack<>();
        nextTaskId = FIRST_TASK_ID;
        syncNewRooms();
    }

    // Tracks any shared room that housekeeping has not registered yet,
    // starting each new room as "Dirty".
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

    // Uses linear searching to find a tracked room by room number.
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

    // Returns every tracked room sorted by cleaning priority
    // (Dirty -> Cleaning -> Inspected -> Ready) using merge sort.
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

    // Converts a housekeeping record into its read-only display view.
    private RoomStatusView toView(HousekeepingRecord record) {
        long turnaround = record.getTurnaroundMinutes();
        String turnaroundText = turnaround < 0 ? "--" : turnaround + " mins";
        return new RoomStatusView(record.getRoomNumber(),
                record.getRoomType().getDisplayName(),
                record.getStatus().getDisplayName(),
                record.getAssignedStaff(), turnaroundText);
    }

    // Assigns a staff member to a room and starts its turnaround timer.
    // This does not change the room's cleaning status and is not logged
    // onto the rollback stack.
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

    // Changes a room's cleaning status and logs the change onto the
    // rollback stack so it can be undone later.
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

    // Pops the most recently logged task off the rollback stack and
    // reverts that room's cleaning status back to what it was before.
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

    // Returns the task currently on top of the rollback stack without
    // removing it, or null when the stack is empty.
    public CleaningTask peekLastTask() {
        return undoStack.isEmpty() ? null : undoStack.peek();
    }

    // Returns how many logged tasks remain available to undo.
    public int getUndoStackSize() {
        return undoStack.size();
    }

    // Returns the statuses a staff member may select when updating a room
    // (Dirty is an initial state only and is not selectable here).
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

    // Returns the current time using the controller's clock.
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // Doubles the tracked-room array when no free position remains.
    private void ensureCapacity() {
        if (recordCount == records.length) {
            HousekeepingRecord[] expanded
                    = new HousekeepingRecord[records.length * 2];
            System.arraycopy(records, 0, expanded, 0, records.length);
            records = expanded;
        }
    }

    // Read-only view of one tracked room, safe to hand to the boundary layer.
    public record RoomStatusView(String roomNumber, String roomType,
            String status, String assignedStaff, String turnaround) {
    }

    // Result of assigning a task or updating a room's cleaning status.
    public record TaskActionResult(boolean success, String message,
            String roomNumber, String staffName, String previousStatus,
            String newStatus, int taskId, int undoStackSize) {
    }

    // Result of rolling back the most recently logged task.
    public record RollbackResult(boolean success, String message, int taskId,
            String roomNumber, String revertedStatus, int remainingStackSize) {
    }

    // One selectable cleaning status option shown on the update-status menu.
    public record StatusOption(int value, String label) {
    }
}