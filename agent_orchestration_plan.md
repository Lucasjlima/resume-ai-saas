# Agent Orchestration Plan: Supabase Auth & Spring Security Implementation

This document details the orchestration plan to integrate Supabase Auth with Spring Security, recrossing the database schema, removing the local user tables/repositories, and protecting the API endpoints.

---

## 1. Jira Tickets Reference

- **Parent Task**: [EDS-24: Implementar autenticação com Supabase + Spring Security](https://lucasjlima2006.atlassian.net/browse/EDS-24)
- **Subtasks**:
  - [EDS-25: Configuração Inicial, Maven, Propriedades e Infraestrutura (Flyway)](https://lucasjlima2006.atlassian.net/browse/EDS-25)
  - [EDS-26: Refatorar Modelos, Repositório e Service do Upload de Currículos](https://lucasjlima2006.atlassian.net/browse/EDS-26)
  - [EDS-27: Filtro JWT do Supabase, Validações e Testes de Integração](https://lucasjlima2006.atlassian.net/browse/EDS-27)

---

## 2. Claude Code Agent Team Initialization Prompt

Copy and paste the prompt below into Claude Code to spin up the developer agent team:

```text
You are spinning up an autonomous developer agent team to implement Supabase Authentication and Spring Security integration.
Please read the orchestration plan: file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/agent_orchestration_plan.md.

Form the following team:
1. Lead Coordinator & Integration Agent (Model: Claude 3.5 Sonnet / Claude 3.7 Sonnet) - responsible for EDS-25 (dependencies, configuration, Flyway schema, security setup).
2. Module-Specific Architect (Model: Claude 3.5 Sonnet) - responsible for EDS-26 (removing local User entity/repository/CRUD, updating Resume entity/service/repository to UUID).
3. Devil's Advocate / QA (Model: Claude 3.5 Sonnet / Claude 3.7 Sonnet) - responsible for EDS-27 (JWT Filter validation, SecurityContext injection, authenticated utility helper, error responses, and endpoint security testing).

Follow the Git Flow and Coding Standards outlined in the orchestration plan. Work inside the branch 'implement-supabase-auth-security', commit conventionally, and push code. Do NOT merge into main; the user will review the code first.
```

---

## 3. Teammate Roles & Objectives

### Role 1: Lead Coordinator & Integration Agent
* **Recommended Model**: Claude 3.5 Sonnet / Claude 3.7 Sonnet
* **Objective**: Establish security dependencies, application properties, base Flyway migration, and core Spring Security configuration.
* **Key Tasks**:
  - Add Spring Security and JJWT dependencies to [pom.xml](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/pom.xml).
  - Add Supabase connection configuration, JPA/Hibernate validations, Flyway settings, and `supabase.jwt.secret` configuration to [application.properties](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/resources/application.properties).
  - Remove existing Flyway migration files from `src/main/resources/db/migration` and create a single migration file [V1__init_schema.sql](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/resources/db/migration/V1__init_schema.sql) with the updated resumes and analyses tables schema.
  - Implement security filter chain class [SecurityConfig.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/security/SecurityConfig.java) allowing `/actuator/health` and securing all other endpoints.

---

### Role 2: Module-Specific Architect
* **Recommended Model**: Claude 3.5 Sonnet
* **Objective**: Remove the local user entities/CRUD logic, refactor references to use `UUID` logic instead of internal user IDs.
* **Key Tasks**:
  - Remove the package `saas.com.br.resume_ai_saas.user` including [User.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/user/entity/User.java) and [UserRepository.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/user/repository/UserRepository.java).
  - Update [Resume.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/resume/entity/Resume.java) to replace `@ManyToOne User` with `@Column(name = "user_id", nullable = false) UUID userId`.
  - Refactor [ResumeRepository.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/resume/repository/ResumeRepository.java) to find resumes using `UUID userId`: `List<Resume> findByUserId(UUID userId)`.
  - Update [ResumeService.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/resume/service/ResumeService.java) to receive a `UUID` instead of `Long` or `User` objects, updating builders/setters.

---

### Role 3: Devil's Advocate / QA
* **Recommended Model**: Claude 3.5 Sonnet / Claude 3.7 Sonnet
* **Objective**: Enforce strict JWT validations, proper Spring Security context integration, exception mapping, and test the endpoints for security coverage.
* **Key Tasks**:
  - Implement [SupabaseJwtFilter.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/security/SupabaseJwtFilter.java) to extract the token, verify the HS256 signature using `JWT_SECRET`, build the Authentication principal, and populate `SecurityContextHolder`.
  - Implement [AuthenticatedUser.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/security/AuthenticatedUser.java) to fetch the user's UUID securely.
  - Refactor [ResumeController.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/resume/controller/ResumeController.java) to remove the `userId` request parameter, resolving it using `AuthenticatedUser.getId()`.
  - Verify exception handler mappings: Ensure JWT parsing errors (expired, signature mismatch, malformed) return a structured HTTP 401 response with JSON body `{"error": "Token inválido ou expirado"}` instead of leaking raw stack traces.
  - Perform validation tests:
    - Call endpoints without a token (Expect 401 Unauthorized).
    - Call endpoints with a malformed/expired token (Expect 401 Unauthorized).
    - Call endpoints with a valid Supabase token (Expect 200 OK).

---

## 4. Coding Standards & Patterns

1. **Clean Architecture / Layers Separation**:
   - **Controllers**: Keep controllers light. Retrieve authenticated context using `AuthenticatedUser.getId()`.
   - **Services**: Enforce business validation rules.
   - **Security Filters**: Handle HTTP interception, parsing, and context mapping. Avoid logic leaks into other layers.
2. **DTO Request Validations**:
   - Apply validations on request payloads using standard annotations.
3. **Structured Custom Exceptions**:
   - Inherit from `RuntimeException` and ensure they are handled inside [GlobalExceptionHandler.java](file:///C:/Users/lucas/.gemini/antigravity/worktrees/resume-ai-saas/implement-supabase-auth-security/src/main/java/saas/com/br/resume_ai_saas/exception/GlobalExceptionHandler.java).

---

## 5. Git Flow Policy

1. **Branch Naming**:
   - Develop inside the current task branch: `implement-supabase-auth-security`.
2. **Commit Policy**:
   - Use Conventional Commits (`feat: ...`, `fix: ...`, `refactor: ...`, `chore: ...`).
3. **Pushes & Merging**:
   - Once all subtasks are verified and green, push the branch to origin.
   - **CRITICAL**: Do NOT merge into `main` or another branch. The user will perform a code review first before any merge occurs.
