package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false)
    private String style;

    @Column(nullable = false)
    private double suggestedPlotWidthMeters;

    @Column(nullable = false)
    private double suggestedPlotDepthMeters;

    @Lob
    @Column(nullable = false)
    private String canvasStateJson;

    @Lob
    @Column(nullable = false)
    private String thumbnailSvg;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
