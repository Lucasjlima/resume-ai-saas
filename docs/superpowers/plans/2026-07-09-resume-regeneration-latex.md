# Resume Regeneration via AI + LaTeX — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** From an existing AI analysis (feedback) of a resume vs. a job description, let the user trigger an async regeneration of the resume that incorporates the suggested improvements and produces a LaTeX-compiled PDF stored in Supabase Storage.

**Architecture:** New `regeneration` module mirroring the existing `analyse` module (controller → service → repository, async via `CompletableFuture` + `applicationTaskExecutor` + `TransactionTemplate`). Pipeline: AI (Spring AI/Gemini, JSON output) → validation → FreeMarker `.tex` template → external LaTeX compiler (tectonic/pdflatex via ProcessBuilder) → Supabase Storage upload. Rate limit via DB count query; automatic retries inside the same row (`retry_count`), user attempts tracked via `attempt_number`.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring AI 2.0 (Google GenAI), JPA/Hibernate + Flyway (Postgres/Supabase), FreeMarker, AWS S3 SDK (Supabase Storage), JUnit 5 + Mockito + MockMvc.

## Spec → codebase adaptations (locked decisions)

- Spec's `resume_feedbacks` **is** the existing `analyses` table (entity `Analysis`, `BIGINT` id). FK column: `analysis_id BIGINT`. Endpoints use `/analyses/{analysisId}` instead of `/feedbacks/{feedbackId}`.
- Base path `/api` (codebase pattern): `POST /api/resumes/{resumeId}/analyses/{analysisId}/regenerations`, etc.
- RLS policies are managed in the Supabase dashboard (V1 has none in migrations) — SQL for the ops note goes in the PR description, not in Flyway.
- Storage keys: `resume-regenerations/{userId}/{regenerationId}.pdf` in the same bucket (`supabase.storage.bucket`).
- `generated_json` persisted as soon as the AI succeeds; automatic retries reuse it instead of re-calling the AI.
- Baseline is broken (hard-delete refactor left `ResumeNotFoundException::new` calls and stale soft-delete tests) — fixed first in Task 0.

## Global Constraints

- Java 21, Spring Boot parent 4.1.0, Spring AI BOM 2.0.0 (already pinned).
- Follow existing module layout: `<module>/{controller,dto/{request,response},entity,exception,mapper,repository,service}`.
- Async pattern: constructor-injected `@Qualifier("applicationTaskExecutor") Executor` + `TransactionTemplate`; state transitions in `transactionTemplate.execute*`.
- Errors: module exception classes handled in `GlobalExceptionHandler` (404 no-leak for ownership misses; 403 via `@PreAuthorize`).
- Config: `regeneration.rate-limit.daily=5`, `regeneration.max-retries=2`, `latex.compiler.command=${LATEX_COMPILER_COMMAND:tectonic}`.
- Tests: JUnit5 + Mockito (`@ExtendWith(MockitoExtension.class)`) for services; `@WebMvcTest` + manual SecurityContext + `@EnableMethodSecurity` import pattern for controllers (copy `ResumeControllerTest` scaffold).

---

### Task 0: Fix broken baseline (pre-existing)

**Files:**
- Modify: `src/main/java/saas/com/br/resume_ai_saas/resume/service/ResumeService.java:110,129,151` — replace `ResumeNotFoundException::new` with `() -> new ResumeNotFoundException(<id>)`.
- Modify: `src/test/java/saas/com/br/resume_ai_saas/security/ResumeSecurityTest.java` — `existsByIdAndUserIdAndDeletedAtIsNull` → `existsByIdAndUserId`.
- Modify: `src/test/java/saas/com/br/resume_ai_saas/resume/service/ResumeServiceTest.java` — hard-delete semantics: `findByIdAndUserId`, `delete(...)` deletes storage object then row; drop soft-delete tests.
- Modify: `src/test/java/saas/com/br/resume_ai_saas/resume/controller/ResumeControllerTest.java` — `softDelete` → `delete`.

- [ ] Fix compile errors, update tests, run `./mvnw test` → all green.
- [ ] Commit `fix: repair build after hard-delete refactor (stale soft-delete tests)`.

### Task 1: Migration + entity + repository

**Files:**
- Create: `src/main/resources/db/migration/V2__resume_regenerations.sql`
- Create: `regeneration/entity/ResumeRegeneration.java`, `regeneration/entity/RegenerationStatus.java`
- Create: `regeneration/repository/ResumeRegenerationRepository.java`

```sql
CREATE TABLE IF NOT EXISTS resume_regenerations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id BIGINT NOT NULL,
    generated_json JSONB,
    pdf_storage_path TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    attempt_number INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_resume_regenerations_analysis FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_resume_regenerations_analysis_id ON resume_regenerations(analysis_id);
```

