/*
 * Author: Bong Xin Yee
 * Tracks the housekeeping cleaning state of a resort room over time.
 */
package entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public final class HousekeepingRecord {
    private static final String UNASSIGNED_LABEL = "Unassigned";

    private final String roomNumber;
    private final RoomType roomType;
    private CleaningStatus status;
    private String assignedStaff;
    private LocalDateTime assignedAt;
    private LocalDateTime lastUpdatedAt;

    // Creates a tracked room starting in the given cleaning status.
    public HousekeepingRecord(String roomNumber, RoomType roomType,
            CleaningStatus status, LocalDateTime createdAt) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        this.roomNumber = roomNumber.trim();
        this.roomType = Objects.requireNonNull(roomType, "Room type is required.");
        this.status = Objects.requireNonNull(status, "Cleaning status is required.");
        this.lastUpdatedAt = Objects.requireNonNull(createdAt, "Created time is required.");
        this.assignedStaff = UNASSIGNED_LABEL;
    }

    // Returns the tracked room number.
    public String getRoomNumber() {
        return roomNumber;
    }

    // Returns the room type used for display purposes.
    public RoomType getRoomType() {
        return roomType;
    }

    // Returns the current cleaning status.
    public CleaningStatus getStatus() {
        return status;
    }

    // Returns the staff member assigned to this room, or "Unassigned".
    public String getAssignedStaff() {
        return assignedStaff;
    }

    // Checks whether a staff member has been assigned to this room.
    public boolean isAssigned() {
        return assignedAt != null;
    }

    // Assigns a staff member and starts the cleaning turnaround timer.
    public void assignStaff(String staffName, LocalDateTime now) {
        if (staffName == null || staffName.isBlank()) {
            throw new IllegalArgumentException("Staff name is required.");
        }
        this.assignedStaff = staffName.trim();
        this.assignedAt = Objects.requireNonNull(now, "Current time is required.");
        this.lastUpdatedAt = now;
    }

    // Changes the cleaning status and returns the status that was replaced.
    public CleaningStatus changeStatus(CleaningStatus newStatus, LocalDateTime now) {
        CleaningStatus previousStatus = status;
        this.status = Objects.requireNonNull(newStatus, "New status is required.");
        this.lastUpdatedAt = Objects.requireNonNull(now, "Current time is required.");
        return previousStatus;
    }

    // Reverts the cleaning status during an undo/rollback operation.
    public void revertStatus(CleaningStatus previousStatus, LocalDateTime now) {
        this.status = Objects.requireNonNull(previousStatus, "Previous status is required.");
        this.lastUpdatedAt = Objects.requireNonNull(now, "Current time is required.");
    }

    // Returns the minutes elapsed since assignment, or -1 when unassigned.
    public long getTurnaroundMinutes() {
        if (assignedAt == null) {
            return -1;
        }
        return Duration.between(assignedAt, lastUpdatedAt).toMinutes();
    }

    // Compares housekeeping records by their unique room number.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof HousekeepingRecord other)) {
            return false;
        }
        return Objects.equals(roomNumber, other.roomNumber);
    }

    // Produces a hash value that matches equals.
    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    // Returns a readable description of this housekeeping record.
    @Override
    public String toString() {
        return "HousekeepingRecord{" + "roomNumber=" + roomNumber
                + ", status=" + status
                + ", assignedStaff=" + assignedStaff + '}';
    }
}