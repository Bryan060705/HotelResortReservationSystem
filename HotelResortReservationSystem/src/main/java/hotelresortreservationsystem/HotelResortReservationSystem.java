/*
 * Author: Bryan Won Chu Ming
 * Application composition root for the VIP room allocation module.
 */
package hotelresortreservationsystem;

import boundary.BookingUI;
import boundary.FrontDeskServiceBoundary;
import boundary.HotelSystemUI;
import boundary.HousekeepingUI;
import control.AuthenticationController;
import control.BookingControl;
import control.HousekeepingController;
import control.HousekeepingReportController;
import control.VipReportController;
import control.VipRoomAllocationController;
import entity.HotelDataStore;
import java.util.Scanner;

public class HotelResortReservationSystem {

    // Creates the control and boundary objects, then starts the console program.
    public static void main(String[] args) {
        AuthenticationController authenticationController
                = new AuthenticationController();
        HotelDataStore sharedHotelData = new HotelDataStore();

        HousekeepingController housekeepingController
                = new HousekeepingController(sharedHotelData);
        HousekeepingReportController housekeepingReportController
                = new HousekeepingReportController(housekeepingController);

        VipRoomAllocationController allocationController
                = new VipRoomAllocationController(sharedHotelData, housekeepingController);
        VipReportController reportController
                = new VipReportController(allocationController);

        BookingControl bookingControl
                = new BookingControl(sharedHotelData, housekeepingController);

        try (Scanner scanner = new Scanner(System.in)) {
            BookingUI bookingUI
                    = new BookingUI(bookingControl, scanner);

            HousekeepingUI housekeepingUI
                    = new HousekeepingUI(housekeepingController, housekeepingReportController, scanner);

            FrontDeskServiceBoundary frontDeskServiceBoundary
                    = new FrontDeskServiceBoundary(sharedHotelData, scanner);

            HotelSystemUI userInterface = new HotelSystemUI(
                    authenticationController, allocationController,
                    reportController, bookingUI, housekeepingUI, frontDeskServiceBoundary, scanner);

            userInterface.run();
        }
    }
}
