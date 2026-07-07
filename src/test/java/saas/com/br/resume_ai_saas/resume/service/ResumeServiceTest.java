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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeService IDOR & soft-delete behavior")
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
        when(resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByIdForUser(resumeId, attackerId))
                .isInstanceOf(ResumeNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("generatePresignedDownloadUrl: non-owner is blocked before any S3 presign call")
    void generatePresignedDownloadUrl_blockedBeforeS3_forNonOwner() {
        when(resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.generatePresignedDownloadUrl(resumeId, attackerId, Duration.ofMinutes(5)))
                .isInstanceOf(ResumeNotFoundException.class);

        verify(storageService, never()).generatePresignedDownloadUrl(any(), any());
    }

    @Test
    @DisplayName("softDelete: stamps deletedAt, saves, and retains the Storage object")
    void softDelete_setsDeletedAt_andRetainsStorageObject() {
        Resume resume = Resume.builder()
                .id(resumeId)
                .userId(ownerId)
                .storageKey("resumes/" + ownerId + "/" + resumeId + ".pdf")
                .build();
        when(resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(resumeId, ownerId))
                .thenReturn(Optional.of(resume));

        service.softDelete(resumeId, ownerId);

        assertThat(resume.getDeletedAt()).isNotNull();
        verify(resumeRepository).save(resume);
        verify(storageService, never()).delete(any());
    }

    @Test
    @DisplayName("softDelete: non-owner delete throws 404 and never saves")
    void softDelete_throwsNotFound_forNonOwner() {
        when(resumeRepository.findByIdAndUserIdAndDeletedAtIsNull(resumeId, attackerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.softDelete(resumeId, attackerId))
                .isInstanceOf(ResumeNotFoundException.class);

        verify(resumeRepository, never()).save(any());
        verifyNoInteractions(storageService);
    }

    @Test
    @DisplayName("findByUserId: lists only active (non soft-deleted) resumes")
    void findByUserId_usesActiveOnlyQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(resumeRepository.findByUserIdAndDeletedAtIsNull(ownerId, pageable))
                .thenReturn(Page.empty(pageable));

        service.findByUserId(ownerId, pageable);

        verify(resumeRepository).findByUserIdAndDeletedAtIsNull(ownerId, pageable);
        verify(resumeRepository, never()).findAll();
    }
}
