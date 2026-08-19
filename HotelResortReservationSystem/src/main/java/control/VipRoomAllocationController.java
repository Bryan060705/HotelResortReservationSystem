/*
 * Author: Bryan Won Chu Ming
 * Orchestrates VIP registration, max-heap queueing, and room allocation.
 */
package control;

import adt.HeapPriorityQueue;
import adt.PriorityQueueInterface;
import entity.AllocationRecord;
import entity.AllocationStatus;
import entity.CleaningStatus;
import entity.HotelDataStore;
import entity.HousekeepingRecord;
import entity.LoyaltyTier;
import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import entity.VipGuest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import utility.SearchAndSortUtility;

public final class VipRoomAllocationController {
    private static final Comparator<VipGuest> VIP_PRIORITY_COMPARATOR
            = VipRoomAllocationController::compareVipPriority;

    private final PriorityQueueInterface<VipGuest> waitingQueue;
    private final Clock clock;
    private final HotelDataStore dataStore;
    private final HousekeepingController housekeepingController;
    private int nextGuestNumber;
    private int nextAllocationNumber;

    // Creates the controller with standalone default instances.
    public VipRoomAllocationController() {
        this(new HotelDataStore(), new HousekeepingController(new HotelDataStore()), Clock.systemDefaultZone(), true);
    }

    // Receives shared hotel data and housekeeping controller used by all integrated modules.
    public VipRoomAllocationController(HotelDataStore dataStore, HousekeepingController housekeepingController) {
        this(dataStore, housekeepingController, Clock.systemDefaultZone(), true);
    }

    // Creates a controller with a selected clock; this is used by the tests.
    VipRoomAllocationController(Clock clock, boolean loadSampleData) {
        this(new HotelDataStore(), new HousekeepingController(new HotelDataStore()), clock, loadSampleData);
    }

    // Creates a controller with shared data, housekeeping controller, and a selected clock for testing.
    VipRoomAllocationController(HotelDataStore dataStore, HousekeepingController housekeepingController,
            Clock clock, boolean loadSampleData) {
        this.dataStore = Objects.requireNonNull(
                dataStore, "Shared hotel data is required.");
        this.housekeepingController = Objects.requireNonNull(
                housekeepingController, "Housekeeping controller is required.");
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        waitingQueue = new HeapPriorityQueue<>(VIP_PRIORITY_COMPARATOR);
        nextGuestNumber = 1001;
        nextAllocationNumber = 1001;
        moveIdentifiersPastExistingData();
        if (loadSampleData && dataStore.isEmpty()) {
            loadSampleData();
        }
    }

