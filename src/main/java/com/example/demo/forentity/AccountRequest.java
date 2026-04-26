package com.example.demo.forentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "account_requests",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "adhar_no"),
            @UniqueConstraint(columnNames = "email"),
            @UniqueConstraint(columnNames = "mobile")
        }
)
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false)
    private String dob;

    @Column(name = "adhar_no", nullable = false, unique = true, length = 12)
    private String adharNo;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 10)
    private String mobile;

    @Column(nullable = false, length = 50)
    private String branch;

    @Column(nullable = false, length = 50)
    private String accountType;  // SAVINGS / CURRENT / FIXED_DEPOSIT

    @Column(nullable = false)
    private double balance;

    @Column(nullable = false, length = 4)
    private String pin;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";   // PENDING / APPROVED / REJECTED

    @Column(name = "account_number", unique = true, length = 20)
    private Long accountNumber;

    // ---------- Constructors ----------
    public AccountRequest() {
    }

    // ---------- Getters & Setters ----------
    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getAdharNo() {
        return adharNo;
    }

    public void setAdharNo(String adharNo) {
        this.adharNo = adharNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ---------- Optional (Debugging) ----------
    public Long getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "AccountRequest{"
                + "id=" + id
                + ", name='" + firstName + " " + lastName + '\''
                + ", email='" + email + '\''
                + ", status='" + status + '\''
                + '}';
    }

}
