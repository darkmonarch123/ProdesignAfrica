package africa.prodesign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "drawing_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawingComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private double canvasX;

    @Column(nullable = false)
    private double canvasY;

    @Column(nullable = false, length = 2000)
    private String text;

    @Builder.Default
    private boolean resolved = false;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