    // Returns the loyalty tier choices for the boundary menu.
    public SelectionOption[] getLoyaltyTierOptions() {
        LoyaltyTier[] tiers = LoyaltyTier.values();
        SelectionOption[] options = new SelectionOption[tiers.length];
        int index = 0;
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            options[index++] = new SelectionOption(
                    tier.getMenuChoice(), tier.getDisplayName());
        }
        return options;
    }

    // Returns the room type choices for the boundary menu.
    public SelectionOption[] getRoomTypeOptions() {
        RoomType[] roomTypes = RoomType.values();
        SelectionOption[] options = new SelectionOption[roomTypes.length];
        int index = 0;
        for (RoomType roomType : RoomType.values()) {
            options[index++] = new SelectionOption(
                    roomType.getMenuChoice(), roomType.getDisplayName());
        }
        return options;
    }

    // Returns the allocation status choices for report filters.
    public String[] getAllocationStatusOptions() {
        AllocationStatus[] statuses = AllocationStatus.values();
        String[] options = new String[statuses.length];
        int index = 0;
        for (AllocationStatus status : AllocationStatus.values()) {
            options[index++] = status.getDisplayName();
        }
        return options;
    }

    // Converts the menu choices and registers a new VIP guest.
    public RegistrationResult registerVipGuest(String name, String contactNumber,
            int loyaltyTierChoice, int roomTypeChoice) {
        return registerVipGuestAt(name, contactNumber,
                LoyaltyTier.fromMenuChoice(loyaltyTierChoice),
                RoomType.fromMenuChoice(roomTypeChoice), now());
    }

    // Creates the guest, inserts the guest into the heap, and returns the result.
    RegistrationResult registerVipGuestAt(String name, String contactNumber,
            LoyaltyTier loyaltyTier, RoomType roomType,
            LocalDateTime registeredAt) {
        String guestId = "VIP" + nextGuestNumber++;
        VipGuest guest = new VipGuest(guestId, name, contactNumber,
                loyaltyTier, roomType, registeredAt);
        dataStore.addVipGuest(guest);
        waitingQueue.enqueue(guest);

        int queuePosition = findPriorityPosition(guestId);
        return new RegistrationResult(
                guest.getGuestId(),
                guest.getName(),
                guest.getLoyaltyTier().getDisplayName(),
                guest.getPreferredRoomType().getDisplayName(),
                guest.getLoyaltyTier().getPriorityScore(),
                queuePosition,
                guest.getRegisteredAt(),
                waitingQueue.size());
    }

    // Converts the heap items into simple rows for the boundary to display.
    public QueueEntry[] getHeapQueueView() {
        QueueEntry[] entries = new QueueEntry[waitingQueue.size()];
        LocalDateTime currentTime = now();
        for (int index = 0; index < waitingQueue.size(); index++) {
            VipGuest guest = waitingQueue.getEntry(index);
            entries[index] = toQueueEntry(guest, index, currentTime);
        }
        return entries;
    }

    // Returns all rooms in room-number order for display.
    public RoomView[] getRoomViews() {
        Room[] sortedRooms = dataStore.getRoomsSnapshot();
        SearchAndSortUtility.mergeSort(sortedRooms,
                Comparator.comparing(Room::getRoomNumber));
        RoomView[] views = new RoomView[sortedRooms.length];
        for (int index = 0; index < sortedRooms.length; index++) {
            Room room = sortedRooms[index];
            HousekeepingRecord hkRecord = housekeepingController.findRecord(room.getRoomNumber());
            String housekeepingStatus = hkRecord == null ? "Not Tracked"
                    : hkRecord.getStatus().getDisplayName();
            String readyForVip = room.isAvailable()
                    && hkRecord != null
                    && hkRecord.getStatus() == CleaningStatus.READY
                    ? "Yes" : "No";
            views[index] = new RoomView(room.getRoomNumber(),
                    room.getRoomType().getDisplayName(),
                    room.getStatus().getDisplayName(),
                    housekeepingStatus,
                    readyForVip);
        }
        return views;
    }

    // Assigns an available matching room to the highest eligible VIP guest.
    public AllocationResult allocateHighestPriorityVip() {
        if (waitingQueue.isEmpty()) {
            return AllocationResult.failure("The VIP priority queue is empty.");
        }

        VipGuest selectedGuest = waitingQueue.removeHighestMatching(
                this::hasAvailableCompatibleRoom);
        if (selectedGuest == null) {
            return AllocationResult.failure(
                    "No compatible clean room (READY) is currently available for any waiting VIP guest.");
        }

        Room selectedRoom = findFirstAvailableRoom(selectedGuest.getPreferredRoomType());
        return allocate(selectedGuest, selectedRoom, false,
                "Highest eligible VIP guest allocated successfully.");
    }

    // Releases a room and automatically allocates it to the best matching guest.
    public AllocationResult markRoomAvailable(String roomNumber) {
        Room room = findRoom(roomNumber);
        if (room == null) {
            return AllocationResult.failure("Room " + roomNumber + " was not found.");
        }

        completeActiveAllocation(room.getRoomNumber());
        room.release();

        HousekeepingRecord hkRecord = housekeepingController.findRecord(room.getRoomNumber());
        boolean isClean = hkRecord != null && hkRecord.getStatus() == CleaningStatus.READY;

        if (!isClean) {
            return AllocationResult.roomReleased(
                    "Room " + room.getRoomNumber()
                    + " was released but is currently DIRTY in Housekeeping. Awaiting cleaning before allocation.",
                    room.getRoomNumber(), room.getRoomType().getDisplayName());
        }

        VipGuest selectedGuest = waitingQueue.removeHighestMatching(
                guest -> guest.getPreferredRoomType() == room.getRoomType());
        if (selectedGuest == null) {
            return AllocationResult.roomReleased(
                    "Room " + room.getRoomNumber()
                    + " is ready and available; no compatible VIP guest is waiting.",
                    room.getRoomNumber(), room.getRoomType().getDisplayName());
        }

        return allocate(selectedGuest, room, true,
                "Room became available and was automatically assigned to the highest-priority compatible VIP.");
    }

    // Returns the number of VIP guests still waiting.
    public int getWaitingGuestCount() {
        return waitingQueue.size();
    }

    // Creates the actual room allocation and saves its history record.
    private AllocationResult allocate(VipGuest guest, Room room,
            boolean automatic, String message) {
        room.allocate();
        LocalDateTime allocatedAt = now();
        long waitingMinutes = nonNegativeMinutesBetween(
                guest.getRegisteredAt(), allocatedAt);
        String allocationId = "ALLOC" + nextAllocationNumber++;
        AllocationRecord record = new AllocationRecord(allocationId,
                guest, room, allocatedAt, waitingMinutes, AllocationStatus.ACTIVE);
        dataStore.addAllocationRecord(record);

        return AllocationResult.allocated(automatic, message,
                record.getAllocationId(), guest.getGuestId(), guest.getName(),
                guest.getLoyaltyTier().getDisplayName(), room.getRoomNumber(),
                room.getRoomType().getDisplayName(), waitingMinutes, allocatedAt);
    }

    // Completes the latest active allocation for a released room.
    private void completeActiveAllocation(String roomNumber) {
        for (int index = dataStore.getAllocationRecordCount() - 1;
                index >= 0; index--) {
            AllocationRecord record = dataStore.getAllocationRecord(index);
            if (record.getRoom().getRoomNumber().equalsIgnoreCase(roomNumber)
                    && record.getStatus() == AllocationStatus.ACTIVE) {
                record.complete(now());
                return;
            }
        }
    }

    // Checks whether a matching room is currently available and READY in housekeeping.
    private boolean hasAvailableCompatibleRoom(VipGuest guest) {
        return findFirstAvailableRoom(guest.getPreferredRoomType()) != null;
    }

    // Finds the available and READY matching room with the smallest room number.
    private Room findFirstAvailableRoom(RoomType roomType) {
        Room selectedRoom = null;
        for (int index = 0; index < dataStore.getRoomCount(); index++) {
            Room room = dataStore.getRoom(index);
            if (room.getRoomType() == roomType && room.isAvailable()) {
                HousekeepingRecord hkRecord = housekeepingController.findRecord(room.getRoomNumber());
                if (hkRecord != null && hkRecord.getStatus() == CleaningStatus.READY) {
                    if (selectedRoom == null || room.getRoomNumber().compareTo(selectedRoom.getRoomNumber()) < 0) {
                        selectedRoom = room;
                    }
                }
            }
        }
        return selectedRoom;
    }

    // Uses linear searching to find a room by room number.
    private Room findRoom(String roomNumber) {
        return dataStore.findRoom(roomNumber);
    }

    // Finds a guest's position when the heap is read by priority order.
    private int findPriorityPosition(String guestId) {
        PriorityQueueInterface<VipGuest> orderedGuests = waitingQueue.copy();
        int position = 1;
        while (!orderedGuests.isEmpty()) {
            if (orderedGuests.dequeue().getGuestId().equals(guestId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    // Converts one guest entity into a queue row for the boundary.
    private QueueEntry toQueueEntry(VipGuest guest, int heapIndex,
            LocalDateTime currentTime) {
        int parentIndex = heapIndex == 0 ? -1 : (heapIndex - 1) / 2;
        return new QueueEntry(heapIndex, parentIndex,
                guest.getGuestId(), guest.getName(),
                guest.getLoyaltyTier().getDisplayName(),
                guest.getPreferredRoomType().getDisplayName(),
                guest.getLoyaltyTier().getPriorityScore(),
                guest.getRegisteredAt(),
                nonNegativeMinutesBetween(guest.getRegisteredAt(), currentTime));
    }

    // Calculates waiting minutes and prevents a negative result.
    private long nonNegativeMinutesBetween(LocalDateTime start,
            LocalDateTime end) {
        return Math.max(0, Duration.between(start, end).toMinutes());
    }

    // Returns the current time without seconds for cleaner output.
    private LocalDateTime now() {
        return LocalDateTime.now(clock).withSecond(0).withNano(0);
    }

    // Adds sample rooms, waiting guests, and past allocations for demonstration.
    private void loadSampleData() {
        addRoom(new Room("101", RoomType.STANDARD_SUITE, RoomStatus.AVAILABLE));
        addRoom(new Room("102", RoomType.STANDARD_SUITE, RoomStatus.OCCUPIED));
        addRoom(new Room("204", RoomType.DELUXE_SUITE, RoomStatus.AVAILABLE));
        addRoom(new Room("205", RoomType.DELUXE_SUITE, RoomStatus.OCCUPIED));
        addRoom(new Room("302", RoomType.EXECUTIVE_VILLA, RoomStatus.AVAILABLE));
        addRoom(new Room("303", RoomType.EXECUTIVE_VILLA, RoomStatus.OCCUPIED));
        addRoom(new Room("501", RoomType.OCEAN_VILLA, RoomStatus.AVAILABLE));
        addRoom(new Room("502", RoomType.OCEAN_VILLA, RoomStatus.OCCUPIED));

        LocalDateTime currentTime = now();
        registerVipGuestAt("Datuk Robert Lim", "012-3001001",
                LoyaltyTier.DIAMOND, RoomType.EXECUTIVE_VILLA,
                currentTime.minusMinutes(18));
        registerVipGuestAt("Sarah Lee", "012-3001002",
                LoyaltyTier.PLATINUM, RoomType.OCEAN_VILLA,
                currentTime.minusMinutes(35));
        registerVipGuestAt("Amir Hassan", "012-3001003",
                LoyaltyTier.DIAMOND, RoomType.DELUXE_SUITE,
                currentTime.minusMinutes(9));
        registerVipGuestAt("Mei Chen", "012-3001004",
                LoyaltyTier.ELITE, RoomType.STANDARD_SUITE,
                currentTime.minusMinutes(52));

        addCompletedSampleAllocation("ALLOC0901", "VIP0901", "Alicia Tan",
                LoyaltyTier.DIAMOND, RoomType.OCEAN_VILLA, "501",
                currentTime.minusDays(3).minusMinutes(45),
                currentTime.minusDays(3), 45);
        addCompletedSampleAllocation("ALLOC0902", "VIP0902", "Daniel Wong",
                LoyaltyTier.PLATINUM, RoomType.DELUXE_SUITE, "204",
                currentTime.minusDays(2).minusMinutes(28),
                currentTime.minusDays(2), 28);
        addCompletedSampleAllocation("ALLOC0903", "VIP0903", "Nur Aisyah",
                LoyaltyTier.ELITE, RoomType.STANDARD_SUITE, "101",
                currentTime.minusDays(1).minusMinutes(62),
                currentTime.minusDays(1), 62);
        addCompletedSampleAllocation("ALLOC0904", "VIP0904", "Kumar Raj",
                LoyaltyTier.DIAMOND, RoomType.EXECUTIVE_VILLA, "302",
                currentTime.minusHours(6).minusMinutes(12),
                currentTime.minusHours(6), 12);
    }

    // Adds a room after checking that the room number is not duplicated.
    void addRoom(Room room) {
        dataStore.addRoom(room);
    }

    // Adds one completed allocation to the sample report history.
    private void addCompletedSampleAllocation(String allocationId,
            String guestId, String guestName, LoyaltyTier loyaltyTier,
            RoomType roomType, String roomNumber, LocalDateTime registeredAt,
            LocalDateTime allocatedAt, long waitingMinutes) {
        Room room = findRoom(roomNumber);
        VipGuest guest = new VipGuest(guestId, guestName, "Sample record",
                loyaltyTier, roomType, registeredAt);
        dataStore.addVipGuest(guest);
        AllocationRecord record = new AllocationRecord(allocationId, guest,
                room, allocatedAt, waitingMinutes, AllocationStatus.COMPLETED);
        record.complete(allocatedAt.plusHours(2));
        dataStore.addAllocationRecord(record);
    }

    // Gives the report controller a priority-ordered copy of waiting guests.
    VipGuest[] getWaitingGuestsSnapshot() {
        PriorityQueueInterface<VipGuest> queueCopy = waitingQueue.copy();
        VipGuest[] guests = new VipGuest[queueCopy.size()];
        int index = 0;
        while (!queueCopy.isEmpty()) {
            guests[index++] = queueCopy.dequeue();
        }
        return guests;
    }

    // Gives the report controller a copy of allocation history.
    AllocationRecord[] getAllocationHistorySnapshot() {
        return dataStore.getAllocationRecordsSnapshot();
    }

    // Avoids duplicate generated IDs when an existing shared store is injected.
    private void moveIdentifiersPastExistingData() {
        while (dataStore.findVipGuest("VIP" + nextGuestNumber) != null) {
            nextGuestNumber++;
        }
        while (dataStore.findAllocationRecord(
                "ALLOC" + nextAllocationNumber) != null) {
            nextAllocationNumber++;
        }
    }

    // Gives the report controller the same current time used by this control.
    LocalDateTime getCurrentDateTime() {
        return now();
    }

    // Compares guests by tier first, then uses FIFO for the same tier.
    private static int compareVipPriority(VipGuest first, VipGuest second) {
        int tierComparison = Integer.compare(
                first.getLoyaltyTier().getPriorityScore(),
                second.getLoyaltyTier().getPriorityScore());
        if (tierComparison != 0) {
            return tierComparison;
        }

        int timeComparison = second.getRegisteredAt()
                .compareTo(first.getRegisteredAt());
        if (timeComparison != 0) {
            return timeComparison;
        }
        return second.getGuestId().compareTo(first.getGuestId());
    }

    // Stores one numbered option that the boundary can display.
    public record SelectionOption(int value, String label) {
    }

    // Stores the information shown after a guest is registered.
    public record RegistrationResult(String guestId, String guestName,
            String loyaltyTier, String preferredRoomType, int priorityScore,
            int queuePosition, LocalDateTime registeredAt, int queueSize) {
    }

    // Stores one row used to display the current heap structure.
    public record QueueEntry(int heapIndex, int parentIndex, String guestId,
            String guestName, String loyaltyTier, String preferredRoomType,
            int priorityScore, LocalDateTime registeredAt, long waitingMinutes) {
    }

    // Stores one room row for the boundary.
    public record RoomView(String roomNumber, String roomType, String status, String housekeepingStatus, String readyForVip) {
    }

    // Stores either a successful allocation or a failure message.
    public record AllocationResult(boolean success, boolean automatic,
            boolean allocationCreated, String message, String allocationId,
            String guestId, String guestName, String loyaltyTier,
            String roomNumber, String roomType, long waitingMinutes,
            LocalDateTime allocatedAt) {

        // Creates a failed allocation result.
        static AllocationResult failure(String message) {
            return new AllocationResult(false, false, false, message,
                    null, null, null, null, null, null, 0, null);
        }

        // Creates a result when a room is released but no guest is allocated.
        static AllocationResult roomReleased(String message, String roomNumber,
                String roomType) {
            return new AllocationResult(true, true, false, message,
                    null, null, null, null, roomNumber, roomType, 0, null);
        }

        // Creates a result containing a successful allocation.
        static AllocationResult allocated(boolean automatic, String message,
                String allocationId, String guestId, String guestName,
                String loyaltyTier, String roomNumber, String roomType,
                long waitingMinutes, LocalDateTime allocatedAt) {
            return new AllocationResult(true, automatic, true, message,
                    allocationId, guestId, guestName, loyaltyTier,
                    roomNumber, roomType, waitingMinutes, allocatedAt);
        }
    }
}

