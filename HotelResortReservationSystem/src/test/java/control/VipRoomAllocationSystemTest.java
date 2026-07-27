/*
 * Author: Bryan Won Chu Ming
 * Dependency-free verification tests for heap ordering, allocation, and reports.
 */
package control;

import adt.HeapPriorityQueue;
import adt.PriorityQueueInterface;
import entity.HotelDataStore;
import entity.LoyaltyTier;
import entity.Room;
import entity.RoomStatus;
import entity.RoomType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

public final class VipRoomAllocationSystemTest {
    private static int passedAssertions;

    // Prevents this test class from being created as an object.
    private VipRoomAllocationSystemTest() {
    }

    // Runs all verification methods.
    public static void main(String[] args) {
        testStaffAuthentication();
        testCustomMaxHeap();
        testSharedHotelDataStore();
        testTierPriorityAndFifo();
        testAutomaticRoomAllocation();
        testFilteredReports();
        System.out.println("All verification tests passed ("
                + passedAssertions + " assertions).");
    }

    // Checks all four staff accounts and rejects an incorrect password.
    private static void testStaffAuthentication() {
        AuthenticationController controller = new AuthenticationController();
        assertCondition(controller.authenticate("tang", "1234").success(),
                "Tang's staff account must be accepted.");
        assertCondition(controller.authenticate("bryan", "1234").success(),
                "Bryan's staff account must be accepted.");
        assertCondition(controller.authenticate("bong", "1234").success(),
                "Bong's staff account must be accepted.");
        assertCondition(controller.authenticate("carret", "1234").success(),
                "Carret's staff account must be accepted.");
        assertCondition(!controller.authenticate("bryan", "wrong").success(),
                "An incorrect password must be rejected.");
    }

    // Checks the basic max heap operations.
    private static void testCustomMaxHeap() {
        PriorityQueueInterface<Integer> heap
                = new HeapPriorityQueue<>(Comparator.naturalOrder());
        heap.enqueue(40);
        heap.enqueue(10);
        heap.enqueue(70);
        heap.enqueue(30);

        assertCondition(heap.getFront() == 70,
                "Max heap must place the largest value at the root.");
        assertCondition(heap.dequeue() == 70,
                "Dequeue must remove the root value.");
        assertCondition(heap.removeHighestMatching(value -> value < 40) == 30,
                "Matching removal must choose the highest eligible value.");
        assertCondition(heap.contains(40),
                "Contains must find an equal stored value.");
        PriorityQueueInterface<Integer> copy = heap.copy();
        assertCondition(copy.dequeue() == 40 && copy.dequeue() == 10
                && heap.size() == 2,
                "Copied priority queue must be descending and independent.");
    }

    // Checks that controllers using one data store observe the same room state.
    private static void testSharedHotelDataStore() {
        HotelDataStore sharedData = new HotelDataStore();
        VipRoomAllocationController allocationController
                = new VipRoomAllocationController(
                        sharedData, fixedClock(), false);
        allocationController.addRoom(new Room("101", RoomType.STANDARD_SUITE,
                RoomStatus.AVAILABLE));
        allocationController.registerVipGuestAt(
                "Shared Guest", "010-9000001", LoyaltyTier.DIAMOND,
                RoomType.STANDARD_SUITE,
                LocalDateTime.of(2026, 7, 27, 14, 30));
        allocationController.allocateHighestPriorityVip();

        assertCondition(sharedData.getVipGuestCount() == 1,
                "Registered VIP profiles must be stored in shared data.");
        assertCondition(sharedData.getAllocationRecordCount() == 1,
                "New allocations must be stored in shared history.");
        assertCondition(sharedData.findRoom("101").getStatus()
                == RoomStatus.OCCUPIED,
                "Room allocation must update the shared room entity.");

        VipRoomAllocationController secondController
                = new VipRoomAllocationController(
                        sharedData, fixedClock(), false);
        assertCondition(secondController.getRoomViews()[0].status()
                .equals("Occupied"),
                "Another controller must see the same shared room status.");
    }

