    /*
    Author: CARRET CHONG KAR LOKE
     */

    package boundary;

    import control.FrontDeskServiceControl;
    import entity.Guest;
    import entity.Room;
    import entity.RoomType;
    import entity.HotelDataStore;


    import java.time.LocalDateTime;
    import java.util.Scanner;

    /**
     * Boundary class for the Front-Desk Service module.
     *
     * Handles all console interaction with the front-desk agent.
     */
    public class FrontDeskServiceBoundary {

        private final FrontDeskServiceControl control;
        private final Scanner scanner;

        /**
         * Creates the Front-Desk Service boundary.
         */
        public FrontDeskServiceBoundary(HotelDataStore hotelDataStore, Scanner scanner) {
        control = new FrontDeskServiceControl(hotelDataStore);
        this.scanner = scanner;
    }

        /**
         * Starts the Front-Desk Service menu.
         */
        public void run() {

            boolean running = true;

            while (running) {

                displayMenu();

                int choice = readInteger("Enter your choice: ");

                System.out.println();

                try {

                    switch (choice) {

                        case 1:
                            addGuest();
                            break;

                        case 2:
                            searchGuest();
                            break;

                        case 3:
                            checkRoomAvailability();
                            break;

                        case 4:
                            viewBilling();
                            break;

                        case 5:
                            displayAllGuests();
                            break;

                        case 6:
                            updateGuestStatus();
                            break;

                        case 7:
                            deleteGuest();
                            break;

                        case 0:
                            running = false;
                            System.out.println(
                                    "Returning to the main system...");
                            break;

                        default:
                            System.out.println(
                                    "Invalid option. Please try again.");
                    }

                } catch (IllegalArgumentException
                        | IllegalStateException exception) {

                    System.out.println(
                            "Error: " + exception.getMessage());
                }

                if (running) {
                    System.out.println();
                    System.out.println(
                            "Press ENTER to continue...");
                    scanner.nextLine();
                }
            }
        }

        /**
         * Displays the Front-Desk Service menu.
         */
        private void displayMenu() {

            System.out.println();
            System.out.println("======================================");
            System.out.println("       TWIN JETS RESORT");
            System.out.println("       FRONT-DESK SERVICE");
            System.out.println("======================================");
            System.out.println("1. Register Guest");
            System.out.println("2. Search Guest");
            System.out.println("3. Check Room Availability");
            System.out.println("4. View Billing Details");
            System.out.println("5. Display All Guests");
            System.out.println("6. Update Guest Status");
            System.out.println("7. Delete Guest");
            System.out.println("0. Return to Main Menu");
            System.out.println("======================================");
        }

        /**
         * Registers a new guest.
         */
        private void addGuest() {

            System.out.println("========== REGISTER GUEST ==========");

            String confirmationNumber = readConfirmationNumber();

            if (control.searchGuest(confirmationNumber) != null) {
                System.out.println(
                        "A guest with this confirmation number already exists.");
                return;
            }

            String name = readText("Guest name: ");
            String phone = readText("Phone number: ");

            System.out.println();
            System.out.println("Room Type:");
            System.out.println("1. Standard Suite");
            System.out.println("2. Deluxe Suite");
            System.out.println("3. Executive Villa");
            System.out.println("4. Ocean Villa");

            int roomChoice = readInteger("Select room type: ");

            RoomType roomType = RoomType.fromMenuChoice(roomChoice);

            String status = readText(
                    "Booking status (Confirmed/Pending/Checked-In): ");

            String roomNumber = readText("Room number: ");

            Guest guest = new Guest(
                    confirmationNumber,
                    name,
                    phone,
                    LocalDateTime.now(),
                    roomType.getDisplayName(),
                    status,
                    roomNumber
            );

            control.addGuest(guest);

            System.out.println();
            System.out.println(
                    "Guest registered successfully.");
        }

        /**
         * Searches for a guest using the confirmation number.
         */
        private void searchGuest() {

            System.out.println("========== SEARCH GUEST ==========");

            String confirmationNumber = readConfirmationNumber();

            long startTime = System.nanoTime();

            Guest guest = control.searchGuest(confirmationNumber);

            long endTime = System.nanoTime();

            if (guest == null) {

                System.out.println();
                System.out.println(
                        "Guest not found.");

            } else {

                System.out.println();
                System.out.println("Guest found!");
                System.out.println("--------------------------------------");
                System.out.println(guest);
                System.out.println("--------------------------------------");
            }

            System.out.printf(
                    "BST search time: %d ns%n",
                    endTime - startTime
            );
        }

        /**
         * Checks available rooms according to room type.
         */
        private void checkRoomAvailability() {

            System.out.println(
                    "========== ROOM AVAILABILITY ==========");

            System.out.println("1. Standard Suite");
            System.out.println("2. Deluxe Suite");
            System.out.println("3. Executive Villa");
            System.out.println("4. Ocean Villa");

            int choice = readInteger("Select room type: ");

            RoomType roomType = RoomType.fromMenuChoice(choice);

            Room[] availableRooms =
                    control.searchAvailableRooms(roomType);

            System.out.println();

            if (availableRooms.length == 0) {

                System.out.println(
                        "No available rooms for "
                        + roomType.getDisplayName() + ".");

                return;
            }

            System.out.println(
                    "Available " + roomType.getDisplayName() + " rooms:");

            System.out.println("--------------------------------------");

            for (Room room : availableRooms) {
                System.out.println(
                        "Room " + room.getRoomNumber()
                        + " - " + room.getStatus().getDisplayName());
            }

            System.out.println("--------------------------------------");
            System.out.println(
                    "Total available rooms: "
                    + availableRooms.length);
        }

        /**
         * Displays billing information for a guest.
         */
        private void viewBilling() {

            System.out.println(
                    "========== BILLING DETAILS ==========");

            String confirmationNumber = readConfirmationNumber();

            System.out.println();

            System.out.println(
                    control.getBillingDetails(confirmationNumber));
        }

        /**
         * Displays all guests using BST in-order traversal.
         */
        private void displayAllGuests() {

            System.out.println(
                    "========== ALL GUESTS ==========");

            Guest[] guests = control.getAllGuests();

            if (guests.length == 0) {

                System.out.println(
                        "No guest records found.");

                return;
            }

            System.out.println(
                    "Guests are displayed in confirmation-number order.");

            System.out.println();

            for (Guest guest : guests) {

                System.out.println("--------------------------------------");
                System.out.println(
                        "Confirmation No.: "
                        + guest.getGuestID());

                System.out.println(
                        "Name            : "
                        + guest.getGuestName());

                System.out.println(
                        "Room            : "
                        + guest.getRoomNumber());

                System.out.println(
                        "Room Type       : "
                        + guest.getRoomType());

                System.out.println(
                        "Status          : "
                        + guest.getStatus());
            }

            System.out.println("--------------------------------------");
            System.out.println(
                    "Total guests: " + guests.length);
        }

        /**
         * Updates the status of an existing guest.
         *
         * The front-desk agent can update the guest status
         * to Pending, Confirmed, Checked-In, or Checked-Out.
         */
        private void updateGuestStatus() {

            System.out.println(
                    "========== UPDATE GUEST STATUS ==========");

            String confirmationNumber = readConfirmationNumber();

            Guest guest = control.searchGuest(confirmationNumber);

            if (guest == null) {

                System.out.println(
                        "Guest not found.");

                return;
            }

            System.out.println();
            System.out.println("Guest found!");
            System.out.println("--------------------------------------");
            System.out.println("Guest Name : " + guest.getGuestName());
            System.out.println("Room       : " + guest.getRoomNumber());
            System.out.println("Current Status : " + guest.getStatus());
            System.out.println("--------------------------------------");

            System.out.println("Select new status:");
            System.out.println("1. Pending");
            System.out.println("2. Confirmed");
            System.out.println("3. Checked-In");
            System.out.println("4. Checked-Out");

            int choice = readInteger("Enter status choice: ");

            String newStatus;

            switch (choice) {
                    case 1:
                        newStatus = "Pending";
                        break;

                    case 2:
                        newStatus = "Confirmed";
                        break;

                    case 3:
                        newStatus = "Checked-In";
                        break;

                    case 4:
                        newStatus = "Checked-Out";
                        break;

                    default:
                        System.out.println(
                            "Invalid status choice.");
                        return;
            }    
            guest.setStatus(newStatus);

            System.out.println();
            System.out.println(
                    "Guest status updated successfully.");
            System.out.println(
                    "New Status: " + guest.getStatus());
        }

        /**
         * Deletes a guest record.
         */
        private void deleteGuest() {

            System.out.println(
                    "========== DELETE GUEST ==========");

            String confirmationNumber = readConfirmationNumber();

            Guest guest = control.searchGuest(confirmationNumber);

            if (guest == null) {

                System.out.println(
                        "Guest not found.");

                return;
            }

            System.out.println(
                    "Guest found: " + guest.getGuestName());

            String confirmation = readText(
                    "Enter YES to confirm deletion: ");

            if (confirmation.equalsIgnoreCase("YES")) {

                boolean deleted =
                        control.deleteGuest(confirmationNumber);

                if (deleted) {
                    System.out.println(
                            "Guest deleted successfully.");
                } else {
                    System.out.println(
                            "Unable to delete guest.");
                }

            } else {

                System.out.println(
                        "Deletion cancelled.");
            }
        }

        /**
         * Reads and validates an 8-digit confirmation number.
         *
         * @return valid confirmation number
         */
        private String readConfirmationNumber() {

            while (true) {

                String value =
                        readText("8-digit confirmation number: ");

                if (value.matches("\\d{8}")) {
                    return value;
                }

                System.out.println(
                        "Invalid confirmation number.");
                System.out.println(
                        "It must contain exactly 8 digits.");
            }
        }

        /**
         * Reads a non-empty text value.
         *
         * @param message prompt
         * @return entered text
         */
        private String readText(String message) {

            while (true) {

                System.out.print(message);

                String value = scanner.nextLine().trim();

                if (!value.isEmpty()) {
                    return value;
                }

                System.out.println(
                        "This field cannot be empty.");
            }
        }

        /**
         * Reads an integer from console input.
         *
         * @param message prompt
         * @return integer
         */
        private int readInteger(String message) {

            while (true) {

                System.out.print(message);

                String input = scanner.nextLine().trim();

                try {

                    return Integer.parseInt(input);

                } catch (NumberFormatException exception) {

                    System.out.println(
                            "Please enter a valid number.");
                }
            }
        }
    }


