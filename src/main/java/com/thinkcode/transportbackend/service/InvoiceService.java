package com.thinkcode.transportbackend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final ClientService clientService;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final JavaMailSender mailSender;
    private final String senderEmail;

    public InvoiceService(
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            ClientService clientService,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@transport.local}") String senderEmail
    ) {
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.clientService = clientService;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public byte[] generateInvoicePdf(UUID clientId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }

        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = clientService.findByIdForCompany(clientId, companyId);

        List<DailyRevenue> revenues = dailyRevenueRepository
                .findAllByVehicleCompanyIdAndVehicleClientIdAndRevenueDateBetween(companyId, clientId, startDate, endDate);

        List<Debt> debts = debtRepository
                .findAllByVehicleCompanyIdAndVehicleClientIdAndDebtDateBetween(companyId, clientId, startDate, endDate);

        BigDecimal totalRevenue = revenues.stream()
                .map(DailyRevenue::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebt = debts.stream()
                .map(Debt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebtPaid = debts.stream()
                .map(Debt::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebtRemaining = totalDebt.subtract(totalDebtPaid);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("Facture client", titleFont));
            document.add(new Paragraph("Client: " + client.getName(), subtitleFont));
            document.add(new Paragraph("Periode: " + startDate + " au " + endDate, subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable revenueTable = new PdfPTable(4);
            revenueTable.setWidthPercentage(100);
            revenueTable.setWidths(new float[]{2.2f, 2.2f, 1.8f, 1.8f});
            addHeaderCell(revenueTable, "Date");
            addHeaderCell(revenueTable, "Vehicule");
            addHeaderCell(revenueTable, "Statut");
            addHeaderCell(revenueTable, "Montant");

            for (DailyRevenue revenue : revenues) {
                revenueTable.addCell(revenue.getRevenueDate().toString());
                revenueTable.addCell(revenue.getVehicle().getMatricule());
                revenueTable.addCell(revenue.getActivityStatus().name());
                revenueTable.addCell(revenue.getAmount().toPlainString());
            }

            document.add(new Paragraph("Recettes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(revenueTable);
            document.add(new Paragraph(" "));

            PdfPTable debtTable = new PdfPTable(4);
            debtTable.setWidthPercentage(100);
            debtTable.setWidths(new float[]{2.2f, 2.6f, 1.6f, 1.6f});
            addHeaderCell(debtTable, "Date");
            addHeaderCell(debtTable, "Motif");
            addHeaderCell(debtTable, "Montant");
            addHeaderCell(debtTable, "Reste");

            for (Debt debt : debts) {
                debtTable.addCell(debt.getDebtDate().toString());
                debtTable.addCell(debt.getReason());
                debtTable.addCell(debt.getAmount().toPlainString());
                debtTable.addCell(debt.getAmount().subtract(debt.getPaidAmount()).toPlainString());
            }

            document.add(new Paragraph("Dettes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(debtTable);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Total recettes: " + totalRevenue.toPlainString(), subtitleFont));
            document.add(new Paragraph("Total dettes: " + totalDebt.toPlainString(), subtitleFont));
            document.add(new Paragraph("Total dettes reglees: " + totalDebtPaid.toPlainString(), subtitleFont));
            document.add(new Paragraph("Total dettes restantes: " + totalDebtRemaining.toPlainString(), subtitleFont));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate invoice PDF");
        } finally {
            document.close();
        }

        return output.toByteArray();
    }

    public void sendInvoiceByEmail(UUID clientId, LocalDate startDate, LocalDate endDate, String recipientEmail) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = clientService.findByIdForCompany(clientId, companyId);
        String recipient = recipientEmail == null || recipientEmail.isBlank() ? client.getEmail() : recipientEmail;

        if (recipient == null || recipient.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No recipient email available for this client");
        }

        byte[] pdf = generateInvoicePdf(clientId, startDate, endDate);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true);
            helper.setFrom(senderEmail);
            helper.setTo(recipient);
            helper.setSubject("Facture flotte - " + client.getName());
            helper.setText("Bonjour,\n\nVeuillez trouver en piece jointe votre facture de flotte.\n\nCordialement,");
            helper.addAttachment("facture-" + client.getName().replace(' ', '-') + ".pdf", () -> new java.io.ByteArrayInputStream(pdf));
            mailSender.send(message);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send invoice email");
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        table.addCell(cell);
    }
}
