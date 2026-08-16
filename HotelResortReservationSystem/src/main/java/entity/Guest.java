/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entity;

import java.time.LocalDateTime;

/**
 *
 * @author user
 */

public class Guest implements Comparable<Guest>{

    private String guestID;
    private String guestName;
    private String phoneNumber;
    private LocalDateTime bookingDate;
    private String roomType;
    private String status;
    private String roomNumber;

    public Guest() {
    }

    public Guest(String guestID, String guestName, String phoneNumber,
            LocalDateTime bookingDate, String roomType, String status, String roomNumber) {
        this.guestID = guestID;
        this.guestName = guestName;
        this.phoneNumber = phoneNumber;
        this.bookingDate = bookingDate;
        this.roomType = roomType;
        this.status = status;
        this.roomNumber = roomNumber;
    }

    public String getGuestID() {
        return guestID;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getStatus() {
        return status;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setGuestID(String guestID) {
        this.guestID = guestID;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    @Override
    public int compareTo(Guest other) {
        return this.guestID.compareTo(other.guestID);
    }
    
    @Override
    public String toString() {
        return String.format(
                "Guest ID: %s\n"
                + "Name: %s\n"
                + "Phone: %s\n"
                + "Booking Date: %s\n"
                + "Room Type: %s\n"
                + "Status: %s\n"
                + "Room Number: %s",
                guestID,
                guestName,
                phoneNumber,
                bookingDate,
                roomType,
                status,
                roomNumber
        );
    }

}
