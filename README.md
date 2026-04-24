# Spring Assignment

Spring Boot 3.x backend for the Grid07 backend engineering assignment. The service uses PostgreSQL for persisted content, Redis for real-time guardrails, and a scheduled notification sweeper for batched bot interaction alerts.

## What This Project Covers

- Post creation
- Comment creation with depth validation
- Post likes
- Redis-backed virality scoring
- Atomic bot reply guardrails
- Redis-backed notification throttling and batching
- Stateless request handling with PostgreSQL as the persistent source of truth

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- PostgreSQL
- Redis
- Docker Compose

## Design Notes

The assignment requires `author_id` to represent either a `User` or a `Bot`. To keep writes predictable and avoid brittle polymorphic mappings, posts and comments store `authorId` together with `isAuthorBot`. That keeps the persistence model explicit while still letting the API validate the author against the correct table before saving.

PostgreSQL stores the durable business data:

- users
- bots
- posts
- comments

Redis acts as the guardrail layer:

- virality counters
- bot reply counters
- bot-to-human cooldown keys
- notification cooldown keys
- pending notification lists

No counters, locks, or notification queues are stored in Java memory.

## Redis Guardrails

### 1. Virality Score

Each post uses the key:

- `post:{id}:virality_score`

Scoring rules:

- bot reply: `+1`
- human comment: `+50`
- human like: `+20`

Virality updates happen in Redis before the response is returned.

### 2. Horizontal Cap

Each post uses:

- `post:{id}:bot_count`

Bot comments are protected with an atomic Redis Lua script that:

1. increments the bot counter
2. rejects the request if the counter exceeds `100`
3. decrements immediately on overflow
4. increments virality only when the comment is allowed

This is what guarantees that 200 simultaneous bot requests cannot persist more than 100 bot comments for the same post.

### 3. Vertical Cap

Comment depth is calculated from the parent comment:

- root comment depth = `1`
- reply depth = `parent.depthLevel + 1`

Any request producing depth `> 20` is rejected before persistence.

### 4. Cooldown Cap

Each bot-to-human interaction uses:

- `cooldown:bot_{botId}:human_{userId}`

Redis uses `SET NX EX` with a 10-minute TTL. If the key already exists, the bot interaction is rejected with `429 Too Many Requests`.

## Thread Safety And Atomic Locking

Thread safety in Phase 2 is guaranteed by keeping the concurrency-sensitive checks inside Redis atomic operations instead of in application memory.

The important part is that the bot comment path does not perform:

- read counter in Java
- compare in Java
- increment later

That pattern would race under concurrent traffic.

Instead, the application executes a Redis Lua script so the counter increment, cap check, cooldown enforcement, and virality update are evaluated as one atomic unit on the Redis server. If Redis rejects the action, the database write is never attempted.

This satisfies the assignment requirement that Redis acts as the gatekeeper and the database transaction only commits if the guardrails allow the action.

## Notification Engine

Only bot interactions trigger notifications.

Redis keys:

- `user:{id}:notif_cooldown`
- `user:{id}:pending_notifs`

Behavior:

- if the notification cooldown does not exist, the service logs `Push Notification Sent to User` and sets a 15-minute cooldown
- if the cooldown already exists, the bot name is appended to the user’s pending Redis list

The scheduler runs every 5 minutes and logs summaries in this format:

- `Summarized Push Notification: Bot X and N others interacted with your posts.`

## Project Structure

```text
src/main/java/com/viral/gateway/
  controller/
  service/
  repository/
  entity/
  config/
  dto/request/
  dto/response/
  exception/
  util/
  scheduler/

src/main/resources/
  application.yml

postman/
  Spring Assignment.postman_collection.json
```

## Seed Data

The application seeds data on the first run if the tables are empty.

Users:

- `1 -> alice`
- `2 -> bob`
- `3 -> carol`

Bots:

- `1 -> newsbot`
- `2 -> helperbot`
- `3 -> financebot`

## Setup

### Prerequisites

- Java 17
- Maven 3.9+
- Docker Desktop or Docker Engine with Compose

