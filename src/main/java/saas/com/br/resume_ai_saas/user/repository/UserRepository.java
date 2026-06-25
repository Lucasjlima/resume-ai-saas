package saas.com.br.resume_ai_saas.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import saas.com.br.resume_ai_saas.user.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
