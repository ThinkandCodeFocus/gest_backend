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
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.dto.InvoiceSummaryResponse;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
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
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final AuthenticatedCompanyProvider authenticatedCompanyProvider;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final JavaMailSender mailSender;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final VehicleRepository vehicleRepository;
    private final String senderEmail;

    public InvoiceService(
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            ClientService clientService,
            ClientRepository clientRepository,
            AuthenticatedCompanyProvider authenticatedCompanyProvider,
            JavaMailSender mailSender,
            AuthenticatedUserProvider authenticatedUserProvider,
            MessageRepository messageRepository,
            UserAccountRepository userAccountRepository,
            VehicleRepository vehicleRepository,
            @Value("${app.mail.from:no-reply@transport.local}") String senderEmail
    ) {
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.authenticatedCompanyProvider = authenticatedCompanyProvider;
        this.mailSender = mailSender;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.vehicleRepository = vehicleRepository;
        this.senderEmail = senderEmail;
    }

    public List<InvoiceSummaryResponse> getInvoiceSummaries(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount currentUser = authenticatedUserProvider.requireUser();

        if (currentUser.getRole() == RoleName.CLIENT) {
            Client client = clientRepository.findByCompanyIdAndEmail(companyId, currentUser.getEmail())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
            return List.of(toInvoiceSummary(companyId, client, startDate, endDate));
        }

        return clientRepository.findAllByCompanyId(companyId).stream()
                .map(client -> toInvoiceSummary(companyId, client, startDate, endDate))
                .toList();
    }

    public byte[] generateAuthenticatedClientInvoicePdf(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount currentUser = authenticatedUserProvider.requireUser();
        Client client = clientRepository.findByCompanyIdAndEmail(companyId, currentUser.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
        return generateInvoicePdf(client.getId(), startDate, endDate);
    }

    public byte[] generateAuthenticatedClientRepairReportPdf(LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        UserAccount currentUser = authenticatedUserProvider.requireUser();
        Client client = clientRepository.findByCompanyIdAndEmail(companyId, currentUser.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Client not found"));
        return generateRepairReportPdf(client.getId(), startDate, endDate);
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

    public byte[] generateRepairReportPdf(UUID clientId, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be greater than or equal to startDate");
        }

        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = clientService.findByIdForCompany(clientId, companyId);
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndClientId(companyId, clientId);
        List<UUID> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        List<MaintenanceRecord> maintenances = maintenanceRecordRepository
                .findAllByVehicleCompanyIdAndMaintenanceDateBetween(companyId, startDate, endDate).stream()
                .filter(item -> item.getVehicle() != null && vehicleIds.contains(item.getVehicle().getId()))
                .sorted((left, right) -> left.getMaintenanceDate().compareTo(right.getMaintenanceDate()))
                .toList();

        BigDecimal totalCost = maintenances.stream()
                .map(MaintenanceRecord::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("Suivi reparations", titleFont));
            document.add(new Paragraph("Client: " + client.getName(), subtitleFont));
            document.add(new Paragraph("Periode: " + startDate + " au " + endDate, subtitleFont));
            document.add(new Paragraph(" "));

            PdfPTable maintenanceTable = new PdfPTable(6);
            maintenanceTable.setWidthPercentage(100);
            maintenanceTable.setWidths(new float[]{1.6f, 1.8f, 1.8f, 3.0f, 2.0f, 1.6f});
            addHeaderCell(maintenanceTable, "Date");
            addHeaderCell(maintenanceTable, "Vehicule");
            addHeaderCell(maintenanceTable, "Type");
            addHeaderCell(maintenanceTable, "Description");
            addHeaderCell(maintenanceTable, "Prestataire");
            addHeaderCell(maintenanceTable, "Total");

            for (MaintenanceRecord maintenance : maintenances) {
                maintenanceTable.addCell(maintenance.getMaintenanceDate().toString());
                maintenanceTable.addCell(maintenance.getVehicle().getMatricule());
                maintenanceTable.addCell(maintenance.getType().name());
                maintenanceTable.addCell(maintenance.getDescription());
                maintenanceTable.addCell(maintenance.getProvider() == null ? "-" : maintenance.getProvider());
                maintenanceTable.addCell(maintenance.getCost().toPlainString());
            }

            document.add(maintenanceTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total maintenance: " + totalCost.toPlainString(), subtitleFont));
            document.add(new Paragraph("Nombre d'interventions: " + maintenances.size(), subtitleFont));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate repair report PDF");
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

    public void sendInvoiceToClientMessages(UUID clientId, LocalDate startDate, LocalDate endDate) {
        UUID companyId = authenticatedCompanyProvider.requireCompanyId();
        Client client = clientService.findByIdForCompany(clientId, companyId);
        UserAccount sender = authenticatedUserProvider.requireUser();

        if (client.getEmail() == null || client.getEmail().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No client email available to resolve client messaging account");
        }

        UserAccount recipient = userAccountRepository.findByCompanyIdAndEmail(companyId, client.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No client user account found for internal messaging"));

        byte[] pdf = generateInvoicePdf(clientId, startDate, endDate);
        String filename = "facture-" + client.getName().replace(' ', '-') + "-" + startDate + "-" + endDate + ".pdf";

        Message message = new Message();
        message.setCompany(sender.getCompany());
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setIsRead(false);
        message.setAttachmentName(filename);
        message.setAttachmentUrl("generated:invoice:" + clientId + ":" + startDate + ":" + endDate + ":" + pdf.length);
        message.setContent(
                "Bonjour " + client.getName()
                        + ", votre facture pour la periode du "
                        + startDate
                        + " au "
                        + endDate
                        + " a ete deposee dans la messagerie interne."
        );
        messageRepository.save(message);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        table.addCell(cell);
    }

    private InvoiceSummaryResponse toInvoiceSummary(UUID companyId, Client client, LocalDate startDate, LocalDate endDate) {
        List<Vehicle> vehicles = vehicleRepository.findByCompanyIdAndClientId(companyId, client.getId());
        List<UUID> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        List<DailyRevenue> revenues = vehicleIds.isEmpty()
                ? List.of()
                : dailyRevenueRepository.findByCompanyIdAndVehicleIdInAndDateRange(companyId, vehicleIds, startDate, endDate);
        List<Debt> debts = debtRepository.findAllByVehicleCompanyIdAndVehicleClientIdAndDebtDateBetween(companyId, client.getId(), startDate, endDate);

        BigDecimal amount = revenues.stream().map(DailyRevenue::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableCash = revenues.stream().map(DailyRevenue::getCompanyShare).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debtTotal = debts.stream().map(Debt::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amortization = vehicles.stream()
                .map(Vehicle::getAmortization)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate previousStart = startDate.minusMonths(1);
        LocalDate previousEnd = endDate.minusMonths(1);
        List<DailyRevenue> previousRevenues = vehicleIds.isEmpty()
                ? List.of()
                : dailyRevenueRepository.findByCompanyIdAndVehicleIdInAndDateRange(companyId, vehicleIds, previousStart, previousEnd);
        BigDecimal previousMonthRevenue = previousRevenues.stream()
                .map(DailyRevenue::getCompanyShare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DailyRevenue> cumulativeRevenues = vehicleIds.isEmpty()
                ? List.of()
                : dailyRevenueRepository.findByCompanyIdAndVehicleIdInAndDateRange(companyId, vehicleIds, LocalDate.of(2024, 1, 1), endDate);
        BigDecimal cumulativeRevenue = cumulativeRevenues.stream()
                .map(DailyRevenue::getCompanyShare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long breakdownEntries = revenues.stream()
                .filter(revenue -> revenue.getActivityStatus() == com.thinkcode.transportbackend.entity.ActivityStatus.BREAKDOWN)
                .count();
        BigDecimal breakdownRate = amount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(breakdownEntries)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(Math.max(revenues.size(), 1L)), 1, java.math.RoundingMode.HALF_UP);

        String status = debtTotal.compareTo(BigDecimal.ZERO) > 0 ? "Envoyee" : "Payee";

        return new InvoiceSummaryResponse(
                "FAC-" + startDate.getYear() + "-" + String.format("%02d", startDate.getMonthValue()) + "-" + client.getName().replace(" ", "").toUpperCase(),
                client.getId(),
                client.getName(),
                client.getEmail(),
                vehicleIds,
                startDate,
                endDate,
                amount,
                status,
                "REPORTING_CLIENT",
                "Suivi_reparations",
                availableCash,
                breakdownRate,
                debtTotal,
                amortization,
                previousMonthRevenue,
                cumulativeRevenue
        );
    }
}
