package com.thinkcode.transportbackend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private DailyRevenueRepository dailyRevenueRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private AuthenticatedCompanyProvider authenticatedCompanyProvider;

    @Mock
    private JavaMailSender mailSender;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                dailyRevenueRepository,
                debtRepository,
                clientService,
                authenticatedCompanyProvider,
            mailSender,
            "no-reply@test.local"
        );
    }

    @Test
    void generateInvoicePdfShouldReturnBytes() {
        UUID companyId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        Client client = new Client();
        client.setName("Client Test");

        when(authenticatedCompanyProvider.requireCompanyId()).thenReturn(companyId);
        when(clientService.findByIdForCompany(clientId, companyId)).thenReturn(client);
        when(dailyRevenueRepository.findAllByVehicleCompanyIdAndVehicleClientIdAndRevenueDateBetween(
                companyId, clientId, startDate, endDate)).thenReturn(List.of());
        when(debtRepository.findAllByVehicleCompanyIdAndVehicleClientIdAndDebtDateBetween(
                companyId, clientId, startDate, endDate)).thenReturn(List.of());

        byte[] pdf = invoiceService.generateInvoicePdf(clientId, startDate, endDate);

        assertTrue(pdf.length > 0);
    }
}
