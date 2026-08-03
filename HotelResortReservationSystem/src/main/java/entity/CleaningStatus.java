/*
 * Author: Bong Xin Yee
 * Represents the cleaning status of a resort room.
 */
package entity;

public enum CleaningStatus {
    DIRTY(0, "Dirty", "Dirty"),
    CLEANING(1, "Cleaning In Progress", "Cleaning In Progress"),
    INSPECTED(2, "Inspected", "Inspected"),
    READY(3, "Ready for Check-In", "Ready for Check-In");

    private final int menuChoice;
    private final String menuLabel;
    private final String displayName;

    CleaningStatus(int menuChoice, String menuLabel, String displayName) {
        this.menuChoice = menuChoice;
        this.menuLabel = menuLabel;
        this.displayName = displayName;
    }

    public int getMenuChoice() {
        return menuChoice;
    }

    public String getMenuLabel() {
        return menuLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Convert menu option (1-3) to status. DIRTY is initial state only.
    public static CleaningStatus fromMenuChoice(int menuChoice) {
        for (CleaningStatus status : values()) {
            if (status != DIRTY && status.menuChoice == menuChoice) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status choice.");
    }
}