# Technology Stack

Not just a list - each row exists for a reason, and the "why" column is what
actually matters in an interview.

## Language & core framework

| Technology | Version | Why |
|---|---|---|
| Java | 17 | LTS release, records and pattern matching used throughout (DTOs are records) |
| Spring Boot | 3.2.4 | Standard for production Java backends; auto-configuration keeps infrastructure wiring (DB, Redis, AMQP, Security) declarative instead of hand-rolled |

## Persistence

| Technology | Version | Why |
|---|---|---|
| MySQL | 8.0 | Relational integrity for chat/message/user relationships; `SELECT ... FOR UPDATE` and a custom SQL function are used directly where atomicity actually matters (sequence generation) |
| Spring Data JPA / Hibernate | (via Spring Boot) | Standard repository abstraction; hand-written JPQL/native `@Query` is used wherever the derived-query naming convention would be unreadable or insufficient |
| Liquibase | (via Spring Boot) | Version-controlled, repeatable schema migrations instead of manual DDL - every environment (dev, CI, prod) ends up with the exact same schema history |

## Caching & ephemeral state

| Technology | Version | Why |
|---|---|---|
| Redis | 7 | Not just a cache here - backs rate limiting (atomic Lua-scripted counters), online presence (TTL-based, self-expiring), idempotency keys, and distributed scheduler locks |

## Messaging

| Technology | Version | Why |
|---|---|---|
| RabbitMQ | via `heidiks/rabbitmq-delayed-message-exchange` image | Real message broker instead of in-process-only event dispatch, so delivery survives the publishing app instance restarting; the delayed-message plugin gives reminders a "deliver at this future timestamp" exchange instead of a polling loop |
| Spring AMQP | (via Spring Boot) | `RabbitTemplate` / `@RabbitListener` - idiomatic Spring integration over raw AMQP client code |

## Real-time transport

| Technology | Version | Why |
|---|---|---|
| WebSocket + STOMP | (via `spring-boot-starter-websocket`) | STOMP gives a subscription/topic model (`/topic/chat/{id}`, `/queue/...`) on top of raw WebSocket frames, matching the pub/sub shape the chat actually needs |

## Security

| Technology | Version | Why |
|---|---|---|
| Spring Security | (via Spring Boot) | Method-level (`@PreAuthorize`) and request-level authorization, pluggable filter chain for the custom JWT/rate-limit filters |
| jjwt | 0.11.5 | JWT issuing and parsing (`JwtUtil`) - signature verification and expiry are handled by a well-audited library instead of hand-rolled token logic |
| BCrypt (`PasswordEncoder`) | (via Spring Security) | Adaptive, salted password hashing |

## Object mapping & boilerplate

| Technology | Version | Why |
|---|---|---|
| MapStruct | 1.5.5 | Compile-time-generated entity-to-DTO mapping - no reflection cost, and mapping bugs surface at build time, not runtime |
| Lombok | 1.18.30 | Removes constructor/getter/setter boilerplate from entities and DTOs |

## Observability & docs

| Technology | Version | Why |
|---|---|---|
| Spring Boot Actuator | (via Spring Boot) | `/actuator/health` for container/orchestrator health probes |
| Micrometer + Prometheus registry | 1.12.4 | Custom application metrics (message throughput, outbox processing, login failures, JWT rejection reasons) in a format Prometheus can scrape directly |
| springdoc-openapi | 2.5.0 | Swagger UI generated from the controllers themselves, so the API docs can't drift out of sync with the actual endpoints |

## Push notifications

| Technology | Version | Why |
|---|---|---|
| web-push (VAPID) | 5.1.2 | Delivers a message to a user who's offline and not holding a WebSocket connection open at all |

## Testing

| Technology | Version | Why |
|---|---|---|
| JUnit 5 | 5.10.2 | Test runner |
| Mockito | (via Spring Boot) | Isolate a class under test from its collaborators for fast, focused unit tests |
| AssertJ | (via Spring Boot) | Fluent, readable assertions |
| MockMvc + `spring-security-test` | (via Spring Boot) | Slice tests that exercise real Spring MVC dispatch and real method-security enforcement (`@WithMockUser`), without booting the full application |
| Testcontainers | (MySQL module + generic containers) | Integration tests against a real MySQL/Redis/RabbitMQ, specifically for the things a mock structurally cannot prove: that a row lock actually serializes concurrent transactions, that a Redis script is really atomic under load, that hand-written JPQL joins return correct rows |

## Build & quality gates

| Technology | Version | Why |
|---|---|---|
| Maven | - | Build tool; Surefire runs the fast unit/slice suite on `mvn test`, Failsafe runs the `*IT` Testcontainers suite separately on `mvn verify`, so a quick local test run never needs Docker |
| Checkstyle | 3.3.1, mate-academy ruleset | Enforced at compile time (`mvn compile` fails the build on a violation), not just a linter you can ignore |

## Containerization & deployment

| Technology | Why |
|---|---|
| Docker (multi-stage build) | Build stage compiles with Maven, runtime stage ships only the JRE + jar - no build tooling in the final image |
| docker-compose | Brings up the full stack (app + MySQL + Redis + RabbitMQ) with dependency-aware healthchecks, so the app container only starts once its dependencies are actually ready, not just "started" |
