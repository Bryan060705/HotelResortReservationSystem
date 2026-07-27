/*
 * Author: Bryan Won Chu Ming
 * Stores a staff account used to log in to the hotel system.
 */
package entity;

import java.util.Objects;

public final class Staff {
    private String staffId;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private int moduleNumber;

    // Creates one staff account with an assigned module.
    public Staff(String staffId, String username, String password,
            String fullName, String role, int moduleNumber) {
        setStaffId(staffId);
        setUsername(username);
        setPassword(password);
        setFullName(fullName);
        setRole(role);
        setModuleNumber(moduleNumber);
    }

    // Returns the staff ID.
    public String getStaffId() {
        return staffId;
    }

    // Changes the unique staff ID.
    public void setStaffId(String staffId) {
        this.staffId = requireText(staffId, "Staff ID");
    }

    // Returns the username used during login.
    public String getUsername() {
        return username;
    }

    // Changes the login username.
    public void setUsername(String username) {
        this.username = requireText(username, "Username");
    }

    // Checks whether an entered password matches this account.
    public boolean hasPassword(String enteredPassword) {
        return password.equals(enteredPassword);
    }

    // Changes the login password.
    public void setPassword(String password) {
        this.password = requireText(password, "Password");
    }

    // Returns the staff member's full name.
    public String getFullName() {
        return fullName;
    }

    // Changes the staff member's full name.
    public void setFullName(String fullName) {
        this.fullName = requireText(fullName, "Full name");
    }

    // Returns the staff member's role.
    public String getRole() {
        return role;
    }

    // Changes the staff role.
    public void setRole(String role) {
        this.role = requireText(role, "Role");
    }

    // Returns the module assigned to this staff member.
    public int getModuleNumber() {
        return moduleNumber;
    }

    // Changes the module assigned to this staff account.
    public void setModuleNumber(int moduleNumber) {
        if (moduleNumber < 1) {
            throw new IllegalArgumentException("Module number must be positive.");
        }
        this.moduleNumber = moduleNumber;
    }

    // Checks that a required text value is provided.
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    // Compares staff members by their unique staff ID.
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Staff other)) {
            return false;
        }
        return Objects.equals(staffId, other.staffId);
    }

    // Produces a hash value that matches equals.
    @Override
    public int hashCode() {
        return Objects.hash(staffId);
    }

    // Returns a readable staff description without exposing the password.
    @Override
    public String toString() {
        return "Staff{" + "staffId=" + staffId
                + ", username=" + username
                + ", fullName=" + fullName
                + ", role=" + role
                + ", moduleNumber=" + moduleNumber + '}';
    }
}
