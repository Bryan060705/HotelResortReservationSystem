/*
 * Author: Bryan Won Chu Ming
 * Represents the supported high-tier loyalty memberships.
 */
package entity;

public enum LoyaltyTier {
    ELITE(1, "Elite", 80),
    PLATINUM(2, "Platinum", 90),
    DIAMOND(3, "Diamond", 100);

    private final int menuChoice;
    private final String displayName;
    private final int priorityScore;

    // Creates one loyalty tier with its menu number and priority score.
    LoyaltyTier(int menuChoice, String displayName, int priorityScore) {
        this.menuChoice = menuChoice;
        this.displayName = displayName;
        this.priorityScore = priorityScore;
    }

    // Returns the number shown in the console menu.
    public int getMenuChoice() {
        return menuChoice;
    }

    // Returns the readable tier name.
    public String getDisplayName() {
        return displayName;
    }

    // Returns the score used to compare guest priority.
    public int getPriorityScore() {
        return priorityScore;
    }

    // Finds a loyalty tier from the number entered by the user.
    public static LoyaltyTier fromMenuChoice(int menuChoice) {
        for (LoyaltyTier tier : values()) {
            if (tier.menuChoice == menuChoice) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Invalid loyalty tier selection.");
    }

    // Finds a loyalty tier from its displayed name.
    public static LoyaltyTier fromDisplayName(String displayName) {
        for (LoyaltyTier tier : values()) {
            if (tier.displayName.equalsIgnoreCase(displayName)
                    || tier.name().equalsIgnoreCase(displayName)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Unsupported loyalty tier: " + displayName);
    }
}
