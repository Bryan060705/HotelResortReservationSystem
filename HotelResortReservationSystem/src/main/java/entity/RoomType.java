/*
 * Author: Bryan Won Chu Ming
 * Represents the room categories that VIP guests may request.
 */
package entity;

public enum RoomType {
    STANDARD_SUITE(1, "Standard Suite"),
    DELUXE_SUITE(2, "Deluxe Suite"),
    EXECUTIVE_VILLA(3, "Executive Villa"),
    OCEAN_VILLA(4, "Ocean Villa");

    private final int menuChoice;
    private final String displayName;

    // Creates one room type with its menu number and display name.
    RoomType(int menuChoice, String displayName) {
        this.menuChoice = menuChoice;
        this.displayName = displayName;
    }

    // Returns the number shown in the console menu.
    public int getMenuChoice() {
        return menuChoice;
    }

    // Returns the readable room type name.
    public String getDisplayName() {
        return displayName;
    }

    // Finds a room type from the number entered by the user.
    public static RoomType fromMenuChoice(int menuChoice) {
        for (RoomType roomType : values()) {
            if (roomType.menuChoice == menuChoice) {
                return roomType;
            }
        }
        throw new IllegalArgumentException("Invalid room type selection.");
    }

    // Finds a room type from its displayed name.
    public static RoomType fromDisplayName(String displayName) {
        for (RoomType roomType : values()) {
            if (roomType.displayName.equalsIgnoreCase(displayName)
                    || roomType.name().equalsIgnoreCase(displayName)) {
                return roomType;
            }
        }
        throw new IllegalArgumentException("Unsupported room type: " + displayName);
    }
}
