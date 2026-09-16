# 🛡️ Production Hardening

Six targeted fixes, each closing a real gap that only shows up once the
happy path stops being the only path: a dependency goes down, a client sends
a malformed request, or someone actually tries to scrape `/actuator`. None
of these are new infrastructure - every fix is a handful of lines in a file
that already existed.

## 1. Redis outage no longer takes down login

**Before:** `AuthRateLimitFilter` called `redisService.isAllowed(...)`
directly. If Redis was unreachable, the resulting `RedisConnectionFailureException`
propagated out of a `Filter` - which runs *before* the `DispatcherServlet`,
so `GlobalExceptionHandler` (a `@RestControllerAdvice`) never got a chance
to see it. Every login and registration attempt would fail with a raw
container error page, even though rate limiting is a secondary, protective
concern, not core to authentication.

**After:** the Redis call is wrapped and fails **open** - on a
`DataAccessException` the request is allowed through and a warning is
logged, instead of rate-limiting (a non-critical dependency) taking down
login (a critical path). The same fix was applied to
`WebSocketSubscriptionGuard`'s subscribe/send rate limits for the same
reason.

*Covered by:* `AuthRateLimitFilterTest#doFilter_redisUnavailable_failsOpenAndPassesThrough`,
`WebSocketSubscriptionGuardTest#preSend_chatSendRedisDown_failsOpenAndPassesThrough`.

## 2. Avatar uploads are validated by content, not trusted by filename

**Before:** `FileServiceImpl` derived the stored file's extension straight
from the client-supplied `originalFilename`, with no allowlist on content
type. A file named `avatar.html` (or `.svg`, `.js`) would be accepted,
stored under `/uploads/**`, and served back statically and
unauthenticated - same-origin as the API itself.

**After:** `saveFile` now rejects anything outside an explicit allowlist
(`image/jpeg`, `image/png`, `image/webp`) before touching the filesystem,
and the stored extension is derived from the *validated* content type, not
from user input.

*Covered by:* `FileServiceImplTest` (allowed type, unsupported type, null
content type, empty file, delete).

## 3. `/actuator/prometheus` is actually scrapable

**Before:** `management.endpoints.web.exposure.include` turned the
endpoint on, but `SecurityAccessConfiguration` only allow-listed
`/actuator/health` - every other actuator path required a valid JWT. A real
Prometheus server has no JWT to present, so metrics collection would 401 on
every scrape if anyone actually pointed monitoring at this service.

**After:** `/actuator/prometheus` is public, matching `/actuator/health`.
Scraping in a real deployment is expected to happen from a trusted internal
network - a separate management port is the next step up if that
assumption ever stops holding, but it isn't needed at this scale.

## 4. Pagination has an upper bound

**Before:** `page`/`size` on `GET /messages/chat/{chatId}` and
`GET /admin/feedback` were plain `int` query params with no validation -
`size=5000000` would happily try to load that many rows in one request.

**After:** `@Min`/`@Max` constraints on both controllers (`@Validated` at
the class level, since constraints on bare method parameters need it), plus
a new `GlobalExceptionHandler` handler for `ConstraintViolationException` so
a violation comes back as the same `VALIDATION_ERROR` shape every other bad
input already returns, instead of a generic 500.

*Covered by:* `MessageControllerTest` (default paging, oversized `size`,
negative `page`, zero `size`).

## 5. Logging profile for anything other than a laptop

**Before:** `logging.level.org.springframework.security=DEBUG` and
`org.example=DEBUG` were the only values that existed, in the same
properties file used to boot the app everywhere - including whatever ran in
CI. DEBUG-level security logging is not something you want on by default
outside a dev machine. Separately, `spring.profiles-active` (note: no dot)
isn't a real Spring property - it was silently doing nothing.

**After:**
- Fixed the typo: `spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}`.
- Added `application-prod.properties`, activated with
  `SPRING_PROFILES_ACTIVE=prod`: `INFO`/`WARN` logging instead of `DEBUG`,
  and `management.endpoint.health.show-details=when-authorized` instead of
  `always` - the health endpoint is public for container healthchecks, and
  `always` hands an unauthenticated caller datasource/disk-space details it
  has no reason to see.

## 6. JWT access tokens are short-lived, refresh tokens are revocable

**Before:** a single JWT with `jwt.expiration=86400000` (24 hours) and no
way to invalidate it. A leaked token - logs, browser storage, a stray
screenshot - stays valid for a full day with nothing the server can do
about it, because a stateless JWT can't be revoked by definition.

**After:** standard access/refresh split, sized for what this project
actually needs - no token-family reuse detection, no device fingerprinting,
no httpOnly-cookie handling (there's no frontend yet to set one from):

- **Access token** (JWT, unchanged mechanism): `jwt.expiration` dropped to
  15 minutes.
- **Refresh token**: an opaque random 256-bit value, **not** a JWT - it
  doesn't need to be decoded, only looked up. Stored in Redis as
  `refresh:<sha256(token)>` → `userId`, TTL 7 days. Only the hash is
  stored, so a Redis dump doesn't hand out usable tokens.
- **`POST /auth/refresh`**: exchanges a valid refresh token for a new
  access/refresh pair and **rotates** - the old refresh token is deleted
  immediately. A stolen token used once locks the legitimate user out on
  their next refresh too, which is a cheap, immediate signal something is
  wrong, instead of a silently reusable long-lived credential.
- **`POST /auth/logout`**: deletes the stored refresh token outright -
  actual, real session revocation, which a bare JWT can never offer.

*Covered by:* `AuthenticationServiceImplTest` (login issues both tokens,
refresh rotates and returns a new pair, unknown/expired/deleted-user tokens
are rejected, logout deletes the stored token),
`AuthenticationControllerTest` (the three `/auth/*` endpoints end-to-end
through validation and `GlobalExceptionHandler`).

**Known, deliberate scope cut:** `stomp-test.html` still only exercises
`/auth/authentication` and stores a single `token`. Wiring the manual test
client to call `/auth/refresh` on expiry is a small, isolated follow-up -
not done here to keep this change to the backend contract itself.
