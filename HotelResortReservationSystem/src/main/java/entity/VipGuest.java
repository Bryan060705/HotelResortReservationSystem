/*
 * Author: Bryan Won Chu Ming
 * Stores a high-tier guest profile waiting for priority allocation.
 */
package entity;

import java.time.LocalDateTime;
import java.util.Objects;

public final class VipGuest {
    private String guestId;
    private String name;
    private String contactNumber;
    private LoyaltyTier loyaltyTier;
    private RoomType preferredRoomType;
    private LocalDateTime registeredAt;

    // Creates a VIP guest profile and validates the required details.
    public VipGuest(String guestId, String name, String contactNumber,
            LoyaltyTier loyaltyTier, RoomType preferredRoomType,
            LocalDateTime registeredAt) {
        setGuestId(guestId);
        setName(name);
        setContactNumber(contactNumber);
        setLoyaltyTier(loyaltyTier);
        setPreferredRoomType(preferredRoomType);
        setRegisteredAt(registeredAt);
    }

    // Checks that a text value is not empty.
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    // Returns the guest ID.
    public String getGuestId() {
        return guestId;
    }

    // Changes the unique guest ID.
    public void setGuestId(String guestId) {
        this.guestId = requireText(guestId, "Guest ID");
    }

    // Returns the guest name.
    public String getName() {
        return name;
    }

    // Changes the guest name.
    public void setName(String name) {
        this.name = requireText(name, "Guest name");
    }

    // Returns the guest contact number.
    public String getContactNumber() {
        return contactNumber;
    }

    // Changes the contact number.
    public void setContactNumber(String contactNumber) {
        this.contactNumber = requireText(contactNumber, "Contact number");
    }

    // Returns the guest loyalty tier.
    public LoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }

    // Changes the guest loyalty tier.
    public void setLoyaltyTier(LoyaltyTier loyaltyTier) {
        this.loyaltyTier = Objects.requireNonNull(
                loyaltyTier, "Loyalty tier is required.");
    }

    // Returns the room type requested by the guest.
    public RoomType getPreferredRoomType() {
        return preferredRoomType;
    }

    // Changes the requested room type.
    public void setPreferredRoomType(RoomType preferredRoomType) {
        this.preferredRoomType = Objects.requireNonNull(
                preferredRoomType, "Preferred room type is required.");
    }

    // Returns the time when the guest joined the queue.
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    // Changes the time when the guest joined the queue.
    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = Objects.requireNonNull(
                registeredAt, "Registration date and time are required.");
    }

    // Compares guests by their unique guest ID.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof VipGuest other)) {
            return false;
        }
        return Objects.equals(guestId, other.guestId);
    }

    // Produces a hash value that matches equals.
    @Override
    public int hashCode() {
        return Objects.hash(guestId);
    }

    // Returns a readable guest description.
    @Override
    public String toString() {
        return "VipGuest{" + "guestId=" + guestId
                + ", name=" + name
                + ", loyaltyTier=" + loyaltyTier
                + ", preferredRoomType=" + preferredRoomType
                + ", registeredAt=" + registeredAt + '}';
    }
}
