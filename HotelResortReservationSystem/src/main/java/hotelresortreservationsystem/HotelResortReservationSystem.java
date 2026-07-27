/*
 * Author: Bryan Won Chu Ming
 * Application composition root for the VIP room allocation module.
 */
package hotelresortreservationsystem;

import boundary.HotelSystemUI;
import control.AuthenticationController;
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
        VipRoomAllocationController allocationController
                = new VipRoomAllocationController(sharedHotelData);
        VipReportController reportController
                = new VipReportController(allocationController);

        try (Scanner scanner = new Scanner(System.in)) {
            HotelSystemUI userInterface = new HotelSystemUI(
                    authenticationController, allocationController,
                    reportController, scanner);
            userInterface.run();
        }
    }
}
