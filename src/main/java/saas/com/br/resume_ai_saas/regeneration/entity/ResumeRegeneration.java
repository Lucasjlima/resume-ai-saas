package saas.com.br.resume_ai_saas.regeneration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import saas.com.br.resume_ai_saas.regeneration.dto.response.GeneratedResume;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_regenerations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeRegeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_json")
    private GeneratedResume generatedJson;

    @Column(name = "pdf_storage_path")
    private String pdfStoragePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegenerationStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
