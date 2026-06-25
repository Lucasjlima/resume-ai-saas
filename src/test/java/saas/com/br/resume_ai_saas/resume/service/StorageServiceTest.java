package saas.com.br.resume_ai_saas.resume.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StorageService Unit Tests")
class StorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
    }

    @Test
    @DisplayName("store() should create upload directory if it does not exist")
    void store_shouldCreateUploadDirectory_whenNotExists() throws Exception {
        Path userDir = tempDir.resolve("1");
        assertThat(userDir).doesNotExist();

        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "PDF content".getBytes()
        );

        storageService.store(1L, file);

        assertThat(userDir).isDirectory();
    }

    @Test
    @DisplayName("store() should save file and return URL starting with local://uploads/")
    void store_shouldSaveFileAndReturnLocalUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "PDF content".getBytes()
        );

        String url = storageService.store(1L, file);

        assertThat(url).startsWith("local://uploads/");
        assertThat(url).contains("resume.pdf");
    }

    @Test
    @DisplayName("store() should include userId in returned URL")
    void store_shouldIncludeUserIdInUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "content".getBytes()
        );

        String url = storageService.store(42L, file);

        assertThat(url).contains("/42/");
    }

    @Test
    @DisplayName("store() should generate unique filename for different calls")
    void store_shouldGenerateUniqueFilename_forDifferentCalls() throws InterruptedException {
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "Content 1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "Content 2".getBytes()
        );

        String url1 = storageService.store(1L, file1);
        // Small sleep to ensure different timestamps
        Thread.sleep(2);
        String url2 = storageService.store(1L, file2);

        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("store() should actually write file contents to disk")
    void store_shouldWriteFileContentsToDisk() throws Exception {
        byte[] expectedContent = "My resume PDF content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", expectedContent
        );

        String url = storageService.store(1L, file);

        // Extract the path portion from the URL
        // url format: local://uploads/{userId}/{filename}
        String relativePath = url.replace("local://uploads/", "");
        Path storedFile = tempDir.resolve(relativePath.replace("/", tempDir.getFileSystem().getSeparator()));
        assertThat(storedFile).exists();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("store() should handle nested user directories correctly")
    void store_shouldHandleMultipleUsersWithSeparateDirectories() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "content".getBytes()
        );

        String url1 = storageService.store(1L, file);
        String url2 = storageService.store(2L, file);

        assertThat(url1).contains("/1/");
        assertThat(url2).contains("/2/");
        assertThat(tempDir.resolve("1")).isDirectory();
        assertThat(tempDir.resolve("2")).isDirectory();
    }
}
