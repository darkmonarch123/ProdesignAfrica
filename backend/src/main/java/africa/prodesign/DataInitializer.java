package africa.prodesign;

import africa.prodesign.entity.ComplianceRuleSet;
import africa.prodesign.entity.MaterialRate;
import africa.prodesign.entity.User;
import africa.prodesign.enums.Role;
import africa.prodesign.repository.ComplianceRuleSetRepository;
import africa.prodesign.repository.MaterialRateRepository;
import africa.prodesign.repository.UserRepository;
import africa.prodesign.template.TemplateCatalogSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds baseline data on startup. Full seed set (compliance rules, material rates,
 * templates) lands in a follow-up pass — see Part 13 of the build spec. This pass
 * wires up the one seed path that must exist before anything else is safe to build
 * on top of: the admin account, created only from env vars, never hardcoded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ComplianceRuleSetRepository ruleSetRepository;
    private final MaterialRateRepository materialRateRepository;
    private final TemplateCatalogSeeder templateCatalogSeeder;
    private final PasswordEncoder passwordEncoder;

    @Value("${prodesign.admin.seed-email}")
    private String adminEmail;

    @Value("${prodesign.admin.seed-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedComplianceRuleSets();
        seedMaterialRates();
        templateCatalogSeeder.seed();
    }

    private void seedAdmin() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("ADMIN_SEED_EMAIL / ADMIN_SEED_PASSWORD not set — skipping admin seed.");
            return;
        }
        if (userRepository.existsByEmail(adminEmail.toLowerCase())) {
            log.info("Admin account already exists for {}", adminEmail);
            return;
        }
        User admin = User.builder()
                .email(adminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("Prodesign Africa Admin")
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded admin account for {}", adminEmail);
    }

    /**
     * Seeds representative setback/coverage/height rule sets for four
     * authorities. These figures are commonly-cited planning-standard values
     * for low-rise residential development, not verified against each
     * authority's current gazetted regulations — replace with
     * authority-confirmed figures before using this for a real submission.
     */
    private void seedComplianceRuleSets() {
        if (ruleSetRepository.count() > 0) {
            log.info("Compliance rule sets already seeded, skipping.");
            return;
        }
        List<ComplianceRuleSet> ruleSets = List.of(
                ComplianceRuleSet.builder()
                        .code("LASDRI")
                        .country("Nigeria")
                        .region("Lagos State")
                        .name("Lagos State Development and Regulation of Infrastructure")
                        .frontSetbackMeters(3.0)
                        .sideSetbackMeters(1.5)
                        .rearSetbackMeters(3.0)
                        .roadReserveMeters(3.0)
                        .maxPlotCoveragePercent(50.0)
                        .maxHeightMeters(9.0)
                        .build(),
                ComplianceRuleSet.builder()
                        .code("FCTDA")
                        .country("Nigeria")
                        .region("Federal Capital Territory")
                        .name("Federal Capital Territory Development Authority")
                        .frontSetbackMeters(4.5)
                        .sideSetbackMeters(2.0)
                        .rearSetbackMeters(3.0)
                        .roadReserveMeters(4.5)
                        .maxPlotCoveragePercent(45.0)
                        .maxHeightMeters(9.0)
                        .build(),
                ComplianceRuleSet.builder()
                        .code("KEBS")
                        .country("Kenya")
                        .region("National")
                        .name("Kenya Bureau of Standards — residential building code")
                        .frontSetbackMeters(6.0)
                        .sideSetbackMeters(3.0)
                        .rearSetbackMeters(3.0)
                        .roadReserveMeters(6.0)
                        .maxPlotCoveragePercent(50.0)
                        .maxHeightMeters(9.0)
                        .build(),
                ComplianceRuleSet.builder()
                        .code("NHBRC")
                        .country("South Africa")
                        .region("National")
                        .name("National Home Builders Registration Council")
                        .frontSetbackMeters(5.0)
                        .sideSetbackMeters(1.5)
                        .rearSetbackMeters(3.0)
                        .roadReserveMeters(5.0)
                        .maxPlotCoveragePercent(60.0)
                        .maxHeightMeters(8.5)
                        .build()
        );
        ruleSetRepository.saveAll(ruleSets);
        log.info("Seeded {} compliance rule sets", ruleSets.size());
    }

    /**
     * Seeds illustrative placeholder unit rates for the BOQ categories BoqService
     * currently prices (blockwork, openings, flooring, plastering). Like the
     * compliance figures, these are reasonable ballpark figures for their
     * category, not a live market pricing feed — replace before quoting a real job.
     */
    private void seedMaterialRates() {
        if (materialRateRepository.count() > 0) {
            log.info("Material rates already seeded, skipping.");
            return;
        }
        List<MaterialRate> rates = List.of(
                MaterialRate.builder()
                        .itemCode("BLOCKWORK_150MM")
                        .category("Blockwork")
                        .description("150mm sandcrete blockwork in cement mortar (net of openings)")
                        .unit("m2")
                        .rateNgn(8500).rateGhs(180).rateKes(2200).rateZar(650)
                        .build(),
                MaterialRate.builder()
                        .itemCode("PLASTERING_2SIDES")
                        .category("Plastering")
                        .description("Cement/sand plastering to both wall faces")
                        .unit("m2")
                        .rateNgn(3200).rateGhs(65).rateKes(850).rateZar(220)
                        .build(),
                MaterialRate.builder()
                        .itemCode("DOOR_STANDARD")
                        .category("Openings")
                        .description("Standard door — supply and fix, including frame and ironmongery")
                        .unit("unit")
                        .rateNgn(65000).rateGhs(1400).rateKes(18000).rateZar(3800)
                        .build(),
                MaterialRate.builder()
                        .itemCode("WINDOW_STANDARD")
                        .category("Openings")
                        .description("Standard window — supply and fix, including frame and glazing")
                        .unit("unit")
                        .rateNgn(45000).rateGhs(950).rateKes(12000).rateZar(2600)
                        .build(),
                MaterialRate.builder()
                        .itemCode("FLOOR_TILING")
                        .category("Flooring")
                        .description("Ceramic floor tiling, standard grade, including screed and adhesive")
                        .unit("m2")
                        .rateNgn(9500).rateGhs(200).rateKes(2400).rateZar(680)
                        .build()
        );
        materialRateRepository.saveAll(rates);
        log.info("Seeded {} material rates", rates.size());
    }
}
