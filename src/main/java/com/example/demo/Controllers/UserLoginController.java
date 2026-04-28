package com.example.demo.Controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.forentity.Account;
import com.example.demo.forentity.AccountRequest;
import com.example.demo.forentity.AtmApplication;
import com.example.demo.forentity.Transaction;
import com.example.demo.forentity.UserReport;
import com.example.demo.repo.AccountRepo;
import com.example.demo.repo.AccountRequestRepo;
import com.example.demo.repo.AtmApplicationRepo;
import com.example.demo.repo.TransactionRepo;
import com.example.demo.repo.UserReportRepo;
import com.example.demo.services.EmailService;
import com.example.demo.services.StatementPdfService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserLoginController {

    @Autowired
    private AccountRepo AccountRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private AccountRequestRepo repo;

    @Autowired
    private UserReportRepo userReportRepo;

    @Autowired
    private AtmApplicationRepo atmApplicationRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StatementPdfService statementPdfService;

    @GetMapping({"/", "/index"})
    public String home() {
        return "index";
    }

    @GetMapping({"/register", "/register.html"})
    public String register(Model model) {
        if (!model.containsAttribute("accountRequest")) {
            model.addAttribute("accountRequest", new AccountRequest());
        }
        return "register";
    }

    @PostMapping("/registerUser")
    public String saveRequest(@ModelAttribute("accountRequest") AccountRequest req, Model model) {

        normalizeRegistrationRequest(req);

        // Set default branch if not provided
        if (req.getBranch() == null || req.getBranch().isBlank()) {
            req.setBranch("Main Branch");
        }

        String duplicateMessage = findDuplicateRegistrationMessage(req);
        if (duplicateMessage != null) {
            req.setPin(null);
            model.addAttribute("errorMessage", duplicateMessage);
            return "register";
        }

        req.setStatus("PENDING");
        try {
            repo.save(req);
        } catch (DataIntegrityViolationException ex) {
            req.setPin(null);
            model.addAttribute("errorMessage", resolveRegistrationErrorMessage(req));
            return "register";
        }

        model.addAttribute("success", "Your account request was submitted successfully. Please wait for admin approval.");
        return "userlogin";
    }

    private void normalizeRegistrationRequest(AccountRequest req) {
        if (req.getEmail() != null) {
            req.setEmail(req.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (req.getMobile() != null) {
            req.setMobile(req.getMobile().trim());
        }
        if (req.getAdharNo() != null) {
            req.setAdharNo(req.getAdharNo().trim());
        }
        if (req.getBranch() != null) {
            req.setBranch(req.getBranch().trim());
        }
        if (req.getPin() != null) {
            req.setPin(req.getPin().trim());
        }
    }

    private String findDuplicateRegistrationMessage(AccountRequest req) {
        if (req.getAdharNo() != null && !req.getAdharNo().isBlank() && repo.existsByAdharNo(req.getAdharNo())) {
            return "This Aadhar number is already used for an account request.";
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && repo.existsByEmailIgnoreCase(req.getEmail())) {
            return "This email address is already used for an account request.";
        }
        if (req.getMobile() != null && !req.getMobile().isBlank() && repo.existsByMobile(req.getMobile())) {
            return "This mobile number is already used for an account request.";
        }
        return null;
    }

    private String resolveRegistrationErrorMessage(AccountRequest req) {
        String duplicateMessage = findDuplicateRegistrationMessage(req);
        if (duplicateMessage != null) {
            return duplicateMessage;
        }
        return "Unable to submit your request right now. Please review your details and try again.";
    }

    @PostMapping("/userlogin")
    public String doUserLogin(
            @RequestParam String accountNumber,
            @RequestParam String pin,
            HttpSession session,
            Model model) {

        Optional<AccountRequest> user
                = repo.findByAccountNumberAndPinAndStatus(
                        accountNumber, pin, "APPROVED");

        if (user.isPresent()) {

            session.setAttribute("userAcc", accountNumber);

            // 🔥 ADD THIS LINE
            session.setAttribute("userEmail", user.get().getEmail());

            return "redirect:/userdashboard";
        }

        model.addAttribute("error", "Invalid Account Number or PIN");
        return "userlogin";
    }

    @GetMapping("/userdashboard")
    public String userDashboard(HttpSession session, Model model) {
        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userDetails.isEmpty()) {
            session.invalidate();
            return "redirect:/userlogin";
        }

        AccountRequest user = userDetails.get();
        model.addAttribute("user", user);
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        return "userdashboard";
    }

    @GetMapping("/userlogout")
    public String userlogout(HttpSession session) {
        session.invalidate();
        return "redirect:/register";
    }

    @GetMapping("/help")
    public String helpPage(HttpSession session, Model model) {
        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userDetails.isPresent()) {
            model.addAttribute("userName", userDetails.get().getFirstName() + " " + userDetails.get().getLastName());
        }
        return "help";
    }

    @PostMapping("/help/report")
    public String submitHelpReport(@RequestParam String subject,
            @RequestParam String description,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userDetails.isEmpty()) {
            return "redirect:/userdashboard";
        }

        UserReport report = new UserReport();
        report.setAccountNumber(Long.parseLong(accNo));
        report.setReportedBy(userDetails.get().getFirstName() + " " + userDetails.get().getLastName());
        report.setSubject(subject);
        report.setDescription(description);
        report.setStatus("NEW");
        report.setCreatedAt(LocalDateTime.now());

        userReportRepo.save(report);

        model.addAttribute("successMessage", "Your report has been submitted to the admin team.");
        model.addAttribute("userName", userDetails.get().getFirstName() + " " + userDetails.get().getLastName());
        return "help";
    }

    @GetMapping("/about")
    public String aboutPage(HttpSession session, Model model) {
        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        userDetails.ifPresent(user -> model.addAttribute("userName", user.getFirstName() + " " + user.getLastName()));
        return "about";
    }

    @GetMapping("/atmApplication")
    public String atmApplicationPage(HttpSession session, Model model) {
        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userDetails.isEmpty()) {
            return "redirect:/userdashboard";
        }

        AccountRequest user = userDetails.get();
        long accountNumber = Long.parseLong(accNo);
        if (atmApplicationRepo.existsByAccountNumber(accountNumber)) {
            model.addAttribute("errorMessage", "You have already submitted an ATM card request for this account. Multiple ATM applications are not allowed.");
        }

        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("accountNumber", user.getAccountNumber());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mobile", user.getMobile());
        return "atmApplication";
    }

    @PostMapping("/atmApplication")
    public String submitAtmApplication(@RequestParam String address,
            @RequestParam String mobile,
            @RequestParam String cardType,
            @RequestParam(required = false) String comments,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userDetails.isEmpty()) {
            return "redirect:/userdashboard";
        }

        AccountRequest user = userDetails.get();
        long accountNumber = Long.parseLong(accNo);

        if (atmApplicationRepo.existsByAccountNumber(accountNumber)) {
            model.addAttribute("errorMessage", "ATM application already exists for this account.");
            model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
            model.addAttribute("accountNumber", user.getAccountNumber());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("mobile", mobile);
            return "atmApplication";
        }

        AtmApplication atmApplication = new AtmApplication();
        atmApplication.setAccountNumber(accountNumber);
        atmApplication.setUserName(user.getFirstName() + " " + user.getLastName());
        atmApplication.setEmail(user.getEmail());
        atmApplication.setMobile(mobile);
        atmApplication.setAddress(address);
        atmApplication.setCardType(cardType);
        atmApplication.setComments(comments == null ? "Not provided" : comments);
        atmApplication.setStatus("PENDING");
        atmApplication.setAppliedAt(LocalDateTime.now());

        atmApplicationRepo.save(atmApplication);

        String toEmail = (String) session.getAttribute("userEmail");
        if (toEmail == null || toEmail.isBlank()) {
            toEmail = user.getEmail();
        }

        emailService.sendAtmApplicationConfirmationEmail(
                toEmail,
                user.getFirstName() + " " + user.getLastName(),
                accountNumber,
                mobile,
                address,
                cardType,
                comments == null ? "Not provided" : comments
        );

        model.addAttribute("successMessage", "ATM application submitted. A confirmation email has been sent to your email address.");
        model.addAttribute("userName", user.getFirstName() + " " + user.getLastName());
        model.addAttribute("accountNumber", user.getAccountNumber());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("mobile", mobile);
        return "atmApplication";
    }

    @GetMapping("/userlogin")
    public String showLoginPage() {
        return "userlogin";
    }

    @Autowired
    private AccountRepo accountRepo;

    @GetMapping("/enterPin")
    public String enterPin(HttpSession session) {

        if (session.getAttribute("userAcc") == null) {
            return "redirect:/userlogin";
        }

        return "pin";
    }

    @PostMapping("/checkPin")
    public String checkPin(@RequestParam String pin,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long accountNumber = Long.parseLong(accNo);

        Account account = accountRepo.findByAccountNo(accountNumber);

        if (account != null) {

            // Now check PIN from approved request table
            Optional<AccountRequest> user
                    = repo.findByAccountNumberAndPinAndStatus(
                            accNo, pin, "APPROVED");

            if (user.isPresent()) {

                model.addAttribute("balance", account.getBalance());
                model.addAttribute("account", account);
                model.addAttribute("user", user.get());
                return "balance";
            }
        }

        model.addAttribute("error", true);
        return "pin";
    }

    @GetMapping("/depositPage")
    public String depositPage(HttpSession session) {

        if (session.getAttribute("userAcc") == null) {
            return "redirect:/userlogin";
        }

        return "deposit";
    }

    @GetMapping("/withdrawPage")
    public String withdrawPage(HttpSession session) {

        if (session.getAttribute("userAcc") == null) {
            return "redirect:/userlogin";
        }

        return "withdraw";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam double amount,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        Long accountNumber = Long.parseLong(accNo);

        Account account = accountRepo.findByAccountNo(accountNumber);

        if (account != null) {

            account.setBalance(account.getBalance() + amount);
            accountRepo.save(account);

            model.addAttribute("msg",
                    "Deposit Successful! New Balance: ₹ " + account.getBalance());
        }

        return "deposit";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam double amount,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        Long accountNumber = Long.parseLong(accNo);

        Account account = accountRepo.findByAccountNo(accountNumber);

        if (account != null) {

            if (account.getBalance() >= amount) {

                account.setBalance(account.getBalance() - amount);
                accountRepo.save(account);

                model.addAttribute("msg",
                        "Withdraw Successful! New Balance: ₹ " + account.getBalance());

            } else {

                model.addAttribute("msg",
                        "Insufficient Balance!");
            }
        }

        return "withdraw";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long accountNumber = Long.parseLong(accNo);

        // Get account info
        Account account = accountRepo.findByAccountNo(accountNumber);

        // Get full user info from approved request
        Optional<AccountRequest> user
                = repo.findByAccountNumberAndStatus(accNo, "APPROVED");

        if (account != null && user.isPresent()) {

            model.addAttribute("account", account);
            model.addAttribute("user", user.get());
        }

        return "profile";
    }

    @GetMapping("/editProfile")
    public String editProfile(HttpSession session, Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> user
                = repo.findByAccountNumberAndStatus(accNo, "APPROVED");

        if (user.isPresent()) {
            model.addAttribute("user", user.get());
        }

        return "editProfile";
    }

    @PostMapping("/updateProfile")
    public String updateProfile(@ModelAttribute AccountRequest formUser,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userOpt = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userOpt.isEmpty()) {
            return "redirect:/userdashboard";
        }

        AccountRequest existingUser = userOpt.get();

        existingUser.setFirstName(formUser.getFirstName());
        existingUser.setLastName(formUser.getLastName());
        existingUser.setEmail(formUser.getEmail());
        existingUser.setMobile(formUser.getMobile());
        existingUser.setBranch(formUser.getBranch());
        existingUser.setGender(formUser.getGender());
        existingUser.setDob(formUser.getDob());

        try {
            repo.save(existingUser);
            session.setAttribute("userEmail", existingUser.getEmail());
            model.addAttribute("msg", "Profile updated successfully.");
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Unable to update profile. Email/Mobile may already exist.");
        }

        model.addAttribute("user", existingUser);
        return "editProfile";
    }

    @PostMapping("/changePin/sendOtp")
    public String sendPinChangeOtp(@RequestParam String oldPin,
            @RequestParam String newPin,
            @RequestParam String confirmPin,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userOpt = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userOpt.isEmpty()) {
            return "redirect:/userdashboard";
        }

        AccountRequest user = userOpt.get();
        model.addAttribute("user", user);

        if (!Pattern.matches("\\d{4}", newPin)) {
            model.addAttribute("pinError", "New PIN must be exactly 4 digits.");
            return "editProfile";
        }

        if (!newPin.equals(confirmPin)) {
            model.addAttribute("pinError", "New PIN and confirm PIN do not match.");
            return "editProfile";
        }

        if (newPin.equals(oldPin)) {
            model.addAttribute("pinError", "New PIN must be different from old PIN.");
            return "editProfile";
        }

        Optional<AccountRequest> pinMatched = repo.findByAccountNumberAndPinAndStatus(accNo, oldPin, "APPROVED");
        if (pinMatched.isEmpty()) {
            model.addAttribute("pinError", "Old PIN is incorrect.");
            return "editProfile";
        }

        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        session.setAttribute("PIN_CHANGE_OTP", otp);
        session.setAttribute("PIN_CHANGE_OTP_TIME", System.currentTimeMillis());
        session.setAttribute("PIN_CHANGE_NEW_PIN", newPin);

        emailService.sendPinChangeOtp(user.getEmail(), otp);

        model.addAttribute("pinOtpSent", true);
        model.addAttribute("pinMsg", "OTP sent to your registered email.");
        return "editProfile";
    }

    @PostMapping("/changePin/verifyOtp")
    public String verifyPinChangeOtp(@RequestParam String otp,
            HttpSession session,
            Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Optional<AccountRequest> userOpt = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (userOpt.isEmpty()) {
            return "redirect:/userdashboard";
        }

        AccountRequest user = userOpt.get();
        model.addAttribute("user", user);

        String sessionOtp = (String) session.getAttribute("PIN_CHANGE_OTP");
        Long otpTime = (Long) session.getAttribute("PIN_CHANGE_OTP_TIME");
        String pendingNewPin = (String) session.getAttribute("PIN_CHANGE_NEW_PIN");

        if (sessionOtp == null || otpTime == null || pendingNewPin == null) {
            model.addAttribute("pinError", "OTP session expired. Please request OTP again.");
            return "editProfile";
        }

        if (System.currentTimeMillis() - otpTime > 5 * 60 * 1000) {
            session.removeAttribute("PIN_CHANGE_OTP");
            session.removeAttribute("PIN_CHANGE_OTP_TIME");
            session.removeAttribute("PIN_CHANGE_NEW_PIN");
            model.addAttribute("pinError", "OTP expired. Please request a new OTP.");
            return "editProfile";
        }

        if (!sessionOtp.equals(otp)) {
            model.addAttribute("pinOtpSent", true);
            model.addAttribute("pinError", "Invalid OTP.");
            return "editProfile";
        }

        user.setPin(pendingNewPin);
        repo.save(user);

        session.removeAttribute("PIN_CHANGE_OTP");
        session.removeAttribute("PIN_CHANGE_OTP_TIME");
        session.removeAttribute("PIN_CHANGE_NEW_PIN");

        model.addAttribute("pinMsg", "PIN changed successfully.");
        return "editProfile";
    }

    @GetMapping("/transferPage")
    public String transferPage(HttpSession session, Model model) {

        String accNo = (String) session.getAttribute("userAcc");

        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long accountNumber = Long.parseLong(accNo);
        Account account = accountRepo.findByAccountNo(accountNumber);

        model.addAttribute("balance", account.getBalance());

        return "transfer";
    }

    @GetMapping("/getAccountName")
    @ResponseBody
    public String getAccountName(@RequestParam Long accountNo) {
        Account acc = accountRepo.findByAccountNo(accountNo);
        if (acc != null) {
            return acc.getUserName();   // make sure Account has getName()
        }
        return "NOT_FOUND";
    }

    // ✅ User Transactions
    @GetMapping("/myTransactions")
    public String myTransactions(HttpSession session, Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long accountNumber = Long.parseLong(accNo);
        List<Transaction> transactions = transactionRepo.findByAccountNoOrderByDateDesc(accountNumber);

        model.addAttribute("transactions", transactions);
        long sentCount = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().toUpperCase().contains("SENT"))
                .count();
        long receivedCount = transactions.stream()
                .filter(t -> t.getType() != null && t.getType().toUpperCase().contains("RECEIVED"))
                .count();
        model.addAttribute("sentCount", sentCount);
        model.addAttribute("receivedCount", receivedCount);
        model.addAttribute("accountNumber", accountNumber);

        return "myTransactions";
    }

    // ✅ Chatbot
    @GetMapping("/chatbot")
    public String chatbot(HttpSession session, Model model) {

        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return "redirect:/userlogin";
        }

        Long accountNumber = Long.parseLong(accNo);
        Account account = accountRepo.findByAccountNo(accountNumber);

        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");

        if (account != null && userDetails.isPresent()) {
            model.addAttribute("account", account);
            model.addAttribute("user", userDetails.get());
        }

        return "chatbot";
    }

    // ✅ Chatbot API Response
    @PostMapping("/chatbot/response")
    @ResponseBody
    public ChatbotReply getChatbotResponse(@RequestParam String message, HttpSession session) {

        ChatbotContext context = resolveChatbotContext(session);
        if (context == null) {
            return new ChatbotReply("Please login first to use the banking assistant.");
        }

        String normalizedMessage = normalizeMessage(message);

        if (normalizedMessage.isBlank()) {
            return new ChatbotReply("Please type your question. You can ask about balance, profile, transfers, recent transactions, or statement PDF.");
        }

        if (isDayBasedReportRequest(normalizedMessage)) {
            return new ChatbotReply("Sorry, I only give month-based, 1 year, or custom date range reports. Day-based reports like 10 days are not supported.");
        }

        if (isMultiYearReportRequest(normalizedMessage)) {
            return new ChatbotReply("Sorry, I can provide only up to 1 year report. You can ask for any number of months, 1 year, or a custom date range.");
        }

        StatementRequest statementRequest = parseStatementRequest(normalizedMessage);
        if (statementRequest != null) {
            return new ChatbotReply(
                    "I prepared your " + statementRequest.label() + " account statement. Tap the button below to download the PDF.",
                    statementRequest.downloadUrl(),
                    "Download " + statementRequest.label() + " PDF");
        }

        if (normalizedMessage.contains("balance") || normalizedMessage.contains("available amount")) {
            return new ChatbotReply("Your current available balance is Rs. " + String.format("%.2f", context.account().getBalance()) + ".");
        }

        if (normalizedMessage.contains("account number") || normalizedMessage.contains("my account")) {
            return new ChatbotReply("Your account number is " + context.account().getAccountNo() + ".");
        }

        if (normalizedMessage.contains("name") || normalizedMessage.contains("profile") || normalizedMessage.contains("email")
                || normalizedMessage.contains("mobile") || normalizedMessage.contains("branch")) {
            AccountRequest user = context.user();
            return new ChatbotReply("Profile details:\n"
                    + "Name: " + user.getFirstName() + " " + user.getLastName() + "\n"
                    + "Email: " + user.getEmail() + "\n"
                    + "Mobile: " + user.getMobile() + "\n"
                    + "Branch: " + user.getBranch() + "\n"
                    + "Account Type: " + user.getAccountType());
        }

        if (normalizedMessage.contains("last transaction") || normalizedMessage.contains("recent transaction")) {
            if (context.transactions().isEmpty()) {
                return new ChatbotReply("You do not have any transaction history yet.");
            }
            Transaction latest = context.transactions().get(0);
            return new ChatbotReply("Your latest transaction was "
                    + blankSafe(latest.getType()) + " of Rs. " + String.format("%.2f", latest.getAmount())
                    + " on " + formatDateTime(latest.getDate())
                    + ". Method: " + blankSafe(latest.getTransferMethod())
                    + ". Remark: " + blankSafe(latest.getNote()) + ".");
        }

        if (normalizedMessage.contains("transaction") || normalizedMessage.contains("statement") || normalizedMessage.contains("report")) {
            return buildTransactionSummaryReply(context, normalizedMessage);
        }

        if (normalizedMessage.contains("transfer") || normalizedMessage.contains("send money") || normalizedMessage.contains("neft")
                || normalizedMessage.contains("rtgs")) {
            return new ChatbotReply("To transfer money, open the Transfer page, enter the beneficiary account number, amount, choose NEFT or RTGS, and confirm using the OTP sent to your registered email.");
        }

        if (normalizedMessage.contains("hello") || normalizedMessage.contains("hi") || normalizedMessage.contains("hey")) {
            return new ChatbotReply("Hello " + context.user().getFirstName() + ". I can help with balance, profile details, recent transactions, transfer guidance, and statement PDF downloads.");
        }

        if (normalizedMessage.contains("help") || normalizedMessage.contains("what can you do")) {
            return new ChatbotReply("You can ask me things like:\n"
                    + "1. What is my balance?\n"
                    + "2. Show my recent transactions\n"
                    + "3. Download my 6 month statement\n"
                    + "4. Download my 1 year statement\n"
                    + "5. Download statement from 2026-01-01 to 2026-03-31\n"
                    + "6. What is my account number?");
        }

        return new ChatbotReply("I can help with balance, account details, profile info, recent transactions, transfer guidance, and statement PDF downloads. Try asking: Download my 3 month statement or Download statement from 2026-01-01 to 2026-03-31.");
    }

    @GetMapping("/chatbot/statement/pdf")
    public ResponseEntity<byte[]> downloadChatbotStatement(
            @RequestParam(required = false) Integer months,
            @RequestParam(required = false) Integer years,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            HttpSession session) {
        ChatbotContext context = resolveChatbotContext(session);
        if (context == null) {
            return ResponseEntity.status(401).build();
        }

        StatementPeriod period = resolveStatementPeriod(months, years, fromDate, toDate);
        if (period == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Transaction> transactions = transactionRepo.findByAccountNoAndDateBetweenOrderByDateDesc(
                context.account().getAccountNo(), period.fromDate().atStartOfDay(), period.toDate().atTime(23, 59, 59));

        byte[] pdfBytes = statementPdfService.generateMonthlyStatement(
                context.user(),
                context.account(),
                transactions,
                period.label(),
                period.fromDate(),
                period.toDate());

        String filename = "statement-" + context.account().getAccountNo() + "-" + period.fileLabel() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private ChatbotReply buildTransactionSummaryReply(ChatbotContext context, String normalizedMessage) {
        List<Transaction> transactions = context.transactions();
        if (transactions.isEmpty()) {
            return new ChatbotReply("You do not have any transaction history yet. Once you start transferring funds, I can summarize it here.");
        }

        int months = detectSummaryMonths(normalizedMessage);
        LocalDateTime rangeStart = LocalDate.now().minusMonths(months).plusDays(1).atStartOfDay();
        List<Transaction> filtered = transactions.stream()
                .filter(transaction -> transaction.getDate() != null && !transaction.getDate().isBefore(rangeStart))
                .toList();

        long sentCount = filtered.stream().filter(transaction -> containsIgnoreCase(transaction.getType(), "sent")).count();
        long receivedCount = filtered.stream().filter(transaction -> containsIgnoreCase(transaction.getType(), "received")).count();
        long neftCount = filtered.stream().filter(transaction -> containsIgnoreCase(transaction.getTransferMethod(), "neft")).count();
        long rtgsCount = filtered.stream().filter(transaction -> containsIgnoreCase(transaction.getTransferMethod(), "rtgs")).count();
        double totalAmount = filtered.stream().mapToDouble(Transaction::getAmount).sum();

        List<String> lines = new ArrayList<>();
        lines.add("Transaction summary for the last " + months + " month(s):");
        lines.add("Total transactions: " + filtered.size());
        lines.add("Sent: " + sentCount + " | Received: " + receivedCount);
        lines.add("NEFT: " + neftCount + " | RTGS: " + rtgsCount);
        lines.add("Total volume: Rs. " + String.format("%.2f", totalAmount));

        if (!filtered.isEmpty()) {
            Transaction latest = filtered.get(0);
            lines.add("Latest: " + blankSafe(latest.getType()) + " of Rs. " + String.format("%.2f", latest.getAmount())
                    + " on " + formatDateTime(latest.getDate()));
        }

        String periodLabel = months == 12 ? "1 year" : months + " month";
        String downloadLabel = "Download " + periodLabel + " statement PDF";
        String downloadUrl = months == 12 ? "/chatbot/statement/pdf?years=1" : "/chatbot/statement/pdf?months=" + months;
        return new ChatbotReply(String.join("\n", lines), downloadUrl, downloadLabel);
    }

    private ChatbotContext resolveChatbotContext(HttpSession session) {
        String accNo = (String) session.getAttribute("userAcc");
        if (accNo == null) {
            return null;
        }

        Long accountNumber = Long.parseLong(accNo);
        Account account = accountRepo.findByAccountNo(accountNumber);
        Optional<AccountRequest> userDetails = repo.findByAccountNumberAndStatus(accNo, "APPROVED");
        if (account == null || userDetails.isEmpty()) {
            return null;
        }

        List<Transaction> transactions = transactionRepo.findByAccountNoOrderByDateDesc(accountNumber);
        return new ChatbotContext(account, userDetails.get(), transactions);
    }

    private StatementRequest parseStatementRequest(String message) {
        if (!(message.contains("pdf") || message.contains("download")) || !(message.contains("statement") || message.contains("transaction report") || message.contains("transaction"))) {
            return null;
        }

        DateRangeRequest customRange = parseCustomRange(message);
        if (customRange != null) {
            return new StatementRequest(
                    "custom range",
                    "/chatbot/statement/pdf?fromDate=" + customRange.fromDate() + "&toDate=" + customRange.toDate());
        }

        if (message.contains("year")) {
            return new StatementRequest("1 year", "/chatbot/statement/pdf?years=1");
        }

        int months = detectSummaryMonths(message);
        return new StatementRequest(months + " month", "/chatbot/statement/pdf?months=" + months);
    }

    private int detectSummaryMonths(String message) {
        int explicitMonths = extractLeadingNumberBeforeKeyword(message, "month");
        if (explicitMonths > 0) {
            return explicitMonths;
        }
        if (message.contains("6 month") || message.contains("six month")) {
            return 6;
        }
        if (message.contains("3 month") || message.contains("three month")) {
            return 3;
        }
        if (message.contains("12 month") || message.contains("one year") || message.contains("year")) {
            return 12;
        }
        return 1;
    }

    private boolean isDayBasedReportRequest(String message) {
        return (message.contains("day") || message.contains("days"))
                && (message.contains("statement") || message.contains("report") || message.contains("transaction"));
    }

    private boolean isMultiYearReportRequest(String message) {
        return (message.contains("2 year") || message.contains("3 year") || message.contains("4 year") || message.contains("5 year")
                || message.contains("two year") || message.contains("three year") || message.contains("four year") || message.contains("five year"))
                && (message.contains("statement") || message.contains("report") || message.contains("transaction"));
    }

    private int extractLeadingNumberBeforeKeyword(String message, String keyword) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s+" + keyword).matcher(message);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    private DateRangeRequest parseCustomRange(String message) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d{4}-\\d{2}-\\d{2})\\s+(?:to|and)\\s+(\\d{4}-\\d{2}-\\d{2})")
                .matcher(message);
        if (!matcher.find()) {
            return null;
        }

        LocalDate from = LocalDate.parse(matcher.group(1));
        LocalDate to = LocalDate.parse(matcher.group(2));
        if (to.isBefore(from)) {
            return null;
        }

        return new DateRangeRequest(from, to);
    }

    private StatementPeriod resolveStatementPeriod(Integer months, Integer years, String fromDate, String toDate) {
        if (fromDate != null && toDate != null && !fromDate.isBlank() && !toDate.isBlank()) {
            LocalDate customFrom = LocalDate.parse(fromDate);
            LocalDate customTo = LocalDate.parse(toDate);
            if (customTo.isBefore(customFrom)) {
                return null;
            }
            return new StatementPeriod(customFrom, customTo, "custom range", customFrom + "_to_" + customTo);
        }

        if (years != null) {
            int safeYears = years == 1 ? 1 : 1;
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusYears(safeYears).plusDays(1);
            return new StatementPeriod(startDate, endDate, "1 year", "1-year");
        }

        int safeMonths = months == null ? 1 : Math.max(1, months);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(safeMonths).plusDays(1);
        return new StatementPeriod(startDate, endDate, safeMonths + " month", safeMonths + "-month");
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ").trim();
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && source.toLowerCase(Locale.ENGLISH).contains(target.toLowerCase(Locale.ENGLISH));
    }

    private String blankSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    public static class ChatbotReply {

        private final String message;
        private final String downloadUrl;
        private final String downloadLabel;

        public ChatbotReply(String message) {
            this(message, null, null);
        }

        public ChatbotReply(String message, String downloadUrl, String downloadLabel) {
            this.message = message;
            this.downloadUrl = downloadUrl;
            this.downloadLabel = downloadLabel;
        }

        public String getMessage() {
            return message;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getDownloadLabel() {
            return downloadLabel;
        }
    }

    private record ChatbotContext(Account account, AccountRequest user, List<Transaction> transactions) {

    }

    private record StatementRequest(String label, String downloadUrl) {

    }

    private record DateRangeRequest(LocalDate fromDate, LocalDate toDate) {

    }

    private record StatementPeriod(LocalDate fromDate, LocalDate toDate, String label, String fileLabel) {

    }

}
