/*
 * Author: Bong Xin Yee
 * Represents the housekeeping cleaning stage of a resort room.
 */
package entity;

public enum CleaningStatus {
    DIRTY(0, "Dirty", "Dirty"),
    CLEANING(1, "Cleaning", "Cleaning"),
    INSPECTED(2, "Inspected", "Inspected"),
    READY(3, "Ready", "Ready for Check-In");

    private final int menuChoice;
    private final String menuLabel;
    private final String displayName;

    // Creates one cleaning status with its menu number and display names.
    CleaningStatus(int menuChoice, String menuLabel, String displayName) {
        this.menuChoice = menuChoice;
        this.menuLabel = menuLabel;
        this.displayName = displayName;
    }

    // Returns the number shown in the "Update Room Status" console menu.
    public int getMenuChoice() {
        return menuChoice;
    }

    // Returns the short label shown next to the menu number.
    public String getMenuLabel() {
        return menuLabel;
    }

    // Returns the full readable status shown in system messages and reports.
    public String getDisplayName() {
        return displayName;
    }

    // Finds a selectable status (Cleaning, Inspected, or Ready) from the
    // number entered by the user. DIRTY is an initial state only and cannot
    // be selected directly from the update-status menu.
    public static CleaningStatus fromMenuChoice(int menuChoice) {
        for (CleaningStatus status : values()) {
            if (status != DIRTY && status.menuChoice == menuChoice) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status selection.");
    }
}