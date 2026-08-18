/*
* Author: Tang Hong Yi
 * Handling booking process, manage guest booking, mange guest queue
 */
package control;

import entity.Guest;
import entity.Room;
import entity.RoomType;
import entity.HotelDataStore;
import entity.HousekeepingRecord;
import entity.CleaningStatus;
import adt.LinkedQueue;
import java.time.LocalDateTime;
import java.util.Iterator;

/**
 *
 * @author user
 */
public class BookingControl {

    private LinkedQueue<Guest> bookingQueue;
    private LinkedQueue<Guest> confirmedBookings;
    private final HotelDataStore hotelDataStore;
    private final HousekeepingController housekeepingController;

    public BookingControl(HotelDataStore hotelDataStore, HousekeepingController housekeepingController) {

        if (hotelDataStore == null) {
            throw new IllegalArgumentException(
                    "Hotel data store is required."
            );
        }

        if (housekeepingController == null) {
            throw new IllegalArgumentException(
                    "Housekeeping controller is required."
            );
        }

        this.hotelDataStore = hotelDataStore;
        this.housekeepingController = housekeepingController;
        bookingQueue = new LinkedQueue<>();
        confirmedBookings = new LinkedQueue<>();
    }

    public Guest registerWalkIn(String guestID,
            String guestName,
            String phoneNumber,
            String roomType) {

        Guest guest = new Guest(
                guestID,
                guestName,
                phoneNumber,
                LocalDateTime.now(),
                roomType,
                "Waiting",
                null
        );

        return bookRoom(guest);
    }

    public Guest bookRoom(Guest guest) {

        Room availableRoom = findAvailableRoom(
                guest.getRoomType()
        );

        if (availableRoom != null) {

            availableRoom.allocate();

            guest.setRoomNumber(
                    availableRoom.getRoomNumber()
            );
            guest.setStatus("Assigned");

            confirmedBookings.enqueue(guest);

        } else {

            guest.setStatus("Waiting");
            guest.setRoomNumber(null);

            bookingQueue.enqueue(guest);
        }

        return guest;
    }

    public Guest assignRoom() {

        if (bookingQueue.isEmpty()) {
            return null;
        }

        Guest guest = bookingQueue.getFront();

        Room availableRoom = findAvailableRoom(
                guest.getRoomType()
        );

        if (availableRoom == null) {
            return null;
        }

        guest = bookingQueue.dequeue();

        availableRoom.allocate();

        guest.setRoomNumber(
                availableRoom.getRoomNumber()
        );
        guest.setStatus("Assigned");

        confirmedBookings.enqueue(guest);

        return guest;
    }

    public LinkedQueue<Guest> getBookingQueue() {
        return bookingQueue;
    }

    public Guest cancelBooking(String guestID) {

        if (bookingQueue.isEmpty()) {
            return null;
        }

        LinkedQueue<Guest> temporaryQueue = new LinkedQueue<>();
        Guest cancelledGuest = null;

        while (!bookingQueue.isEmpty()) {

            Guest guest = bookingQueue.dequeue();

            if (guest.getGuestID().equalsIgnoreCase(guestID)
                    && cancelledGuest == null) {

                guest.setStatus("Cancelled");
                cancelledGuest = guest;

            } else {
                temporaryQueue.enqueue(guest);
            }
        }

        while (!temporaryQueue.isEmpty()) {
            bookingQueue.enqueue(temporaryQueue.dequeue());
        }

        return cancelledGuest;
    }

    public boolean hasConfirmedBookings() {
        return !confirmedBookings.isEmpty();
    }

    public Iterator<Guest> getConfirmedBookingIterator() {
        return confirmedBookings.getIterator();
    }

    public int getConfirmedBookingCount() {
        return confirmedBookings.size();
    }

    public int getWaitingGuestCount() {
        return bookingQueue.size();
    }

    public Guest getFirstWaitingGuest() {
        return bookingQueue.getFront();
    }

    public int getAssignedGuestCount() {
        return confirmedBookings.size();
    }

    private Guest searchQueue(LinkedQueue<Guest> queue, String guestID) {

        Iterator<Guest> iterator = queue.getIterator();

        while (iterator.hasNext()) {

            Guest guest = iterator.next();

            if (guest.getGuestID().equalsIgnoreCase(guestID)) {
                return guest;
            }
        }

        return null;
    }

    public Guest searchWaitingGuestByID(String guestID) {
        return searchQueue(bookingQueue, guestID);
    }

    public Guest searchConfirmedGuestByID(String guestID) {
        return searchQueue(confirmedBookings, guestID);
    }

    public Guest searchGuestByID(String guestID) {

        Guest guest = searchWaitingGuestByID(guestID);

        if (guest != null) {
            return guest;
        }

        return searchConfirmedGuestByID(guestID);
    }

    public Guest[] getConfirmedGuestsArray() {

        Guest[] guest = new Guest[confirmedBookings.size()];
        Iterator<Guest> iterator = confirmedBookings.getIterator();

        int index = 0;

        while (iterator.hasNext()) {
            guest[index] = iterator.next();
            index++;
        }

        return guest;
    }

    public Guest[] getConfirmedGuestsSortedByBookingDate() {

        Guest[] guests = getConfirmedGuestsArray();

        for (int i = 1; i < guests.length; i++) {

            Guest currentGuest = guests[i];
            int j = i - 1;

            while (j >= 0
                    && guests[j].getBookingDate()
                            .isAfter(currentGuest.getBookingDate())) {

                guests[j + 1] = guests[j];
                j--;
            }

            guests[j + 1] = currentGuest;
        }

        return guests;
    }

    private RoomType convertRoomType(String roomType) {
        return RoomType.fromDisplayName(roomType);
    }

    // Room must be AVAILABLE in HotelDataStore AND marked READY in Housekeeping
    private Room findAvailableRoom(String requestedRoomType) {

        RoomType roomType = convertRoomType(requestedRoomType);
        Room[] rooms = hotelDataStore.getRoomsSnapshot();

        for (Room room : rooms) {
            if (room.getRoomType() == roomType && room.isAvailable()) {
                HousekeepingRecord hkRecord = housekeepingController.findRecord(room.getRoomNumber());
                if (hkRecord != null && hkRecord.getStatus() == CleaningStatus.READY) {
                    return room;
                }
            }
        }

        return null;
    }
    
    // Counts only rooms that are both AVAILABLE and READY for check-in
    public int getAvailableRoomCount(RoomType requestedType) {

        int count = 0;
        Room[] rooms = hotelDataStore.getRoomsSnapshot();

        for (Room room : rooms) {
            if (room.getRoomType() == requestedType && room.isAvailable()) {
                HousekeepingRecord hkRecord = housekeepingController.findRecord(room.getRoomNumber());
                if (hkRecord != null && hkRecord.getStatus() == CleaningStatus.READY) {
                    count++;
                }
            }
        }

        return count;
    }
}
