/*
 * Author: Bryan Won Chu Ming
 * Generates filtered and sorted management reports for the VIP module.
 */
package control;

import entity.AllocationRecord;
import entity.AllocationStatus;
import entity.LoyaltyTier;
import entity.RoomType;
import entity.VipGuest;
import utility.SearchAndSortUtility;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Locale;

public class VipReportController {
    private static final int REPORT_WIDTH = 116;
    private static final DateTimeFormatter DATE_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VipRoomAllocationController allocationController;

    // Receives the allocation control that provides guest and allocation data.
    public VipReportController(VipRoomAllocationController allocationController) {
        if (allocationController == null) {
            throw new IllegalArgumentException("Allocation controller is required.");
        }
        this.allocationController = allocationController;
    }

    // Filters and sorts waiting guests, then builds the first management report.
    public ReportResult generatePriorityQueueReport(QueueReportFilter filter) {
        if (filter == null) {
            return ReportResult.failure("Queue report filter is required.");
        }
        if (filter.minimumWaitingMinutes() < 0) {
            return ReportResult.failure("Minimum waiting minutes cannot be negative.");
        }

        LoyaltyTier tier;
        RoomType roomType;
        try {
            tier = parseTier(filter.loyaltyTier());
            roomType = parseRoomType(filter.roomType());
        } catch (IllegalArgumentException exception) {
            return ReportResult.failure(exception.getMessage());
        }

        LocalDateTime reportTime = allocationController.getCurrentDateTime();
        VipGuest[] source = allocationController.getWaitingGuestsSnapshot();
        VipGuest[] searched = searchByGuestId(source, filter.guestId());
        VipGuest[] matchBuffer = new VipGuest[searched.length];
        int matchCount = SearchAndSortUtility.linearSearch(searched, matchBuffer,
                guest -> (tier == null || guest.getLoyaltyTier() == tier)
                && (roomType == null || guest.getPreferredRoomType() == roomType)
                && waitingMinutes(guest, reportTime) >= filter.minimumWaitingMinutes());
        VipGuest[] sorted = copyGuests(matchBuffer, matchCount);
        SearchAndSortUtility.mergeSort(
                sorted, queueComparator(filter.sortChoice(), reportTime));

        String report = buildPriorityQueueReport(
                sorted, filter, reportTime, queueSortLabel(filter.sortChoice()));
        return ReportResult.success(report);
    }

    // Filters and sorts allocation history, then builds the second report.
    public ReportResult generateAllocationPerformanceReport(
            AllocationReportFilter filter) {
        if (filter == null) {
            return ReportResult.failure("Allocation report filter is required.");
        }

        LoyaltyTier tier;
        RoomType roomType;
        AllocationStatus status;
        LocalDate startDate;
        LocalDate endDate;
        try {
            tier = parseTier(filter.loyaltyTier());
            roomType = parseRoomType(filter.roomType());
            status = parseAllocationStatus(filter.allocationStatus());
            startDate = parseOptionalDate(filter.startDate(), LocalDate.of(1900, 1, 1));
            endDate = parseOptionalDate(filter.endDate(), LocalDate.of(2999, 12, 31));
        } catch (IllegalArgumentException exception) {
            return ReportResult.failure(exception.getMessage());
        }

        if (startDate.isAfter(endDate)) {
            return ReportResult.failure("Start date cannot be after end date.");
        }

        AllocationRecord[] source
                = allocationController.getAllocationHistorySnapshot();
        AllocationRecord[] searched = searchAllocationByGuestId(
                source, filter.guestId());
        AllocationRecord[] matchBuffer = new AllocationRecord[searched.length];
        int matchCount = SearchAndSortUtility.linearSearch(
                searched, matchBuffer, record -> {
                    LocalDate allocationDate = record.getAllocatedAt().toLocalDate();
                    return (tier == null || record.getGuest().getLoyaltyTier() == tier)
                            && (roomType == null
                            || record.getRoom().getRoomType() == roomType)
                            && (status == null || record.getStatus() == status)
                            && !allocationDate.isBefore(startDate)
                            && !allocationDate.isAfter(endDate);
                });
        AllocationRecord[] sorted = copyAllocationRecords(matchBuffer, matchCount);
        SearchAndSortUtility.mergeSort(
                sorted, allocationComparator(filter.sortChoice()));

        String report = buildAllocationPerformanceReport(sorted, filter,
                allocationController.getCurrentDateTime(),
                allocationSortLabel(filter.sortChoice()));
        return ReportResult.success(report);
    }

