# ADR 0001: Begin as a Spring Modulith modular monolith

- Status: accepted
- Date: 2026-09-21

## Context

Pantry has substantial domain complexity but no demonstrated need for independently scaled services. Its plan, matching, pricing, basket, and recovery operations also need clear consistency boundaries. Premature services would add distributed transactions and operational cost before the domain is stable.

## Decision

Use Java 21, Spring Boot, and Spring Modulith in one deployable application. Each business module owns its data and exposes application-level APIs/events. Cross-module repository access and cross-module JPA relationships are forbidden. Use Modulith verification tests to enforce boundaries.

Use PostgreSQL and Flyway as the source of truth. Module events handle in-process asynchronous collaboration with durable publication tracking where needed.

## Consequences

The application is simple to run and test while retaining boundaries that can later support extraction. Some resource-heavy work may need background executors, but Kafka and service decomposition remain evidence-driven decisions.
