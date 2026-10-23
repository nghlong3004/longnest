---
name: longnest-backend
description: Design, implement or review Longnest Java/Spring backend services, mappings, persistence, DTO/API contracts and runtime configuration. Not for frontend or product-policy discovery.
---

# Longnest backend

## Start

Read the project root AGENTS.md and docs/WORK_STATUS.md.
Reconcile current authorization and ownership before changing files.
This skill does not authorize implementation, installations or deployment.

Canonical backend rules:
[BACKEND_RULES.md](../../../docs/BACKEND_RULES.md).

## Select the relevant branches

Read each selected section completely, including its required linked contracts.

- Java, services and mapping:
  “Style, mappers and services” and the Lombok portion of
  “Lombok and shared beans”.
- Persistence:
  “Persistence and schema ownership”.
  PostgreSQL is required; use JPA/Hibernate and Flyway, not H2.
- DTOs, API and pagination:
  “Shared pagination first” and “DTOs and future HTTP”.
- Configuration and execution:
  the shared-bean portion of “Lombok and shared beans” and
  “Virtual-thread blocking workload”.

Read multiple branches only when the task crosses their boundaries.
Consult “Verified references and limits” for framework-specific details.

## Keep context consistent

Keep product, domain, services, architecture and work status canonical
under docs/. Link rather than duplicate their contracts.

After a material decision or interface change, update the owning document
and affected links. Lead maintains the designated work-status source.

Distinguish approved rules from dependency proposals, design from
implementation, and verified behavior from unrun checks.

Preserve unrelated edits, credentials and private session state.