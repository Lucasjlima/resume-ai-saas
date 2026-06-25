package saas.com.br.resume_ai_saas.resume.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import saas.com.br.resume_ai_saas.resume.entity.ExtractionMethod;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.exception.UserNotFoundException;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.user.entity.User;
import saas.com.br.resume_ai_saas.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService Unit Tests")
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ResumeTextExtractionService textExtractionService;

    @InjectMocks
    private ResumeService resumeService;

    private User testUser;
    private Resume testResume;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .createdAt(Instant.now())
                .build();

        testResume = Resume.builder()
                .id(10L)
                .user(testUser)
                .fileName("resume.pdf")
                .fileUrl("local://uploads/1/resume.pdf")
                .rawText("Sample resume text")
                .extractionMethod(ExtractionMethod.PDF_PARSER)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("findByUserId() should return list of resumes for the user")
    void findByUserId_shouldReturnListOfResumes() {
        when(resumeRepository.findByUserId(1L)).thenReturn(List.of(testResume));

        List<Resume> result = resumeService.findByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testResume);
        verify(resumeRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("findByUserId() should return empty list when user has no resumes")
    void findByUserId_shouldReturnEmptyList_whenNoResumes() {
        when(resumeRepository.findByUserId(1L)).thenReturn(List.of());

        List<Resume> result = resumeService.findByUserId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findById() should return resume when found")
    void findById_shouldReturnResume_whenFound() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(testResume));

        Resume result = resumeService.findById(10L);

        assertThat(result).isEqualTo(testResume);
        assertThat(result.getId()).isEqualTo(10L);
        verify(resumeRepository).findById(10L);
    }

    @Test
    @DisplayName("findById() should throw ResumeNotFoundException when resume not found")
    void findById_shouldThrowResumeNotFoundException_whenNotFound() {
        when(resumeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.findById(99L))
                .isInstanceOf(ResumeNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("upload() should throw UserNotFoundException when user does not exist")
    void upload_shouldThrowUserNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf",
                "application/pdf", "PDF content".getBytes());

        assertThatThrownBy(() -> resumeService.upload(99L, file))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");

        verify(storageService, never()).store(anyLong(), any());
        verify(textExtractionService, never()).extract(any());
        verify(resumeRepository, never()).save(any());
    }

    @Test
    @DisplayName("upload() should store file, extract text, set PDF_PARSER method, and save")
    void upload_shouldStoreFileExtractTextAndSave() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf",
                "application/pdf", "PDF content".getBytes());
        String expectedUrl = "local://uploads/1/123456_resume.pdf";
        String expectedText = "Extracted resume text";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(storageService.store(1L, file)).thenReturn(expectedUrl);
        when(textExtractionService.extract(file)).thenReturn(expectedText);
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        Resume result = resumeService.upload(1L, file);

        assertThat(result.getUser()).isEqualTo(testUser);
        assertThat(result.getFileName()).isEqualTo("resume.pdf");
        assertThat(result.getFileUrl()).isEqualTo(expectedUrl);
        assertThat(result.getRawText()).isEqualTo(expectedText);
        assertThat(result.getExtractionMethod()).isEqualTo(ExtractionMethod.PDF_PARSER);
        assertThat(result.getCreatedAt()).isNotNull();

        verify(storageService).store(1L, file);
        verify(textExtractionService).extract(file);
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    @DisplayName("upload() should set extraction method to PDF_PARSER regardless of file type")
    void upload_shouldSetExtractionMethodToPdfParser() {
        MockMultipartFile file = new MockMultipartFile("file", "my-cv.pdf",
                "application/pdf", "content".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(storageService.store(eq(1L), any())).thenReturn("local://uploads/1/my-cv.pdf");
        when(textExtractionService.extract(any())).thenReturn("CV text");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        Resume result = resumeService.upload(1L, file);

        assertThat(result.getExtractionMethod()).isEqualTo(ExtractionMethod.PDF_PARSER);
    }

    @Test
    @DisplayName("delete() should delete resume when it exists")
    void delete_shouldDeleteResume_whenExists() {
        when(resumeRepository.existsById(10L)).thenReturn(true);

        resumeService.delete(10L);

        verify(resumeRepository).deleteById(10L);
    }

    @Test
    @DisplayName("delete() should throw ResumeNotFoundException when resume does not exist")
    void delete_shouldThrowResumeNotFoundException_whenNotFound() {
        when(resumeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> resumeService.delete(99L))
                .isInstanceOf(ResumeNotFoundException.class)
                .hasMessageContaining("99");

        verify(resumeRepository, never()).deleteById(anyLong());
    }
}
