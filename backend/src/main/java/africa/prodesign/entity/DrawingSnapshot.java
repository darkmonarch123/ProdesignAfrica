package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "drawing_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String schemaVersion;

    @Lob
    @Column(nullable = false)
    private String canvasStateJson;

    @Column(nullable = false)
    private String createdByUserId;

    @Builder.Default
    private boolean isAutoSave = true;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
