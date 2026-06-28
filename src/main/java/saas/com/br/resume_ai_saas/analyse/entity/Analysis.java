package saas.com.br.resume_ai_saas.analyse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import saas.com.br.resume_ai_saas.analyse.dto.response.Feedback;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.time.Instant;

@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feedback_json", nullable = false)
    private Feedback feedback;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
