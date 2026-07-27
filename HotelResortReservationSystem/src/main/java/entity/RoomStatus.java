/*
 * Author: Bryan Won Chu Ming
 * Represents whether a room can currently be assigned.
 */
package entity;

public enum RoomStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied");

    private final String displayName;

    // Creates a room status with a readable name.
    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    // Returns the readable room status.
    public String getDisplayName() {
        return displayName;
    }
}
