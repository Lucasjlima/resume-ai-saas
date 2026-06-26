# Agent Orchestration Plan: Service Refactoring & Transaction Optimization

This document details the orchestration plan to refactor `AnalysisService` and `AiAnalysisService`, mapping directly to Jira tickets created in project **Engenharia De Software (EDS)**.

---

## 1. Jira Tickets Reference

- **Parent Task**: [EDS-22: Refactor AnalysisService and AiAnalysisService](https://lucasjlima2006.atlassian.net/browse/EDS-22)
- **Subtasks**:
  - [EDS-23: Implement refactoring for AnalysisService and AiAnalysisService](https://lucasjlima2006.atlassian.net/browse/EDS-23)

---

## 2. Claude Code Agent Team Initialization Prompt

Copy and paste the prompt below into Claude Code to spin up the developer agent:

```text
You are spinning up an autonomous developer agent to execute the refactoring of AnalysisService and AiAnalysisService in a Spring Boot application.
Please read the orchestration plan: file:///c:/Users/lucas/GITHUB/resume-ai-saas/agent_orchestration_plan.md.

Form the following team:
1. Developer Agent (Model: Claude 3.5 Sonnet / Gemini 3.5 Flash) - responsible for implementing EDS-23, relocating DTO records, updating AnalysisMapper, and optimizing transaction scopes to prevent long-running transactions during HTTP calls.

Follow the Git Flow and Coding Standards outlined in the orchestration plan. Work inside the branch 'refactor-ai-gson-parsing', commit conventionally, and push code. Do not commit directly to main.
```

---

## 3. Teammate Roles & Objectives

### Role 1: Developer Agent
* **Recommended Model**: Claude 3.5 Sonnet / Gemini 3.5 Flash
* **Objective**: Refactor the analysis package to clean up code, move inner records to separate DTOs, encapsulate building logic in AnalysisMapper, and minimize database transaction scopes.
* **Key Tasks**:
  - Move `Feedback` and `AiAnalysisResult` records from [AiAnalysisService.java](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/analyse/service/AiAnalysisService.java) to a new response DTO package.
  - Implement/update [AnalysisMapper.java](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/analyse/mapper/AnalysisMapper.java) to include entity-building methods, keeping builders out of the service layer.
  - Clean up [AiAnalysisService.java](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/analyse/service/AiAnalysisService.java) to import the relocated records.
  - Refactor [AnalysisService.java](file:///c:/Users/lucas/GITHUB/resume-ai-saas/src/main/java/saas/com/br/resume_ai_saas/analyse/service/AnalysisService.java):
    - Remove the class-level/global `@Transactional` (or methods where they shouldn't be).
    - Ensure the main `analyze()` method is NOT transactional so that the external HTTP call to the ChatClient runs outside database transactions.
    - Split database operations into separate methods annotated with `@Transactional`:
      - `initializeAnalysis()` (Short Transaction to save initial pending state).
      - `completeAnalysisWithSuccess()` (Short Transaction to update analysis results).
      - `completeAnalysisWithFailure()` (Short Transaction to update failed state).
    - Rely on JPA dirty checking (no redundant `.save()` on managed entities inside transactional methods).

---

## 4. Coding Standards & Patterns

To maintain codebase uniformity, the developer must adhere to the following patterns:

1. **Controller-Service-Mapper Separation**:
   - **Controllers**: Interact with services using entities, and map to DTOs using Mappers.
   - **Services**: Contain business rules and transaction scopes. Services must avoid exposing builders and should delegate mapping to Mappers.
   - **Mappers**: Contain mapping and entity creation logic.
2. **Short-Lived Transactions**:
   - Do not use `@Transactional` on methods containing network/HTTP calls (like `ChatClient.prompt()`). Keep transactions as short and fast as possible to avoid database connection starvation.

---

## 5. Git Flow Policy

1. **Branch Naming**:
   - The developer agent must work on the existing branch `refactor-ai-gson-parsing`.
2. **Commit Policy**:
   - Use Conventional Commits (`feat: ...`, `fix: ...`, `refactor: ...`, `chore: ...`).
3. **Pushes**:
   - Push to `origin refactor-ai-gson-parsing` once the work is complete.
