package com.example.demo.Controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.forentity.Account;
import com.example.demo.forentity.AccountRequest;
import com.example.demo.forentity.Admin;
import com.example.demo.forentity.AtmApplication;
import com.example.demo.forentity.Transaction;
import com.example.demo.forentity.UserReport;
import com.example.demo.repo.AccountRepo;
import com.example.demo.repo.AccountRequestRepo;
import com.example.demo.repo.Adminrepo;
import com.example.demo.repo.AtmApplicationRepo;
import com.example.demo.repo.TransactionRepo;
import com.example.demo.repo.UserReportRepo;
import com.example.demo.services.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminLoginController {

    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy");

    @Autowired
    private Adminrepo adminRepo;

    @Autowired
    private AccountRequestRepo requestRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private UserReportRepo userReportRepo;

    @Autowired
    private AtmApplicationRepo atmApplicationRepo;

    @Autowired
    private EmailService emailService;

    // ================= ADMIN LOGIN =================
    @GetMapping("/admin")
    public String login() {
        return "adminlogin";
    }

    @PostMapping("/adminlogin")
    public String doLogin(@RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Optional<Admin> user = adminRepo.findByEmail(email);

        if (user.isPresent() && user.get().getPassword().equals(password)) {
            session.setAttribute("adminEmail", email);
            return "redirect:/admindashboard";
        }

        model.addAttribute("error", "Invalid Email or Password");
        return "adminlogin";
    }

    // ================= DASHBOARD =================
    @GetMapping("/admindashboard")
    public String dashboard(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        // Get total users (from AccountRequest table)
        long totalUsers = requestRepo.count();

        // Get total accounts
        long totalAccounts = accountRepo.count();

        // Get total balance from all accounts
        double totalBalance = accountRepo.getTotalBalance();

        // Get today's transactions count
        long todayTransactions = transactionRepo.countTodayTransactions();

        // Get pending requests
        long pendingRequests = requestRepo.countByStatus("PENDING");

        // Get blocked/rejected users
        long blockedUsers = requestRepo.countByStatus("REJECTED");

        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.atTime(23, 59, 59);
        List<Transaction> currentMonthTransactions = transactionRepo.findTransactionsByDateRange(monthStart, monthEnd);
        Map<Long, String> accountNames = buildAccountNameMap();

        // Last 7 days transactions trend
        LocalDate startDate = today.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = today.atTime(23, 59, 59);

        List<Transaction> recentTransactions = transactionRepo.findTransactionsByDateRange(startDateTime, endDateTime);
        Map<LocalDate, Long> dailyCounts = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            dailyCounts.put(startDate.plusDays(i), 0L);
        }

        for (Transaction transaction : recentTransactions) {
            LocalDate txDate = transaction.getDate().toLocalDate();
            if (dailyCounts.containsKey(txDate)) {
                dailyCounts.put(txDate, dailyCounts.get(txDate) + 1);
            }
        }

        DateTimeFormatter chartDateFormat = DateTimeFormatter.ofPattern("dd MMM");
        List<String> lineChartLabels = new ArrayList<>();
        List<Long> lineChartValues = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : dailyCounts.entrySet()) {
            lineChartLabels.add(entry.getKey().format(chartDateFormat));
            lineChartValues.add(entry.getValue());
        }

        // Transfer method distribution
        Map<String, Long> transferMethodCount = new LinkedHashMap<>();
        transferMethodCount.put("NEFT", 0L);
        transferMethodCount.put("RTGS", 0L);
        transferMethodCount.put("Other", 0L);

        for (Transaction transaction : recentTransactions) {
            String method = transaction.getTransferMethod() == null ? "" : transaction.getTransferMethod().trim().toUpperCase();
            if ("NEFT".equals(method)) {
                transferMethodCount.put("NEFT", transferMethodCount.get("NEFT") + 1);
            } else if ("RTGS".equals(method)) {
                transferMethodCount.put("RTGS", transferMethodCount.get("RTGS") + 1);
            } else {
                transferMethodCount.put("Other", transferMethodCount.get("Other") + 1);
            }
        }

        List<String> pieChartLabels = new ArrayList<>(transferMethodCount.keySet());
        List<Long> pieChartValues = new ArrayList<>(transferMethodCount.values());

        long monthlyTransactions = currentMonthTransactions.size();
        double monthlyVolume = 0;
        long neftTransactions = 0;
        long rtgsTransactions = 0;
        double highestTransactionAmount = 0;

        for (Transaction transaction : currentMonthTransactions) {
            monthlyVolume += transaction.getAmount();
            highestTransactionAmount = Math.max(highestTransactionAmount, transaction.getAmount());

            String method = transaction.getTransferMethod() == null ? "" : transaction.getTransferMethod().trim().toUpperCase();
            if ("NEFT".equals(method)) {
                neftTransactions++;
            } else if ("RTGS".equals(method)) {
                rtgsTransactions++;
            }
        }

        List<Transaction> latestTransactions = transactionRepo.findAll(Sort.by(Sort.Direction.DESC, "date"));
        List<DashboardTransactionView> recentTransactionsView = latestTransactions.stream()
                .limit(5)
                .map(transaction -> toDashboardTransactionView(transaction, accountNames))
                .toList();

        long userReportsCount = userReportRepo.count();
        long atmApplicationCount = atmApplicationRepo.count();
        List<UserReport> latestReports = userReportRepo.findAllByOrderByCreatedAtDesc()
                .stream().limit(5).toList();

        // Add all attributes to model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalBalance", String.format("%.2f", totalBalance));
        model.addAttribute("todayTransactions", todayTransactions);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("blockedUsers", blockedUsers);
        model.addAttribute("totalRequests", requestRepo.count());
        model.addAttribute("totalTransactions", transactionRepo.count());
        model.addAttribute("userReportsCount", userReportsCount);
        model.addAttribute("atmApplicationCount", atmApplicationCount);
        model.addAttribute("latestReports", latestReports);
        model.addAttribute("lineChartLabels", lineChartLabels);
        model.addAttribute("lineChartValues", lineChartValues);
        model.addAttribute("pieChartLabels", pieChartLabels);
        model.addAttribute("pieChartValues", pieChartValues);
        model.addAttribute("monthlyTransactions", monthlyTransactions);
        model.addAttribute("monthlyVolume", String.format("%.2f", monthlyVolume));
        model.addAttribute("neftTransactions", neftTransactions);
        model.addAttribute("rtgsTransactions", rtgsTransactions);
        model.addAttribute("highestTransactionAmount", String.format("%.2f", highestTransactionAmount));
        model.addAttribute("recentTransactions", recentTransactionsView);

        return "admindashboard";
    }

    // ================= VIEW USERS =================
    @GetMapping("/adminuser")
    public String adminUsers(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        model.addAttribute("users", requestRepo.findAll());
        return "adminuser";
    }

    // ================= VIEW ACCOUNTS =================
    @GetMapping("/adminaccount")
    public String accounts(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        model.addAttribute("accounts", accountRepo.findAll());
        return "adminaccount";
    }

    // ================= VIEW REQUESTS =================
    @GetMapping("/adminRequest")
    public String requests(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        model.addAttribute("requests", requestRepo.findAll());
        return "adminRequest";
    }

    // ================= VIEW ATM APPLICATIONS =================
    @GetMapping("/atmApplications")
    public String viewAtmApplications(HttpSession session,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error,
            Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);
        model.addAttribute("applications", atmApplicationRepo.findAllByOrderByAppliedAtDesc());
        return "atmApplications";
    }

    @PostMapping("/approveAtmApplication")
    public String approveAtmApplication(@RequestParam Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        Optional<AtmApplication> opt = atmApplicationRepo.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addAttribute("error", "ATM application not found.");
            return "redirect:/atmApplications";
        }

        AtmApplication application = opt.get();
        if ("APPROVED".equalsIgnoreCase(application.getStatus())) {
            redirectAttributes.addAttribute("success", "ATM application already approved.");
            return "redirect:/atmApplications";
        }

        application.setStatus("APPROVED");
        if (application.getAtmCardNumber() == null || application.getAtmCardNumber().isBlank()) {
            application.setAtmCardNumber(generateUniqueAtmCardNumber());
        }
        atmApplicationRepo.save(application);

        emailService.sendAtmApplicationApprovedEmail(
                application.getEmail(),
                application.getUserName(),
                application.getAccountNumber(),
                application.getCardType(),
                application.getAtmCardNumber());

        redirectAttributes.addAttribute("success", "ATM application approved and email sent successfully.");
        return "redirect:/atmApplications";
    }

    // ================= APPROVE REQUEST =================
    @PostMapping("/approveRequest")
    public String approveRequest(@RequestParam int id,
            HttpSession session) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        Optional<AccountRequest> opt = requestRepo.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/adminRequest";
        }

        AccountRequest r = opt.get();

        // Generate 10-digit unique Account Number
        Random random = new Random();
        Long accNo = 1000000000L + random.nextInt(900000000);

        // Create Account
        Account acc = new Account();
        acc.setAccountNo(accNo);
        acc.setUserName(r.getFirstName() + " " + r.getLastName());
        acc.setBalance(r.getBalance());

        accountRepo.save(acc);

        // Update Request
        r.setStatus("APPROVED");
        r.setAccountNumber(accNo);
        requestRepo.save(r);

        // Send Email
        emailService.sendApprovalEmail(
                r.getEmail(),
                r.getFirstName() + " " + r.getLastName(),
                String.valueOf(accNo),
                r.getAdharNo(),
                r.getBalance(),
                r.getPin()
        );

        return "redirect:/adminRequest";
    }

    // ================= VIEW ALL TRANSACTIONS =================
    @GetMapping("/transaction")
    public String viewTransactions(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        List<Transaction> transactions
                = transactionRepo.findAll(Sort.by(Sort.Direction.DESC, "date"));

        model.addAttribute("transactions", transactions);

        return "transactions";
    }

    // ================= VIEW USER REPORTS =================
    @GetMapping("/userReports")
    public String viewUserReports(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        model.addAttribute("reports", userReportRepo.findAllByOrderByCreatedAtDesc());
        return "userReports";
    }

    // ================= EDIT ACCOUNT =================
    @GetMapping("/editAccount/{id}")
    public String showEditPage(@PathVariable Long id,
            HttpSession session,
            Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        Account account = accountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        model.addAttribute("account", account);
        return "editaccount";
    }

    @PostMapping("/updateAccount")
    public String updateAccount(@RequestParam Long id,
            @RequestParam String userName,
            HttpSession session) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        Account existingAccount = accountRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        existingAccount.setUserName(userName);
        accountRepo.save(existingAccount);

        return "redirect:/adminaccount";
    }

    // ================= DELETE ACCOUNT =================
    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam Long id,
            HttpSession session) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        accountRepo.findById(id).ifPresent(accountRepo::delete);

        return "redirect:/adminaccount";
    }

    // ================= REPORTS =================
    @GetMapping("/reports")
    public String showReports(HttpSession session, Model model) {

        if (session.getAttribute("adminEmail") == null) {
            return "redirect:/admin";
        }

        // Get dashboard statistics for reports
        long totalUsers = requestRepo.count();
        long totalAccounts = accountRepo.count();
        double totalBalance = accountRepo.getTotalBalance();
        long todayTransactions = transactionRepo.countTodayTransactions();
        long pendingRequests = requestRepo.countByStatus("PENDING");
        long approvedRequests = requestRepo.countByStatus("APPROVED");
        List<Transaction> allTransactions = transactionRepo.findAll();
        allTransactions.sort(Comparator.comparing(Transaction::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        Map<Long, String> accountNames = buildAccountNameMap();
        LinkedHashMap<YearMonth, List<TransactionReportRow>> monthlyBuckets = new LinkedHashMap<>();
        for (Transaction transaction : allTransactions) {
            if (transaction.getDate() == null) {
                continue;
            }
            YearMonth bucketKey = YearMonth.from(transaction.getDate());
            monthlyBuckets.computeIfAbsent(bucketKey, ignored -> new ArrayList<>())
                    .add(toTransactionReportRow(transaction, accountNames));
        }

        List<MonthlyTransactionReport> monthlyReports = new ArrayList<>();
        for (Map.Entry<YearMonth, List<TransactionReportRow>> entry : monthlyBuckets.entrySet()) {
            double monthTotalAmount = 0;
            for (TransactionReportRow row : entry.getValue()) {
                monthTotalAmount += row.getAmount();
            }

            monthlyReports.add(new MonthlyTransactionReport(
                    entry.getKey().format(MONTH_LABEL_FORMAT),
                    entry.getValue().size(),
                    String.format("%.2f", monthTotalAmount),
                    entry.getValue()));
        }

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalAccounts", totalAccounts);
        model.addAttribute("totalBalance", String.format("%.2f", totalBalance));
        model.addAttribute("todayTransactions", todayTransactions);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("approvedRequests", approvedRequests);
        model.addAttribute("monthlyReports", monthlyReports);

        return "reports";
    }

    // ================= LOGOUT =================
    @GetMapping("/logout")
    public String adminlogout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin";
    }

    private Map<Long, String> buildAccountNameMap() {
        Map<Long, String> accountNames = new HashMap<>();
        for (Account account : accountRepo.findAll()) {
            accountNames.put(account.getAccountNo(), account.getUserName());
        }
        return accountNames;
    }

    private DashboardTransactionView toDashboardTransactionView(Transaction transaction, Map<Long, String> accountNames) {
        String type = transaction.getType() != null ? transaction.getType() : "-";
        String method = transaction.getTransferMethod() != null && !transaction.getTransferMethod().isBlank()
                ? transaction.getTransferMethod()
                : "-";
        return new DashboardTransactionView(
                formatAccountLabel(transaction.getFromAccount(), accountNames),
                formatAccountLabel(transaction.getToAccount(), accountNames),
                type,
                method,
                String.format("%.2f", transaction.getAmount()));
    }

    private TransactionReportRow toTransactionReportRow(Transaction transaction, Map<Long, String> accountNames) {
        String method = transaction.getTransferMethod() != null && !transaction.getTransferMethod().isBlank()
                ? transaction.getTransferMethod()
                : "-";
        String remark = transaction.getNote() != null && !transaction.getNote().isBlank()
                ? transaction.getNote()
                : "-";
        String type = transaction.getType() != null && !transaction.getType().isBlank()
                ? transaction.getType()
                : "-";

        return new TransactionReportRow(
                transaction.getId(),
                transaction.getDate() != null ? transaction.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "-",
                formatAccountLabel(transaction.getFromAccount(), accountNames),
                formatAccountLabel(transaction.getToAccount(), accountNames),
                type,
                method,
                remark,
                transaction.getAmount());
    }

    private String formatAccountLabel(Long accountNumber, Map<Long, String> accountNames) {
        if (accountNumber == null) {
            return "-";
        }

        String accountName = accountNames.get(accountNumber);
        if (accountName == null || accountName.isBlank()) {
            return String.valueOf(accountNumber);
        }

        return accountName + " (" + accountNumber + ")";
    }

    private String generateUniqueAtmCardNumber() {
        String cardNumber;
        do {
            StringBuilder builder = new StringBuilder("4");
            for (int i = 0; i < 15; i++) {
                builder.append(ThreadLocalRandom.current().nextInt(10));
            }
            cardNumber = builder.toString();
        } while (atmApplicationRepo.existsByAtmCardNumber(cardNumber));

        return cardNumber;
    }

    public static class DashboardTransactionView {

        private final String fromAccount;
        private final String toAccount;
        private final String type;
        private final String method;
        private final String amount;

        public DashboardTransactionView(String fromAccount, String toAccount, String type, String method, String amount) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.type = type;
            this.method = method;
            this.amount = amount;
        }

        public String getFromAccount() {
            return fromAccount;
        }

        public String getToAccount() {
            return toAccount;
        }

        public String getType() {
            return type;
        }

        public String getMethod() {
            return method;
        }

        public String getAmount() {
            return amount;
        }
    }

    public static class TransactionReportRow {

        private final Long id;
        private final String transactionDate;
        private final String fromAccount;
        private final String toAccount;
        private final String type;
        private final String method;
        private final String remark;
        private final double amount;

        public TransactionReportRow(Long id, String transactionDate, String fromAccount, String toAccount,
                String type, String method, String remark, double amount) {
            this.id = id;
            this.transactionDate = transactionDate;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.type = type;
            this.method = method;
            this.remark = remark;
            this.amount = amount;
        }

        public Long getId() {
            return id;
        }

        public String getTransactionDate() {
            return transactionDate;
        }

        public String getFromAccount() {
            return fromAccount;
        }

        public String getToAccount() {
            return toAccount;
        }

        public String getType() {
            return type;
        }

        public String getMethod() {
            return method;
        }

        public String getRemark() {
            return remark;
        }

        public double getAmount() {
            return amount;
        }
    }

    public static class MonthlyTransactionReport {

        private final String monthLabel;
        private final int transactionCount;
        private final String totalAmount;
        private final List<TransactionReportRow> transactions;

        public MonthlyTransactionReport(String monthLabel, int transactionCount, String totalAmount,
                List<TransactionReportRow> transactions) {
            this.monthLabel = monthLabel;
            this.transactionCount = transactionCount;
            this.totalAmount = totalAmount;
            this.transactions = transactions;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public String getTotalAmount() {
            return totalAmount;
        }

        public List<TransactionReportRow> getTransactions() {
            return transactions;
        }
    }
}
