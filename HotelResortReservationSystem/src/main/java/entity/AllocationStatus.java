/*
 * Author: Bryan Won Chu Ming
 * Represents the lifecycle state of a VIP room allocation.
 */
package entity;

public enum AllocationStatus {
    ACTIVE("Active"),
    COMPLETED("Completed");

    private final String displayName;

    // Creates an allocation status with a readable name.
    AllocationStatus(String displayName) {
        this.displayName = displayName;
    }

    // Returns the readable allocation status.
    public String getDisplayName() {
        return displayName;
    }

    // Finds an allocation status from its displayed name.
    public static AllocationStatus fromDisplayName(String displayName) {
        for (AllocationStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)
                    || status.name().equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported allocation status: " + displayName);
    }
}
