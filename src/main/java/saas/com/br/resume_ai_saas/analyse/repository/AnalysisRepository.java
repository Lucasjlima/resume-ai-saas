package saas.com.br.resume_ai_saas.analyse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.com.br.resume_ai_saas.analyse.entity.Analysis;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByResumeId(Long resumeId);
}