    // Checks tier priority and FIFO ordering for the same tier.
    private static void testTierPriorityAndFifo() {
        VipRoomAllocationController controller = newController();
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 15, 0);
        controller.registerVipGuestAt("Platinum Guest", "010-1000001",
                LoyaltyTier.PLATINUM, RoomType.STANDARD_SUITE,
                now.minusMinutes(30));
        controller.registerVipGuestAt("Younger Diamond", "010-1000002",
                LoyaltyTier.DIAMOND, RoomType.STANDARD_SUITE,
                now.minusMinutes(5));
        controller.registerVipGuestAt("Older Diamond", "010-1000003",
                LoyaltyTier.DIAMOND, RoomType.STANDARD_SUITE,
                now.minusMinutes(10));

        VipRoomAllocationController.QueueEntry root
                = controller.getHeapQueueView()[0];
        assertCondition(root.guestName().equals("Older Diamond"),
                "Diamond must outrank Platinum and same-tier guests must use FIFO.");
        assertCondition(root.parentIndex() == -1,
                "Highest-priority guest must be the heap root.");
    }

    // Checks automatic allocation when an occupied room is released.
    private static void testAutomaticRoomAllocation() {
        VipRoomAllocationController controller = newController();
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 15, 0);
        controller.addRoom(new Room("101", RoomType.STANDARD_SUITE,
                RoomStatus.OCCUPIED));
        controller.registerVipGuestAt("Elite Guest", "010-2000001",
                LoyaltyTier.ELITE, RoomType.STANDARD_SUITE,
                now.minusMinutes(40));
        controller.registerVipGuestAt("Diamond Guest", "010-2000002",
                LoyaltyTier.DIAMOND, RoomType.STANDARD_SUITE,
                now.minusMinutes(8));

        VipRoomAllocationController.AllocationResult result
                = controller.markRoomAvailable("101");
        assertCondition(result.success() && result.automatic(),
                "A released room must trigger automatic allocation.");
        assertCondition(result.guestName().equals("Diamond Guest"),
                "Automatic allocation must select the highest loyalty tier.");
        assertCondition(result.roomNumber().equals("101"),
                "Automatic allocation must use the released compatible room.");
        assertCondition(controller.getWaitingGuestCount() == 1,
                "Allocated guest must be removed from the heap.");
    }

    // Checks report filters, searching, sorting, and summaries.
    private static void testFilteredReports() {
        VipRoomAllocationController controller = newController();
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 15, 0);
        controller.addRoom(new Room("204", RoomType.DELUXE_SUITE,
                RoomStatus.AVAILABLE));
        controller.registerVipGuestAt("Diamond Guest", "010-3000001",
                LoyaltyTier.DIAMOND, RoomType.DELUXE_SUITE,
                now.minusMinutes(12));
        controller.registerVipGuestAt("Elite Guest", "010-3000002",
                LoyaltyTier.ELITE, RoomType.DELUXE_SUITE,
                now.minusMinutes(50));

        VipReportController reportController = new VipReportController(controller);
        VipReportController.ReportResult queueReport
                = reportController.generatePriorityQueueReport(
                        new VipReportController.QueueReportFilter(
                                "", "Diamond", "Deluxe Suite", 10, 1));
        assertCondition(queueReport.success(),
                "Valid multi-criteria queue report must succeed.");
        assertCondition(queueReport.reportText().contains("Diamond Guest")
                && !queueReport.reportText().contains("Elite Guest"),
                "Queue report must apply tier, room type, and wait filters.");

        controller.allocateHighestPriorityVip();
        VipReportController.ReportResult allocationReport
                = reportController.generateAllocationPerformanceReport(
                        new VipReportController.AllocationReportFilter(
                                "", "Diamond", "Deluxe Suite", "Active",
                                "2026-07-27", "2026-07-27", 2));
        assertCondition(allocationReport.success(),
                "Valid allocation performance report must succeed.");
        assertCondition(allocationReport.reportText()
                .contains("Total Allocations      : 1"),
                "Allocation report must summarize the filtered record count.");
        assertCondition(allocationReport.reportText()
                .contains("Binary Search")
                && allocationReport.reportText().contains("Merge Sort"),
                "Reports must disclose combined searching and sorting algorithms.");
    }

    // Creates an empty controller with a fixed time for repeatable tests.
    private static VipRoomAllocationController newController() {
        return new VipRoomAllocationController(fixedClock(), false);
    }

    // Returns the fixed clock shared by repeatable test controllers.
    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-27T07:00:00Z"),
                ZoneId.of("Asia/Kuala_Lumpur"));
    }

    // Stops the test and shows a message when a condition is false.
    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        passedAssertions++;
    }
}
