package saas.com.br.resume_ai_saas.resume.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;
import saas.com.br.resume_ai_saas.exception.ResumeNotFoundException;
import saas.com.br.resume_ai_saas.resume.entity.Resume;
import saas.com.br.resume_ai_saas.resume.repository.ResumeRepository;
import saas.com.br.resume_ai_saas.storage.service.ResumeStorageService;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService IDOR & delete behavior")
class ResumeServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeStorageService storageService;
    @Mock private ResumeTextExtractionService textExtractionService;
    @Mock private ResumeTextRefinementService textRefinementService;
    @Mock private Executor executor;
    @Mock private TransactionTemplate transactionTemplate;

    private ResumeService service;

    private final UUID resumeId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID attackerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ResumeService(resumeRepository, storageService, textExtractionService,
                textRefinementService, executor, transactionTemplate);
    }

    @Test
    @DisplayName("findByIdForUser: cross-user access throws 404 and never touches Storage")
    void findByIdForUser_throwsNotFound_andNeverTouchesStorage_forNonOwner() {
        when(resumeRepository.findByIdAndUserId(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForUser(resumeId, attackerId))
                .isInstanceOf(ResumeNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("generatePresignedDownloadUrl: non-owner is blocked before any S3 presign call")
    void generatePresignedDownloadUrl_blockedBeforeS3_forNonOwner() {
        when(resumeRepository.findByIdAndUserId(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generatePresignedDownloadUrl(resumeId, attackerId, Duration.ofMinutes(5)))
                .isInstanceOf(ResumeNotFoundException.class);

        verify(storageService, never()).generatePresignedDownloadUrl(any(), any());
    }

    @Test
    @DisplayName("delete: removes the Storage object and the row for the owner")
    void delete_removesStorageObject_andRow() {
        Resume resume = Resume.builder()
                .id(resumeId)
                .userId(ownerId)
                .storageKey("resumes/" + ownerId + "/" + resumeId + ".pdf")
                .build();
        when(resumeRepository.findByIdAndUserId(resumeId, ownerId))
                .thenReturn(Optional.of(resume));

        service.delete(resumeId, ownerId);

        verify(storageService).delete("resumes/" + ownerId + "/" + resumeId + ".pdf");
        verify(resumeRepository).delete(resume);
    }

    @Test
    @DisplayName("delete: non-owner delete throws 404 and never deletes anything")
    void delete_throwsNotFound_forNonOwner() {
        when(resumeRepository.findByIdAndUserId(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(resumeId, attackerId))
                .isInstanceOf(ResumeNotFoundException.class);

        verify(resumeRepository, never()).delete(any(Resume.class));
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("findByUserId: queries only the authenticated user's resumes")
    void findByUserId_scopesQueryToUser() {
        Pageable pageable = PageRequest.of(0, 20);
        when(resumeRepository.findByUserId(ownerId, pageable))
                .thenReturn(Page.empty(pageable));

        service.findByUserId(ownerId, pageable);

        verify(resumeRepository).findByUserId(ownerId, pageable);
        verify(resumeRepository, never()).findAll();
    }
}
