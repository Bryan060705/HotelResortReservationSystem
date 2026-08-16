/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boundary;

/**
 *
 * @author user
 */
import adt.LinkedQueue;
import java.util.Scanner;
import java.util.Iterator;
import java.time.format.DateTimeFormatter;
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

            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();

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
        System.out.println("4. View Waiting Queue");
        System.out.println("5. Assigned Guest Report");
        System.out.println("6. View Available Rooms ");
        System.out.println("7. View search Guest");
        System.out.println("0. Exit");
        System.out.println("======================================");
    }

    private void registerWalkInUI() {

        System.out.println("\n====== WALK-IN REGISTRATION ======");

        System.out.print("Enter Guest ID: ");
        String guestID = scanner.nextLine().trim();

        String guestName = readValidName();

        String phoneNumber = readValidPhoneNumber();

        System.out.println("\nRoom Type");
        System.out.println("1. Standard Suite");
        System.out.println("2. Deluxe Suite");
        System.out.println("3. Executive Villa");
        System.out.println("4. Ocean Villa");
        System.out.print("Choose room type: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

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
                guestID,
                guestName,
                phoneNumber,
                roomType
        );

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
        Guest guest = bookingControl.assignRoom();

        if (guest == null) {
            System.out.println("There are no room to be assign" + "There may no waiting guest" + "the request room type is unavailable");
        } else {

            System.out.println("\nGuest successfully assigned!");
            System.out.println("Guest ID   : " + guest.getGuestID());
            System.out.println("Guest Name : " + guest.getGuestName());
            System.out.println("Room Type  : " + guest.getRoomType());
            System.out.println("Room Number: " + guest.getRoomNumber());
            System.out.println("Status     : " + guest.getStatus());

        }
    }

    public void displayWaitingQueueReportUI() {

        LinkedQueue<Guest> queue = bookingControl.getBookingQueue();

        if (queue.isEmpty()) {
            System.out.println("\nThere are no waiting guests.");
            return;
        }

        Iterator<Guest> iterator = queue.getIterator();

        int position = 1;
        int standardSuiteCount = 0;
        int deluxeSuiteCount = 0;
        int executiveVillaCount = 0;
        int oceanVillaCount = 0;

        System.out.println(
                "\n=============================================================="
        );
        System.out.println("                 WAITING GUEST REPORT");
        System.out.println(
                "=============================================================="
        );

        while (iterator.hasNext()) {

            Guest guest = iterator.next();

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
        System.out.println(
                "Total Waiting Guests : "
                + bookingControl.getWaitingGuestCount()
        );
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

        System.out.println(
                "=============================================================="
        );
    }

    public void cancelBookingUI() {

        System.out.println("\n===== CANCEL BOOKING =====");

        System.out.print("Enter Guest ID: ");
        String guestID = scanner.nextLine();

        Guest cancelledGuest = bookingControl.cancelBooking(guestID);

        if (cancelledGuest == null) {
            System.out.println("Guest ID not found in the waiting queue.");
        } else {
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
                    "Invalid phone number. Enter a Malaysian mobile number "
                    + "such as 0123456789."
            );
        }
    }

    public void displayAvailableRoomsUI() {

    System.out.println("\n===== AVAILABLE ROOMS =====");

    System.out.println("Standard Suite  : "
            + bookingControl.getAvailableRoomCount(
                    RoomType.STANDARD_SUITE));

    System.out.println("Deluxe Suite    : "
            + bookingControl.getAvailableRoomCount(
                    RoomType.DELUXE_SUITE));

    System.out.println("Executive Villa : "
            + bookingControl.getAvailableRoomCount(
                    RoomType.EXECUTIVE_VILLA));

    System.out.println("Ocean Villa     : "
            + bookingControl.getAvailableRoomCount(
                    RoomType.OCEAN_VILLA));

    System.out.println("===========================");
}

    public void displayAssignedGuestReportUI() {

        if (!bookingControl.hasConfirmedBookings()) {
            System.out.println("\nThere are no assigned guests.");
            return;
        }

        Iterator<Guest> iterator
                = bookingControl.getConfirmedBookingIterator();

        int number = 1;
        int standardCount = 0;
        int deluxeCount = 0;
        int suiteCount = 0;

        System.out.println("\n"
                + "==============================================================");
        System.out.println("                  ASSIGNED GUEST REPORT");
        System.out.println(
                "=============================================================="
        );

        while (iterator.hasNext()) {
            Guest guest = iterator.next();

            System.out.println("Assigned Guest No. : " + number);
            System.out.println("Guest ID           : "
                    + guest.getGuestID());
            System.out.println("Guest Name         : "
                    + guest.getGuestName());
            System.out.println("Phone Number       : "
                    + guest.getPhoneNumber());
            System.out.println("Room Type          : "
                    + guest.getRoomType());
            System.out.println("Room Number        : "
                    + guest.getRoomNumber());
            System.out.println("Booking Date       : "
                    + guest.getBookingDate().format(DATE_FORMATTER));
            System.out.println("Status             : "
                    + guest.getStatus());
            System.out.println(
                    "--------------------------------------------------------------"
            );

            if (guest.getRoomType().equalsIgnoreCase("Standard")) {
                standardCount++;
            } else if (guest.getRoomType().equalsIgnoreCase("Deluxe")) {
                deluxeCount++;
            } else if (guest.getRoomType().equalsIgnoreCase("Suite")) {
                suiteCount++;
            }

            number++;
        }

        System.out.println("\nREPORT SUMMARY");
        System.out.println("Total Assigned Guests : "
                + bookingControl.getConfirmedBookingCount());
        System.out.println("Standard Rooms         : " + standardCount);
        System.out.println("Deluxe Rooms           : " + deluxeCount);
        System.out.println("Suite Rooms            : " + suiteCount);
        System.out.println(
                "=============================================================="
        );
    }

    public void searchGuestUI() {
        System.out.println("\n====SEARCH GUEST====");

        System.out.println("Enter Guest ID:");
        String guestID = scanner.nextLine();

        Guest guest = bookingControl.searchGuestByID(guestID);

        if (guest == null) {
            System.out.println("\n Guset not FOund");
        } else {
            System.out.println("\n========== GUEST DETAILS ==========");
            System.out.println("Guest ID      : " + guest.getGuestID());
            System.out.println("Guest Name    : " + guest.getGuestName());
            System.out.println("Phone Number  : " + guest.getPhoneNumber());
            System.out.println("Room Type     : " + guest.getRoomType());
            System.out.println("Room Number   : "
                    + (guest.getRoomNumber() == null
                    ? "-"
                    : guest.getRoomNumber()));

            System.out.println("Booking Date  : " + guest.getBookingDate().format(DATE_FORMATTER));

            System.out.println("Status        : " + guest.getStatus());
            System.out.println("===================================");

        }
    }
}
