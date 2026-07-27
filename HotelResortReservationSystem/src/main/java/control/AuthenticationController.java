/*
 * Author: Bryan Won Chu Ming
 * Checks staff usernames and passwords before the main menu is opened.
 */
package control;

import entity.Staff;

public class AuthenticationController {
    private final Staff[] staffAccounts;

    // Creates the controller and loads the four group member accounts.
    public AuthenticationController() {
        staffAccounts = loadStaffAccounts();
    }

    // Searches for a matching username and password.
    public LoginResult authenticate(String username, String password) {
        for (Staff staff : staffAccounts) {
            if (staff.getUsername().equalsIgnoreCase(username.trim())
                    && staff.hasPassword(password)) {
                return new LoginResult(true, "Login successful.",
                        staff.getStaffId(), staff.getFullName(),
                        staff.getRole(), staff.getModuleNumber());
            }
        }
        return new LoginResult(false, "Invalid username or password.",
                "", "", "", 0);
    }

    // Adds the four project members as simple demonstration accounts.
    private Staff[] loadStaffAccounts() {
        return new Staff[]{
            new Staff("STF001", "tang", "1234",
                    "Tang Hong Yi", "Front Desk Agent", 1),
            new Staff("STF002", "bryan", "1234",
                    "Bryan Won Chu Ming", "VIP Manager", 2),
            new Staff("STF003", "bong", "1234",
                    "Bong Xin Yee", "Housekeeping Lead", 3),
            new Staff("STF004", "carret", "1234",
                    "Carret Chong Kar Loke", "Service Lead", 4)
        };
    }

    // Stores the safe staff information returned to the boundary after login.
    public record LoginResult(boolean success, String message, String staffId,
            String fullName, String role, int moduleNumber) {
    }
}
