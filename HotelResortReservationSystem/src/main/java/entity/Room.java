/*
 * Author: Bryan Won Chu Ming
 * Stores a resort room and its current allocation availability.
 */
package entity;

import java.util.Objects;

public final class Room {
    private String roomNumber;
    private RoomType roomType;
    private RoomStatus status;

    // Creates a room and checks that all required data is provided.
    public Room(String roomNumber, RoomType roomType, RoomStatus status) {
        setRoomNumber(roomNumber);
        setRoomType(roomType);
        setStatus(status);
    }

    // Returns the room number.
    public String getRoomNumber() {
        return roomNumber;
    }

    // Changes the room number after validating it.
    public void setRoomNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.isBlank()) {
            throw new IllegalArgumentException("Room number is required.");
        }
        this.roomNumber = roomNumber.trim();
    }

    // Returns the type of this room.
    public RoomType getRoomType() {
        return roomType;
    }

    // Changes the room type.
    public void setRoomType(RoomType roomType) {
        this.roomType = Objects.requireNonNull(roomType, "Room type is required.");
    }

    // Returns the current room status.
    public RoomStatus getStatus() {
        return status;
    }

    // Changes the current room status.
    public void setStatus(RoomStatus status) {
        this.status = Objects.requireNonNull(status, "Room status is required.");
    }

    // Checks whether the room can be assigned to a guest.
    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    // Changes an available room to occupied after allocation.
    public void allocate() {
        if (!isAvailable()) {
            throw new IllegalStateException("Room " + roomNumber + " is not available.");
        }
        status = RoomStatus.OCCUPIED;
    }

    // Changes the room status back to available.
    public void release() {
        status = RoomStatus.AVAILABLE;
    }

    // Compares rooms by their unique room number.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Room other)) {
            return false;
        }
        return Objects.equals(roomNumber, other.roomNumber);
    }

    // Produces a hash value that matches equals.
    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    // Returns a readable room description.
    @Override
    public String toString() {
        return "Room{" + "roomNumber=" + roomNumber
                + ", roomType=" + roomType
                + ", status=" + status + '}';
    }
}
