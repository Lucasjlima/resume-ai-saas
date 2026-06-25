package saas.com.br.resume_ai_saas.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.exception.EmailAlreadyRegisteredException;
import saas.com.br.resume_ai_saas.user.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyRegisteredException(user.getEmail());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, String name, String email) {
        User user = findById(id);
        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) {
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new EmailAlreadyRegisteredException(email);
            }
            user.setEmail(email);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
