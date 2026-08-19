/*
* Author: Tang Hong Yi
 * Console boundary for booking, guest registration/cancellation, waitng guest report and assigned guest report
 */
package boundary;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import control.BookingControl;
import entity.Guest;
import entity.RoomType;

public class BookingUI {

    private final Scanner scanner;
    private final BookingControl bookingControl;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public BookingUI(BookingControl bookingControl,
            Scanner scanner) {

        if (bookingControl == null || scanner == null) {
            throw new IllegalArgumentException(
                    "Booking control and scanner are required."
            );
        }

        this.bookingControl = bookingControl;
        this.scanner = scanner;
    }

    public void startMenu() {
        int choice;

        do {
            displayMenu();

            choice = readValidInteger(
                    "Enter your choice: ",
                    0,
                    7
            );

            switch (choice) {
                case 1:
                    registerWalkInUI();
                    break;

                case 2:
                    assignRoomUI();
                    break;

                case 3:
                    cancelBookingUI();
                    break;

                case 4:
                    displayWaitingQueueReportUI();
                    break;

                case 5:
                    displayAssignedGuestReportUI();
                    break;

                case 6:
                    displayAvailableRoomsUI();
                    break;

                case 7:
                    searchGuestUI();
                    break;

                case 0:
                    System.out.println(
                            "Thank you for using Twin Jets Resort Management System."
                    );
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 0);
    }

    private void displayMenu() {
        System.out.println("\n===================================================================");
        System.out.println("              [1] WALK-IN BOOKING & RESERVATION");
        System.out.println("===================================================================");
        System.out.println("1. Register Walk-In and Book Room");
        System.out.println("2. Assign Room to Waiting Guest");
        System.out.println("3. Cancel Booking");
        System.out.println("4. View Waiting Normal Guest Queue");
        System.out.println("5. Assigned Normal Guest Report");
        System.out.println("6. View Available Rooms ");
        System.out.println("7. View search Guest");
        System.out.println("0. Back to Main Menu");
        System.out.println("======================================");
    }

    private void registerWalkInUI() {

        System.out.println("\n====== WALK-IN REGISTRATION ======");

        String guestName = readValidName();
        String phoneNumber = readValidPhoneNumber();

        System.out.println("\nRoom Type");
        System.out.println("1. Standard Suite");
        System.out.println("2. Deluxe Suite");
        System.out.println("3. Executive Villa");
        System.out.println("4. Ocean Villa");
        int choice = readValidInteger(
                "Choose room type: ",
                1,
                4
        );

        String roomType;

        switch (choice) {
            case 1:
                roomType = "Standard Suite";
                break;

            case 2:
                roomType = "Deluxe Suite";
                break;

            case 3:
                roomType = "Executive Villa";
                break;

            case 4:
                roomType = "Ocean Villa";
                break;

            default:
                System.out.println("Invalid room type.");
                return;
        }

        Guest guest = bookingControl.registerWalkIn(
                guestName,
                phoneNumber,
                roomType
        );

        if (guest == null) {
            System.out.println("Registration failed.");
            return;
        }

        if ("Assigned".equals(guest.getStatus())) {
            System.out.println("A room is available.");
            System.out.println("Guest ID : " + guest.getGuestID());
            System.out.println("Room Type: " + guest.getRoomType());
            System.out.println("Room Number: " + guest.getRoomNumber());
            System.out.println("Status    : " + guest.getStatus());

        } else {
            System.out.println("No requested room is currently available.");
            System.out.println("The guest has been added to the waiting queue.");
            System.out.println("Guest ID : " + guest.getGuestID());
            System.out.println("Status   : " + guest.getStatus());
        }
        System.out.println("Walk-in registration completed.");
    }

    public void assignRoomUI() {
        BookingControl.AssignRoomResult result
                = bookingControl.assignRoom();

        if (!result.success()) {
            System.out.println(result.message());
            return;
        }

        System.out.println("\nGuest successfully assigned!");
        System.out.println("Guest Type  : " + result.guestType());
        System.out.println("Guest ID    : " + result.guestID());
        System.out.println("Guest Name  : " + result.guestName());
        System.out.println("Room Type   : " + result.roomType());
        System.out.println("Room Number : " + result.roomNumber());
    }

    public void displayWaitingQueueReportUI() {

        System.out.println("\n===== WAITING GUEST REPORT =====");

        String roomTypeFilter
                = selectRoomTypeFilter();

        LocalDate dateFilter
                = selectDateFilter();

        Guest[] guests
                = bookingControl.getFilteredWaitingGuests(
                        roomTypeFilter,
                        dateFilter
                );

        if (guests.length == 0) {

            System.out.println(
                    "\nNo waiting guests match "
                    + "the selected filters."
            );

            return;
        }

        int position = 1;
        int standardSuiteCount = 0;
        int deluxeSuiteCount = 0;
        int executiveVillaCount = 0;
        int oceanVillaCount = 0;

        System.out.println("\n==============================================================");
        System.out.println("                 WAITING GUEST REPORT");
        System.out.println("==============================================================");

        System.out.println("Room Type Filter : " + roomTypeFilter);

        System.out.println(
                "Date Filter      : "+ (dateFilter == null ? "All Dates" : dateFilter.format(
                                DateTimeFormatter.ofPattern("dd-MM-yyyy")))
        );

        System.out.println("------------------------------------------------");

        for (Guest guest : guests) {

            System.out.println("Queue Position : " + position);
            System.out.println("Guest ID       : " + guest.getGuestID());
            System.out.println("Guest Name     : " + guest.getGuestName());
            System.out.println("Phone Number   : " + guest.getPhoneNumber());
            System.out.println("Room Type      : " + guest.getRoomType());
            System.out.println(
                    "Booking Date   : "
                    + guest.getBookingDate().format(DATE_FORMATTER)
            );
            System.out.println("Status         : " + guest.getStatus());
            System.out.println(
                    "--------------------------------------------------------------"
            );

            if (guest.getRoomType().equalsIgnoreCase("Standard Suite")) {
                standardSuiteCount++;

            } else if (guest.getRoomType()
                    .equalsIgnoreCase("Deluxe Suite")) {
                deluxeSuiteCount++;

            } else if (guest.getRoomType()
                    .equalsIgnoreCase("Executive Villa")) {
                executiveVillaCount++;

            } else if (guest.getRoomType()
                    .equalsIgnoreCase("Ocean Villa")) {
                oceanVillaCount++;
            }

            position++;
        }

        Guest firstGuest = bookingControl.getFirstWaitingGuest();

        System.out.println("\nREPORT SUMMARY");
        System.out.println("Total Waiting Guests : " + +guests.length);
        System.out.println(
                "Standard Suite        : " + standardSuiteCount
        );
        System.out.println(
                "Deluxe Suite          : " + deluxeSuiteCount
        );
        System.out.println(
                "Executive Villa       : " + executiveVillaCount
        );
        System.out.println(
                "Ocean Villa           : " + oceanVillaCount
        );

        System.out.println("\nNEXT GUEST IN QUEUE");
        System.out.println("Guest ID   : " + firstGuest.getGuestID());
        System.out.println("Guest Name : " + firstGuest.getGuestName());
        System.out.println("Room Type  : " + firstGuest.getRoomType());

        System.out.println("==============================================================");
    }

    public void cancelBookingUI() {

        System.out.println("\n===== CANCEL BOOKING =====");

        String guestID = readValidGuestID("Enter Guest ID to cancel: ");

        Guest cancelledGuest = bookingControl.cancelBooking(guestID);

        if (cancelledGuest == null) {
            System.out.println("Guest ID not found.");
        } 
        else {
            System.out.println("\nBooking cancelled successfully.");
            System.out.println("Guest ID   : " + cancelledGuest.getGuestID());
            System.out.println("Guest Name : " + cancelledGuest.getGuestName());
            System.out.println("Room Type  : " + cancelledGuest.getRoomType());
            System.out.println("Status     : " + cancelledGuest.getStatus());
        }
    }

    private String readValidName() {

        while (true) {
            System.out.print("Enter Guest Name: ");
            String guestName = scanner.nextLine().trim();

            if (guestName.isEmpty()) {
                System.out.println("Guest name cannot be empty.");
            } else if (!guestName.matches("[A-Za-z ]+")) {
                System.out.println(
                        "Guest name may contain letters and spaces only."
                );
            } else {
                return guestName;
            }
        }
    }

    private String readValidPhoneNumber() {

        while (true) {
            System.out.print("Enter Phone Number: ");
            String phoneNumber = scanner.nextLine().trim();

            if (phoneNumber.matches("01\\d{8,9}")) {
                return phoneNumber;
            }

            System.out.println(
                    "Invalid phone number. Enter valid mobile number "
                    + "such as 0123456789."
            );
        }
    }

    public void displayAvailableRoomsUI() {

        System.out.println("\n===== AVAILABLE ROOMS =====");

        System.out.println("Standard Suite  : "+ bookingControl.getAvailableRoomCount(RoomType.STANDARD_SUITE));

        System.out.println("Deluxe Suite    : "+ bookingControl.getAvailableRoomCount(RoomType.DELUXE_SUITE));

        System.out.println("Executive Villa : "+ bookingControl.getAvailableRoomCount(RoomType.EXECUTIVE_VILLA));

        System.out.println("Ocean Villa     : "+ bookingControl.getAvailableRoomCount(RoomType.OCEAN_VILLA));

        System.out.println("===========================");
    }

    public void displayAssignedGuestReportUI() {

        System.out.println("\n===== ASSIGNED GUEST REPORT =====");

        String roomTypeFilter
                = selectRoomTypeFilter();

        LocalDate dateFilter
                = selectDateFilter();

        Guest[] guests= bookingControl.getFilteredAssignedGuests(roomTypeFilter,dateFilter);

        if (guests.length == 0) {

            System.out.println("\n No assigned guests match the selected filters.");

            return;
        }

        int standardSuiteCount = 0;
        int deluxeSuiteCount = 0;
        int executiveVillaCount = 0;
        int oceanVillaCount = 0;

        System.out.println("\n==============================================================");
        System.out.println("                  ASSIGNED GUEST REPORT");
        System.out.println("==============================================================");

        System.out.println("Sorted By        : Booking Date "+ "(Earliest to Latest)");

        System.out.println("Room Type Filter : " + roomTypeFilter);

        System.out.println("Date Filter      : "+ (dateFilter == null ? "All Dates": dateFilter.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));

        System.out.println("--------------------------------------------------------------");

        int number = 1;

        for (Guest guest : guests) {

            System.out.println(
                    "Assigned Guest No. : " + number
            );

            System.out.println(
                    "Guest ID           : "
                    + guest.getGuestID()
            );

            System.out.println(
                    "Guest Name         : "
                    + guest.getGuestName()
            );

            System.out.println(
                    "Phone Number       : "
                    + guest.getPhoneNumber()
            );

            System.out.println(
                    "Room Type          : "
                    + guest.getRoomType()
            );

            System.out.println(
                    "Room Number        : "
                    + guest.getRoomNumber()
            );

            System.out.println(
                    "Booking Date       : "
                    + guest.getBookingDate()
                            .format(DATE_FORMATTER)
            );

            System.out.println(
                    "Status             : "
                    + guest.getStatus()
            );

            System.out.println("--------------------------------------------------------------");

            number++;
        }

        System.out.println("\nREPORT SUMMARY");

        System.out.println("Total Assigned Guests : " + guests.length);

        System.out.println("Standard Suite         : " + standardSuiteCount);

        System.out.println("Deluxe Suite           : " + deluxeSuiteCount);

        System.out.println("Executive Villa        : " + executiveVillaCount);

        System.out.println("Ocean Villa            : " + oceanVillaCount);

        System.out.println("==============================================================");
    }

    public void searchGuestUI() {
        System.out.println("\n====SEARCH GUEST====");

        String guestID = readValidGuestID(
                "Enter Guest ID to search: "
        );

        Guest guest = bookingControl.searchGuestByID(guestID);

        if (guest == null) {
            System.out.println("\n Guset not FOund");
        } else {
            System.out.println("\n========== GUEST DETAILS ==========");
            System.out.println("Guest ID      : " + guest.getGuestID());
            System.out.println("Guest Name    : " + guest.getGuestName());
            System.out.println("Phone Number  : " + guest.getPhoneNumber());
            System.out.println("Room Type     : " + guest.getRoomType());
            System.out.println("Room Number   : "+ (guest.getRoomNumber() == null ? "-" : guest.getRoomNumber()));
            System.out.println("Booking Date  : " + guest.getBookingDate().format(DATE_FORMATTER));
            System.out.println("Status        : " + guest.getStatus());
            System.out.println("===================================");
        }
    }

    private int readValidInteger(String prompt, int min, int max) {

        while (true) {
            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);

                if (value >= min && value <= max) {
                    return value;
                }

                System.out.println("Please enter a number between "+ min + " and " + max + ".");

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private String readValidGuestID(String prompt) {

        while (true) {
            System.out.print(prompt);

            String guestID = scanner.nextLine().trim().toUpperCase();

            if (guestID.matches("G\\d{4}")) {
                return guestID;
            }

            System.out.println("Invalid Guest ID format.Please follow the format e.g. 'G0001'."
            );
        }
    }

    private String selectRoomTypeFilter() {

        System.out.println("\n===== ROOM TYPE FILTER =====");
        System.out.println("1. All Room Types");
        System.out.println("2. Standard Suite");
        System.out.println("3. Deluxe Suite");
        System.out.println("4. Executive Villa");
        System.out.println("5. Ocean Villa");

        int choice = readValidInteger("Choose filter: ",1,5);

        switch (choice) {
            case 1:
                return "All";

            case 2:
                return "Standard Suite";

            case 3:
                return "Deluxe Suite";

            case 4:
                return "Executive Villa";

            case 5:
                return "Ocean Villa";

            default:
                return "All";
        }
    }

    private LocalDate selectDateFilter() {

        System.out.println("\n===== BOOKING DATE FILTER =====");
        System.out.println("1. All Dates");
        System.out.println("2. Specific Date");

        int choice = readValidInteger("Choose filter: ",1,2);

        if (choice == 1) {
            return null;
        }

        DateTimeFormatter formatter
                = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        while (true) {

            System.out.print("Enter date (dd-MM-yyyy): ");

            String input = scanner.nextLine().trim();

            try {

                return LocalDate.parse(
                        input,
                        formatter
                );

            } catch (DateTimeParseException e) {

                System.out.println("Invalid date. Example: 20-08-2026");
            }
        }
    }

}
