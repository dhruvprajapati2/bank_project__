package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendApprovalEmail(String toEmail,
            String fullName,
            String accountNumber,
            String aadharNumber,
            double openingAmount,
            String pin) {

        try {

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper
                    = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("🎉 Bank Account Approved - PALTINUM RESERVE BANK");

            String htmlContent
                    = "<div style='font-family: Arial; background:#f4f6f9; padding:20px;'>"
                    + "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
                    + "<h2 style='color:#2E86C1; text-align:center;'>National Trust Bank</h2>"
                    + "<hr>"
                    + "<h3 style='color:green;'>Account Approved Successfully 🎉</h3>"
                    + "<p>Dear <b>" + fullName + "</b>,</p>"
                    + "<p>Your bank account has been successfully approved. Below are your account details:</p>"
                    + "<table style='width:100%; border-collapse:collapse;'>"
                    + "<tr><td><b>Account Number:</b></td><td>" + accountNumber + "</td></tr>"
                    + "<tr><td><b>User Name:</b></td><td>" + fullName + "</td></tr>"
                    + "<tr><td><b>Aadhaar Number:</b></td><td>" + aadharNumber + "</td></tr>"
                    + "<tr><td><b>Opening Balance:</b></td><td>₹ " + openingAmount + "</td></tr>"
                    + "<tr><td><b>PIN:</b></td><td>" + pin + "</td></tr>"
                    + "</table>"
                    + "<br>"
                    + "<p style='color:red; font-weight:bold;'>"
                    + "⚠ Please change your PIN after first login."
                    + "</p>"
                    + "<hr>"
                    + "<p style='text-align:center; font-size:12px; color:gray;'>"
                    + "Thank you for choosing National Trust Bank"
                    + "</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    // ✅ Send OTP for Transfer

    public void sendTransferOtp(String toEmail, String otp) {

        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing for OTP");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("OTP for Money Transfer");

        message.setText(
                "Dear Customer,\n\n"
                + "Your OTP for money transfer is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "Do NOT share this OTP with anyone.\n\n"
                + "Regards,\n"
                + "National Trust Bank"
        );

        mailSender.send(message);
    }

    public void sendPinChangeOtp(String toEmail, String otp) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing for PIN OTP");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("OTP for PIN Change");
        message.setText(
                "Dear Customer,\n\n"
                + "Your OTP for PIN change is: " + otp + "\n\n"
                + "This OTP is valid for 5 minutes.\n\n"
                + "If you did not request this, please contact support immediately.\n\n"
                + "Regards,\n"
                + "Platinum Reserve Bank"
        );

        mailSender.send(message);
    }

    // ✅ Send Debit Email (Money sent from account)
    public void sendDebitEmail(String toEmail, String senderName, long fromAccount, long toAccount, double amount, String method) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("💸 Debit Alert - Money Transfer from Your Account");

            String htmlContent
                    = "<div style='font-family: Arial; background:#fff3cd; padding:20px;'>"
                    + "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px; border-left:5px solid #ff6b6b;'>"
                    + "<h2 style='color:#ff6b6b;'>💸 Debit Alert</h2>"
                    + "<p>Dear <b>" + senderName + "</b>,</p>"
                    + "<p>A transfer has been initiated from your account.</p>"
                    + "<table style='width:100%; border-collapse:collapse; margin:15px 0;'>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>From Account:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + fromAccount + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>To Account:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + toAccount + "</td></tr>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>Amount:</b></td><td style='padding:10px; border:1px solid #ddd;'>₹ " + String.format("%.2f", amount) + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>Method:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + method + "</td></tr>"
                    + "</table>"
                    + "<p style='color:red; font-weight:bold;'>⚠ If you did not authorize this transaction, contact us immediately.</p>"
                    + "<hr>"
                    + "<p style='text-align:center; font-size:12px; color:gray;'>National Trust Bank</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    // ✅ Send Credit Email (Money received in account)
    public void sendCreditEmail(String toEmail, String receiverName, long fromAccount, long toAccount, double amount, String method) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("✅ Credit Alert - Money Received in Your Account");

            String htmlContent
                    = "<div style='font-family: Arial; background:#d4edda; padding:20px;'>"
                    + "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px; border-left:5px solid #28a745;'>"
                    + "<h2 style='color:#28a745;'>✅ Credit Alert</h2>"
                    + "<p>Dear <b>" + receiverName + "</b>,</p>"
                    + "<p>You have received money in your account.</p>"
                    + "<table style='width:100%; border-collapse:collapse; margin:15px 0;'>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>From Account:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + fromAccount + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>To Account:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + toAccount + "</td></tr>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>Amount:</b></td><td style='padding:10px; border:1px solid #ddd;'>₹ " + String.format("%.2f", amount) + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>Method:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + method + "</td></tr>"
                    + "</table>"
                    + "<p style='color:green; font-weight:bold;'>✓ Your account has been credited successfully.</p>"
                    + "<hr>"
                    + "<p style='text-align:center; font-size:12px; color:gray;'>National Trust Bank</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendAtmApplicationConfirmationEmail(String toEmail,
            String fullName,
            long accountNumber,
            String mobile,
            String address,
            String cardType,
            String comments) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing for ATM confirmation");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("✅ ATM Card Application Received - Platinum Reserve Bank");

            String htmlContent
                    = "<div style='font-family: Arial; background:#eef2ff; padding:20px;'>"
                    + "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
                    + "<h2 style='color:#0f172a;'>ATM Card Application Received</h2>"
                    + "<p>Dear <b>" + fullName + "</b>,</p>"
                    + "<p>We have received your ATM application request. We will review it and get back to you shortly.</p>"
                    + "<table style='width:100%; border-collapse:collapse; margin:15px 0;'>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>Account Number:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + accountNumber + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>Mobile:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + mobile + "</td></tr>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>Address:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + address + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #ddd;'><b>ATM Card Type:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + cardType + "</td></tr>"
                    + "<tr style='background:#f8f9fa;'><td style='padding:10px; border:1px solid #ddd;'><b>Additional Notes:</b></td><td style='padding:10px; border:1px solid #ddd;'>" + comments + "</td></tr>"
                    + "</table>"
                    + "<p style='color:#0f172a; font-weight:bold;'>Thank you for choosing Platinum Reserve Bank.</p>"
                    + "<hr>"
                    + "<p style='text-align:center; font-size:12px; color:gray;'>If you have questions, contact support@platinumreservebank.com</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendAtmApplicationApprovedEmail(String toEmail,
            String fullName,
            long accountNumber,
            String cardType,
            String atmCardNumber) {
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is missing for ATM approval");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("ATM Card Application Approved - Platinum Reserve Bank");

            String htmlContent
                    = "<div style='font-family: Arial; background:#f0fdf4; padding:20px;'>"
                    + "<div style='max-width:600px; margin:auto; background:white; padding:24px; border-radius:12px; border-top:4px solid #16a34a;'>"
                    + "<h2 style='color:#166534; margin-top:0;'>ATM Card Application Approved</h2>"
                    + "<p>Dear <b>" + fullName + "</b>,</p>"
                    + "<p>Your ATM card request has been approved successfully.</p>"
                    + "<table style='width:100%; border-collapse:collapse; margin:16px 0;'>"
                    + "<tr><td style='padding:10px; border:1px solid #d1d5db;'><b>Account Number</b></td><td style='padding:10px; border:1px solid #d1d5db;'>" + accountNumber + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #d1d5db;'><b>Card Type</b></td><td style='padding:10px; border:1px solid #d1d5db;'>" + cardType + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #d1d5db;'><b>ATM Card Number</b></td><td style='padding:10px; border:1px solid #d1d5db;'>" + atmCardNumber + "</td></tr>"
                    + "<tr><td style='padding:10px; border:1px solid #d1d5db;'><b>Status</b></td><td style='padding:10px; border:1px solid #d1d5db;'>APPROVED</td></tr>"
                    + "</table>"
                    + "<p>Your ATM card will be delivered to your home address within 15 days. Please keep checking your registered email for dispatch or activation updates.</p>"
                    + "<hr>"
                    + "<p style='text-align:center; font-size:12px; color:gray;'>Thank you for banking with Platinum Reserve Bank.</p>"
                    + "</div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
