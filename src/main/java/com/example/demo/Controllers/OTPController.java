package com.example.demo.Controllers;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.forentity.Account;
import com.example.demo.forentity.Transaction;
import com.example.demo.repo.AccountRepo;
import com.example.demo.repo.AccountRequestRepo;
import com.example.demo.repo.TransactionRepo;
import com.example.demo.services.EmailService;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class OTPController {

    @Autowired
    private AccountRequestRepo accountRequestRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private EmailService emailService;

    OTPController(TransactionRepo transactionRepo) {
        this.transactionRepo = transactionRepo;
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam Long toAccount,
            @RequestParam double amount,
            @RequestParam String transferType,
            @RequestParam(required = false) String note,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long fromAccount = Long.parseLong(accNo);

        Account sender = accountRepo.findByAccountNo(fromAccount);
        Account receiver = accountRepo.findByAccountNo(toAccount);

        if (receiver == null) {
            model.addAttribute("msg", "Receiver account not found");
            return "transfer";
        }

        if (fromAccount.equals(toAccount)) {
            model.addAttribute("msg", "Cannot transfer to same account");
            return "transfer";
        }

        if ("RTGS".equals(transferType) && amount < 200000) {
            model.addAttribute("msg", "RTGS requires minimum ₹2,00,000");
            return "transfer";
        }

        if (sender.getBalance() < amount) {
            model.addAttribute("msg", "Insufficient Balance");
            return "transfer";
        }

        // 🔐 Generate OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // 🔐 Store details in SESSION
        session.setAttribute("OTP", otp);
        session.setAttribute("OTP_TIME", System.currentTimeMillis());
        session.setAttribute("TO_ACCOUNT", toAccount);
        session.setAttribute("AMOUNT", amount);
        session.setAttribute("TRANSFER_TYPE", transferType);
        session.setAttribute("NOTE", note);

        // ✅ Get email safely (session recommended)
        String email = (String) session.getAttribute("userEmail");
        if (email == null) {
            model.addAttribute("msg", "Registered email not found");
            return "transfer";
        }

        emailService.sendTransferOtp(email, otp);

        model.addAttribute("showOtp", true);
        model.addAttribute("msg", "OTP sent to your registered email");

        return "transfer";
    }

    @Transactional
    @PostMapping("/verifyOtp")
    public String verifyOtp(@RequestParam String otp,
            HttpSession session,
            Model model) {

        String sessionOtp = (String) session.getAttribute("OTP");
        Long otpTime = (Long) session.getAttribute("OTP_TIME");

        if (sessionOtp == null || otpTime == null) {
            model.addAttribute("msg", "OTP expired");
            return "transfer";
        }

        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            model.addAttribute("msg", "OTP expired");
            return "transfer";
        }

        if (!otp.equals(sessionOtp)) {
            model.addAttribute("showOtp", true);
            model.addAttribute("msg", "Invalid OTP");
            return "transfer";
        }

        // ✅ OTP VERIFIED – PERFORM TRANSFER
        Long fromAccount = Long.parseLong((String) session.getAttribute("userAcc"));
        Long toAccount = (Long) session.getAttribute("TO_ACCOUNT");
        double amount = (double) session.getAttribute("AMOUNT");
        String transferType = (String) session.getAttribute("TRANSFER_TYPE");
        String note = (String) session.getAttribute("NOTE");

        Account sender = accountRepo.findByAccountNo(fromAccount);
        Account receiver = accountRepo.findByAccountNo(toAccount);

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        accountRepo.save(sender);
        accountRepo.save(receiver);

        // 🧾 Transaction history
        Transaction sent = new Transaction();
        sent.setAccountNo(fromAccount);
        sent.setFromAccount(fromAccount);
        sent.setToAccount(toAccount);
        sent.setType("TRANSFER SENT");
        sent.setAmount(amount);
        sent.setTransferMethod(transferType);
        sent.setNote(note);
        sent.setDate(LocalDateTime.now());
        transactionRepo.save(sent);

        Transaction received = new Transaction();
        received.setAccountNo(toAccount);
        received.setFromAccount(fromAccount);
        received.setToAccount(toAccount);
        received.setType("TRANSFER RECEIVED");
        received.setAmount(amount);
        received.setTransferMethod(transferType);
        received.setNote(note);
        received.setDate(LocalDateTime.now());
        transactionRepo.save(received);

        // 📧 Send Email Notifications
        String senderEmail = (String) session.getAttribute("userEmail");

        // Get receiver email
        var receiverAccount = accountRequestRepo.findByAccountNumber(toAccount);
        if (receiverAccount.isPresent()) {
            String receiverEmail = receiverAccount.get().getEmail();
            String senderName = (String) session.getAttribute("userAcc");
            String receiverName = receiverAccount.get().getFirstName() + " " + receiverAccount.get().getLastName();

            // Send debit email to sender
            emailService.sendDebitEmail(senderEmail, senderName, fromAccount, toAccount, amount, transferType);

            // Send credit email to receiver
            emailService.sendCreditEmail(receiverEmail, receiverName, fromAccount, toAccount, amount, transferType);
        }

        // 🧹 Clear OTP session
        session.removeAttribute("OTP");
        session.removeAttribute("OTP_TIME");
        session.removeAttribute("TO_ACCOUNT");
        session.removeAttribute("AMOUNT");
        session.removeAttribute("TRANSFER_TYPE");
        session.removeAttribute("NOTE");

        model.addAttribute("msg", "✅ Money transferred successfully via " + transferType);

        return "transfer";
    }
}
