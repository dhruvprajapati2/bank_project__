package com.example.demo.forentity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String pin;   // store encrypted PIN (recommended)

    @Column(nullable = false)
    private String status = "PENDING";  // PENDING / APPROVED / REJECTED

    // Optional (Recommended for bank project)
    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    // Default Constructor
    public User() {
    }

    // Parameterized Constructor
    public User(String fullName, String email, String accountNumber, String pin) {
        this.fullName = fullName;
        this.email = email;
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.status = "PENDING";
        this.balance = BigDecimal.ZERO;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
