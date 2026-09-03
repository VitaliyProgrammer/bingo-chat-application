# 💬 Bingo Chat Application — Spring Boot Back-End

[![CI](https://github.com/VitaliyProgrammer/bingo-chat-application/actions/workflows/ci.yml/badge.svg)](https://github.com/VitaliyProgrammer/bingo-chat-application/actions/workflows/ci.yml)
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

## 📌 Introduction

**Bingo Chat Application** is a production-style back-end for a real-time messenger,
built with Spring Boot. It's not a CRUD demo with a WebSocket bolted on - the point
of the project is everything that makes a chat product actually survivable in
production: guaranteed message delivery even if a broker or the app itself crashes
mid-flight, correct behavior under concurrent writers, rate limiting against abuse,
and a security layer that was tested against real attack-shaped inputs, not just
happy paths.

The full technical breakdown lives in [`docs/`](docs/01-overview.md) - this README
is the map, not the territory.

## 🎯 What this project demonstrates

- 🧩 Two independent, correctly-scoped authentication mechanisms for two transports
  that don't share a threading model (REST vs STOMP/WebSocket)
- 📡 Reliable, at-least-once event delivery via the **Outbox pattern**, backed by a
  real broker (RabbitMQ) with a scheduled fallback so nothing gets silently lost
- 🔒 Concurrency correctness proven against a **real** database under real concurrent
  load (Testcontainers), not assumed from reading the code
- 🐛 A real, self-discovered production bug (non-atomic Redis rate-limit counter that
  could permanently lock a user out) - found, fixed with an atomic Lua script, and
  covered by a regression test
- ⚙ Idempotency and distributed locking with Redis, so the system is safe to scale
  to multiple instances without double-processing anything
- 🧪 A test suite split by cost: fast mocked unit/slice tests on every `mvn test`,
  and a separate Testcontainers integration suite on `mvn verify` for the things a
  mock structurally cannot prove

## 🧩 Features

**💬 Messaging**
- Private and group chats, with transferable group ownership
- Real-time delivery over WebSocket/STOMP with `SENT → DELIVERED → READ` tracking
- Message editing, pinning, replies, and capped emoji reactions in group chats
- Typing indicators, online presence, delayed reminders
- Web Push notifications for offline participants

**🛡 Moderation & safety**
- User-to-user blocking, enforced on both messaging and group invites
- Admin-only group moderation, protected by tested method-level authorization
- Rate limiting on auth endpoints and WebSocket subscribe/send, against brute-force and flood

**🔁 Reliability**
- Outbox pattern: a message is never "sent" without a guaranteed delivery event
- Scheduled fallback re-processes anything the fast async path missed
- Idempotency guards on every consumer against at-least-once redelivery

## 🏗 Architecture at a glance

```mermaid
graph TD
    Client["Browser / Mobile Client"]

    subgraph App["Spring Boot Application"]
        REST["REST Controllers"]
        WS["WebSocket / STOMP Controllers"]
        SEC["Security layer:<br/>JwtAuthenticationFilter, JwtChannelInterceptor,<br/>AuthRateLimitFilter, WebSocketSubscriptionGuard"]
        SVC["Service Layer"]
        OUTBOX["Outbox Publisher + Processor"]
    end

    MySQL[("MySQL 8")]
    Redis[("Redis 7")]
    RabbitMQ{{"RabbitMQ<br/>+ delayed-message plugin"}}

    Client -->|"HTTPS REST"| REST
    Client -->|"WS / STOMP"| WS
    REST --> SEC
    WS --> SEC
    SEC --> SVC
    SVC --> MySQL
    SVC --> Redis
    SVC --> OUTBOX
    OUTBOX --> MySQL
    OUTBOX -->|"publish"| RabbitMQ
    RabbitMQ -->|"consume"| SVC
    SVC -->|"broadcast"| WS
    WS -->|"push"| Client
```

Full sequence diagrams for authentication (HTTP vs WebSocket) and the complete
message send-to-delivery pipeline are in
[`docs/02-architecture.md`](docs/02-architecture.md).

## 🧱 Domain model

```mermaid
classDiagram
    class User {
        Long id
        String email
        String nickname
        String password
        String avatarUrl
        LocalDateTime lastInOnline
    }
    class Role {
        Long id
        RoleName roleName
    }
    class Chat {
        Long id
        ChatType chatType
        Long ownerId
        Long lastMessageSequence
        String lastMessageText
    }
    class Message {
        Long id
        String content
        MessageStatus status
        Long sequence
        Boolean isPinned
        LocalDateTime reminderAt
    }
    class MessageReaction {
        Long id
        String emoji
    }
    class BlockedUser {
        Long id
        LocalDateTime createdAt
    }
    class OutboxEvent {
        Long id
        String eventType
        String payload
        Boolean processed
        Integer retryCount
    }

    User "0..*" --> "0..*" Role : has
    User "1" --> "0..*" Message : sends
    User "0..*" --> "0..*" Chat : "participates in"
    Chat "1" --> "0..*" Message : contains
    Message "1" --> "0..*" MessageReaction : has
    Message "0..1" --> "0..1" Message : "replies to"
    User "1" --> "0..*" MessageReaction : reacts
    User "1" --> "0..*" BlockedUser : blocks
```

## 🧰 Technology stack

| Layer | Technology |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.2.4 |
| Real-time transport | WebSocket + STOMP |
| Security | Spring Security, JWT (jjwt), BCrypt |
| Persistence | Spring Data JPA / Hibernate, MySQL 8, Liquibase |
| Caching & ephemeral state | Redis 7 |
| Messaging | RabbitMQ + delayed-message plugin, Spring AMQP |
| Mapping | MapStruct, Lombok |
| Docs & observability | springdoc-openapi (Swagger), Actuator, Micrometer + Prometheus |
| Push notifications | Web Push (VAPID) |
| Testing | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers |
| Build & quality | Maven (Surefire + Failsafe), Checkstyle |
| Deployment | Docker (multi-stage build), Docker Compose |

Full stack breakdown with the reasoning behind every choice:
[`docs/03-tech-stack.md`](docs/03-tech-stack.md).

## 🐳 Infrastructure & Deployment

Docker Compose orchestrates the entire stack - app, MySQL, Redis, and RabbitMQ -
with dependency-aware healthchecks, so the app container only starts once every
dependency is actually ready, not just "started".

## 🛠 Getting started

**Prerequisites:** Docker, Docker Compose, a browser.

```bash
git clone https://github.com/VitaliyProgrammer/bingo-chat-application.git
cd bingo-chat-application
docker-compose up --build
```

Once it's up:

| What | Where |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| Health check | `http://localhost:8080/actuator/health` |
| STOMP test page | `http://localhost:8080/stomp-test.html` |

```bash
docker-compose down   # stop everything
```

## 🧪 Testing

```bash
mvn test      # fast unit + slice tests (Mockito, MockMvc) - no Docker required
mvn verify    # + integration tests against real MySQL/Redis/RabbitMQ (Testcontainers)
```

The integration suite exists specifically to prove what a mock structurally can't:
that a `SELECT ... FOR UPDATE` lock actually serializes concurrent writers, that a
Redis rate-limit counter is atomic under real concurrent load, and that hand-written
JPQL joins return correct rows against a real database.

## 📘 Documentation

- [**Overview & features**](docs/01-overview.md) — what the project does, quick start, how to run the tests
- [**Architecture**](docs/02-architecture.md) — authentication flows, message delivery pipeline, concurrency patterns, data model
- [**Tech stack**](docs/03-tech-stack.md) — every technology used, and why
