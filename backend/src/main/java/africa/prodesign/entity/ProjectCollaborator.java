package africa.prodesign.entity;

import africa.prodesign.enums.CollabRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "project_collaborators", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"projectId", "userId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollabRole role;

    @Builder.Default
    private Instant invitedAt = Instant.now();
}
