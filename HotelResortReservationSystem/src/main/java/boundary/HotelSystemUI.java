/*
 * Author: Bryan Won Chu Ming
 * Console boundary for login, menus, VIP allocation, and management reports.
 */
package boundary;

import control.AuthenticationController;
import control.HousekeepingController;
import control.VipReportController;
import control.VipRoomAllocationController;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public class HotelSystemUI {
    private final BookingUI bookingUI;
    private final HousekeepingUI housekeepingUI;
    private static final int SCREEN_WIDTH = 116;
    private static final DateTimeFormatter DATE_TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthenticationController authenticationController;
    private final VipRoomAllocationController allocationController;
    private final VipReportController reportController;
    private final HousekeepingController housekeepingController;
    private final Scanner scanner;

    // Receives the control objects, module boundary objects, and the Scanner used for user input.
    public HotelSystemUI(AuthenticationController authenticationController,
            VipRoomAllocationController allocationController,
            VipReportController reportController,
            HousekeepingController housekeepingController,
            BookingUI bookingUI,
            HousekeepingUI housekeepingUI,
            Scanner scanner) {
        if (authenticationController == null || allocationController == null
                || reportController == null || housekeepingController == null
                || bookingUI == null || housekeepingUI == null || scanner == null) {
            throw new IllegalArgumentException(
                    "Controllers, boundary UIs, and input scanner are required.");
        }
        this.authenticationController = authenticationController;
        this.allocationController = allocationController;
        this.reportController = reportController;
        this.housekeepingController = housekeepingController;
        this.bookingUI = bookingUI;
        this.housekeepingUI = housekeepingUI;
        this.scanner = scanner;
    }

    // Runs the login screen and returns to it whenever a staff member logs out.
    public void run() {
        boolean systemRunning = true;
        while (systemRunning) {
            AuthenticationController.LoginResult loggedInStaff = login();
            if (loggedInStaff == null) {
                systemRunning = false;
            } else {
                runMainMenu(loggedInStaff);
            }
        }
        printDivider('=');
        System.out.println("Hotel resort reservation system closed successfully.");
        printDivider('=');
    }

    // Repeats the login process until the credentials are correct or 0 is entered.
    private AuthenticationController.LoginResult login() {
        while (true) {
            displayLoginScreen();
            System.out.print("Username (0=Exit) : ");
            String username = readLine().trim();
            if (username.equals("0")) {
                return null;
            }
            if (username.isBlank()) {
                System.out.println("[ERROR] Username is required.");
                pause();
                continue;
            }

            System.out.print("Password          : ");
            String password = readLine();
            AuthenticationController.LoginResult result
                    = authenticationController.authenticate(username, password);
            if (result.success()) {
                System.out.println("[SUCCESS] Welcome, " + result.fullName() + ".");
                return result;
            }

            System.out.println("[ERROR] " + result.message());
            pause();
        }
    }

    // Displays the staff login screen.
    private void displayLoginScreen() {
        printDivider('=');
        printCentered("TWIN JETS RESORT MANAGEMENT SYSTEM");
        printCentered("STAFF LOGIN");
        printDivider('=');
        System.out.println("SYSTEM TIME : "
                + LocalDateTime.now().format(DATE_TIME_FORMAT));
        printDivider('-');
    }

    // Runs the full system main menu for the logged-in staff member.
    private void runMainMenu(AuthenticationController.LoginResult loggedInStaff) {
        boolean loggedIn = true;
        while (loggedIn) {
            displayMainMenu(loggedInStaff);
            int choice = readInt("[COMMAND] > Select choice (0-5): ", 0, 5);
            System.out.println();
            switch (choice) {
                case 1:
                    bookingUI.startMenu();
                    break;
                case 2:
                    runVipModule(loggedInStaff);
                    break;
                case 3:
                    housekeepingUI.run(loggedInStaff);
                    break;
                case 4:
//                    displayUnavailableModule(
//                            "Front-Desk Service & Lookup", "Carret Chong Kar Loke");
//                    pause();
                      System.out.println("\n[INFO] Front-Desk Service module is temporarily disabled due to incomplete ADT implementation.");
                      System.out.println("[NOTE] Please refer to Git commits and restore once BinarySearchTree is implemented.");
                      pause();
                      break;
                case 5:
                    displayReportMenu();
                    break;
                case 0:
                    loggedIn = false;
                    System.out.println("[SYSTEM] Logout successful.");
                    break;
                default:
                    throw new IllegalStateException("Unexpected menu choice.");
            }
        }
    }

    // Displays the full system menu after login.
    private void displayMainMenu(AuthenticationController.LoginResult loggedInStaff) {
        printDivider('=');
        printCentered("TWIN JETS RESORT MANAGEMENT SYSTEM");
        printCentered("MAIN MENU");
        printDivider('=');
        System.out.println("LOGGED IN STAFF : " + loggedInStaff.fullName()
                + " (" + loggedInStaff.role() + ")");
        System.out.println("STAFF ID        : " + loggedInStaff.staffId());
        printDivider('-');
        System.out.println("  1. Walk-In Registration & Booking       (Tang Hong Yi)");
        System.out.println("  2. VIP Priority Room Allocation         (Bryan Won Chu Ming)");
        System.out.println("  3. Housekeeping & Task Log              (Bong Xin Yee)");
        System.out.println("  4. Front-Desk Service & Lookup          (Carret Chong Kar Loke)");
        System.out.println("  5. System Analytical Reports");
        System.out.println("  0. Logout System");
        printDivider('-');
    }

    // Shows a message for another group member's module.
    private void displayUnavailableModule(String moduleName, String ownerName) {
        printDivider('=');
        printCentered(moduleName.toUpperCase(Locale.ROOT));
        printDivider('=');
        System.out.println("MODULE OWNER : " + ownerName);
        System.out.println("[INFO] This module is handled by another team member.");
    }

    // Runs the VIP module until the user returns to the system main menu.
    private void runVipModule(AuthenticationController.LoginResult loggedInStaff) {
        boolean moduleOpen = true;
        while (moduleOpen) {
            displayVipModuleMenu(loggedInStaff);
            int choice = readInt("[COMMAND] > Select choice (0-5): ", 0, 5);
            System.out.println();
            switch (choice) {
                case 1:
                    registerVipGuest();
                    break;
                case 2:
                    displayPriorityHeap();
                    break;
                case 3:
                    allocateHighestPriorityVip();
                    break;
                case 4:
                    displayRooms();
                    break;
                case 5:
                    markRoomAvailable();
                    break;
                case 0:
                    moduleOpen = false;
                    break;
                default:
                    throw new IllegalStateException("Unexpected VIP module choice.");
            }
            if (moduleOpen) {
                pause();
            }
        }
    }

    // Displays the choices in the VIP room allocation module.
    private void displayVipModuleMenu(
            AuthenticationController.LoginResult loggedInStaff) {
        printDivider('=');
        printCentered("[2] VIP & LOYALTY PRIORITY ROOM ALLOCATION");
        printDivider('=');
        System.out.println("LOGGED IN STAFF : " + loggedInStaff.fullName()
                + " (" + loggedInStaff.role() + ")");
        System.out.println("SYSTEM TIME     : "
                + LocalDateTime.now().format(DATE_TIME_FORMAT));
        System.out.println("WAITING VIPS    : "
                + allocationController.getWaitingGuestCount());
        printDivider('-');
        System.out.println("  1. Register High-Tier VIP Guest");
        System.out.println("  2. Display Reorganized Priority Heap (Custom PriorityQueue ADT)");
        System.out.println("  3. Allocate Available Room to Highest-Priority Eligible VIP");
        System.out.println("  4. Display Room Availability");
        System.out.println("  5. Mark Room Available (Automatic Priority Allocation)");
        System.out.println("  0. Return to Main Menu");
        printDivider('-');
    }

    // Reads guest details and sends them to the allocation control.
    private void registerVipGuest() {
        System.out.println("--- INPUT VIP DETAILS ---");
        String guestName = readRequiredText("VIP Guest Name     : ");
        String contactNumber = readRequiredText("Contact Number     : ");

        System.out.println("Loyalty Tier       :");
        displaySelectionOptions(allocationController.getLoyaltyTierOptions(), false);
        int tierChoice = readInt("Select Tier        : ", 1, 3);

        System.out.println("Preferred Room Type:");
        displaySelectionOptions(allocationController.getRoomTypeOptions(), false);
        int roomTypeChoice = readInt("Select Room Type   : ", 1, 4);

        try {
            VipRoomAllocationController.RegistrationResult result
                    = allocationController.registerVipGuest(guestName,
                            contactNumber, tierChoice, roomTypeChoice);
            System.out.println();
            System.out.println("[SYSTEM] Inserting VIP record into custom PriorityQueue ADT...");
            System.out.printf(Locale.ROOT, "Guest ID           : %s%n", result.guestId());
            System.out.printf(Locale.ROOT, "Loyalty Tier       : %s%n", result.loyaltyTier());
            System.out.printf(Locale.ROOT, "Preferred Room     : %s%n", result.preferredRoomType());
            System.out.printf(Locale.ROOT, "Priority Score     : %d/100%n", result.priorityScore());
            System.out.printf(Locale.ROOT, "Priority Position  : #%d of %d%n",
                    result.queuePosition(), result.queueSize());
            System.out.println("[SUCCESS] Heap reorganized; VIP guest queued successfully.");
        } catch (IllegalArgumentException exception) {
            System.out.println("[ERROR] " + exception.getMessage());
        }
    }

    // Displays the actual parent and child positions in the max heap.
    private void displayPriorityHeap() {
        VipRoomAllocationController.QueueEntry[] entries
                = allocationController.getHeapQueueView();
        printDivider('=');
        printCentered("REORGANIZED VIP PRIORITY MAX HEAP");
        printDivider('=');
        System.out.println("Heap rule: Parent always has equal or higher priority than its children.");
        System.out.println("Tier order: Diamond > Platinum > Elite; same-tier guests use FIFO.");
        printDivider('-');
        System.out.printf(Locale.ROOT,
                "%-5s %-7s %-9s %-22s %-10s %-18s %5s %8s %-16s%n",
                "Index", "Parent", "Guest ID", "Guest Name", "Tier",
                "Preferred Room", "Score", "Wait", "Registered");
        printDivider('-');

        for (VipRoomAllocationController.QueueEntry entry : entries) {
            String parent = entry.parentIndex() < 0 ? "ROOT"
                    : Integer.toString(entry.parentIndex());
            System.out.printf(Locale.ROOT,
                    "%-5d %-7s %-9s %-22.22s %-10s %-18.18s %5d %6d m %-16s%n",
                    entry.heapIndex(), parent, entry.guestId(), entry.guestName(),
                    entry.loyaltyTier(), entry.preferredRoomType(),
                    entry.priorityScore(), entry.waitingMinutes(),
                    entry.registeredAt().format(DATE_TIME_FORMAT));
        }
        if (entries.length == 0) {
            System.out.println("The VIP priority heap is empty.");
        }
        printDivider('=');
    }

    // Requests a manual allocation for the highest eligible VIP guest.
    private void allocateHighestPriorityVip() {
        System.out.println("[SYSTEM] Searching heap for the highest-priority guest with a compatible room...");
        VipRoomAllocationController.AllocationResult result
                = allocationController.allocateHighestPriorityVip();
        displayAllocationResult(result);
    }

    // Displays every room and its current availability.
    private void displayRooms() {
        VipRoomAllocationController.RoomView[] rooms
                = allocationController.getRoomViews();
        printDivider('=');
        printCentered("ROOM AVAILABILITY");
        printDivider('=');
        System.out.printf(Locale.ROOT, "%-12s %-24s %-12s%n",
                "Room No.", "Room Type", "Status");
        printDivider('-');
        for (VipRoomAllocationController.RoomView room : rooms) {
            System.out.printf(Locale.ROOT, "%-12s %-24s %-12s%n",
                    room.roomNumber(), room.roomType(), room.status());
        }
        printDivider('=');
    }

    // Releases a room and allows the control to perform automatic allocation.
    private void markRoomAvailable() {
        System.out.println("--- RELEASE ROOM / AUTO-ALLOCATE ---");
        String roomNumber = readRequiredText("Enter Room Number   : ");
        System.out.println("[SYSTEM] Releasing room and searching the max heap by room type...");
        VipRoomAllocationController.AllocationResult result
                = allocationController.markRoomAvailable(roomNumber);
        displayAllocationResult(result);
    }

    // Displays either the successful allocation details or an error message.
    private void displayAllocationResult(
            VipRoomAllocationController.AllocationResult result) {
        if (!result.success()) {
            System.out.println("[STATUS] " + result.message());
            return;
        }
        System.out.println("[SUCCESS] " + result.message());
        if (!result.allocationCreated()) {
            System.out.printf(Locale.ROOT, "Room Available      : %s (%s)%n",
                    result.roomNumber(), result.roomType());
            return;
        }
        System.out.printf(Locale.ROOT, "Allocation ID       : %s%n", result.allocationId());
        System.out.printf(Locale.ROOT, "VIP Guest           : %s - %s%n",
                result.guestId(), result.guestName());
        System.out.printf(Locale.ROOT, "Loyalty Tier        : %s%n", result.loyaltyTier());
        System.out.printf(Locale.ROOT, "Room Assigned       : %s (%s)%n",
                result.roomNumber(), result.roomType());
        System.out.printf(Locale.ROOT, "Waiting Time        : %d minutes%n",
                result.waitingMinutes());
        System.out.printf(Locale.ROOT, "Allocation Mode     : %s%n",
                result.automatic() ? "Automatic on room release" : "Manual command");
    }

    // Runs the report menu until the user returns to the main menu.
    private void displayReportMenu() {
        boolean reportMenuOpen = true;
        while (reportMenuOpen) {
            printDivider('=');
            printCentered("VIP MANAGEMENT REPORTS");
            printDivider('=');
            System.out.println("  1. VIP Priority Queue & Allocation Readiness Report");
            System.out.println("  2. VIP Room Allocation Performance Report");
            System.out.println("  0. Return to Main Menu");
            printDivider('-');
            int choice = readInt("[COMMAND] > Select report (0-2): ", 0, 2);
            System.out.println();
            switch (choice) {
                case 1:
                    generateQueueReport();
                    break;
                case 2:
                    generateAllocationReport();
                    break;
                case 0:
                    reportMenuOpen = false;
                    break;
                default:
                    throw new IllegalStateException("Unexpected report choice.");
            }
            if (reportMenuOpen) {
                pause();
            }
        }
    }

    // Reads queue report filters and asks the report control to generate it.
    private void generateQueueReport() {
        System.out.println("--- QUEUE REPORT FILTERS ---");
        String guestId = readOptionalText("Guest ID (blank=All)       : ");
        String tier = readTierFilter();
        String roomType = readRoomTypeFilter();
        long minimumWait = readNonNegativeLong(
                "Minimum waiting mins (blank=0): ", 0);
        System.out.println("Sort Order: [1] Priority DESC  [2] Waiting Time DESC  [3] Guest ID ASC");
        int sortChoice = readInt("Select Sort Order             : ", 1, 3);

        VipReportController.QueueReportFilter filter
                = new VipReportController.QueueReportFilter(
                        guestId, tier, roomType, minimumWait, sortChoice);
        VipReportController.ReportResult result
                = reportController.generatePriorityQueueReport(filter);
        displayReportResult(result);
    }

    // Reads allocation report filters and asks the report control to generate it.
    private void generateAllocationReport() {
        System.out.println("--- ALLOCATION PERFORMANCE REPORT FILTERS ---");
        String guestId = readOptionalText("Guest ID (blank=All)       : ");
        String startDate = readOptionalText(
                "Start Date yyyy-MM-dd (blank=All): ");
        String endDate = readOptionalText(
                "End Date yyyy-MM-dd (blank=All)  : ");
        String tier = readTierFilter();
        String roomType = readRoomTypeFilter();
        String allocationStatus = readStatusFilter();
        System.out.println("Sort Order: [1] Allocation Time DESC  [2] Waiting Time DESC"
                + "  [3] Loyalty Priority DESC");
        int sortChoice = readInt("Select Sort Order             : ", 1, 3);

        VipReportController.AllocationReportFilter filter
                = new VipReportController.AllocationReportFilter(
                        guestId, tier, roomType, allocationStatus,
                        startDate, endDate, sortChoice);
        VipReportController.ReportResult result
                = reportController.generateAllocationPerformanceReport(filter);
        displayReportResult(result);
    }

    // Prints a generated report or its validation error.
    private void displayReportResult(VipReportController.ReportResult result) {
        System.out.println();
        if (!result.success()) {
            System.out.println("[ERROR] " + result.message());
            return;
        }
        System.out.print(result.reportText());
        System.out.println("[SUCCESS] " + result.message());
    }

    // Reads an optional loyalty tier filter.
    private String readTierFilter() {
        System.out.println("Loyalty Tier: [0] All");
        displaySelectionOptions(allocationController.getLoyaltyTierOptions(), false);
        int choice = readInt("Select Tier Filter            : ", 0, 3);
        return choice == 0 ? "All"
                : findOptionLabel(allocationController.getLoyaltyTierOptions(), choice);
    }

    // Reads an optional room type filter.
    private String readRoomTypeFilter() {
        System.out.println("Room Type: [0] All");
        displaySelectionOptions(allocationController.getRoomTypeOptions(), false);
        int choice = readInt("Select Room Type Filter       : ", 0, 4);
        return choice == 0 ? "All"
                : findOptionLabel(allocationController.getRoomTypeOptions(), choice);
    }

    // Reads an optional allocation status filter.
    private String readStatusFilter() {
        String[] statuses = allocationController.getAllocationStatusOptions();
        System.out.println("Allocation Status: [0] All");
        for (int index = 0; index < statuses.length; index++) {
            System.out.printf(Locale.ROOT, "  [%d] %s%n", index + 1, statuses[index]);
        }
        int choice = readInt("Select Status Filter          : ", 0, statuses.length);
        return choice == 0 ? "All" : statuses[choice - 1];
    }

    // Displays a numbered list of options.
    private void displaySelectionOptions(
            VipRoomAllocationController.SelectionOption[] options,
            boolean includeAll) {
        if (includeAll) {
            System.out.println("  [0] All");
        }
        for (VipRoomAllocationController.SelectionOption option : options) {
            System.out.printf(Locale.ROOT, "  [%d] %s%n",
                    option.value(), option.label());
        }
    }

    // Finds the display label for a selected menu number.
    private String findOptionLabel(
            VipRoomAllocationController.SelectionOption[] options,
            int value) {
        for (VipRoomAllocationController.SelectionOption option : options) {
            if (option.value() == value) {
                return option.label();
            }
        }
        throw new IllegalArgumentException("Selection was not found.");
    }

    // Reads text repeatedly until the user enters a non-empty value.
    private String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = readLine();
            if (!value.isBlank()) {
                return value.trim();
            }
            System.out.println("[ERROR] This value is required.");
        }
    }

    // Reads text that may be left blank.
    private String readOptionalText(String prompt) {
        System.out.print(prompt);
        return readLine().trim();
    }

    // Reads and validates a whole number within a given range.
    private int readInt(String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String value = readLine();
            try {
                int number = Integer.parseInt(value.trim());
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException exception) {
                // The validation message below is sufficient for the actor.
            }
            System.out.printf(Locale.ROOT,
                    "[ERROR] Enter a number from %d to %d.%n", minimum, maximum);
        }
    }

    // Reads zero or a positive whole number.
    private long readNonNegativeLong(String prompt, long defaultValue) {
        while (true) {
            System.out.print(prompt);
            String value = readLine().trim();
            if (value.isBlank()) {
                return defaultValue;
            }
            try {
                long number = Long.parseLong(value);
                if (number >= 0) {
                    return number;
                }
            } catch (NumberFormatException exception) {
                // The validation message below is sufficient for the actor.
            }
            System.out.println("[ERROR] Enter zero or a positive whole number.");
        }
    }

    // Reads one line safely and returns zero when the input stream ends.
    private String readLine() {
        if (!scanner.hasNextLine()) {
            return "0";
        }
        return scanner.nextLine();
    }

    // Waits for ENTER before showing the next screen.
    private void pause() {
        if (scanner.hasNextLine()) {
            System.out.print(System.lineSeparator()
                    + "Press ENTER to continue...");
            scanner.nextLine();
            System.out.println();
        }
    }

    // Prints a horizontal divider across the console.
    private void printDivider(char character) {
        System.out.println(String.valueOf(character).repeat(SCREEN_WIDTH));
    }

    // Prints a title near the centre of the console width.
    private void printCentered(String text) {
        int padding = Math.max(0, (SCREEN_WIDTH - text.length()) / 2);
        System.out.println(" ".repeat(padding) + text);
    }
}