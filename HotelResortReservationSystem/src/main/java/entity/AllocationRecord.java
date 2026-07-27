/*
 * Author: Bryan Won Chu Ming
 * Stores the auditable result of assigning a room to a VIP guest.
 */
package entity;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AllocationRecord {
    private String allocationId;
    private VipGuest guest;
    private Room room;
    private LocalDateTime allocatedAt;
    private long waitingMinutes;
    private AllocationStatus status;
    private LocalDateTime completedAt;

    // Creates a record whenever a room is assigned to a VIP guest.
    public AllocationRecord(String allocationId, VipGuest guest, Room room,
            LocalDateTime allocatedAt, long waitingMinutes,
            AllocationStatus status) {
        setAllocationId(allocationId);
        setGuest(guest);
        setRoom(room);
        setAllocatedAt(allocatedAt);
        setWaitingMinutes(waitingMinutes);
        setStatus(status);
    }

    // Returns the allocation ID.
    public String getAllocationId() {
        return allocationId;
    }

    // Changes the unique allocation ID.
    public void setAllocationId(String allocationId) {
        if (allocationId == null || allocationId.isBlank()) {
            throw new IllegalArgumentException("Allocation ID is required.");
        }
        this.allocationId = allocationId.trim();
    }

    // Returns the guest in this allocation.
    public VipGuest getGuest() {
        return guest;
    }

    // Changes the guest associated with this allocation.
    public void setGuest(VipGuest guest) {
        this.guest = Objects.requireNonNull(guest, "Guest is required.");
    }

    // Returns the room in this allocation.
    public Room getRoom() {
        return room;
    }

    // Changes the room associated with this allocation.
    public void setRoom(Room room) {
        this.room = Objects.requireNonNull(room, "Room is required.");
    }

    // Returns the date and time of allocation.
    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    // Changes the allocation date and time.
    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = Objects.requireNonNull(
                allocatedAt, "Allocation date and time are required.");
    }

    // Returns how many minutes the guest waited.
    public long getWaitingMinutes() {
        return waitingMinutes;
    }

    // Changes the recorded waiting time.
    public void setWaitingMinutes(long waitingMinutes) {
        if (waitingMinutes < 0) {
            throw new IllegalArgumentException("Waiting minutes cannot be negative.");
        }
        this.waitingMinutes = waitingMinutes;
    }

    // Returns the current allocation status.
    public AllocationStatus getStatus() {
        return status;
    }

    // Changes the allocation status.
    public void setStatus(AllocationStatus status) {
        this.status = Objects.requireNonNull(status,
                "Allocation status is required.");
    }

    // Returns the date and time when the allocation ended.
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    // Changes the completion date and time.
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // Marks an active allocation as completed.
    public void complete(LocalDateTime completedAt) {
        this.completedAt = Objects.requireNonNull(
                completedAt, "Completion date and time are required.");
        status = AllocationStatus.COMPLETED;
    }

    // Compares allocation records by their unique allocation ID.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof AllocationRecord other)) {
            return false;
        }
        return Objects.equals(allocationId, other.allocationId);
    }

    // Produces a hash value that matches equals.
    @Override
    public int hashCode() {
        return Objects.hash(allocationId);
    }

    // Returns a readable allocation description.
    @Override
    public String toString() {
        return "AllocationRecord{" + "allocationId=" + allocationId
                + ", guest=" + guest
                + ", room=" + room
                + ", allocatedAt=" + allocatedAt
                + ", waitingMinutes=" + waitingMinutes
                + ", status=" + status
                + ", completedAt=" + completedAt + '}';
    }
}