**Interfaces (produced):**
- `ResumeRegeneration` — `UUID id` (GenerationType.UUID), `Analysis analysis` (ManyToOne LAZY), `GeneratedResume generatedJson` (`@JdbcTypeCode(SqlTypes.JSON)`), `String pdfStoragePath`, `RegenerationStatus status`, `String failureReason`, `int retryCount`, `int attemptNumber`, `Instant createdAt`, `Instant updatedAt`. Lombok `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- `RegenerationStatus`: `PENDING, PROCESSING, DONE, FAILED`.
- Repository: `List<ResumeRegeneration> findByAnalysisId(Long analysisId)`; `int countByAnalysisId(Long analysisId)`; `@Query` `long countUserAttemptsSince(UUID userId, Instant since)` (join `r.analysis.resume.userId`); `@Query` `List<String> findPdfStoragePathsByResumeId(UUID resumeId)` (non-null paths).

- [ ] Write entity/enum/repository + migration; compile; commit `feat(regeneration): add resume_regenerations schema, entity and repository`.

### Task 2: GeneratedResume DTO + validation

**Files:**
- Create: `regeneration/dto/response/GeneratedResume.java` (record + nested records, `@JsonProperty` snake_case: `dados_pessoais{nome,email,telefone,linkedin,localizacao}`, `resumo_profissional`, `experiencias[{cargo,empresa,data_inicio,data_fim,atual,bullets[]}]`, `educacao[{curso,instituicao,data_inicio,data_fim}]`, `skills[]`, `certificacoes[{nome,instituicao,data}]`, `idiomas[{idioma,nivel}]`)
- Create: `regeneration/service/GeneratedResumeValidator.java`, `regeneration/exception/InvalidGeneratedResumeException.java`
- Test: `regeneration/service/GeneratedResumeValidatorTest.java`

**Interfaces (produced):** `GeneratedResume validateAndNormalize(GeneratedResume input)` — throws `InvalidGeneratedResumeException` when `dados_pessoais`/`nome`/`resumo_profissional` missing or blank; returns a copy with null lists normalized to `[]` (spec: absent sections are empty arrays, never invented).

- [ ] TDD: failing tests (missing nome → throws; null lists → `[]`; valid passes through) → implement → green → commit.

### Task 3: AI regeneration service

**Files:**
- Create: `regeneration/service/AiResumeRegenerationService.java`
- (No unit test — thin ChatClient wrapper, same as `AiAnalysisService`.)

**Interfaces (produced):** `GeneratedResume regenerate(String resumeText, Feedback feedback, String jobDescription)` — ChatClient (gemini-2.5-flash, JSON mime, temp 0.1), prompt rules: restructure into the exact JSON schema; incorporate improvements/gaps from feedback; NEVER invent facts, employers, dates or certifications; absent sections = `[]`; dates copied free-form as found; same language as the resume; returns `.entity(GeneratedResume.class)`.

- [ ] Implement + compile + commit.

### Task 4: LaTeX escaping + FreeMarker template rendering

**Files:**
- Modify: `pom.xml` — add `org.freemarker:freemarker` (version from Boot BOM).
- Create: `regeneration/service/LatexEscaper.java` (static `escape(String)`: `\ { } $ & # ^ _ % ~` → LaTeX-safe)
- Create: `src/main/resources/templates/latex/resume.tex.ftl` (single-column resume; conditional sections; expects pre-escaped model)
- Create: `regeneration/service/LatexTemplateService.java`
- Test: `regeneration/service/LatexEscaperTest.java`, `regeneration/service/LatexTemplateServiceTest.java`

**Interfaces (produced):** `String render(GeneratedResume resume)` — escapes every string field, builds template model, processes `resume.tex.ftl`, returns `.tex` source. Throws `LatexTemplateException` (create in `regeneration/exception/`) on template errors.

- [ ] TDD: escaper tests (`&` → `\&`, `100%` → `100\%`, backslash → `\textbackslash{}` etc.) → implement.
- [ ] TDD: template test (render sample → contains `\section*{...}` with escaped values; empty certificacoes → no Certificações section; `atual=true` → "Atual") → implement template + service → green → commit.

### Task 5: LaTeX compilation

**Files:**
- Create: `regeneration/service/LatexCompilationService.java`, `regeneration/exception/LatexCompilationException.java`
- Test: `regeneration/service/LatexCompilationServiceTest.java`

**Interfaces (produced):** `byte[] compile(String texSource)` — writes `resume.tex` in a fresh temp dir, runs `${latex.compiler.command}` (`tectonic resume.tex` default; also supports pdflatex-style flags transparently since both accept the file argument), 120s timeout, checks exit code + PDF existence, returns bytes, always cleans temp dir. Throws `LatexCompilationException` with tail of compiler output.

- [ ] TDD: nonexistent compiler command → `LatexCompilationException` (portable failure-path test) → implement → green → commit.

### Task 6: Storage support for regenerated PDFs

**Files:**
- Modify: `storage/service/ResumeStorageService.java`
- Test: extend none (S3 client wrapper, same as existing untested methods).

