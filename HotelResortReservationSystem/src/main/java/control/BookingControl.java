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
import java.time.LocalDate;

public class BookingControl {

    private LinkedQueue<Guest> bookingQueue;
    private LinkedQueue<Guest> confirmedBookings;
    private final HotelDataStore hotelDataStore;
    private final HousekeepingController housekeepingController;
    private final VipRoomAllocationController vipController;
    private int nextGuestID = 1;

    public BookingControl(HotelDataStore hotelDataStore, HousekeepingController housekeepingController,VipRoomAllocationController vipController) {

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
        this.vipController = vipController;
        
        bookingQueue = new LinkedQueue<>();
        confirmedBookings = new LinkedQueue<>();
    }

    public Guest registerWalkIn(String guestName,
            String phoneNumber,
            String roomType) {

        String guestID = generateGuestID();

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

    private Guest assignStandardRoom() {

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
    
    
    
    //assign ID to Guest
    private String generateGuestID() {

        String guestID;

        do {
            guestID = String.format("G%04d", nextGuestID);
            nextGuestID++;
        } while (searchGuestByID(guestID) != null);

        return guestID;
    }
    
    //record Guest info when assign room
    public record AssignRoomResult(
        boolean success,
        String guestType,
        String guestID,
        String guestName,
        String roomNumber,
        String roomType,
        String message
) {
}
    
    
    public AssignRoomResult assignRoom() {

    // VIP gets priority first
    VipRoomAllocationController.AllocationResult vipResult
            = vipController.allocateHighestPriorityVip();

    if (vipResult.allocationCreated()) {

        return new AssignRoomResult(
                true,
                "VIP",
                vipResult.guestId(),
                vipResult.guestName(),
                vipResult.roomNumber(),
                vipResult.roomType(),
                vipResult.message()
        );
    }

    // If no eligible VIP can be assigned,try the normal FIFO waiting queue
    
    Guest guest = assignStandardRoom();

    if (guest != null) {

        return new AssignRoomResult(
                true,
                "Standard",
                guest.getGuestID(),
                guest.getGuestName(),
                guest.getRoomNumber(),
                guest.getRoomType(),
                "Standard waiting guest assigned successfully."
        );
    }

    return new AssignRoomResult(
            false,
            null,
            null,
            null,
            null,
            null,
            "No eligible VIP or standard guest can currently be assigned."
    );
}
    
    private boolean matchesFilter(
        Guest guest,
        String roomTypeFilter,
        LocalDate dateFilter) {

    boolean roomMatches =
            roomTypeFilter == null 
            || roomTypeFilter.equalsIgnoreCase("All")
            || guest.getRoomType().equalsIgnoreCase(roomTypeFilter);

    boolean dateMatches =
            dateFilter == null
            || guest.getBookingDate()
                    .toLocalDate()
                    .equals(dateFilter);

    return roomMatches && dateMatches;
}
    
   public Guest[] getFilteredWaitingGuests(
        String roomTypeFilter,
        LocalDate dateFilter) {

    // First count matching guests
    int count = 0;

    Iterator<Guest> iterator = bookingQueue.getIterator();

    while (iterator.hasNext()) {

        Guest guest = iterator.next();

        if (matchesFilter(
                guest,
                roomTypeFilter,
                dateFilter)) {

            count++;
        }
    }

    // Create array with correct size
    Guest[] filteredGuests = new Guest[count];

    iterator = bookingQueue.getIterator();

    int index = 0;

    while (iterator.hasNext()) {

        Guest guest = iterator.next();

        if (matchesFilter(
                guest,
                roomTypeFilter,
                dateFilter)) {

            filteredGuests[index] = guest;
            index++;
        }
    }

    return filteredGuests;
}
    
   public Guest[] getFilteredAssignedGuests(
        String roomTypeFilter,
        LocalDate dateFilter) {

    Guest[] sortedGuests =
            getConfirmedGuestsSortedByBookingDate();

    int count = 0;

    // Count matches
    for (Guest guest : sortedGuests) {

        if (matchesFilter(
                guest,
                roomTypeFilter,
                dateFilter)) {

            count++;
        }
    }

    Guest[] filteredGuests = new Guest[count];

    int index = 0;

    for (Guest guest : sortedGuests) {

        if (matchesFilter(
                guest,
                roomTypeFilter,
                dateFilter)) {

            filteredGuests[index] = guest;
            index++;
        }
    }

    return filteredGuests;
}
   
    
}
