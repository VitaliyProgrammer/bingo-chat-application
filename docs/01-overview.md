# Bingo Chat Application — Overview

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-delayed--exchange-FF6600?logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-black?logo=websocket&logoColor=white)

Backend for a real-time messenger, built with Spring Boot. It covers everything a
production chat product needs on the backend side: authenticated real-time
messaging over WebSocket, reliable delivery guarantees, moderation, and the
security hardening (rate limiting, audit logging) that separates a demo from
something you could actually deploy.

This document is the entry point. For how it works internally, see
[`02-architecture.md`](02-architecture.md). For the full technology list and the
reasoning behind each choice, see [`03-tech-stack.md`](03-tech-stack.md).

## What it does

**Messaging**
- Private and group chats, with group ownership and transferable ownership
- Real-time delivery over WebSocket/STOMP, with `SENT → DELIVERED → READ` status tracking
- Message editing, pinning, replies, and emoji reactions (capped per message in group chats)
- Typing indicators and online presence
- Message reminders (delayed, delivered at a specific future time)
- Web Push notifications for participants who are offline when a message arrives

**Moderation & safety**
- User-to-user blocking, enforced on both private messaging and group invites
- Admin-only group chat blocking/unblocking, protected by method-level authorization
- Rate limiting on authentication endpoints and WebSocket subscriptions/sends, to blunt
  brute-force and flood attempts

**Reliability**
- Outbox pattern: a message is never "sent" from the database's point of view without a
  guaranteed, at-least-once delivery event, even if the app crashes right after the commit
- A scheduled fallback re-processes anything the fast path missed - no event is silently lost
- Idempotency guards at every consumer, so a redelivered event never gets broadcast twice

## Quick start

```bash
docker-compose up --build
```

This brings up MySQL, Redis, RabbitMQ (with the delayed-message plugin) and the
application itself, in the right order, waiting on each dependency's healthcheck.

Once it's up:
- API docs: `http://localhost:8080/swagger-ui/index.html`
- Health check: `http://localhost:8080/actuator/health`
- A minimal STOMP test page for manually poking the WebSocket API: `http://localhost:8080/stomp-test.html`

## Testing

The project separates fast feedback from slow, high-confidence checks:

```bash
mvn test      # unit + slice tests (MockMvc, mocked collaborators) - no Docker required
mvn verify    # adds integration tests against real MySQL/Redis/RabbitMQ via Testcontainers
```

The integration suite exists specifically to prove things a mock can't: that a
`SELECT ... FOR UPDATE` lock actually serializes concurrent writers, that a Redis
rate-limit counter is atomic under real concurrent load, and that hand-written JPQL
joins behave correctly against a real MySQL instance.
