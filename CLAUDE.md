# Project Standards & Agent Guidelines

Read this file **before writing any code**. These are non-negotiable patterns established in this codebase.

---

## Architecture: Controller → Service → Mapper

Every module follows a strict three-layer separation:

| Layer | Responsibility | Must NOT do |
|---|---|---|
| **Controller** | Receives requests, delegates to Service, maps results via Mapper | Contain business logic, build entities, manage transactions |
| **Service** | Business rules, transaction orchestration | Build entities with `.builder()`, map to DTOs, expose builders |
| **Mapper** | Entity creation (`toEntity`), entity mutation (`applyX`), response mapping (`toResponse`) | Contain business logic, access repositories |

### Mapper methods follow this naming convention:
- `toEntity(...)` — builds a new entity from parameters
- `toResponse(Entity)` — maps entity to response DTO
- `applySuccess(Entity, ...)` / `applyFailure(Entity, ...)` — mutates an existing managed entity

---

## Transaction Management

**This is the most critical rule.** Violations cause database connection starvation under load.

### NEVER do this:
```java
// BAD: @Transactional wrapping external I/O
@Transactional
public Result doSomething() {
    Entity entity = repository.save(buildEntity());  // DB
    var result = externalService.call();              // HTTP / file I/O / PDF parsing
    entity.setResult(result);                         // DB
    return repository.save(entity);                   // redundant save
}
```

### ALWAYS do this:
```java
// GOOD: I/O outside transaction, short @Transactional methods for DB ops
public Result doSomething() {
    Entity entity = self.initialize();                // short TX 1
    try {
        var result = externalService.call();           // NO transaction held
        self.completeWithSuccess(entity.getId(), result); // short TX 2
    } catch (Exception e) {
        self.completeWithFailure(entity.getId(), e);  // short TX 3
    }
    return self.findById(entity.getId());
}

@Transactional
public Entity initialize() { ... }

@Transactional
public void completeWithSuccess(Long id, Result result) {
    Entity entity = repository.findById(id).orElseThrow();
    Mapper.applySuccess(entity, result);
    // NO .save() — JPA dirty checking handles it
}
```

### Rules:
1. **No `@Transactional` on methods containing**: HTTP calls (`ChatClient`, `RestTemplate`, `WebClient`), file I/O (`StorageService`, `Files.*`), PDF/document parsing, or any blocking external operation.
2. **Short-lived transactions only**: each `@Transactional` method should do one focused DB operation and return.
3. **Self-injection with `@Lazy`**: Spring AOP proxies don't intercept self-calls. When a non-transactional method calls `@Transactional` methods on the same bean, inject `self` via constructor with `@Lazy`:
   ```java
   public MyService(..., @Lazy MyService self) {
       this.self = self;
   }
   ```
   Then call `self.transactionalMethod()` instead of `this.transactionalMethod()`.
4. **No redundant `.save()`**: inside a `@Transactional` method, managed entities are automatically persisted by JPA dirty checking. Only call `.save()` for **new** (detached) entities.
5. **`@Transactional(readOnly = true)`** for all read-only queries (`findById`, `findByX`).

---

## REST API Design

### Flat resource paths
```
/api/resumes
/api/analyses
/api/users
```

**Do NOT** nest resources in class-level `@RequestMapping`:
```java
// BAD
@RequestMapping("/api/users/{userId}/resumes")

// GOOD
@RequestMapping("/api/resumes")
```

### Parameter placement
- **`@PathVariable`**: only for the resource's own identity — `GET /api/resumes/{id}`
- **`@RequestParam`**: for filtering or association — `GET /api/resumes?userId=1`, `POST /api/analyses?resumeId=5`

### Do NOT pass unused path variables
If a method doesn't use a parameter, it shouldn't be in the signature or the path.

---

## DTO Organization

```
module/
├── controller/
├── dto/
│   ├── request/     ← input records (validated with Jakarta)
│   └── response/    ← output records
├── entity/
├── exception/
├── mapper/
├── repository/
└── service/
```

- DTOs are **Java records**, not classes.
- **No inner records/classes inside services** — extract to `dto/request/` or `dto/response/`.
- Response records returned by AI/external services (e.g. `AiAnalysisResult`, `Feedback`) go in `dto/response/`.

---

## Git & Commits

- **Branch**: work on the feature branch, never commit to `main` directly.
- **Conventional Commits**: `feat:`, `fix:`, `refactor:`, `chore:`, `test:`, `docs:`
  - Scope in parentheses when touching a specific module: `refactor(analyse):`, `fix(resume):`
- **Compile before committing**: always run `./mvnw compile -q` and confirm zero errors.
- **Push** after committing.

---

## Pre-Flight Checklist (before writing code)

1. Does my method have `@Transactional` AND call any external service or I/O? → **Split it.**
2. Am I building an entity with `.builder()` inside a Service? → **Move to Mapper.**
3. Am I calling `.save()` on a managed entity inside `@Transactional`? → **Remove it.**
4. Am I adding a `@PathVariable` that some endpoints won't use? → **Use `@RequestParam` or per-method paths.**
5. Am I defining a record/class inside a Service? → **Extract to `dto/` package.**
6. Does `./mvnw compile -q` pass? → **Don't commit until it does.**
