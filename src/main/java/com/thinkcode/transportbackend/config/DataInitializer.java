package com.thinkcode.transportbackend.config;

import com.thinkcode.transportbackend.entity.ActivityStatus;
import com.thinkcode.transportbackend.entity.AuditLog;
import com.thinkcode.transportbackend.entity.Client;
import com.thinkcode.transportbackend.entity.Company;
import com.thinkcode.transportbackend.entity.DailyRevenue;
import com.thinkcode.transportbackend.entity.Debt;
import com.thinkcode.transportbackend.entity.DebtStatus;
import com.thinkcode.transportbackend.entity.Driver;
import com.thinkcode.transportbackend.entity.MaintenanceRecord;
import com.thinkcode.transportbackend.entity.MaintenanceType;
import com.thinkcode.transportbackend.entity.Message;
import com.thinkcode.transportbackend.entity.Notification;
import com.thinkcode.transportbackend.entity.NotificationType;
import com.thinkcode.transportbackend.entity.PlanningEvent;
import com.thinkcode.transportbackend.entity.RevenueRule;
import com.thinkcode.transportbackend.entity.RoleName;
import com.thinkcode.transportbackend.entity.SystemSetting;
import com.thinkcode.transportbackend.entity.UserAccount;
import com.thinkcode.transportbackend.entity.Vehicle;
import com.thinkcode.transportbackend.entity.VehicleStatus;
import com.thinkcode.transportbackend.entity.VehicleType;
import com.thinkcode.transportbackend.repository.AuditLogRepository;
import com.thinkcode.transportbackend.repository.ClientRepository;
import com.thinkcode.transportbackend.repository.CompanyRepository;
import com.thinkcode.transportbackend.repository.DailyRevenueRepository;
import com.thinkcode.transportbackend.repository.DebtRepository;
import com.thinkcode.transportbackend.repository.DriverRepository;
import com.thinkcode.transportbackend.repository.DriverAbsenceRepository;
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.NotificationRepository;
import com.thinkcode.transportbackend.repository.PlanningEventRepository;
import com.thinkcode.transportbackend.repository.RevenueRuleRepository;
import com.thinkcode.transportbackend.repository.SystemSettingRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final DriverRepository driverRepository;
    private final DriverAbsenceRepository driverAbsenceRepository;
    private final ClientRepository clientRepository;
    private final VehicleRepository vehicleRepository;
    private final DailyRevenueRepository dailyRevenueRepository;
    private final DebtRepository debtRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final NotificationRepository notificationRepository;
    private final MessageRepository messageRepository;
    private final PlanningEventRepository planningEventRepository;
    private final RevenueRuleRepository revenueRuleRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AuditLogRepository auditLogRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String senderEmail;
    private final String brevoApiKey;
    private final String brevoFromEmail;
    private final String brevoFromName;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(
            CompanyRepository companyRepository,
            UserAccountRepository userAccountRepository,
            DriverRepository driverRepository,
            DriverAbsenceRepository driverAbsenceRepository,
            ClientRepository clientRepository,
            VehicleRepository vehicleRepository,
            DailyRevenueRepository dailyRevenueRepository,
            DebtRepository debtRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            NotificationRepository notificationRepository,
            MessageRepository messageRepository,
            PlanningEventRepository planningEventRepository,
            RevenueRuleRepository revenueRuleRepository,
            SystemSettingRepository systemSettingRepository,
            AuditLogRepository auditLogRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            @Value("${app.mail.from:no-reply@transport.local}") String senderEmail,
            @Value("${BREVO_API_KEY:}") String brevoApiKey,
            @Value("${MAIL_FROM_EMAIL:}") String brevoFromEmail,
            @Value("${MAIL_FROM_NAME:}") String brevoFromName
    ) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.driverRepository = driverRepository;
        this.driverAbsenceRepository = driverAbsenceRepository;
        this.clientRepository = clientRepository;
        this.vehicleRepository = vehicleRepository;
        this.dailyRevenueRepository = dailyRevenueRepository;
        this.debtRepository = debtRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.notificationRepository = notificationRepository;
        this.messageRepository = messageRepository;
        this.planningEventRepository = planningEventRepository;
        this.revenueRuleRepository = revenueRuleRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.auditLogRepository = auditLogRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.senderEmail = senderEmail;
        this.brevoApiKey = brevoApiKey;
        this.brevoFromEmail = brevoFromEmail;
        this.brevoFromName = brevoFromName;
    }

    @Override
    public void run(String... args) {
        // NOTE: This wipes the database on every backend startup.
        // Keep disabled in local/production once the client starts adding data.
        // clearDatabase();
        relaxDriverEmailUniqueConstraint();
        // NOTE: Automatic bootstrap creation is disabled.
        // The client account and production data already exist in database.
    }

    private void relaxDriverEmailUniqueConstraint() {
        try {
            var indexNames = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'driver'
                      AND column_name = 'email'
                      AND non_unique = 0
                      AND index_name <> 'PRIMARY'
                    """,
                    String.class
            );
            for (String indexName : indexNames) {
                jdbcTemplate.execute("ALTER TABLE driver DROP INDEX " + indexName);
            }
        } catch (Exception ex) {
            logger.debug("Driver email unique index already relaxed or unavailable", ex);
        }
    }

    private void clearDatabase() {
        auditLogRepository.deleteAll();
        planningEventRepository.deleteAll();
        messageRepository.deleteAll();
        notificationRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        debtRepository.deleteAll();
        dailyRevenueRepository.deleteAll();
        vehicleRepository.deleteAll();
        driverAbsenceRepository.deleteAll();
        driverRepository.deleteAll();
        clientRepository.deleteAll();
        userAccountRepository.deleteAll();
        revenueRuleRepository.deleteAll();
        systemSettingRepository.deleteAll();
        companyRepository.deleteAll();
    }

    private void seedWelcomeMessage(Company company, UserAccount user, String rawPassword) {
        saveMessage(
                company,
                user,
                user,
                buildAccountEmailBody(user.getFullName(), user.getEmail(), rawPassword),
                false,
                LocalDateTime.now()
        );
    }

    private UserAccount createUser(Company company, String fullName, String email, RoleName role, String rawPassword) {
        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCompany(company);
        return userAccountRepository.save(user);
    }

    private void sendAccountEmail(UserAccount user, String rawPassword) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Votre compte ERP KIL Services");
            helper.setText(buildAccountEmailBody(user.getFullName(), user.getEmail(), rawPassword), false);
            mailSender.send(message);
        } catch (Exception ex) {
            logger.warn("SMTP email failed, trying Brevo API for {}", user.getEmail(), ex);
            if (!sendBrevoEmail(user, rawPassword)) {
                logger.warn("Brevo email failed for {}", user.getEmail());
            }
        }
    }

    private String buildAccountEmailBody(String fullName, String email, String rawPassword) {
        return "Bonjour " + fullName + ",\n\n"
                + "Votre compte vient d'etre cree sur l'ERP de KIL Services.\n\n"
                + "Pour vous connecter, veuillez utiliser :\n\n"
                + "Identifiant : " + email + "\n"
                + "Mot de passe : " + rawPassword + "\n\n"
                + "Lien de connexion : https://erpkilservices.com/";
    }

    private boolean sendBrevoEmail(UserAccount user, String rawPassword) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) return false;
        if (brevoFromEmail == null || brevoFromEmail.isBlank()) return false;

        String senderName = (brevoFromName == null || brevoFromName.isBlank()) ? "KIL Services" : brevoFromName;
        String payload = "{"
                + "\"sender\":{\"name\":\"" + escapeJson(senderName) + "\",\"email\":\"" + escapeJson(brevoFromEmail) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(user.getEmail()) + "\",\"name\":\"" + escapeJson(user.getFullName()) + "\"}],"
                + "\"subject\":\"Votre compte ERP KIL Services\","
                + "\"textContent\":\"" + escapeJson(buildAccountEmailBody(user.getFullName(), user.getEmail(), rawPassword)) + "\""
                + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            logger.warn("Brevo API error for {}", user.getEmail(), ex);
            return false;
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private Driver createDriver(Company company, String fullName, String email, String phone, String license, RoleName role, String status, int score) {
        Driver driver = new Driver();
        driver.setCompany(company);
        driver.setFullName(fullName);
        driver.setEmail(email);
        driver.setPhoneNumber(phone);
        driver.setLicenseNumber(license);
        driver.setRole(role);
        driver.setStatus(status);
        driver.setPerformanceScore(score);
        return driverRepository.save(driver);
    }

    private Client createClient(Company company, String name, String email, String phone) {
        Client client = new Client();
        client.setCompany(company);
        client.setName(name);
        client.setEmail(email);
        client.setPhoneNumber(phone);
        return clientRepository.save(client);
    }

    private Vehicle createVehicle(
            Company company,
            String matricule,
            VehicleType type,
            VehicleStatus status,
            Driver driver,
            Client client,
            String amortization,
            LocalDate startDate,
            String dailyTarget
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCompany(company);
        vehicle.setMatricule(matricule);
        vehicle.setType(type);
        vehicle.setStatus(status);
        vehicle.setDriver(driver);
        vehicle.setClient(client);
        vehicle.setAmortization(new BigDecimal(amortization));
        vehicle.setStartDate(startDate);
        vehicle.setDailyTarget(new BigDecimal(dailyTarget));
        return vehicleRepository.save(vehicle);
    }

    private void seedRevenueRules(Company company) {
        revenueRuleRepository.save(new RevenueRule(company, "OBJECTIVE_CAR", new BigDecimal("13500"), "Objectif voiture"));
        revenueRuleRepository.save(new RevenueRule(company, "DRIVER_SHARE_CAR", new BigDecimal("1500"), "Part chauffeur voiture"));
        revenueRuleRepository.save(new RevenueRule(company, "COMPANY_SHARE_CAR", new BigDecimal("2000"), "Part entreprise voiture"));
        revenueRuleRepository.save(new RevenueRule(company, "CLIENT_SHARE_CAR", new BigDecimal("10000"), "Part client voiture"));
        revenueRuleRepository.save(new RevenueRule(company, "OBJECTIVE_MOTO_TAXI", new BigDecimal("6500"), "Objectif moto-taxi"));
        revenueRuleRepository.save(new RevenueRule(company, "COMPANY_SHARE_MOTO_TAXI", new BigDecimal("1000"), "Part entreprise moto-taxi"));
        revenueRuleRepository.save(new RevenueRule(company, "CLIENT_SHARE_MOTO_TAXI", new BigDecimal("5500"), "Part client moto-taxi"));
        revenueRuleRepository.save(new RevenueRule(company, "OBJECTIVE_MOTORBIKE", new BigDecimal("3500"), "Objectif moto"));
        revenueRuleRepository.save(new RevenueRule(company, "COMPANY_SHARE_MOTORBIKE", new BigDecimal("1000"), "Part entreprise moto"));
        revenueRuleRepository.save(new RevenueRule(company, "CLIENT_SHARE_MOTORBIKE", new BigDecimal("2500"), "Part client moto"));
    }

    private void seedSystemSettings() {
        systemSettingRepository.save(new SystemSetting("CONFIG.dossierFacturesId", "drive-demo-folder", "Dossier des factures PDF"));
        systemSettingRepository.save(new SystemSetting("REPORTING_CLIENT_TEMPLATE", "REPORTING_CLIENT", "Template facture client"));
        systemSettingRepository.save(new SystemSetting("REPAIR_REPORT_TEMPLATE", "Suivi_reparations", "Template suivi reparations"));
    }

    private void seedRevenues(Map<String, Vehicle> vehicles) {
        saveRevenue(vehicles.get("veh-001"), "2026-03-27", "12900", ActivityStatus.ACTIVE, "1433", "1911", "9556", "600", null);
        saveRevenue(vehicles.get("veh-002"), "2026-03-27", "0", ActivityStatus.BREAKDOWN, "0", "0", "0", "0", "En panne");
        saveRevenue(vehicles.get("veh-003"), "2026-03-27", "5200", ActivityStatus.ACTIVE, "0", "800", "4400", "1300", null);
        saveRevenue(vehicles.get("veh-004"), "2026-03-27", "3500", ActivityStatus.ACTIVE, "0", "1000", "2500", "0", null);
        saveRevenue(vehicles.get("veh-005"), "2026-03-27", "0", ActivityStatus.SICK, "0", "0", "0", "0", "Chauffeur malade");
        saveRevenue(vehicles.get("veh-006"), "2026-03-27", "7100", ActivityStatus.ACTIVE, "0", "1092", "6008", "0", null);
        saveRevenue(vehicles.get("veh-007"), "2026-03-27", "0", ActivityStatus.PARKED, "0", "0", "0", "0", "Au parc");
        saveRevenue(vehicles.get("veh-008"), "2026-03-27", "3100", ActivityStatus.ACTIVE, "0", "886", "2214", "400", null);
        saveRevenue(vehicles.get("veh-001"), "2026-03-26", "13800", ActivityStatus.ACTIVE, "1500", "2044", "10256", "0", null);
        saveRevenue(vehicles.get("veh-003"), "2026-03-26", "0", ActivityStatus.RAIN, "0", "0", "0", "0", "Pluie");
        saveRevenue(vehicles.get("veh-004"), "2026-03-26", "2900", ActivityStatus.ACTIVE, "0", "829", "2071", "600", null);
        saveRevenue(vehicles.get("veh-006"), "2026-03-25", "6200", ActivityStatus.ACTIVE, "0", "954", "5246", "300", null);
    }

    private void saveRevenue(Vehicle vehicle, String date, String amount, ActivityStatus status, String driverShare, String companyShare, String clientShare, String generatedDebt, String observation) {
        DailyRevenue revenue = new DailyRevenue();
        revenue.setVehicle(vehicle);
        revenue.setRevenueDate(LocalDate.parse(date));
        revenue.setAmount(new BigDecimal(amount));
        revenue.setActivityStatus(status);
        revenue.setDriverShare(new BigDecimal(driverShare));
        revenue.setCompanyShare(new BigDecimal(companyShare));
        revenue.setClientShare(new BigDecimal(clientShare));
        revenue.setGeneratedDebt(new BigDecimal(generatedDebt));
        revenue.setObservation(observation);
        dailyRevenueRepository.save(revenue);
    }

    private void seedDebts(Map<String, Vehicle> vehicles, Map<String, Driver> drivers) {
        saveDebt(vehicles.get("veh-001"), drivers.get("drv-001"), "2026-03-27", "600", "Recette inferieure a l'objectif journalier", "chauffeur", "Objectif non atteint", DebtStatus.OPEN);
        saveDebt(vehicles.get("veh-003"), drivers.get("drv-003"), "2026-03-27", "1300", "Ecart objectif moto-taxi", "client", "Recette insuffisante", DebtStatus.OPEN);
        saveDebt(vehicles.get("veh-004"), drivers.get("drv-004"), "2026-03-26", "1800", "Erreur de remise precedente", "entreprise", "Regularisation caisse", DebtStatus.PAID);
        saveDebt(vehicles.get("veh-005"), drivers.get("drv-005"), "2026-03-24", "5500", "Mission non assuree sans relais", "chauffeur", "Absence non justifiee", DebtStatus.OPEN);
    }

    private void saveDebt(Vehicle vehicle, Driver driver, String date, String amount, String reason, String beneficiary, String typeDebt, DebtStatus status) {
        Debt debt = new Debt();
        debt.setVehicle(vehicle);
        debt.setDriver(driver);
        debt.setDebtDate(LocalDate.parse(date));
        debt.setAmount(new BigDecimal(amount));
        debt.setReason(reason);
        debt.setBeneficiary(beneficiary);
        debt.setTypeDebt(typeDebt);
        debt.setStatus(status);
        if (status == DebtStatus.PAID) {
            debt.setPaidAmount(new BigDecimal(amount));
        }
        debtRepository.save(debt);
    }

    private void seedMaintenances(Map<String, Vehicle> vehicles) {
        saveMaintenance(vehicles.get("veh-002"), "2026-03-27", MaintenanceType.REPAIR, "Alternateur et courroie", "92000", "25000", 2, "Garage Medina Pro", "https://drive.google.com/file/d/repair-001", false, null);
        saveMaintenance(vehicles.get("veh-001"), "2026-03-25", MaintenanceType.OIL_CHANGE, "Vidange moteur et filtre huile", "15000", "6000", 1, "Fast Lube Dakar", "https://drive.google.com/file/d/repair-002", true, "Prestation tres proche sur moins de 30 jours");
        saveMaintenance(vehicles.get("veh-001"), "2026-03-16", MaintenanceType.OIL_CHANGE, "Vidange moteur et filtre huile", "15000", "6000", 1, "Fast Lube Dakar", "https://drive.google.com/file/d/repair-003", false, null);
        saveMaintenance(vehicles.get("veh-003"), "2026-03-20", MaintenanceType.BREAKDOWN, "Remplacement plaquettes de frein", "24000", "9000", 1, "MotoLab", "https://drive.google.com/file/d/repair-004", true, "Description proche et cout voisin sur 30 jours");
        saveMaintenance(vehicles.get("veh-003"), "2026-03-10", MaintenanceType.BREAKDOWN, "Remplacement plaquettes frein", "23000", "9000", 1, "MotoLab", "https://drive.google.com/file/d/repair-005", false, null);
        saveMaintenance(vehicles.get("veh-005"), "2026-03-14", MaintenanceType.FINE, "Amende stationnement", "0", "0", 1, "Municipalite", "https://drive.google.com/file/d/repair-006", false, null);
        saveMaintenance(vehicles.get("veh-008"), "2026-03-08", MaintenanceType.OTHER, "Kit casque et retroviseur", "12000", "3000", 2, "MotoLab", "https://drive.google.com/file/d/repair-007", false, null);
    }

    private void saveMaintenance(
            Vehicle vehicle,
            String date,
            MaintenanceType type,
            String description,
            String partsCost,
            String laborCost,
            int pieceCount,
            String provider,
            String documentUrl,
            boolean suspectedDuplicate,
            String fraudReason
    ) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        record.setMaintenanceDate(LocalDate.parse(date));
        record.setType(type);
        record.setDescription(description);
        record.setPartsCost(new BigDecimal(partsCost));
        record.setLaborCost(new BigDecimal(laborCost));
        record.setCost(new BigDecimal(partsCost).add(new BigDecimal(laborCost)));
        record.setPieceCount(pieceCount);
        record.setProvider(provider);
        record.setDocumentUrl(documentUrl);
        record.setSuspectedDuplicate(suspectedDuplicate);
        record.setFraudReason(fraudReason);
        record.setPieceLinesJson("[{\"description\":\"" + description + "\",\"prixPiece\":" + partsCost + "}]");
        maintenanceRecordRepository.save(record);
    }

    private void seedNotifications(Company company) {
        saveNotification(company, "Dette auto creee", "DK-3401-CC a genere 1 300 FCFA de dette client.", "/dashboard/debts", NotificationType.WARNING, false);
        saveNotification(company, "Validation maintenance attendue", "Le devis alternateur de DK-2402-BB attend la direction.", "/dashboard/maintenance", NotificationType.INFO, false);
        saveNotification(company, "Facture prete a etre envoyee", "Le package PDF Transit Horizon est complet avec le suivi reparations.", "/dashboard/invoicing", NotificationType.SUCCESS, true);
        saveNotification(company, "Alerte absence", "Fatou Kane absente, reaffectation recommandee avant 10h00.", "/dashboard/planning", NotificationType.ERROR, false);
    }

    private void saveNotification(Company company, String title, String message, String link, NotificationType type, boolean read) {
        Notification notification = new Notification();
        notification.setCompany(company);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setType(type);
        notification.setRead(read);
        notificationRepository.save(notification);
    }

    private void seedMessages(Company company, Map<String, UserAccount> users) {
        saveMessage(company, users.get("responsable"), users.get("chauffeur"), "Confirme la rotation aeroport et la remise recette avant 19h.", true, LocalDateTime.parse("2026-03-27T07:50:00"));
        saveMessage(company, users.get("chauffeur"), users.get("responsable"), "Vehicule ok, mais trafic dense. Je remonte le point a midi.", true, LocalDateTime.parse("2026-03-27T08:03:00"));
        saveMessage(company, users.get("assistant"), users.get("direction"), "Le dossier facture Transit Horizon contient les liens Drive et les deux PDFs.", false, LocalDateTime.parse("2026-03-27T09:20:00"));
        saveMessage(company, users.get("client"), users.get("assistant"), "Merci de joindre aussi le recap des maintenances sur la periode de mars.", false, LocalDateTime.parse("2026-03-27T09:31:00"));
    }

    private void saveMessage(Company company, UserAccount sender, UserAccount recipient, String content, boolean read, LocalDateTime createdAt) {
        Message message = new Message();
        message.setCompany(company);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setIsRead(read);
        message.setCreatedAt(createdAt);
        if (read) {
            message.setReadAt(createdAt.plusMinutes(5));
        }
        messageRepository.save(message);
    }

    private void seedPlanning(Company company, Map<String, UserAccount> users) {
        savePlanning(company, "2026-03-27", "Matin", "Mission", "Rotation aeroport corporate", users.get("chauffeur"), "Haute");
        savePlanning(company, "2026-03-27", "Matin", "Maintenance", "Validation devis alternateur", users.get("responsable"), "Critique");
        savePlanning(company, "2026-03-27", "Apres-midi", "Briefing", "Point exploitation avec assistants", users.get("assistant"), "Normale");
        savePlanning(company, "2026-03-27", "Soir", "Facturation", "Preparation dossier client Transit Horizon", users.get("direction"), "Haute");
        savePlanning(company, "2026-03-28", "Matin", "Absence", "Relais chauffeur sur DK-2403-EE", users.get("responsable"), "Critique");
    }

    private void savePlanning(Company company, String date, String slot, String type, String title, UserAccount owner, String priority) {
        PlanningEvent event = new PlanningEvent();
        event.setCompany(company);
        event.setEventDate(LocalDate.parse(date));
        event.setSlot(slot);
        event.setType(type);
        event.setTitle(title);
        event.setOwner(owner);
        event.setPriority(priority);
        planningEventRepository.save(event);
    }

    private void seedAudit(Company company) {
        saveAudit(company, "direction@orbitfleet.app", "maintenance.validate", "maintenance", "Validation devis alternateur DK-2402-BB");
        saveAudit(company, "assistant@orbitfleet.app", "revenue.create", "revenue", "Saisie recette DK-3401-CC et dette auto 1 300");
        saveAudit(company, "exploitation@orbitfleet.app", "planning.reassign", "planning", "Reaffectation mission DK-2403-EE suite absence chauffeur");
    }

    private void saveAudit(Company company, String actorEmail, String action, String module, String afterData) {
        AuditLog auditLog = new AuditLog();
        auditLog.setCompany(company);
        auditLog.setActorEmail(actorEmail);
        auditLog.setAction(action);
        auditLog.setModule(module);
        auditLog.setAfterData(afterData);
        auditLogRepository.save(auditLog);
    }
}
