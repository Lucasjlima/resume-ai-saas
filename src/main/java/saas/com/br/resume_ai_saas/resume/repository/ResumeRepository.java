package saas.com.br.resume_ai_saas.resume.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.com.br.resume_ai_saas.resume.entity.Resume;

import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserId(UUID userId);
}
