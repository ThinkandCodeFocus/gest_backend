package com.thinkcode.transportbackend.controller;

import com.thinkcode.transportbackend.dto.InvoiceEmailRequest;
import com.thinkcode.transportbackend.dto.InvoiceMessageRequest;
import com.thinkcode.transportbackend.dto.InvoiceSummaryResponse;
import com.thinkcode.transportbackend.dto.DailyRevenueResponse;
import com.thinkcode.transportbackend.dto.ClientAccountSummary;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.service.InvoiceService;
import com.thinkcode.transportbackend.service.AuthenticatedCompanyProvider;
import com.thinkcode.transportbackend.service.DailyRevenueService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT', 'CLIENT')")
    public List<InvoiceSummaryResponse> getInvoiceSummaries(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return invoiceService.getInvoiceSummaries(startDate, endDate);
    }

    @GetMapping("/client-account-summary")
    @PreAuthorize("hasRole('CLIENT')")
    public ClientAccountSummary getClientAccountSummary(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return invoiceService.getClientAccountSummary(startDate, endDate);
    }

    @GetMapping("/pdf/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<byte[]> generateInvoicePdf(
            @PathVariable UUID clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdf = invoiceService.generateInvoicePdf(clientId, startDate, endDate);
        String fileName = "facture-" + clientId + "-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(pdf);
    }

    @GetMapping("/repair-report/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public ResponseEntity<byte[]> downloadRepairReportPdf(
            @RequestParam UUID clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdf = invoiceService.generateRepairReportPdf(clientId, startDate, endDate);
        String fileName = "suivi-reparations-" + clientId + "-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(pdf);
    }

    @GetMapping("/client/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> downloadClientPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdf = invoiceService.generateAuthenticatedClientInvoicePdf(startDate, endDate);
        String fileName = "facture-client-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(pdf);
    }

    @GetMapping("/client/repair-report/pdf")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<byte[]> downloadClientRepairReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        byte[] pdf = invoiceService.generateAuthenticatedClientRepairReportPdf(startDate, endDate);
        String fileName = "suivi-reparations-client-" + startDate + "-" + endDate + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(pdf);
    }

    @PostMapping("/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public Map<String, String> sendByEmail(@Valid @RequestBody InvoiceEmailRequest request) {
        invoiceService.sendInvoiceByEmail(
                request.clientId(),
                request.startDate(),
                request.endDate(),
                request.recipientEmail()
        );
        return Map.of("status", "sent");
    }

    @PostMapping("/message")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'ASSISTANT')")
    public Map<String, String> sendToClientMessages(@Valid @RequestBody InvoiceMessageRequest request) {
        invoiceService.sendInvoiceToClientMessages(
                request.clientId(),
                request.startDate(),
                request.endDate()
        );
        return Map.of("status", "sent_to_messages");
    }
}