### Start PostgreSQL And Redis

From the project root:

```powershell
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`

If you already had an older PostgreSQL volume on this machine and the credentials do not match, reset the local containers once before retrying:

```powershell
docker compose down -v
docker compose up -d
```

### Run The Application

```powershell
mvn clean spring-boot:run
```

The API starts on:

- `http://localhost:8080`

## Configuration

`application.yml` supports environment overrides:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`

Default local values are already configured for the provided `docker-compose.yml`.

## API Endpoints

### Create Post

`POST /api/posts`

```json
{
  "authorId": 1,
  "isAuthorBot": false,
  "content": "My first post"
}
```

### Get Post

`GET /api/posts/{postId}`

Returns the post with the current Redis-backed `viralityScore`.

### Add Comment

`POST /api/posts/{postId}/comments`

Human comment example:

```json
{
  "authorId": 2,
  "isAuthorBot": false,
  "content": "Nice post!"
}
```

Bot comment example:

```json
{
  "authorId": 1,
  "isAuthorBot": true,
  "content": "Bot reply"
}
```

Nested reply example:

```json
{
  "authorId": 2,
  "isAuthorBot": false,
  "content": "Reply to a comment",
  "parentId": 10
}
```

### Like Post

`POST /api/posts/{postId}/like`

```json
{
  "userId": 3
}
```

## Postman Collection

The repo includes the exported collection here:

- [Spring Assignment.postman_collection.json](postman/Spring%20Assignment.postman_collection.json)

Import that file into Postman and run the requests in this order:

1. `Create Post`
2. `Get Post`
3. `Add Human Comment`
4. `Add Bot Comment`
5. `Like Post`

The `Create Post` request stores the generated id into the collection variable `postId`.

## Manual Verification

### Basic Flow

1. Create a post.
2. Fetch the post with `GET /api/posts/{id}`.
3. Add a human comment and confirm virality increases by `50`.
4. Add a bot comment and confirm virality increases by `1`.
5. Like the post and confirm virality increases by `20`.

### Horizontal Cap Test

Create a bot-authored post first so the 10-minute bot-to-human cooldown does not interfere with the cap test.

Example:

```powershell
curl.exe -X POST "http://localhost:8080/api/posts" `
  -H "Content-Type: application/json" `
  -d "{\"authorId\":2,\"isAuthorBot\":true,\"content\":\"Bot test post\"}"
```

Then run:

```powershell
$jobs = 1..200 | ForEach-Object {
  Start-Job -ScriptBlock {
    param($i)
    $body = @{ authorId = 1; isAuthorBot = $true; content = "bot-$i"; parentId = $null } | ConvertTo-Json
    try {
      Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/posts/2/comments" -ContentType "application/json" -Body $body | Out-Null
      "SUCCESS"
    }
    catch {
      $_.Exception.Response.StatusCode.value__
    }
  } -ArgumentList $_
}

$results = Receive-Job -Wait -AutoRemoveJob $jobs
$results | Group-Object | Sort-Object Count -Descending
```

Expected result:

- exactly `100` successful bot comments
- remaining requests return `429`

### Vertical Cap Test

1. Create a root comment.
2. Reply repeatedly using the last comment id as `parentId`.
3. The request that would create depth `21` must fail with `400`.

### Cooldown Test

1. Create a human-authored post.
2. Send a bot comment to that post.
3. Immediately send another bot comment from the same bot to the same human target.
4. The second request must fail with `429`.

### Notification Test

1. Create a human-authored post.
2. Send one bot comment and confirm the log contains `Push Notification Sent to User`.
3. Send more bot interactions within 15 minutes.
4. Wait for the scheduled sweep.
5. Confirm the log contains `Summarized Push Notification: Bot X and N others interacted with your posts.`

## Notes For Reviewers

- The application is stateless.
- Redis is the guardrail layer, not an in-memory cache substitute.
- Database persistence happens only after Redis allows the operation.
- The `GET /api/posts/{id}` endpoint safely reads Redis numeric values even when Redis serialization returns different numeric types.
