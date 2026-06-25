# Agent Orchestration Plan: User CRUD & AI Resume Analysis

This document details the tasks and orchestration plan to implement the User CRUD operations and the AI-based Resume Analysis service, mapping directly to Jira tickets created in project **Engenharia De Software (EDS)**.

---

## 1. Jira Tickets Reference

- **Parent Task**: [EDS-17: User CRUD & AI Resume Analysis Implementation](https://lucasjlima2006.atlassian.net/browse/EDS-17)
- **Subtasks**:
  - [EDS-18: User CRUD, DTO Validation & Custom Exceptions](https://lucasjlima2006.atlassian.net/browse/EDS-18)
  - [EDS-19: Resume File Upload & Text Extraction](https://lucasjlima2006.atlassian.net/browse/EDS-19)
  - [EDS-20: AI Resume Analysis Service via Google GenAI](https://lucasjlima2006.atlassian.net/browse/EDS-20)
  - [EDS-21: QA, Validation & End-to-End Integration Tests](https://lucasjlima2006.atlassian.net/browse/EDS-21)

---

## 2. Claude Code Agent Team Initialization Prompt

Copy and paste the prompt below into Claude Code to spin up the team:

```text
You are spinning up an autonomous developer agent team to implement User CRUD and AI-based Resume Analysis in a Spring Boot application. 
Please read the orchestration plan: file:///c:/Users/lucas/GITHUB/resume-ai-saas/agent_orchestration_plan.md.

Form the following team:
1. Lead Coordinator & Integration Agent (Model: Claude 3.5 Sonnet / Claude 3.7 Sonnet) - responsible for overall codebase consistency, pom.xml configurations, application context, and end-to-end integration.
2. User CRUD Architect (Model: Claude 3.5 Sonnet) - responsible for implementing EDS-18 (User CRUD, DTO validation, Mapper, and Controller-Service architecture).
3. Resume Upload & Extraction Architect (Model: Claude 3.5 Sonnet) - responsible for implementing EDS-19 (Local storage simulator, multipart file upload, PDF parser, and Resume service/repository).
4. AI Analysis Architect (Model: Claude 3.5 Sonnet) - responsible for implementing EDS-20 (Spring AI Google GenAI integration, resume-job description scoring logic, JSON response format parsing, and Analysis repository).
5. QA & Devil's Advocate Agent (Model: Claude 3.5 Sonnet / Claude 3.7 Sonnet) - responsible for implementing EDS-21 (Unit/Integration testing, mocking GenAI, validating exceptions, and edge-cases).

Follow the Git Flow and Coding Standards outlined in the orchestration plan. Each module developer should create their respective branch, commit conventionally, and open a PR via GitHub MCP. Ensure no direct commits to main.
```

---

## 3. Teammate Roles & Objectives

### Role 1: Lead Coordinator & Integration Agent
* **Recommended Model**: Claude 3.5/3.7 Sonnet
* **Objective**: Maintain workspace integration, configure general infrastructure, properties, dependencies, and resolve merge conflicts.
* **Key Tasks**:
  - Add `spring-boot-starter-validation` dependency to `pom.xml`.
  - Configure `application.properties` with database, storage directory paths, and Google GenAI API configurations.
  - Coordinate the assembly of controllers, services, and repositories.

### Role 2: User CRUD Architect
* **Recommended Model**: Claude 3.5 Sonnet
* **Objective**: Implement the complete User CRUD API (EDS-18) adhering to the Controller-Service-Mapper design pattern.
* **Key Tasks**:
  - Create [UserRepository](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/user/repository/UserRepository.java).
  - Implement [UserService](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/user/service/UserService.java) operating solely with pure [User](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/user/entity/User.java) entities.
  - Implement request and response DTOs (`UserRequest`, `UserResponse`) with JSR-380 validation annotations (`@NotBlank`, `@Email`, etc.).
  - Implement [UserMapper](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/user/mapper/UserMapper.java) for mapping.
  - Implement [UserController](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/user/controller/UserController.java) to handle conversion to/from requests and responses, interacting with the service using only pure entities.
  - Implement custom runtime exceptions (e.g. `EmailAlreadyRegisteredException`, `UserNotFoundException`) and map them in a [GlobalExceptionHandler](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/exception/GlobalExceptionHandler.java).

### Role 3: Resume Upload & Extraction Architect
* **Recommended Model**: Claude 3.5 Sonnet
* **Objective**: Implement multipart resume file upload, simulated local storage, and text extraction from PDF files (EDS-19).
* **Key Tasks**:
  - Create [ResumeRepository](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/resume/repository/ResumeRepository.java).
  - Implement `StorageService` to save uploaded files locally under a directory (e.g., `uploads/`) and output a simulation URL.
  - Implement `ResumeTextExtractionService` using `spring-ai-pdf-document-reader` (or Apache PDFBox if needed) to extract raw text from incoming PDFs.
  - Implement `ResumeService` and `ResumeController` (`/api/users/{userId}/resumes`) converting requests to pure entities and saving the extracted text under `rawText` and extraction method under `extractionMethod`.

### Role 4: AI Analysis Architect
* **Recommended Model**: Claude 3.5 Sonnet
* **Objective**: Build the AI engine comparing resume raw text with user-provided job descriptions using Google GenAI (EDS-20).
* **Key Tasks**:
  - Create [AnalysisRepository](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/analyse/repository/AnalysisRepository.java).
  - Implement `AiAnalysisService` using Spring AI's ChatModel with Gemini.
  - Formulate structured prompts instructing Gemini to output analysis JSON (including `overallScore` between 0-100 and a structured feedback array of strengths, gaps, and actionable improvements).
  - Implement `AnalysisService` and `AnalysisController` (`/api/resumes/{resumeId}/analyses`) allowing users to request analyses and query results.

### Role 5: QA & Devil's Advocate Agent
* **Recommended Model**: Claude 3.5/3.7 Sonnet
* **Objective**: Ensure validations work, cover exceptions, and implement unit and integration tests (EDS-21).
* **Key Tasks**:
  - Implement unit tests for UserService and UserController.
  - Implement integration tests for PDF upload, storage simulation, and Mocked Gemini calls.
  - Enforce handling of edge cases (e.g., empty files, oversized PDFs, failed Gemini API calls, missing fields).

---

## 4. Coding Standards & Patterns

To maintain codebase uniformity, all developers must adhere to the following patterns:

1. **Controller-Service-Mapper Separation**:
   - **Controllers**: Absolutely **never** pass requests or return responses/DTOs to/from services. Controllers must perform the mapping explicitly using standard Mapper static methods.
   - **Services**: Accept pure entities or native types (e.g., IDs) and return pure entities. All business rules, transactions (`@Transactional`), and exception throwing must reside in services.
   - **Mappers**: Pure stateless mapper classes (e.g., `UserMapper.toEntity(request)` and `UserMapper.toResponse(entity)`).
2. **DTO Validations**:
   - Annotate request objects with `@NotNull`, `@NotBlank`, `@Email`, `@Size`, etc.
   - Ensure `@Valid` is present on controller `@RequestBody` inputs.
3. **Structured Exceptions**:
   - Every domain business exception must extend a base custom Exception or directly `RuntimeException`.
   - Implement a [GlobalExceptionHandler](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/exception/GlobalExceptionHandler.java) returning structured error bodies (e.g. including error messages, status codes, and timestamps).
4. **JSON Handling**:
   - The analysis feedback must be stored in the Postgres `analyses.feedback_json` column. We use `@JdbcTypeCode(SqlTypes.JSON)` for mapping `String` fields containing JSON to the DB column.

---

## 5. Git Flow Policy

1. **Branch Naming**:
   - `feature/EDS-18-user-crud`
   - `feature/EDS-19-resume-upload`
   - `feature/EDS-20-ai-analysis`
   - `feature/EDS-21-tests`
2. **Commit Policy**:
   - Use Conventional Commits (`feat: ...`, `fix: ...`, `test: ...`, `refactor: ...`).
3. **Pull Requests**:
   - Once a subtask is complete, push the branch and open a Pull Request using the GitHub MCP server, referencing the Jira ticket key in the PR title.
