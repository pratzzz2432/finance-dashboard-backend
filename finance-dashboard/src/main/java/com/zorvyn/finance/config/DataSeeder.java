package com.zorvyn.finance.config;

import com.zorvyn.finance.domain.entity.FinancialRecord;
import com.zorvyn.finance.domain.entity.Role;
import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.RecordType;
import com.zorvyn.finance.domain.enums.RoleType;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.RoleRepository;
import com.zorvyn.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds initial data on startup so the app is ready to use out of the box.
 * Only runs if the DB is empty (safe for restarts during dev).
 * In production you'd use Flyway/Liquibase migrations instead.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // skip if data already exists (idempotent)
        if (roleRepository.count() > 0) {
            return;
        }

        Role viewerRole = roleRepository.save(new Role(RoleType.VIEWER));
        Role analystRole = roleRepository.save(new Role(RoleType.ANALYST));
        Role adminRole = roleRepository.save(new Role(RoleType.ADMIN));

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@zorvyn.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFullName("System Admin");
        admin.setRole(adminRole);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        User analyst = new User();
        analyst.setUsername("analyst");
        analyst.setEmail("analyst@zorvyn.com");
        analyst.setPassword(passwordEncoder.encode("analyst123"));
        analyst.setFullName("Finance Analyst");
        analyst.setRole(analystRole);
        analyst.setStatus(UserStatus.ACTIVE);
        userRepository.save(analyst);

        User viewer = new User();
        viewer.setUsername("viewer");
        viewer.setEmail("viewer@zorvyn.com");
        viewer.setPassword(passwordEncoder.encode("viewer123"));
        viewer.setFullName("Dashboard Viewer");
        viewer.setRole(viewerRole);
        viewer.setStatus(UserStatus.ACTIVE);
        userRepository.save(viewer);

        seedFinancialRecords(admin);
    }

    private void seedFinancialRecords(User admin) {
        createRecord(new BigDecimal("5000.00"), RecordType.INCOME, "Salary", LocalDate.of(2026, 1, 5), "Monthly salary - January", admin);
        createRecord(new BigDecimal("1200.00"), RecordType.EXPENSE, "Rent", LocalDate.of(2026, 1, 7), "Office rent payment", admin);
        createRecord(new BigDecimal("350.00"), RecordType.EXPENSE, "Utilities", LocalDate.of(2026, 1, 10), "Electricity and internet", admin);
        createRecord(new BigDecimal("800.00"), RecordType.INCOME, "Freelance", LocalDate.of(2026, 1, 15), "Consulting gig", admin);
        createRecord(new BigDecimal("150.00"), RecordType.EXPENSE, "Office Supplies", LocalDate.of(2026, 1, 20), "Printer cartridges", admin);

        createRecord(new BigDecimal("5000.00"), RecordType.INCOME, "Salary", LocalDate.of(2026, 2, 5), "Monthly salary - February", admin);
        createRecord(new BigDecimal("1200.00"), RecordType.EXPENSE, "Rent", LocalDate.of(2026, 2, 7), "Office rent payment", admin);
        createRecord(new BigDecimal("280.00"), RecordType.EXPENSE, "Utilities", LocalDate.of(2026, 2, 10), "Electricity and internet", admin);
        createRecord(new BigDecimal("2000.00"), RecordType.INCOME, "Investments", LocalDate.of(2026, 2, 18), "Dividend payout", admin);
        createRecord(new BigDecimal("500.00"), RecordType.EXPENSE, "Marketing", LocalDate.of(2026, 2, 22), "Ad campaign", admin);

        createRecord(new BigDecimal("5000.00"), RecordType.INCOME, "Salary", LocalDate.of(2026, 3, 5), "Monthly salary - March", admin);
        createRecord(new BigDecimal("1200.00"), RecordType.EXPENSE, "Rent", LocalDate.of(2026, 3, 7), "Office rent payment", admin);
        createRecord(new BigDecimal("320.00"), RecordType.EXPENSE, "Utilities", LocalDate.of(2026, 3, 10), "Electricity and internet", admin);
        createRecord(new BigDecimal("1500.00"), RecordType.INCOME, "Freelance", LocalDate.of(2026, 3, 14), "Web development project", admin);
        createRecord(new BigDecimal("200.00"), RecordType.EXPENSE, "Office Supplies", LocalDate.of(2026, 3, 25), "Stationery and toner", admin);
    }

    private void createRecord(BigDecimal amount, RecordType type, String category,
                               LocalDate date, String description, User createdBy) {
        FinancialRecord record = new FinancialRecord();
        record.setAmount(amount);
        record.setType(type);
        record.setCategory(category);
        record.setDate(date);
        record.setDescription(description);
        record.setCreatedBy(createdBy);
        record.setDeleted(false);
        recordRepository.save(record);
    }
}
