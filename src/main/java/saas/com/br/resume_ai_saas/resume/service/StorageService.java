package saas.com.br.resume_ai_saas.resume.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    public String store(UUID userId, MultipartFile file) {
        try {
            Path userDir = Paths.get(uploadDir, userId.toString());
            Files.createDirectories(userDir);
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path destination = userDir.resolve(filename);
            file.transferTo(destination.toFile());
            return "local://uploads/" + userId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}
