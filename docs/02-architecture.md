# Architecture — how it works under the hood

This document walks through the parts of the system that aren't obvious from the
API surface alone: how authentication differs between REST and WebSocket, how a
message actually gets from one client's screen to another's, and why certain
decisions (Outbox pattern, pessimistic locking, distributed locks) exist at all.

## 1. Component overview

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

## 2. Authentication — two independent mechanisms

REST and WebSocket don't share an authentication path, because they don't share a
threading model. `SecurityContextHolder` is backed by a `ThreadLocal` - it lives for
the duration of one HTTP request on one thread. STOMP frames over an already-open
WebSocket connection are handled on a completely different thread pool
(`clientInboundChannel`), where that `ThreadLocal` was never populated. So the two
paths are deliberately separate components.

### 2.1 REST (HTTP)

```mermaid
sequenceDiagram
    participant C as Client
    participant F as JwtAuthenticationFilter
    participant U as JwtUtil
    participant S as SecurityContextHolder
    participant Ctrl as Controller

    C->>F: HTTP request + Authorization: Bearer <token>
    F->>U: validateToken(token)
    alt token valid
        U-->>F: returns normally
        F->>S: set Authentication
        F->>Ctrl: filterChain.doFilter()
        Ctrl-->>C: 200 + response body
    else token expired / tampered
        U-->>F: throws JwtTokenExpiredException / InvalidJwtTokenException
        F-->>C: 401 Unauthorized, clean JSON body
    end
```

`JwtAuthenticationFilter` runs before `DispatcherServlet` in the filter chain, so a
`@RestControllerAdvice` never gets a chance to handle anything it throws - the
filter has to catch and translate its own exceptions into an HTTP response itself.

### 2.2 WebSocket (STOMP)

```mermaid
sequenceDiagram
    participant C as Client
    participant I as JwtChannelInterceptor
    participant U as JwtUtil
    participant DB as UserRepository
    participant G as WebSocketSubscriptionGuard

    Note over C,I: STOMP CONNECT (once, at the start of the session)
    C->>I: CONNECT + Authorization header inside the STOMP frame
    I->>U: validateToken(token)
    U-->>I: returns normally
    I->>DB: findByEmail(email)
    DB-->>I: User
    I->>I: accessor.setUser(new WebSocketPrincipal(userId))
    Note over I: identity is now bound to this session for its whole lifetime

    Note over C,G: every SUBSCRIBE after that
    C->>G: SUBSCRIBE /topic/chat/{id}
    G->>G: is the caller a participant? under the rate limit?
    G-->>C: allowed, or WebSocketAccessDeniedException

    Note over C,I: any later SEND frame
    C->>I: SEND /app/chat.send
    Note over I: validateToken() is NOT called again here -<br/>only CONNECT triggers it
```

That last note is a deliberate, documented trade-off, not an oversight: a token that
expires mid-session is not re-checked on every frame. It's covered by a test
(`JwtChannelInterceptorTest.preSend_sendCommandWithExpiredToken_isNeverRevalidated`)
specifically so the behavior is visible and intentional rather than an unverified
assumption.

## 3. Sending a message: from click to the other screen

```mermaid
sequenceDiagram
    participant A as Client A (sender)
    participant WC as WebSocketController
    participant MS as MessageServiceImpl
    participant DB as MySQL
    participant OP as OutboxEventPublisher
    participant TEL as OutBoxEventProcessor
    participant MQ as RabbitMQ
    participant OC as OutboxRabbitConsumer
    participant EL as MessageEventListener
    participant B as Client B (recipient)

    A->>WC: SEND /app/chat.send
    WC->>MS: sendMessage(request, principal)
    MS->>DB: lock chat row (SELECT ... FOR UPDATE)
    MS->>DB: get_next_message_sequence(chatId) [atomic DB function]
    MS->>DB: INSERT Message, UPDATE Chat
    MS->>OP: publish(OutboxEvent MESSAGE_SENT)
    OP->>DB: INSERT OutboxEvent — same transaction as the message
    Note over MS,DB: transaction commits here - message and outbox row<br/>succeed or fail together
    OP-->>TEL: in-process event, fires only AFTER_COMMIT
    TEL->>MQ: convertAndSend(outbox.exchange, event)
    MQ->>OC: @RabbitListener delivers it
    OC->>EL: handleMessageSent(event)
    EL->>B: /topic/chat/{chatId} — real-time broadcast
    EL->>A: /queue/delivery — delivery ack to the sender
    Note over TEL: if this fast path never fires (crash, broker unreachable),<br/>a @Scheduled poll every 10s picks up anything still unprocessed
```

### Why Outbox instead of just publishing directly?

If `sendMessageInternal()` called `messagingTemplate.convertAndSend(...)` directly,
and the process crashed - or RabbitMQ was briefly unreachable - right after the
database commit, the message would exist in the database but nobody would ever be
told to deliver it. The Outbox pattern turns "deliver this" into a row in the same
transaction as the business data: either both the message and the delivery
obligation are committed together, or neither is. Publishing to RabbitMQ is a
separate, retryable step that can safely happen after the fact without risking data
loss - and if the fast, event-driven path misses it, the scheduled poller in
`OutBoxEventProcessor.process()` guarantees it still goes out, just up to 10 seconds
later.

Every consumer (`OutboxRabbitConsumer`, `RemindersRabbitConsumer`, and each handler
inside `MessageEventListener`) additionally checks a Redis idempotency key before
acting, since RabbitMQ and the retry logic both guarantee **at-least-once**
delivery, not exactly-once - a message can legitimately be redelivered, and nothing
should be broadcast to a client twice because of it.

## 4. Reliability & concurrency patterns, in one table

| Pattern | Where | What it prevents |
|---|---|---|
| Outbox Pattern | `OutboxEventPublisher` + `OutBoxEventProcessor` | Losing a delivery event if the app crashes between the DB commit and publishing to RabbitMQ |
| Pessimistic row lock | `chatRepository.lockChatForUpdate()` | Two concurrent messages in the same chat getting the same `sequence` number |
| Atomic DB function | `get_next_message_sequence()` (MySQL) | The increment-then-read race that a plain `SELECT MAX(sequence)+1` would have under load |
| Distributed lock via Redis | `SchedulerLockManager` | Two application instances running the same `@Scheduled` job at once, if the app is ever scaled horizontally |
| Idempotency keys (Redis `SETNX`) | Every RabbitMQ consumer | Broadcasting the same event twice after an at-least-once redelivery |
| Atomic rate-limit counter (Redis Lua script) | `RedisServiceImpl.isAllowed()` | A window where a crash between `INCR` and `EXPIRE` would leave a key permanently stuck past its limit |
| TTL-based presence | `RedisService.setUserOnline()` | Needing an explicit disconnect signal - a client that vanishes (crash, dead network) just stops renewing the key and "goes offline" on its own |

## 5. Data model

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

`OutboxEvent` is deliberately not linked to the other entities above - it's not a
domain relationship, it's an infrastructure record (event type, JSON payload,
processing state) written in the same transaction as whatever business change
triggered it. See section 3 for how it's actually used.
