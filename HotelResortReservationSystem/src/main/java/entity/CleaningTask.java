/*
 * Author: Bong Xin Yee
 * Represents one logged room-status change, used as a rollback stack entry.
 */
package entity;

import java.time.LocalDateTime;
import java.util.Objects;

public final class CleaningTask {
    private final int taskId;
    private final String roomNumber;
    private final String staffName;
    private final CleaningStatus previousStatus;
    private final CleaningStatus newStatus;
    private final LocalDateTime loggedAt;

    // Creates a task log entry describing one room-status change.
    public CleaningTask(int taskId, String roomNumber, String staffName,
            CleaningStatus previousStatus, CleaningStatus newStatus,
            LocalDateTime loggedAt) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        this.taskId = taskId;
        this.roomNumber = roomNumber.trim();
        this.staffName = staffName == null || staffName.isBlank()
                ? "Unassigned" : staffName.trim();
        this.previousStatus = Objects.requireNonNull(
                previousStatus, "Previous status is required.");
        this.newStatus = Objects.requireNonNull(
                newStatus, "New status is required.");
        this.loggedAt = Objects.requireNonNull(
                loggedAt, "Logged time is required.");
    }

    // Returns the unique task identifier shown on the rollback stack screen.
    public int getTaskId() {
        return taskId;
    }

    // Returns the room number this task changed.
    public String getRoomNumber() {
        return roomNumber;
    }

    // Returns the staff member on record when this task was logged.
    public String getStaffName() {
        return staffName;
    }

    // Returns the status the room held before this task ran.
    public CleaningStatus getPreviousStatus() {
        return previousStatus;
    }

    // Returns the status the room was changed to by this task.
    public CleaningStatus getNewStatus() {
        return newStatus;
    }

    // Returns when this task was logged onto the rollback stack.
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    // Produces the one-line summary used on the rollback stack screen,
    // e.g. "Room 302 changed to 'Ready for Check-In'".
    public String getSummary() {
        return "Room " + roomNumber + " changed to '"
                + newStatus.getDisplayName() + "'";
    }

    // Returns a readable description of this task log entry.
    @Override
    public String toString() {
        return "CleaningTask{" + "taskId=" + taskId
                + ", roomNumber=" + roomNumber
                + ", previousStatus=" + previousStatus
                + ", newStatus=" + newStatus + '}';
    }
}