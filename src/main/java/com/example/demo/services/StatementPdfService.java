package com.example.demo.services;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.forentity.Account;
import com.example.demo.forentity.AccountRequest;
import com.example.demo.forentity.Transaction;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class StatementPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generateMonthlyStatement(AccountRequest user, Account account, List<Transaction> transactions,
            String periodLabel, LocalDate fromDate, LocalDate toDate) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Platinum Reserve Bank - Account Statement", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Requested period: " + periodLabel + " | " + fromDate + " to " + toDate, textFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16);
            document.add(subtitle);

            document.add(new Paragraph("Customer: " + user.getFirstName() + " " + user.getLastName(), headingFont));
            document.add(new Paragraph("Account Number: " + user.getAccountNumber(), textFont));
            document.add(new Paragraph("Email: " + user.getEmail(), textFont));
            document.add(new Paragraph("Current Balance: Rs. " + String.format("%.2f", account.getBalance()), textFont));
            document.add(new Paragraph(" ", textFont));

            PdfPTable table = new PdfPTable(new float[]{1.1f, 2.2f, 1.6f, 1.6f, 1.4f, 1.1f, 1.8f, 1.3f});
            table.setWidthPercentage(100);

            addHeaderCell(table, "ID", headingFont);
            addHeaderCell(table, "Date", headingFont);
            addHeaderCell(table, "Type", headingFont);
            addHeaderCell(table, "From", headingFont);
            addHeaderCell(table, "To", headingFont);
            addHeaderCell(table, "Method", headingFont);
            addHeaderCell(table, "Remark", headingFont);
            addHeaderCell(table, "Amount", headingFont);

            double totalAmount = 0;
            for (Transaction transaction : transactions) {
                totalAmount += transaction.getAmount();
                addBodyCell(table, transaction.getId() != null ? String.valueOf(transaction.getId()) : "-", textFont);
                addBodyCell(table, transaction.getDate() != null ? transaction.getDate().format(DATE_TIME_FORMAT) : "-", textFont);
                addBodyCell(table, blankSafe(transaction.getType()), textFont);
                addBodyCell(table, transaction.getFromAccount() != null ? String.valueOf(transaction.getFromAccount()) : "-", textFont);
                addBodyCell(table, transaction.getToAccount() != null ? String.valueOf(transaction.getToAccount()) : "-", textFont);
                addBodyCell(table, blankSafe(transaction.getTransferMethod()), textFont);
                addBodyCell(table, blankSafe(transaction.getNote()), textFont);
                addBodyCell(table, "Rs. " + String.format("%.2f", transaction.getAmount()), textFont);
            }

            if (transactions.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No transactions found for the selected period.", textFont));
                emptyCell.setColspan(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(10);
                table.addCell(emptyCell);
            }

            document.add(table);
            document.add(new Paragraph(" ", textFont));
            document.add(new Paragraph("Total transactions: " + transactions.size(), headingFont));
            document.add(new Paragraph("Total transaction volume: Rs. " + String.format("%.2f", totalAmount), headingFont));

            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate statement PDF", exception);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String blankSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
