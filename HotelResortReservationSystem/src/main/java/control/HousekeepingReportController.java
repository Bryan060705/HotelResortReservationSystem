/* Author: Bong Xin Yee
 * Handles data aggregation and filtering for the two housekeeping reports
 */
package control;

import entity.CleaningStatus;
import entity.CleaningTask;
import entity.HousekeepingRecord;
import java.time.LocalDateTime;
import java.util.Comparator;
import utility.SearchAndSortUtility;

public final class HousekeepingReportController {

    private final HousekeepingController housekeepingController;

    public HousekeepingReportController(HousekeepingController housekeepingController) {
        this.housekeepingController = housekeepingController;
    }

    // Report 1: Room cleaning summary and urgency breakdown
    public record StatusMetric(String statusName, int roomCount, double percentage) {}

    public record CleaningSummaryReport(
            LocalDateTime generatedAt,
            int totalRooms,
            int unassignedRooms,
            int inProgressRooms,
            int readyRooms,
            StatusMetric[] statusMetrics,
            HousekeepingRecord[] filteredRooms
    ) {}

    public CleaningSummaryReport generateCleaningSummaryReport(CleaningStatus filterStatus) {
        HousekeepingRecord[] allRooms = housekeepingController.getAllRecordsSnapshot();
        int total = allRooms.length;

        int[] counts = new int[CleaningStatus.values().length];
        int unassigned = 0;
        int inProgress = 0;
        int ready = 0;

        // First count the rooms and check which ones match the filter
        int matchCount = 0;
        for (HousekeepingRecord record : allRooms) {
            counts[record.getStatus().ordinal()]++;

            if (!record.isAssigned()) {
                unassigned++;
            }

            if (record.getStatus() == CleaningStatus.CLEANING) {
                inProgress++;
            }

            if (record.getStatus() == CleaningStatus.READY) {
                ready++;
            }

            if (filterStatus == null || record.getStatus() == filterStatus) {
                matchCount++;
            }
        }

        // Create an array for the rooms that match the selected status
        HousekeepingRecord[] filtered = new HousekeepingRecord[matchCount];
        int idx = 0;

        for (HousekeepingRecord record : allRooms) {
            if (filterStatus == null || record.getStatus() == filterStatus) {
                filtered[idx++] = record;
            }
        }

        // Calculate the percentage for each cleaning status
        StatusMetric[] metrics = new StatusMetric[CleaningStatus.values().length];

        for (CleaningStatus status : CleaningStatus.values()) {
            int count = counts[status.ordinal()];
            double percentage = total == 0 ? 0.0 : (count * 100.0) / total;

            metrics[status.ordinal()] =
                    new StatusMetric(status.getDisplayName(), count, percentage);
        }

        return new CleaningSummaryReport(
                LocalDateTime.now(),
                total,
                unassigned,
                inProgress,
                ready,
                metrics,
                filtered
        );
    }

    // Report 2: Audit history log with staff and room filters
    public record TaskLogAuditReport(
            LocalDateTime generatedAt,
            int totalLogsFound,
            CleaningTask[] logs
    ) {}

    public TaskLogAuditReport generateTaskLogAuditReport(String staffFilter, String roomFilter) {
        CleaningTask[] allLogs = housekeepingController.getAuditHistorySnapshot();

        // Count the logs that match the selected filters
        int matchCount = 0;

        for (CleaningTask log : allLogs) {
            boolean staffMatch = (staffFilter == null || staffFilter.isBlank())
                    || log.getStaffName().equalsIgnoreCase(staffFilter.trim());

            boolean roomMatch = (roomFilter == null || roomFilter.isBlank())
                    || log.getRoomNumber().equalsIgnoreCase(roomFilter.trim());

            if (staffMatch && roomMatch) {
                matchCount++;
            }
        }

        // Store the matching logs in a new array
        CleaningTask[] matched = new CleaningTask[matchCount];
        int idx = 0;

        for (CleaningTask log : allLogs) {
            boolean staffMatch = (staffFilter == null || staffFilter.isBlank())
                    || log.getStaffName().equalsIgnoreCase(staffFilter.trim());

            boolean roomMatch = (roomFilter == null || roomFilter.isBlank())
                    || log.getRoomNumber().equalsIgnoreCase(roomFilter.trim());

            if (staffMatch && roomMatch) {
                matched[idx++] = log;
            }
        }

        // Sort the logs so the latest tasks appear first
        SearchAndSortUtility.mergeSort(
                matched,
                Comparator.comparing(CleaningTask::getLoggedAt).reversed()
        );

        return new TaskLogAuditReport(LocalDateTime.now(), matchCount, matched);
    }
}