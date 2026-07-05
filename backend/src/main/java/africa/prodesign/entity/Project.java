package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    private String location;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private String ownerId;

    private String complianceRuleCode;

    /** Plot width in meters (X axis in canvas coordinates), null until the user sets it. */
    private Double plotWidthMeters;

    /** Plot depth in meters (Y axis in canvas coordinates), null until the user sets it. */
    private Double plotDepthMeters;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
