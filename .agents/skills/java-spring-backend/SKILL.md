---
name: java-spring-backend
description: Apply Java/Spring Boot coding conventions for backend implementation and review, including mapping, dependency injection, Lombok, DTOs, persistence, and API documentation.
---

# Java / Spring Boot Backend Skill

Apply these conventions whenever implementing or reviewing Java/Spring Boot backend code.

- Clean Code: Follow "Google Java Style" (https://google.github.io/styleguide/javaguide.html). Use clear naming, cohesive classes, small focused methods, and single responsibilities. Avoid duplicated logic, unnecessary abstractions, and manual boilerplate.

- Java Imports: Prefer imported types and simple names over fully qualified names (FQNs). Use fully qualified names only to resolve naming conflicts.

- MapStruct: Use the existing MapStruct "1.6.3" configuration for object mapping. Inject mapper beans instead of manually constructing DTOs, creating repetitive mapping helpers, or instantiating mappers with "new". Mappers must not enforce authorization, business rules, or invent defaults.

- Dependency Injection & Bean Configuration: Prefer Spring Boot auto-configured beans. Define custom or shared beans in dedicated "@Configuration" classes using "@Bean", then inject them through constructors. Do not repeatedly instantiate shared dependencies or create duplicate beans.

- Spring Stereotypes: Use "@Service" on service implementations and "@Repository" on custom persistence adapters where appropriate. Use Spring Data repository interfaces directly; avoid redundant implementations. Keep transport, business logic, mapping, and persistence responsibilities separate.

- Lombok: Prefer "@RequiredArgsConstructor" for classes with injected final dependencies. Use "@Getter" and selective "@Setter" instead of "@Data". Avoid redundant annotations on records and preserve encapsulation.

- JPA Entities: Use "@NoArgsConstructor(access = AccessLevel.PROTECTED)", "@AllArgsConstructor", and "@Builder". Do not use "@RequiredArgsConstructor" on entities. Ensure constructors and builders preserve entity invariants and do not expose uncontrolled mutation of managed state.

- DTOs: Use plain Java records without Lombok for request/response DTOs. Follow conventional names such as "CreateTransactionRequest", "UpdateTransactionRequest", and "TransactionResponse". Keep DTOs separate from JPA entities and internal domain state.

- OpenAPI Documentation: Use Swagger/OpenAPI "@Schema" annotations to document DTOs and every record component with meaningful descriptions and representative examples. Document required fields, formats, constraints, and response semantics accurately.

- Jakarta Validation: Use validation annotations that reflect actual domain constraints. Preserve absent-versus-null semantics for partial updates. OpenAPI documentation does not replace runtime validation.
- Persistence: Use PostgreSQL with Spring Data JPA/Hibernate and Flyway. Do not substitute H2 or use raw JDBC as the primary repository design. Keep "ddl-auto: validate" and "sql.init.mode: never". Never edit an applied migration.

- Domain Contracts: Reuse existing rules for money, dates, transactions, authorization, locking, replay, consistency, and pagination. Reuse the shared "TransactionPage"/cursor contract instead of creating competing contracts or duplicate helpers.

- Runtime & Infrastructure: Inject shared configured infrastructure. Follow the project's Java and virtual-thread configuration. Respect connection-pool limits, transaction boundaries, and resource lifecycle. Spring Boot 4 uses Jackson 3 by default; use Jackson 2 only at the Telegram SDK boundary when required by the SDK.

- Verification: Preserve existing behavior and invariants. Report only checks actually run. Never claim compilation, dependency compatibility, migration success, or runtime behavior without evidence.