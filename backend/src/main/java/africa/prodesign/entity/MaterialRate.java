package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A material/labor unit rate used to price BOQ line items. Rates are stored
 * per currency to support the multi-country pricing shown on the landing
 * page (NGN/GHS/KES/ZAR). Seeded with illustrative market-rate figures in
 * DataInitializer — like the compliance rule sets, these are reasonable
 * placeholder figures, not a live pricing feed, and should be replaced with
 * real sourced rates (or a live rates API) before being used for an actual
 * quotation.
 */
@Entity
@Table(name = "material_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialRate {

    @Id
    private String itemCode; // e.g. "BLOCKWORK_150MM"

    @Column(nullable = false)
    private String category; // e.g. "Blockwork", "Openings", "Flooring", "Plastering"

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String unit; // e.g. "m2", "m3", "unit"

    @Column(nullable = false)
    private double rateNgn;

    @Column(nullable = false)
    private double rateGhs;

    @Column(nullable = false)
    private double rateKes;

    @Column(nullable = false)
    private double rateZar;
}