    // Uses merge sort and binary search when an exact guest ID is entered.
    private VipGuest[] searchByGuestId(VipGuest[] source,
            String guestId) {
        if (isAllOrBlank(guestId)) {
            return source.clone();
        }
        VipGuest[] idSorted = source.clone();
        SearchAndSortUtility.mergeSort(idSorted,
                Comparator.comparing(VipGuest::getGuestId,
                        String.CASE_INSENSITIVE_ORDER));
        VipGuest found = SearchAndSortUtility.binarySearchByTextKey(
                idSorted, VipGuest::getGuestId, guestId);
        return found == null ? new VipGuest[0] : new VipGuest[]{found};
    }

    // Finds every allocation belonging to an exact guest ID.
    private AllocationRecord[] searchAllocationByGuestId(
            AllocationRecord[] source, String guestId) {
        if (isAllOrBlank(guestId)) {
            return source.clone();
        }
        AllocationRecord[] idSorted = source.clone();
        SearchAndSortUtility.mergeSort(idSorted,
                Comparator.comparing(record -> record.getGuest().getGuestId(),
                        String.CASE_INSENSITIVE_ORDER));
        AllocationRecord found = SearchAndSortUtility.binarySearchByTextKey(
                idSorted, record -> record.getGuest().getGuestId(), guestId);
        if (found == null) {
            return new AllocationRecord[0];
        }
        AllocationRecord[] matchBuffer = new AllocationRecord[idSorted.length];
        int matchCount = SearchAndSortUtility.linearSearch(idSorted, matchBuffer,
                record -> record.getGuest().getGuestId()
                        .equalsIgnoreCase(guestId.trim()));
        return copyAllocationRecords(matchBuffer, matchCount);
    }

