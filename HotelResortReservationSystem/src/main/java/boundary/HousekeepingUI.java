/**
 * Author: Bong Xin Yee
 * Dedicated console boundary interface for Housekeeping and Task Log Management.
 */
package boundary;

import control.AuthenticationController.LoginResult;
import control.HousekeepingController;
import control.HousekeepingController.RollbackResult;
import control.HousekeepingController.RoomStatusView;
import control.HousekeepingController.StatusOption;
import control.HousekeepingController.TaskActionResult;
import control.HousekeepingReportController;
import control.HousekeepingReportController.CleaningSummaryReport;
import control.HousekeepingReportController.StatusMetric;
import control.HousekeepingReportController.TaskLogAuditReport;
import entity.CleaningStatus;
import entity.CleaningTask;
import entity.HousekeepingRecord;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public final class HousekeepingUI {

    private static final int SCREEN_WIDTH = 116;
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    private final HousekeepingController controller;
    private final HousekeepingReportController reportController;
    private final Scanner scanner;

    public HousekeepingUI(HousekeepingController controller, HousekeepingReportController reportController, Scanner scanner) {
        this.controller = controller;
        this.reportController = reportController;
        this.scanner = scanner;
    }

    public void run(LoginResult loggedInStaff) {
        boolean running = true;
        while (running) {
            printDivider('=');
            printCentered("HOUSEKEEPING & TASK LOG MANAGEMENT");
            printDivider('=');
            System.out.println("STAFF ON DUTY  : " + loggedInStaff.fullName() + " (" + loggedInStaff.role() + ")");
            System.out.println("ROLLBACK STACK : " + controller.getUndoStackSize() + " task(s) available on ArrayStack ADT");
            printDivider('-');
            System.out.println("  1. Display All Room Statuses");
            System.out.println("  2. Assign Housekeeping Staff to Room");
            System.out.println("  3. Update Room Cleaning Status");
            System.out.println("  4. Rollback Last Status Change (LIFO Stack)");
            System.out.println("  5. Report 1: Room Cleaning & Urgency Summary");
            System.out.println("  6. Report 2: Housekeeping Task Log & Audit");
            System.out.println("  0. Return to Main Menu");
            printDivider('-');

            int choice = readInt("[COMMAND] > Select choice (0-6): ", 0, 6);
            System.out.println();

            switch (choice) {
                case 1 -> displayAllRooms();
                case 2 -> assignStaff();
                case 3 -> updateStatus();
                case 4 -> rollback();
                case 5 -> handleReport1();
                case 6 -> handleReport2();
                case 0 -> running = false;
            }

            if (running) {
                pause();
            }
        }
    }

    private void displayAllRooms() {
        RoomStatusView[] rooms = controller.getAllRoomStatusesSnapshot();
        printDivider('=');
        printCentered("ALL ROOM STATUSES");
        printDivider('=');
        if (rooms.length == 0) {
            System.out.println("No room status records available.");
            return;
        }

        System.out.printf(Locale.ROOT, "%-12s %-20s %-20s %-20s %-16s%n",
                "Room No.", "Room Type", "Status", "Assigned Staff", "Service Duration");
        printDivider('-');
        for (RoomStatusView room : rooms) {
            System.out.printf(Locale.ROOT, "%-12s %-20s %-20s %-20s %-16s%n",
                    room.roomNumber(), room.roomType(), room.status(), room.assignedStaff(), room.turnaround());
        }
    }

    private void assignStaff() {
        boolean continueAssigning = true;

        while (continueAssigning) {
            printDivider('-');
            printCentered("ASSIGN HOUSEKEEPING STAFF TO ROOM");
            printDivider('-');

            HousekeepingRecord record = null;
            String roomNumber = "";

            // Validate and loop until a valid room number is provided or cancelled
            while (record == null) {
                roomNumber = readRequiredText("Room Number (0=Cancel) : ");

                // Allow returning to menu
                if ("0".equals(roomNumber) || "X".equalsIgnoreCase(roomNumber)) {
                    System.out.println("[INFO] Operation cancelled.");
                    return;
                }

                record = controller.findRecord(roomNumber);
                if (record == null) {
                    System.out.println();
                    System.out.println("[ERROR] Room " + roomNumber + " is not tracked by housekeeping.");
                    System.out.print("Do you want to try again? (Y/N): ");
                    String retry = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(retry)) {
                        return;
                    }
                    System.out.println();
                    continue;
                }

                // 1. Guard: Block assignment if room is already clean/ready
                if (record.getStatus() == entity.CleaningStatus.READY) {
                    System.out.println();
                    System.out.println("[ERROR] Room " + roomNumber + " is already READY for guests. Assignment rejected.");
                    System.out.print("Do you want to try another room? (Y/N): ");
                    String retry = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(retry)) {
                        return;
                    }
                    record = null; // reset to prompt for room number again
                    System.out.println();
                    continue;
                }

                // 2. Guard: Confirmation if room is already assigned to a staff member
                if (record.isAssigned()) {
                    System.out.println();
                    System.out.println("[WARNING] Room " + roomNumber + " is currently assigned to: " + record.getAssignedStaff());
                    System.out.print("Do you want to reassign this room to another staff? (Y/N): ");
                    String confirm = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(confirm)) {
                        record = null; // reset to prompt for room number again
                        System.out.println();
                        continue;
                    }
                }
            }

            // Room is valid and confirmed -> show selectable duty roster
            String[] staffList = controller.getDutyStaff();
            System.out.println();
            System.out.println("Select Staff on Duty:");
            for (int i = 0; i < staffList.length; i++) {
                System.out.printf(Locale.ROOT, "  [%d] %s%n", (i + 1), staffList[i]);
            }

            int choice = readInt("Select Staff (1-" + staffList.length + ") : ", 1, staffList.length);
            String selectedStaff = staffList[choice - 1];

            TaskActionResult result = controller.assignCleaningTask(roomNumber, selectedStaff);
            System.out.println();
            if (!result.success()) {
                System.out.println("[ERROR] " + result.message());
            } else {
                System.out.println("[SUCCESS] " + result.message());
                System.out.println("Room Number        : " + result.roomNumber());
                System.out.println("Assigned Staff     : " + selectedStaff);
                System.out.println("Status             : " + result.newStatus());
            }

            // Ask if supervisor wants to assign another room immediately
            System.out.println();
            System.out.print("Do you want to assign another room? (Y/N): ");
            String answer = scanner.nextLine().trim();
            if (!"Y".equalsIgnoreCase(answer)) {
                continueAssigning = false;
            }
            System.out.println();
        }
    }

    private void updateStatus() {
        boolean continueUpdatingRooms = true;

        while (continueUpdatingRooms) {
            printDivider('-');
            printCentered("UPDATE ROOM CLEANING STATUS");
            printDivider('-');

            // 1. Filtered Overview: Only display rooms with assigned housekeeping personnel
            RoomStatusView[] allRooms = controller.getAllRoomStatusesSnapshot();
            int assignedCount = 0;
            for (RoomStatusView room : allRooms) {
                if (!"Unassigned".equalsIgnoreCase(room.assignedStaff())) {
                    assignedCount++;
                }
            }

            if (assignedCount == 0) {
                System.out.println("[INFO] No rooms currently assigned to housekeeping staff.");
                System.out.println("       Please assign staff to rooms first (Option 2).");
                return;
            }

            System.out.println("ASSIGNED ROOMS READY FOR STATUS UPDATE:");
            System.out.printf(Locale.ROOT, "  %-10s %-18s %-22s %-20s%n", "Room No.", "Room Type", "Status", "Assigned Staff");
            System.out.println("  -------------------------------------------------------------------------");

            for (RoomStatusView room : allRooms) {
                if (!"Unassigned".equalsIgnoreCase(room.assignedStaff())) {
                    System.out.printf(Locale.ROOT, "  %-10s %-18s %-22s %-20s%n",
                            room.roomNumber(),
                            room.roomType(),
                            room.status(),
                            room.assignedStaff()
                    );
                }
            }
            printDivider('-');

            HousekeepingRecord record = null;
            String roomNumber = "";

            // 2. Validate room number input
            while (record == null) {
                roomNumber = readRequiredText("Room Number (0=Cancel) : ");

                if ("0".equals(roomNumber) || "X".equalsIgnoreCase(roomNumber)) {
                    System.out.println("[INFO] Operation cancelled.");
                    return;
                }

                record = controller.findRecord(roomNumber);
                if (record == null) {
                    System.out.println();
                    System.out.println("[ERROR] Room " + roomNumber + " is not tracked by housekeeping.");
                    System.out.print("Do you want to try again? (Y/N): ");
                    String retry = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(retry)) {
                        return;
                    }
                    System.out.println();
                    continue;
                }

                // Guard: Reject update if room has no assigned staff
                if (!record.isAssigned()) {
                    System.out.println();
                    System.out.println("[ERROR] Room " + roomNumber + " has no assigned staff. Status cannot be updated.");
                    System.out.print("Do you want to try another room? (Y/N): ");
                    String retry = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(retry)) {
                        return;
                    }
                    record = null; // reset to prompt for room number again
                    System.out.println();
                    continue;
                }

                // Guard: Block if already completed/ready
                if (record.getStatus() == CleaningStatus.READY) {
                    System.out.println();
                    System.out.println("[INFO] Room " + roomNumber + " is already 'Ready for Check-In'. No further forward updates available.");
                    System.out.print("Do you want to try another room? (Y/N): ");
                    String retry = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(retry)) {
                        return;
                    }
                    record = null; // reset to prompt for room number again
                    System.out.println();
                }
            }

            // 3. Inner Loop: Allows updating the SAME room multiple times in sequence
            boolean updateSameRoom = true;
            while (updateSameRoom) {
                // Fetch fresh status and dynamic options
                record = controller.findRecord(roomNumber);
                StatusOption[] options = controller.getAvailableStatusTransitions(record.getStatus());

                if (options.length == 0 || record.getStatus() == CleaningStatus.READY) {
                    System.out.println("\n[INFO] Room " + record.getRoomNumber() + " has reached 'Ready for Check-In' (Final State).");
                    break;
                }

                System.out.println();
                System.out.println("Selected Room      : " + record.getRoomNumber() + " (" + record.getRoomType().getDisplayName() + ")");
                System.out.println("Assigned Staff     : " + record.getAssignedStaff());
                System.out.println("Current Status     : " + record.getStatus().getDisplayName());
                System.out.println();

                System.out.println("Select New Status (0 / 'exit' to Cancel):");
                for (int i = 0; i < options.length; i++) {
                    System.out.printf(Locale.ROOT, "  [%d] %s%n", (i + 1), options[i].label());
                }

                int choice = readIntOrCancel("Select Status (1-" + options.length + ") : ", 1, options.length);

                // If user entered 0, exit, EXIT, etc.
                if (choice == -1) {
                    System.out.println("[INFO] Status update cancelled for Room " + roomNumber + ".");
                    updateSameRoom = false;
                    break;
                }

                int selectedStatusValue = options[choice - 1].value();

                TaskActionResult result = controller.updateRoomStatus(roomNumber, selectedStatusValue);
                System.out.println();
                if (!result.success()) {
                    System.out.println("[ERROR] " + result.message());
                } else {
                    System.out.println("[SYSTEM] Status change pushed to custom ArrayStack ADT.");
                    System.out.println("[SUCCESS] " + result.message());
                    System.out.println("Room Number        : " + result.roomNumber());
                    System.out.println("Previous Status    : " + result.previousStatus());
                    System.out.println("New Status         : " + result.newStatus());
                }

                // Check if the room has reached the final state (READY)
                record = controller.findRecord(roomNumber);
                if (record.getStatus() == CleaningStatus.READY) {
                    System.out.println("\n[INFO] Room " + record.getRoomNumber() + " is now 'Ready for Check-In' (Final State).");
                    updateSameRoom = false;
                } else {
                    // Question 1: Continue updating the SAME room?
                    System.out.println();
                    System.out.print("Do you want to continue updating Room " + roomNumber + "? (Y/N): ");
                    String sameAnswer = scanner.nextLine().trim();
                    if (!"Y".equalsIgnoreCase(sameAnswer)) {
                        updateSameRoom = false;
                    }
                }
            }

            // Question 2: Update ANOTHER room?
            System.out.println();
            System.out.print("Do you want to update another room? (Y/N): ");
            String anotherAnswer = scanner.nextLine().trim();
            if (!"Y".equalsIgnoreCase(anotherAnswer)) {
                continueUpdatingRooms = false;
            }
            System.out.println();
        }
    }

    private void rollback() {
        boolean continueRollback = true;

        while (continueRollback) {
            printDivider('-');
            printCentered("ROLLBACK LAST STATUS CHANGE");
            printDivider('-');

            if (controller.getUndoStackSize() == 0) {
                System.out.println("[INFO] Rollback stack is empty (0 tasks available on ArrayStack ADT).");
                return;
            }

            // 1. Peek at top of LIFO stack to show the user what will be reverted
            CleaningTask lastTask = controller.peekLastTask();
            System.out.println("LIFO Top Task on Stack : Task #" + lastTask.getTaskId()
                    + " [Room " + lastTask.getRoomNumber() + "]");
            System.out.println("Current Assigned Staff : " + lastTask.getStaffName());
            System.out.println("Action to Revert       : Revert status from '" 
                    + lastTask.getNewStatus().getDisplayName() + "' back to '" 
                    + lastTask.getPreviousStatus().getDisplayName() + "'");
            System.out.println("Tasks Remaining on ADT : " + controller.getUndoStackSize());
            System.out.println();

            String confirm = readRequiredText("Confirm rollback of this task? (Y/N): ");
            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("[INFO] Rollback cancelled.");
                return;
            }

            // 2. Pop and execute the rollback
            RollbackResult result = controller.rollbackLastTask();
            System.out.println();
            if (!result.success()) {
                System.out.println("[ERROR] " + result.message());
                return;
            }

            System.out.println("[SYSTEM] Task #" + result.taskId() + " popped from custom ArrayStack ADT (LIFO).");
            System.out.println("[SUCCESS] " + result.message());
            System.out.println("Room Number            : " + result.roomNumber());
            System.out.println("Reverted Status        : " + result.revertedStatus());
            System.out.println("Remaining Tasks on ADT : " + result.remainingStackSize());

            // 3. If there are still older tasks on the stack, ask if they want to undo another
            if (result.remainingStackSize() > 0) {
                System.out.println();
                System.out.print("Do you want to rollback another previous task? (Y/N): ");
                String answer = scanner.nextLine().trim();
                if (!"Y".equalsIgnoreCase(answer)) {
                    continueRollback = false;
                }
                System.out.println();
            } else {
                System.out.println("\n[INFO] All changes have been reverted. Stack is now empty.");
                continueRollback = false;
            }
        }
    }
    
    private void handleReport1() {
        System.out.println("Filter by Status: [0] All  [1] Cleaning  [2] Inspected  [3] Ready");
        int opt = readInt("Select Status Filter : ", 0, 3);
        CleaningStatus filter = opt == 0 ? null : CleaningStatus.values()[opt];

        CleaningSummaryReport report = reportController.generateCleaningSummaryReport(filter);

        printDivider('=');
        printCentered("REPORT 1: ROOM CLEANING & URGENCY SUMMARY");
        printDivider('=');
        System.out.println("Generated At        : " + report.generatedAt().format(DATE_TIME_FORMAT));
        System.out.println("Total Rooms Tracked : " + report.totalRooms());
        System.out.println("Unassigned Rooms    : " + report.unassignedRooms());
        System.out.println("Rooms In Progress   : " + report.inProgressRooms());
        System.out.println("Rooms Ready         : " + report.readyRooms());
        printDivider('-');
        System.out.printf(Locale.ROOT, "%-24s %-12s %-12s%n", "Status Category", "Rooms", "Share");
        printDivider('-');
        for (StatusMetric metric : report.statusMetrics()) {
            System.out.printf(Locale.ROOT, "%-24s %-12d %-9.1f%%%n",
                    metric.statusName(), metric.roomCount(), metric.percentage());
        }
        printDivider('-');
        System.out.printf(Locale.ROOT, "%-12s %-20s %-20s %-20s%n", "Room No.", "Type", "Status", "Assigned Staff");
        printDivider('-');
        for (HousekeepingRecord r : report.filteredRooms()) {
            System.out.printf(Locale.ROOT, "%-12s %-20s %-20s %-20s%n",
                    r.getRoomNumber(), r.getRoomType().getDisplayName(),
                    r.getStatus().getDisplayName(), r.getAssignedStaff());
        }
    }

    private void handleReport2() {
        String staff = readOptionalText("Filter by Staff Name (Leave blank for all): ");
        String room = readOptionalText("Filter by Room Number (Leave blank for all): ");

        TaskLogAuditReport report = reportController.generateTaskLogAuditReport(staff, room);

        printDivider('=');
        printCentered("REPORT 2: HOUSEKEEPING TASK LOG & ACTIVITY AUDIT");
        printDivider('=');
        System.out.println("Generated At     : " + report.generatedAt().format(DATE_TIME_FORMAT));
        System.out.println("Total Logs Found : " + report.totalLogsFound());
        printDivider('-');
        System.out.printf(Locale.ROOT, "%-10s %-10s %-20s %-22s -> %-22s %-16s%n",
                "Task ID", "Room", "Staff", "Previous Status", "New Status", "Logged At");
        printDivider('-');
        for (CleaningTask log : report.logs()) {
            System.out.printf(Locale.ROOT, "%-10d %-10s %-20s %-22s -> %-22s %-16s%n",
                    log.getTaskId(), log.getRoomNumber(), log.getStaffName(),
                    log.getPreviousStatus().getDisplayName(), log.getNewStatus().getDisplayName(),
                    log.getLoggedAt().format(DATE_TIME_FORMAT));
        }
    }

    
    // Scanner input validation & screen formatting helpers
    private String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) return "";
            String input = scanner.nextLine().trim();
            if (!input.isBlank()) return input;
            System.out.println("[ERROR] This value is required.");
        }
    }

    private String readOptionalText(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }
    
    private int readIntOrCancel(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) return -1;
            String text = scanner.nextLine().trim();

            if ("0".equals(text) 
                    || "exit".equalsIgnoreCase(text) 
                    || "cancel".equalsIgnoreCase(text) 
                    || "x".equalsIgnoreCase(text)) {
                return -1;
            }

            try {
                int val = Integer.parseInt(text);
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}

            System.out.printf(Locale.ROOT, "[ERROR] Enter a number from %d to %d (or '0' / 'exit' to cancel).%n", min, max);
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) return min;
            String text = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(text);
                if (val >= min && val <= max) return val;
            } catch (NumberFormatException ignored) {}
            System.out.printf(Locale.ROOT, "[ERROR] Enter a number from %d to %d.%n", min, max);
        }
    }

    private void pause() {
        System.out.print(System.lineSeparator() + "Press ENTER to continue...");
        if (scanner.hasNextLine()) scanner.nextLine();
        System.out.println();
    }

    private void printDivider(char c) {
        System.out.println(String.valueOf(c).repeat(SCREEN_WIDTH));
    }

    private void printCentered(String text) {
        int padding = Math.max(0, (SCREEN_WIDTH - text.length()) / 2);
        System.out.println(" ".repeat(padding) + text);
    }
}