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
import com.thinkcode.transportbackend.repository.MaintenanceRecordRepository;
import com.thinkcode.transportbackend.repository.MessageRepository;
import com.thinkcode.transportbackend.repository.NotificationRepository;
import com.thinkcode.transportbackend.repository.PlanningEventRepository;
import com.thinkcode.transportbackend.repository.RevenueRuleRepository;
import com.thinkcode.transportbackend.repository.SystemSettingRepository;
import com.thinkcode.transportbackend.repository.UserAccountRepository;
import com.thinkcode.transportbackend.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final DriverRepository driverRepository;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            CompanyRepository companyRepository,
            UserAccountRepository userAccountRepository,
            DriverRepository driverRepository,
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
            PasswordEncoder passwordEncoder
    ) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.driverRepository = driverRepository;
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
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (companyRepository.count() > 0) {
            return;
        }

        Company company = new Company();
        company.setName("OrbitFleet Demo");
        company.setCode("OF01");
        company = companyRepository.save(company);

        Map<String, UserAccount> users = new HashMap<>();
        users.put("direction", createUser(company, "Aissatou Fall", "direction@orbitfleet.app", RoleName.ADMIN));
        users.put("responsable", createUser(company, "Mamadou Ndiaye", "exploitation@orbitfleet.app", RoleName.OPERATIONS_MANAGER));
        users.put("assistant", createUser(company, "Seynabou Ba", "assistant@orbitfleet.app", RoleName.ASSISTANT));
        users.put("chauffeur", createUser(company, "Ousmane Diallo", "chauffeur@orbitfleet.app", RoleName.DRIVER));
        users.put("client", createUser(company, "Transit Horizon", "ops@transit-horizon.sn", RoleName.CLIENT));
        users.put("jokk", createUser(company, "Jokk Mobility", "flotte@jokkmobility.io", RoleName.CLIENT));
        users.put("sahel", createUser(company, "Sahel Corporate", "transport@sahel-corp.com", RoleName.CLIENT));
        users.put("rapid", createUser(company, "Rapid Loop", "client@rapidloop.africa", RoleName.CLIENT));

        Map<String, Driver> drivers = new HashMap<>();
        drivers.put("drv-001", createDriver(company, "Ousmane Diallo", "chauffeur@orbitfleet.app", "+221 77 900 10 11", "PERMIS-001", RoleName.DRIVER, "En course", 94));
        drivers.put("drv-002", createDriver(company, "Moussa Faye", "moussa@orbitfleet.app", "+221 77 900 10 12", "PERMIS-002", RoleName.DRIVER, "Disponible", 82));
        drivers.put("drv-003", createDriver(company, "Cheikh Gueye", "cheikh@orbitfleet.app", "+221 77 900 10 13", "PERMIS-003", RoleName.DRIVER, "En course", 88));
        drivers.put("drv-004", createDriver(company, "Malick Seck", "malick@orbitfleet.app", "+221 77 900 10 14", "PERMIS-004", RoleName.DRIVER, "Disponible", 79));
        drivers.put("drv-005", createDriver(company, "Fatou Kane", "fatou@orbitfleet.app", "+221 77 900 10 15", "PERMIS-005", RoleName.DRIVER, "Absent", 68));
        drivers.put("drv-006", createDriver(company, "Ibrahima Toure", "ibrahima@orbitfleet.app", "+221 77 900 10 16", "PERMIS-006", RoleName.DRIVER, "En course", 91));
        drivers.put("drv-007", createDriver(company, "Awa Sy", "awa@orbitfleet.app", "+221 77 900 10 17", "PERMIS-007", RoleName.DRIVER, "Conge", 86));
        drivers.put("drv-008", createDriver(company, "Bamba Diop", "bamba@orbitfleet.app", "+221 77 900 10 18", "PERMIS-008", RoleName.DRIVER, "Disponible", 84));
        drivers.put("ops-001", createDriver(company, "Mamadou Ndiaye", "exploitation@orbitfleet.app", "+221 77 400 20 01", "OPS-001", RoleName.OPERATIONS_MANAGER, "Disponible", 97));
        drivers.put("ops-002", createDriver(company, "Seynabou Ba", "assistant@orbitfleet.app", "+221 77 400 20 02", "AST-001", RoleName.ASSISTANT, "Disponible", 92));

        Map<String, Client> clients = new HashMap<>();
        clients.put("Transit Horizon", createClient(company, "Transit Horizon", "ops@transit-horizon.sn", "+221 77 100 00 01"));
        clients.put("Jokk Mobility", createClient(company, "Jokk Mobility", "flotte@jokkmobility.io", "+221 78 220 11 00"));
        clients.put("Sahel Corporate", createClient(company, "Sahel Corporate", "transport@sahel-corp.com", "+221 76 805 44 31"));
        clients.put("Rapid Loop", createClient(company, "Rapid Loop", "client@rapidloop.africa", "+221 70 404 88 19"));

        Map<String, Vehicle> vehicles = new HashMap<>();
        vehicles.put("veh-001", createVehicle(company, "DK-2401-AA", VehicleType.CAR, VehicleStatus.ASSIGNED, drivers.get("drv-001"), clients.get("Transit Horizon"), "8200000", LocalDate.of(2025, 12, 2), "13500"));
        vehicles.put("veh-002", createVehicle(company, "DK-2402-BB", VehicleType.CAR, VehicleStatus.IN_MAINTENANCE, drivers.get("drv-002"), clients.get("Transit Horizon"), "7900000", LocalDate.of(2025, 11, 16), "13500"));
        vehicles.put("veh-003", createVehicle(company, "DK-3401-CC", VehicleType.MOTO_TAXI, VehicleStatus.ASSIGNED, drivers.get("drv-003"), clients.get("Jokk Mobility"), "1800000", LocalDate.of(2026, 1, 3), "6500"));
        vehicles.put("veh-004", createVehicle(company, "DK-3402-DD", VehicleType.MOTORBIKE, VehicleStatus.ASSIGNED, drivers.get("drv-004"), clients.get("Jokk Mobility"), "950000", LocalDate.of(2026, 1, 21), "3500"));
        vehicles.put("veh-005", createVehicle(company, "DK-2403-EE", VehicleType.CAR, VehicleStatus.OUT_OF_SERVICE, drivers.get("drv-005"), clients.get("Sahel Corporate"), "9000000", LocalDate.of(2025, 9, 10), "13500"));
        vehicles.put("veh-006", createVehicle(company, "DK-3403-FF", VehicleType.MOTO_TAXI, VehicleStatus.ASSIGNED, drivers.get("drv-006"), clients.get("Rapid Loop"), "1650000", LocalDate.of(2026, 2, 1), "6500"));
        vehicles.put("veh-007", createVehicle(company, "DK-2404-GG", VehicleType.CAR, VehicleStatus.AVAILABLE, drivers.get("drv-007"), clients.get("Sahel Corporate"), "8700000", LocalDate.of(2025, 10, 12), "13500"));
        vehicles.put("veh-008", createVehicle(company, "DK-3404-HH", VehicleType.MOTORBIKE, VehicleStatus.ASSIGNED, drivers.get("drv-008"), clients.get("Rapid Loop"), "980000", LocalDate.of(2026, 2, 12), "3500"));

        seedRevenueRules(company);
        seedSystemSettings();
        seedRevenues(vehicles);
        seedDebts(vehicles, drivers);
        seedMaintenances(vehicles);
        seedNotifications(company);
        seedMessages(company, users);
        seedPlanning(company, users);
        seedAudit(company);
    }

    private UserAccount createUser(Company company, String fullName, String email, RoleName role) {
        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("demo1234"));
        user.setRole(role);
        user.setCompany(company);
        return userAccountRepository.save(user);
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