    // Selects the comparison rule for sorting the priority queue report.
    private Comparator<VipGuest> queueComparator(int sortChoice,
            LocalDateTime reportTime) {
        if (sortChoice == 2) {
            return Comparator.comparingLong(
                    (VipGuest guest) -> waitingMinutes(guest, reportTime))
                    .reversed()
                    .thenComparing(VipGuest::getGuestId);
        }
        if (sortChoice == 3) {
            return Comparator.comparing(VipGuest::getGuestId,
                    String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparingInt(
                (VipGuest guest) -> guest.getLoyaltyTier().getPriorityScore())
                .reversed()
                .thenComparing(VipGuest::getRegisteredAt)
                .thenComparing(VipGuest::getGuestId);
    }

    // Selects the comparison rule for sorting the allocation report.
    private Comparator<AllocationRecord> allocationComparator(int sortChoice) {
        if (sortChoice == 2) {
            return Comparator.comparingLong(AllocationRecord::getWaitingMinutes)
                    .reversed()
                    .thenComparing(Comparator.comparing(
                            AllocationRecord::getAllocatedAt).reversed());
        }
        if (sortChoice == 3) {
            return Comparator.comparingInt(
                    (AllocationRecord record) -> record.getGuest()
                            .getLoyaltyTier().getPriorityScore())
                    .reversed()
                    .thenComparing(Comparator.comparing(
                            AllocationRecord::getAllocatedAt).reversed());
        }
        return Comparator.comparing(AllocationRecord::getAllocatedAt)
                .reversed()
                .thenComparing(AllocationRecord::getAllocationId);
    }

    // Formats the filtered waiting guests and their summary as a console report.
    private String buildPriorityQueueReport(VipGuest[] guests,
            QueueReportFilter filter, LocalDateTime reportTime,
            String sortLabel) {
        StringBuilder report = new StringBuilder();
        appendReportHeader(report, "VIP PRIORITY QUEUE & ALLOCATION READINESS REPORT");
        appendMetadata(report, "Generated By", "Bryan Won Chu Ming (VIP Manager)");
        appendMetadata(report, "Generated At", reportTime.format(DATE_TIME_FORMAT));
        appendMetadata(report, "Filters Applied", String.format(Locale.ROOT,
                "Guest ID=%s | Tier=%s | Room Type=%s | Minimum Wait=%d mins",
                displayFilter(filter.guestId()), displayFilter(filter.loyaltyTier()),
                displayFilter(filter.roomType()), filter.minimumWaitingMinutes()));
        appendMetadata(report, "Search Algorithm",
                "Binary Search (exact Guest ID, when supplied) + Linear Search (multi-criteria filters)");
        appendMetadata(report, "Sort Algorithm", "Merge Sort - " + sortLabel);
        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        report.append(String.format(Locale.ROOT,
                "%-4s %-9s %-21s %-10s %-18s %5s %8s %-16s%n",
                "No.", "Guest ID", "Guest Name", "Tier", "Preferred Room",
                "Score", "Wait", "Registered"));
        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());

        int rowNumber = 1;
        long totalWaitingMinutes = 0;
        int[] tierCounts = new int[LoyaltyTier.values().length];
        int[] waitingByRoomType = new int[RoomType.values().length];
        for (VipGuest guest : guests) {
            long wait = waitingMinutes(guest, reportTime);
            report.append(String.format(Locale.ROOT,
                    "%-4d %-9s %-21.21s %-10s %-18.18s %5d %6d m %-16s%n",
                    rowNumber++, guest.getGuestId(), guest.getName(),
                    guest.getLoyaltyTier().getDisplayName(),
                    guest.getPreferredRoomType().getDisplayName(),
                    guest.getLoyaltyTier().getPriorityScore(), wait,
                    guest.getRegisteredAt().format(DATE_TIME_FORMAT)));
            totalWaitingMinutes += wait;
            tierCounts[guest.getLoyaltyTier().ordinal()]++;
            waitingByRoomType[guest.getPreferredRoomType().ordinal()]++;
        }
        if (guests.length == 0) {
            report.append("No waiting VIP guests matched the selected criteria.")
                    .append(System.lineSeparator());
        }

        int immediatelyAllocatable = calculateImmediatelyAllocatable(waitingByRoomType);
        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        report.append("SUMMARY").append(System.lineSeparator());
        appendMetadata(report, "Matching VIP Guests", Integer.toString(guests.length));
        appendMetadata(report, "Tier Breakdown", String.format(Locale.ROOT,
                "Diamond=%d | Platinum=%d | Elite=%d",
                tierCounts[LoyaltyTier.DIAMOND.ordinal()],
                tierCounts[LoyaltyTier.PLATINUM.ordinal()],
                tierCounts[LoyaltyTier.ELITE.ordinal()]));
        appendMetadata(report, "Average Waiting Time",
                guests.length == 0 ? "0.0 mins"
                        : String.format(Locale.ROOT, "%.1f mins",
                                (double) totalWaitingMinutes / guests.length));
        appendMetadata(report, "Immediately Allocatable",
                immediatelyAllocatable + " guest(s), based on available matching rooms");
        report.append("=".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        return report.toString();
    }

    // Formats the filtered allocation records and summary as a console report.
    private String buildAllocationPerformanceReport(
            AllocationRecord[] records, AllocationReportFilter filter,
            LocalDateTime reportTime, String sortLabel) {
        StringBuilder report = new StringBuilder();
        appendReportHeader(report, "VIP ROOM ALLOCATION PERFORMANCE REPORT");
        appendMetadata(report, "Generated By", "Bryan Won Chu Ming (VIP Manager)");
        appendMetadata(report, "Generated At", reportTime.format(DATE_TIME_FORMAT));
        appendMetadata(report, "Business Cycle", String.format(Locale.ROOT,
                "%s to %s", displayDateFilter(filter.startDate(), "Earliest"),
                displayDateFilter(filter.endDate(), "Latest")));
        appendMetadata(report, "Filters Applied", String.format(Locale.ROOT,
                "Guest ID=%s | Tier=%s | Room Type=%s | Status=%s",
                displayFilter(filter.guestId()), displayFilter(filter.loyaltyTier()),
                displayFilter(filter.roomType()),
                displayFilter(filter.allocationStatus())));
        appendMetadata(report, "Search Algorithm",
                "Binary Search (exact Guest ID, when supplied) + Linear Search (multi-criteria filters)");
        appendMetadata(report, "Sort Algorithm", "Merge Sort - " + sortLabel);
        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        report.append(String.format(Locale.ROOT,
                "%-10s %-9s %-18s %-10s %-5s %-18s %7s %-16s %-10s%n",
                "Alloc ID", "Guest ID", "Guest Name", "Tier", "Room",
                "Room Type", "Wait", "Allocated", "Status"));
        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());

        long totalWait = 0;
        long fastestWait = Long.MAX_VALUE;
        long slowestWait = 0;
        int activeCount = 0;
        int completedCount = 0;
        int[] tierCounts = new int[LoyaltyTier.values().length];
        for (AllocationRecord record : records) {
            report.append(String.format(Locale.ROOT,
                    "%-10s %-9s %-18.18s %-10s %-5s %-18.18s %5d m %-16s %-10s%n",
                    record.getAllocationId(), record.getGuest().getGuestId(),
                    record.getGuest().getName(),
                    record.getGuest().getLoyaltyTier().getDisplayName(),
                    record.getRoom().getRoomNumber(),
                    record.getRoom().getRoomType().getDisplayName(),
                    record.getWaitingMinutes(),
                    record.getAllocatedAt().format(DATE_TIME_FORMAT),
                    record.getStatus().getDisplayName()));
            totalWait += record.getWaitingMinutes();
            fastestWait = Math.min(fastestWait, record.getWaitingMinutes());
            slowestWait = Math.max(slowestWait, record.getWaitingMinutes());
            if (record.getStatus() == AllocationStatus.ACTIVE) {
                activeCount++;
            } else {
                completedCount++;
            }
            tierCounts[record.getGuest().getLoyaltyTier().ordinal()]++;
        }
        if (records.length == 0) {
            report.append("No allocation records matched the selected criteria.")
                    .append(System.lineSeparator());
        }

        report.append("-".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        report.append("SUMMARY").append(System.lineSeparator());
        appendMetadata(report, "Total Allocations", Integer.toString(records.length));
        appendMetadata(report, "Allocation Status", String.format(Locale.ROOT,
                "Active=%d | Completed=%d", activeCount, completedCount));
        appendMetadata(report, "Tier Breakdown", String.format(Locale.ROOT,
                "Diamond=%d | Platinum=%d | Elite=%d",
                tierCounts[LoyaltyTier.DIAMOND.ordinal()],
                tierCounts[LoyaltyTier.PLATINUM.ordinal()],
                tierCounts[LoyaltyTier.ELITE.ordinal()]));
        appendMetadata(report, "Average Waiting Time",
                records.length == 0 ? "0.0 mins"
                        : String.format(Locale.ROOT, "%.1f mins",
                                (double) totalWait / records.length));
        appendMetadata(report, "Wait Range",
                records.length == 0 ? "N/A"
                        : fastestWait + " mins (fastest) to "
                        + slowestWait + " mins (slowest)");
        report.append("=".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        return report.toString();
    }

    // Counts how many filtered guests can receive an available matching room.
    private int calculateImmediatelyAllocatable(int[] waitingByRoomType) {
        int[] availableByRoomType = new int[RoomType.values().length];
        for (VipRoomAllocationController.RoomView room
                : allocationController.getRoomViews()) {
            if ("Available".equalsIgnoreCase(room.status())) {
                RoomType roomType = RoomType.fromDisplayName(room.roomType());
                availableByRoomType[roomType.ordinal()]++;
            }
        }

        int allocatable = 0;
        for (int index = 0; index < waitingByRoomType.length; index++) {
            allocatable += Math.min(waitingByRoomType[index],
                    availableByRoomType[index]);
        }
        return allocatable;
    }

    // Copies a selected number of guest matches into an exact-size array.
    private VipGuest[] copyGuests(VipGuest[] source, int count) {
        VipGuest[] copy = new VipGuest[count];
        for (int index = 0; index < count; index++) {
            copy[index] = source[index];
        }
        return copy;
    }

    // Copies allocation matches into an exact-size array.
    private AllocationRecord[] copyAllocationRecords(
            AllocationRecord[] source, int count) {
        AllocationRecord[] copy = new AllocationRecord[count];
        for (int index = 0; index < count; index++) {
            copy[index] = source[index];
        }
        return copy;
    }

    // Adds a centred title and divider lines to a report.
    private void appendReportHeader(StringBuilder report, String title) {
        report.append("=".repeat(REPORT_WIDTH)).append(System.lineSeparator());
        int leftPadding = Math.max(0, (REPORT_WIDTH - title.length()) / 2);
        report.append(" ".repeat(leftPadding)).append(title)
                .append(System.lineSeparator());
        report.append("=".repeat(REPORT_WIDTH)).append(System.lineSeparator());
    }

    // Adds one labelled information line to a report.
    private void appendMetadata(StringBuilder report, String label, String value) {
        report.append(String.format(Locale.ROOT, "%-22s : %s%n", label, value));
    }

    // Converts a tier filter from text to the entity value.
    private LoyaltyTier parseTier(String value) {
        return isAllOrBlank(value) ? null : LoyaltyTier.fromDisplayName(value);
    }

    // Converts a room type filter from text to the entity value.
    private RoomType parseRoomType(String value) {
        return isAllOrBlank(value) ? null : RoomType.fromDisplayName(value);
    }

    // Converts a status filter from text to the entity value.
    private AllocationStatus parseAllocationStatus(String value) {
        return isAllOrBlank(value) ? null
                : AllocationStatus.fromDisplayName(value);
    }

    // Converts an optional yyyy-MM-dd value into a date.
    private LocalDate parseOptionalDate(String value, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return defaultDate;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid date '" + value + "'. Use yyyy-MM-dd format.");
        }
    }

    // Calculates how long a guest has been waiting at report time.
    private long waitingMinutes(VipGuest guest, LocalDateTime reportTime) {
        return Math.max(0, Duration.between(
                guest.getRegisteredAt(), reportTime).toMinutes());
    }

    // Returns a readable explanation of the selected queue sort.
    private String queueSortLabel(int sortChoice) {
        if (sortChoice == 2) {
            return "Waiting Time DESC";
        }
        if (sortChoice == 3) {
            return "Guest ID ASC";
        }
        return "Loyalty Priority DESC, Registration Time ASC";
    }

    // Returns a readable explanation of the selected allocation sort.
    private String allocationSortLabel(int sortChoice) {
        if (sortChoice == 2) {
            return "Waiting Time DESC";
        }
        if (sortChoice == 3) {
            return "Loyalty Priority DESC, Allocation Time DESC";
        }
        return "Allocation Time DESC";
    }

    // Displays blank filter values as All.
    private String displayFilter(String value) {
        return isAllOrBlank(value) ? "All" : value.trim();
    }

    // Displays a default label when a date filter is blank.
    private String displayDateFilter(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    // Checks whether a filter means that all records should be included.
    private boolean isAllOrBlank(String value) {
        return value == null || value.isBlank()
                || "All".equalsIgnoreCase(value.trim());
    }

    // Stores the filter choices for the priority queue report.
    public record QueueReportFilter(String guestId, String loyaltyTier,
            String roomType, long minimumWaitingMinutes, int sortChoice) {
    }

    // Stores the filter choices for the allocation performance report.
    public record AllocationReportFilter(String guestId, String loyaltyTier,
            String roomType, String allocationStatus, String startDate,
            String endDate, int sortChoice) {
    }

    // Stores the generated report or a validation error.
    public record ReportResult(boolean success, String message, String reportText) {
        // Creates a successful report result.
        static ReportResult success(String reportText) {
            return new ReportResult(true, "Report generated successfully.", reportText);
        }

        // Creates a failed report result containing a validation message.
        static ReportResult failure(String message) {
            return new ReportResult(false, message, "");
        }
    }
}
