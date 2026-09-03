# 🧰 Technology Stack

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-6DB33F?logo=springboot&logoColor=white)
![WebSocket](https://img.shields.io/badge/Spring%20WebSocket-6.1.5-009688?logo=websocket&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.2.3-FF69B4?logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JJWT-0.11.5-000000?logo=jsonwebtokens&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-6.4.4-4B0082?logo=hibernate&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Liquibase](https://img.shields.io/badge/Liquibase-4.24.0-2962FF?logo=liquibase&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-delayed--exchange-FF6600?logo=rabbitmq&logoColor=white)
![MapStruct](https://img.shields.io/badge/MapStruct-1.5.5-5C6BC0)
![Lombok](https://img.shields.io/badge/Lombok-1.18.30-BC2732)
![Swagger](https://img.shields.io/badge/springdoc--openapi-2.5.0-85EA2D?logo=swagger&logoColor=white)
![Actuator](https://img.shields.io/badge/Actuator%20%2B%20Micrometer-Prometheus-E6522C?logo=prometheus&logoColor=white)
![WebPush](https://img.shields.io/badge/Web%20Push-VAPID%205.1.2-7B68EE)
![JUnit5](https://img.shields.io/badge/JUnit5-5.10.2-25A162?logo=junit5&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-MySQL%20%7C%20Redis%20%7C%20RabbitMQ-1D63ED?logo=testcontainers&logoColor=white)
![Checkstyle](https://img.shields.io/badge/Checkstyle-3.3.1-555555)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)

Not just a list - each row exists for a reason, and the "why" column is what
actually matters in an interview. Every version below was verified against
`mvn dependency:tree`, not guessed.

## 🧠 Language & core framework

| Technology | Version | Why |
|---|---|---|
| Java | 17 | LTS release, records and pattern matching used throughout (DTOs are records) |
| Spring Boot | 3.2.4 | Standard for production Java backends; auto-configuration keeps infrastructure wiring (DB, Redis, AMQP, Security) declarative instead of hand-rolled |

## 💾 Persistence

| Technology | Version | Why |
|---|---|---|
| MySQL | 8.0 | Relational integrity for chat/message/user relationships; `SELECT ... FOR UPDATE` and a custom SQL function are used directly where atomicity actually matters (sequence generation) |
| Hibernate / Spring Data JPA | 6.4.4.Final | Standard repository abstraction; hand-written JPQL/native `@Query` is used wherever the derived-query naming convention would be unreadable or insufficient |
| Liquibase | 4.24.0 | Version-controlled, repeatable schema migrations instead of manual DDL - every environment (dev, CI, prod) ends up with the exact same schema history |

## ⚡ Caching & ephemeral state

| Technology | Version | Why |
|---|---|---|
| Redis | 7 | Not just a cache here - backs rate limiting (atomic Lua-scripted counters), online presence (TTL-based, self-expiring), idempotency keys, and distributed scheduler locks |
| Lettuce (Redis client) | 6.3.2 | Spring Data Redis's default driver - async-capable, netty-based |

## 📨 Messaging

| Technology | Version | Why |
|---|---|---|
| RabbitMQ | via `heidiks/rabbitmq-delayed-message-exchange` image | Real message broker instead of in-process-only event dispatch, so delivery survives the publishing app instance restarting; the delayed-message plugin gives reminders a "deliver at this future timestamp" exchange instead of a polling loop |
| Spring AMQP | (amqp-client 5.19.0) | `RabbitTemplate` / `@RabbitListener` - idiomatic Spring integration over raw AMQP client code |

## 🔌 Real-time transport

| Technology | Version | Why |
|---|---|---|
| Spring WebSocket + STOMP | 6.1.5 | STOMP gives a subscription/topic model (`/topic/chat/{id}`, `/queue/...`) on top of raw WebSocket frames, matching the pub/sub shape the chat actually needs |

## 🔐 Security

| Technology | Version | Why |
|---|---|---|
| Spring Security | 6.2.3 | Method-level (`@PreAuthorize`) and request-level authorization, pluggable filter chain for the custom JWT/rate-limit filters |
| jjwt | 0.11.5 | JWT issuing and parsing (`JwtUtil`) - signature verification and expiry are handled by a well-audited library instead of hand-rolled token logic |
| BCrypt (`PasswordEncoder`) | via Spring Security | Adaptive, salted password hashing |

## 🧩 Object mapping & boilerplate

| Technology | Version | Why |
|---|---|---|
| MapStruct | 1.5.5 | Compile-time-generated entity-to-DTO mapping - no reflection cost, and mapping bugs surface at build time, not runtime |
| Lombok | 1.18.30 | Removes constructor/getter/setter boilerplate from entities and DTOs |

## 📊 Observability & docs

| Technology | Version | Why |
|---|---|---|
| Spring Boot Actuator | 3.2.4 | `/actuator/health` for container/orchestrator health probes |
| Micrometer + Prometheus registry | 1.12.4 | Custom application metrics (message throughput, outbox processing, login failures, JWT rejection reasons) in a format Prometheus can scrape directly |
| springdoc-openapi | 2.5.0 | Swagger UI generated from the controllers themselves, so the API docs can't drift out of sync with the actual endpoints |

## 🔔 Push notifications

| Technology | Version | Why |
|---|---|---|
| web-push (VAPID) | 5.1.2 | Delivers a message to a user who's offline and not holding a WebSocket connection open at all |
| Bouncy Castle | 1.85.2 | Cryptographic primitives web-push needs for VAPID key signing |

## 🧪 Testing

| Technology | Version | Why |
|---|---|---|
| JUnit 5 | 5.10.2 | Test runner |
| Mockito | 5.7.0 | Isolate a class under test from its collaborators for fast, focused unit tests |
| AssertJ | 3.24.2 | Fluent, readable assertions |
| MockMvc + `spring-security-test` | via Spring Boot | Slice tests that exercise real Spring MVC dispatch and real method-security enforcement (`@WithMockUser`), without booting the full application |
| Testcontainers | (MySQL module + generic containers) | Integration tests against a real MySQL/Redis/RabbitMQ, specifically for the things a mock structurally cannot prove: that a row lock actually serializes concurrent transactions, that a Redis script is really atomic under load, that hand-written JPQL joins return correct rows |

## 🛠 Build & quality gates

| Technology | Version | Why |
|---|---|---|
| Maven | - | Build tool; Surefire runs the fast unit/slice suite on `mvn test`, Failsafe runs the `*IT` Testcontainers suite separately on `mvn verify`, so a quick local test run never needs Docker |
| Checkstyle | 3.3.1, mate-academy ruleset | Enforced at compile time (`mvn compile` fails the build on a violation), not just a linter you can ignore |

## 🐳 Containerization & deployment

| Technology | Why |
|---|---|
| Docker (multi-stage build) | Build stage compiles with Maven, runtime stage ships only the JRE + jar - no build tooling in the final image |
| docker-compose | Brings up the full stack (app + MySQL + Redis + RabbitMQ) with dependency-aware healthchecks, so the app container only starts once its dependencies are actually ready, not just "started" |
