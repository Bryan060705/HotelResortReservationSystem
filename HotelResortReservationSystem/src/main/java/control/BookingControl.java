/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import entity.Guest;
import entity.Room;
import entity.RoomType;
import entity.HotelDataStore;
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

    public BookingControl(HotelDataStore hotelDataStore) {

        if (hotelDataStore == null) {
            throw new IllegalArgumentException(
                    "Hotel data store is required."
            );
        }

        this.hotelDataStore = hotelDataStore;
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

    private Room findAvailableRoom(String requestedRoomType) {

        RoomType roomType = convertRoomType(requestedRoomType);
        Room[] rooms = hotelDataStore.getRoomsSnapshot();

        for (Room room : rooms) {
            if (room.getRoomType() == roomType
                    && room.isAvailable()) {
                return room;
            }
        }

        return null;
    }
    
    public int getAvailableRoomCount(RoomType requestedType) {

    int count = 0;

    Room[] rooms = hotelDataStore.getRoomsSnapshot();

    for (Room room : rooms) {
        if (room.getRoomType() == requestedType
                && room.isAvailable()) {

            count++;
        }
    }

    return count;
}
}
