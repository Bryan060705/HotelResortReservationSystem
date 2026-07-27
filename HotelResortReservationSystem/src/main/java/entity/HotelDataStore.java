/*
 * Author: Bryan Won Chu Ming
 * Stores shared hotel entities so integrated modules use the same data.
 */
package entity;

import java.util.Objects;

public final class HotelDataStore {
    private static final int DEFAULT_CAPACITY = 10;

    private Room[] rooms;
    private VipGuest[] vipGuests;
    private AllocationRecord[] allocationRecords;
    private int roomCount;
    private int vipGuestCount;
    private int allocationRecordCount;

    // Creates an empty shared data store using ordinary arrays.
    public HotelDataStore() {
        rooms = new Room[DEFAULT_CAPACITY];
        vipGuests = new VipGuest[DEFAULT_CAPACITY];
        allocationRecords = new AllocationRecord[DEFAULT_CAPACITY];
    }

    // Adds one unique room to the shared room array.
    public void addRoom(Room room) {
        Objects.requireNonNull(room, "Room is required.");
        if (findRoom(room.getRoomNumber()) != null) {
            throw new IllegalArgumentException(
                    "Duplicate room number: " + room.getRoomNumber());
        }
        ensureRoomCapacity();
        rooms[roomCount++] = room;
    }

    // Uses linear searching to find a shared room by room number.
    public Room findRoom(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }
        for (int index = 0; index < roomCount; index++) {
            if (rooms[index].getRoomNumber()
                    .equalsIgnoreCase(roomNumber.trim())) {
                return rooms[index];
            }
        }
        return null;
    }

    // Returns the number of shared rooms.
    public int getRoomCount() {
        return roomCount;
    }

    // Returns one room at the selected position.
    public Room getRoom(int position) {
        checkPosition(position, roomCount);
        return rooms[position];
    }

    // Returns a shallow copy containing the same shared room entities.
    public Room[] getRoomsSnapshot() {
        Room[] copy = new Room[roomCount];
        System.arraycopy(rooms, 0, copy, 0, roomCount);
        return copy;
    }

    // Adds one unique VIP guest profile to the shared guest array.
    public void addVipGuest(VipGuest guest) {
        Objects.requireNonNull(guest, "VIP guest is required.");
        if (findVipGuest(guest.getGuestId()) != null) {
            throw new IllegalArgumentException(
                    "Duplicate VIP guest ID: " + guest.getGuestId());
        }
        ensureVipGuestCapacity();
        vipGuests[vipGuestCount++] = guest;
    }

    // Uses linear searching to find a VIP guest by guest ID.
    public VipGuest findVipGuest(String guestId) {
        if (guestId == null) {
            return null;
        }
        for (int index = 0; index < vipGuestCount; index++) {
            if (vipGuests[index].getGuestId().equalsIgnoreCase(guestId.trim())) {
                return vipGuests[index];
            }
        }
        return null;
    }

    // Returns the number of shared VIP guest profiles.
    public int getVipGuestCount() {
        return vipGuestCount;
    }

    // Returns a copy of the shared VIP guest references.
    public VipGuest[] getVipGuestsSnapshot() {
        VipGuest[] copy = new VipGuest[vipGuestCount];
        System.arraycopy(vipGuests, 0, copy, 0, vipGuestCount);
        return copy;
    }

    // Adds one unique allocation record to shared history.
    public void addAllocationRecord(AllocationRecord record) {
        Objects.requireNonNull(record, "Allocation record is required.");
        if (findAllocationRecord(record.getAllocationId()) != null) {
            throw new IllegalArgumentException(
                    "Duplicate allocation ID: " + record.getAllocationId());
        }
        ensureAllocationCapacity();
        allocationRecords[allocationRecordCount++] = record;
    }

    // Uses linear searching to find an allocation by allocation ID.
    public AllocationRecord findAllocationRecord(String allocationId) {
        if (allocationId == null) {
            return null;
        }
        for (int index = 0; index < allocationRecordCount; index++) {
            if (allocationRecords[index].getAllocationId()
                    .equalsIgnoreCase(allocationId.trim())) {
                return allocationRecords[index];
            }
        }
        return null;
    }

    // Returns the number of shared allocation records.
    public int getAllocationRecordCount() {
        return allocationRecordCount;
    }

    // Returns one allocation record at the selected position.
    public AllocationRecord getAllocationRecord(int position) {
        checkPosition(position, allocationRecordCount);
        return allocationRecords[position];
    }

    // Returns a copy of the shared allocation record references.
    public AllocationRecord[] getAllocationRecordsSnapshot() {
        AllocationRecord[] copy
                = new AllocationRecord[allocationRecordCount];
        System.arraycopy(allocationRecords, 0, copy, 0,
                allocationRecordCount);
        return copy;
    }

    // Checks whether the shared store contains no hotel data.
    public boolean isEmpty() {
        return roomCount == 0 && vipGuestCount == 0
                && allocationRecordCount == 0;
    }

    // Doubles the room array when no free position remains.
    private void ensureRoomCapacity() {
        if (roomCount == rooms.length) {
            Room[] expanded = new Room[rooms.length * 2];
            System.arraycopy(rooms, 0, expanded, 0, rooms.length);
            rooms = expanded;
        }
    }

    // Doubles the VIP guest array when no free position remains.
    private void ensureVipGuestCapacity() {
        if (vipGuestCount == vipGuests.length) {
            VipGuest[] expanded = new VipGuest[vipGuests.length * 2];
            System.arraycopy(vipGuests, 0, expanded, 0, vipGuests.length);
            vipGuests = expanded;
        }
    }

    // Doubles the allocation array when no free position remains.
    private void ensureAllocationCapacity() {
        if (allocationRecordCount == allocationRecords.length) {
            AllocationRecord[] expanded
                    = new AllocationRecord[allocationRecords.length * 2];
            System.arraycopy(allocationRecords, 0, expanded, 0,
                    allocationRecords.length);
            allocationRecords = expanded;
        }
    }

    // Validates an array position before an entity is returned.
    private void checkPosition(int position, int count) {
        if (position < 0 || position >= count) {
            throw new IndexOutOfBoundsException(
                    "Invalid shared data position: " + position);
        }
    }

    // Returns a short description without printing every stored entity.
    @Override
    public String toString() {
        return "HotelDataStore{" + "rooms=" + roomCount
                + ", vipGuests=" + vipGuestCount
                + ", allocationRecords=" + allocationRecordCount + '}';
    }
}
