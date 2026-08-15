/*
 * Author: Bong Xin Yee
 * UI class for displaying housekeeping summary reports.
 */
package boundary;

import control.HousekeepingController.HousekeepingSummaryReport;
import control.HousekeepingController.StaffWorkload;
import control.HousekeepingController.StatusCount;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class HousekeepingUI {

    private static final int SCREEN_WIDTH = 116;
    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Print summary report details
    public void displaySummaryReport(HousekeepingSummaryReport report) {
        if (report == null) {
            System.out.println("[ERROR] No housekeeping summary report is available.");
            return;
        }

        printDivider('=');
        printCentered("HOUSEKEEPING SUMMARY REPORT");
        printDivider('=');

        System.out.println("Generated At        : "
                + report.generatedAt().format(DATE_TIME_FORMAT));
        System.out.println("Total Rooms Tracked  : " + report.totalRoomsTracked());
        System.out.println("Rooms Assigned       : " + report.assignedRooms());
        System.out.println("Rooms Unassigned     : " + report.unassignedRooms());
        System.out.println("Pending Rollbacks    : " + report.pendingRollbackTasks()
                + " task(s) on the ArrayStack ADT");
        System.out.printf(Locale.ROOT, "Avg. Turnaround Time : %.1f mins%n",
                report.averageTurnaroundMinutes());

        printDivider('-');
        printCentered("ROOM STATUS BREAKDOWN");
        printDivider('-');

        StatusCount[] statusBreakdown = report.statusBreakdown();
        if (statusBreakdown.length == 0) {
            System.out.println("No room status data available.");
        } else {
            System.out.printf(Locale.ROOT, "%-24s %-10s %-10s %-20s%n",
                    "Status", "Rooms", "Share", "Distribution");
            printDivider('-');
            for (StatusCount status : statusBreakdown) {
                System.out.printf(Locale.ROOT, "%-24s %-10d %-9.1f%% %s%n",
                        status.statusLabel(), status.roomCount(),
                        status.percentageOfTotal(),
                        buildBar(status.percentageOfTotal()));
            }
        }

        printDivider('-');
        printCentered("STAFF WORKLOAD (BUSIEST FIRST)");
        printDivider('-');

        StaffWorkload[] staffWorkload = report.staffWorkload();
        if (staffWorkload.length == 0) {
            System.out.println("No staff currently have rooms assigned.");
        } else {
            System.out.printf(Locale.ROOT, "%-24s %-12s %-14s %-12s%n",
                    "Staff Name", "Total", "In Progress", "Completed");
            printDivider('-');
            for (StaffWorkload staff : staffWorkload) {
                System.out.printf(Locale.ROOT, "%-24s %-12d %-14d %-12d%n",
                        staff.staffName(), staff.totalRoomsAssigned(),
                        staff.roomsInProgress(), staff.roomsCompleted());
            }
            System.out.println();
            System.out.println("Busiest Staff        : " + report.busiestStaffName()
                    + " (" + report.busiestStaffRoomCount() + " room(s))");
        }

        printDivider('=');
    }

    // Generate text-based progress bar for percentages
    private String buildBar(double percentageOfTotal) {
        int barWidth = 10;
        int filled = (int) Math.round(
                Math.min(100.0, Math.max(0.0, percentageOfTotal)) / 100.0 * barWidth);
        return "[" + "#".repeat(filled) + "-".repeat(barWidth - filled) + "]";
    }

    // Print divider line across screen width
    private void printDivider(char character) {
        System.out.println(String.valueOf(character).repeat(SCREEN_WIDTH));
    }

    // Center align title text in console
    private void printCentered(String text) {
        int padding = Math.max(0, (SCREEN_WIDTH - text.length()) / 2);
        System.out.println(" ".repeat(padding) + text);
    }
}