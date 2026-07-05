package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A named set of setback/coverage/height rules for a country or state-level
 * building authority (e.g. LASDRI for Lagos State). Seeded on startup in
 * DataInitializer.
 *
 * The figures seeded here are representative planning-standard figures
 * commonly cited for residential low-rise development, NOT verified against
 * the current gazetted regulations of each authority. They exist so the
 * compliance engine has real rules to evaluate against; treat them as
 * placeholders to be replaced with authority-verified figures before this
 * is used for an actual submission.
 */
@Entity
@Table(name = "compliance_rule_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceRuleSet {

    @Id
    private String code; // e.g. "LASDRI"

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double frontSetbackMeters;

    @Column(nullable = false)
    private double sideSetbackMeters;

    @Column(nullable = false)
    private double rearSetbackMeters;

    @Column(nullable = false)
    private double roadReserveMeters;

    @Column(nullable = false)
    private double maxPlotCoveragePercent;

    @Column(nullable = false)
    private double maxHeightMeters;
}