**Interfaces (produced):** `String uploadRegeneration(UUID userId, UUID regenerationId, byte[] pdfBytes)` → key `resume-regenerations/{userId}/{regenerationId}.pdf`; `deleteAllForUser` also purges prefix `resume-regenerations/{userId}/`. Reuse existing `download(String)`/`delete(String)`.

- [ ] Implement + compile + commit.

### Task 7: Regeneration pipeline service (rate limit, retries, async)

**Files:**
- Create: `regeneration/service/RegenerationService.java`
- Create: `regeneration/exception/RegenerationNotFoundException.java`, `regeneration/exception/RegenerationRateLimitExceededException.java`, `regeneration/exception/RegenerationNotReadyException.java`
- Create: `regeneration/mapper/RegenerationMapper.java`
- Modify: `src/main/resources/application.properties` (rate limit / retries / compiler command properties)
- Test: `regeneration/service/RegenerationServiceTest.java`

**Interfaces (produced):**
- `ResumeRegeneration regenerate(UUID resumeId, Long analysisId, UUID userId)` — validates chain (analysis belongs to resume → else `AnalysisNotFoundException`), rate-limit count last 24h ≥ N → `RegenerationRateLimitExceededException`, `attemptNumber = countByAnalysisId + 1`, saves PENDING, fires `CompletableFuture.runAsync(() -> processRegenerationAsync(id), executor)`.
- `void processRegenerationAsync(UUID regenerationId)` — loop: PROCESSING → (reuse persisted `generatedJson` or call AI + validate + persist json) → render → compile → upload → DONE. On exception: `retryCount < maxRetries` → increment & loop; else FAILED + `failureReason`. `attemptNumber` never changes here.
- `ResumeRegeneration findByIdForResume(UUID regenerationId, UUID resumeId)` — chain check else `RegenerationNotFoundException`.
- `List<ResumeRegeneration> findByAnalysis(UUID resumeId, Long analysisId)`.
- `byte[] downloadPdf(UUID regenerationId, UUID resumeId)` — only when DONE, else `RegenerationNotReadyException`.

- [ ] TDD (mock repo/AI/latex/storage/TransactionTemplate-as-passthrough): rate limit throws & saves nothing; attempt_number = prior count+1; chain mismatch → 404 exceptions; success path → DONE + pdf path; one failure then success → retryCount=1, DONE, attempt unchanged; retries exhausted → FAILED + reason; json reuse → AI called once across retries; download not DONE → throws → implement → green → commit.

### Task 8: Controller + response DTOs + exception handling

**Files:**
- Create: `regeneration/controller/RegenerationController.java`, `regeneration/dto/response/RegenerationResponse.java`
- Modify: `exception/GlobalExceptionHandler.java` (404 list + 429 + 409 handlers)
- Test: `regeneration/controller/RegenerationControllerTest.java`

**Interfaces (produced):**
- `RegenerationResponse(UUID id, Long analysisId, RegenerationStatus status, int attemptNumber, int retryCount, String failureReason, Instant createdAt, Instant updatedAt)`.
- `POST /api/resumes/{resumeId}/analyses/{analysisId}/regenerations` → 202 + body; `GET /api/resumes/{resumeId}/regenerations/{id}` → 200; `GET .../regenerations/{id}/download` → 200 `application/pdf` (Content-Disposition attachment); `GET /api/resumes/{resumeId}/analyses/{analysisId}/regenerations` → 200 list. All `@PreAuthorize("@resumeSecurity.isOwner(#resumeId)")`.

- [ ] TDD (WebMvcTest scaffold from `ResumeControllerTest`): owner POST → 202; non-owner POST → 403, service never called; rate-limited → 429; GET status 200; download DONE → 200 pdf bytes; download not ready → 409; cross-user GET → 404 → implement → green → commit.

### Task 9: Resume delete → purge regenerated PDFs from Storage

**Files:**
- Modify: `resume/service/ResumeService.java` (`delete`: fetch `findPdfStoragePathsByResumeId`, delete each object, then delete row → cascade)
- Test: extend `resume/service/ResumeServiceTest.java`

- [ ] TDD: delete removes regeneration storage objects + resume object + row; non-owner still 404 & storage untouched → implement → green → commit.

### Task 10: Verification + PR

- [ ] `./mvnw test` full suite green.
- [ ] Push branch `claude/resume-regeneration-ai-latex-f83149`, open PR to `main` with spec-adaptation notes + Supabase RLS ops note.

## Self-review

- Spec coverage: §3 schema→T1; §3.2 JSON→T2; §4 pipeline→T3–T7; §5 retry→T7; §5.1 storage→T6; §6 rate limit→T7; §7 endpoints→T8; §8 security→T8 (`@PreAuthorize`) + chain checks T7, RLS→PR note; §10 cascade+storage→T9. Out of scope honored (no frontend, no LaTeX infra deploy, single template, no date parsing).
- Type consistency: `GeneratedResume` produced T2, consumed T3/T4/T7; `Feedback` reused from analyse module.
