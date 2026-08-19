/*
 * Author: CARRET CHONG KAR LOKE
 */
package control;

import adt.BinarySearchTree;
import entity.Guest;
import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import entity.HotelDataStore;


import java.time.LocalDateTime;

/**
 * Controls the business logic of the Front-Desk Service module.
 *
 * The module uses a Binary Search Tree to efficiently search
 * guest records by their 8-digit confirmation number.
 */
public class FrontDeskServiceControl {

    private final BinarySearchTree<Guest> guestTree;

    private final  HotelDataStore hotelDataStore;

    /**
     * Creates a Front-Desk Service controller.
     */
    public FrontDeskServiceControl(HotelDataStore hotelDataStore) {

    if (hotelDataStore == null) {
        throw new IllegalArgumentException(
                "Hotel data store is required.");
    }

    this.hotelDataStore = hotelDataStore;
    guestTree = new BinarySearchTree<>();
    loadSampleGuests();
}

    /**
     * Adds a guest into the Binary Search Tree.
     *
     * @param guest guest record
     */
    public void addGuest(Guest guest) {

        if (guest == null) {
            throw new IllegalArgumentException("Guest cannot be null.");
        }

        validateConfirmationNumber(guest.getGuestID());

        guestTree.insert(guest);
    }

    /**
     * Searches for a guest using the unique 8-digit
     * confirmation number.
     *
     * @param confirmationNumber guest confirmation number
     * @return guest information if found, otherwise null
     */
    public Guest searchGuest(String confirmationNumber) {

        validateConfirmationNumber(confirmationNumber);

        Guest searchKey = new Guest(
                confirmationNumber,
                "",
                "",
                LocalDateTime.now(),
                "",
                "",
                ""
        );

        return guestTree.search(searchKey);
    }

    /**
     * Deletes a guest using the confirmation number.
     *
     * @param confirmationNumber confirmation number
     * @return true if deleted
     */
    public boolean deleteGuest(String confirmationNumber) {

        validateConfirmationNumber(confirmationNumber);

        Guest guest = searchGuest(confirmationNumber);

        if (guest == null) {
            return false;
        }

        return guestTree.delete(guest);
    }

    /**
     * Returns all guests in confirmation-number order.
     *
     * @return guest array
     */
    public Guest[] getAllGuests() {

        Object[] objects = guestTree.inOrder();

        Guest[] guests = new Guest[objects.length];

        for (int i = 0; i < objects.length; i++) {
            guests[i] = (Guest) objects[i];
        }

        return guests;
    }

    /**
     * Searches rooms by room type.
     *
     * @param roomType requested room type
     * @return available rooms
     */
    public Room[] searchAvailableRooms(RoomType roomType) {

    if (roomType == null) {
        throw new IllegalArgumentException(
                "Room type is required.");
    }

    Room[] allRooms = hotelDataStore.getRoomsSnapshot();

    int count = 0;

    for (Room room : allRooms) {
        if (room.getRoomType() == roomType
                && room.getStatus() == RoomStatus.AVAILABLE) {
            count++;
        }
    }

    Room[] availableRooms = new Room[count];

    int index = 0;

    for (Room room : allRooms) {
        if (room.getRoomType() == roomType
                && room.getStatus() == RoomStatus.AVAILABLE) {

            availableRooms[index++] = room;
        }
    }

    return availableRooms;
}

    /**
     * Returns all rooms.
     *
     * @return room array
     */
    public Room[] getAllRooms() {
    return hotelDataStore.getRoomsSnapshot();
}

    /**
     * Generates billing information for a guest.
     *
     * The room rate is determined by the guest's room type.
     *
     * @param confirmationNumber guest confirmation number
     * @return billing information
     */
    public String getBillingDetails(String confirmationNumber) {

        Guest guest = searchGuest(confirmationNumber);

        if (guest == null) {
            return "Guest not found.";
        }

        double roomRate = getRoomRate(guest.getRoomType());

        String paymentStatus = getPaymentStatus(guest);

        return String.format(
                "========== BILLING DETAILS ==========%n"
                + "Confirmation No. : %s%n"
                + "Guest Name       : %s%n"
                + "Room Type        : %s%n"
                + "Room Number      : %s%n"
                + "Estimated Rate   : RM %.2f per night%n"
                + "Booking Status   : %s%n"
                + "Payment Status   : %s%n"
                + "======================================",
                guest.getGuestID(),
                guest.getGuestName(),
                guest.getRoomType(),
                guest.getRoomNumber(),
                roomRate,
                guest.getStatus(),
                paymentStatus
        );
    }

    /**
     * Returns the number of guests currently stored.
     *
     * @return guest count
     */
    public int getGuestCount() {
        return guestTree.size();
    }

    /**
     * Validates that the confirmation number contains exactly
     * eight numeric digits.
     *
     * @param confirmationNumber confirmation number
     */
    private void validateConfirmationNumber(String confirmationNumber) {

        if (confirmationNumber == null
                || !confirmationNumber.matches("\\d{8}")) {

            throw new IllegalArgumentException(
                    "Confirmation number must contain exactly 8 digits.");
        }
    }

    /**
     * Returns a sample room rate.
     *
     * @param roomType room type
     * @return nightly rate
     */
    private double getRoomRate(String roomType) {

        if (roomType == null) {
            return 0.00;
        }

        if (roomType.equalsIgnoreCase("Standard Suite")) {
            return 250.00;
        }

        if (roomType.equalsIgnoreCase("Deluxe Suite")) {
            return 380.00;
        }

        if (roomType.equalsIgnoreCase("Executive Villa")) {
            return 550.00;
        }

        if (roomType.equalsIgnoreCase("Ocean Villa")) {
            return 750.00;
        }

        return 0.00;
    }

    /**
     * Determines a simple payment status from the guest booking status.
     *
     * @param guest guest record
     * @return payment status
     */
    private String getPaymentStatus(Guest guest) {

        if (guest.getStatus() == null) {
            return "Pending";
        }

        if (guest.getStatus().equalsIgnoreCase("Confirmed")
                || guest.getStatus().equalsIgnoreCase("Checked-In")) {

            return "Paid";
        }

        return "Pending";
    }
    /**
     * Loads sample guest records for testing and demonstration.
     */
    private void loadSampleGuests() {
        addGuest(new Guest(
                "12345678",
                "Alice Tan",
                "0123456789",
                LocalDateTime.now(),
                "Standard Suite",
                "Confirmed",
                "101"
        ));

        addGuest(new Guest(
                "23456789",
                "Daniel Lim",
                "0134567890",
                LocalDateTime.now(),
                "Deluxe Suite",
                "Checked-In",
                "201"
        ));

        addGuest(new Guest(
                "34567890",
                "Sarah Wong",
                "0145678901",
                LocalDateTime.now(),
                "Ocean Villa",
                "Confirmed",
                "301"
        ));
    }
    }
