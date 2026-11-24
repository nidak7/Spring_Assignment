# Spring Assignment: Post and Comment API with Redis Guardrails

This is a Spring Boot backend for a small social feed where users and bots can post, comment and like. I used PostgreSQL to store the data and Redis to enforce limits, like how many bot comments a post can get.

## What it does

- Create posts, comment on posts (reply depth limit of 20) and like posts
- Keeps a virality score per post in Redis (bot reply +1, human comment +50, human like +20)
- Allows at most 100 bot comments per post, using a Redis Lua script so the count stays correct with many requests at once
- Blocks the same bot from talking to the same human again within 10 minutes (HTTP 429)
- When a bot interacts with a user's post, it sends one notification, then holds the next ones in a Redis list for 15 minutes; a scheduled job logs a summary of pending ones every 5 minutes
- Seeds 3 users and 3 bots on first start

## Tech used

Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL, Redis, Maven, Docker Compose

## How to run

```bash
docker compose up -d
mvn clean spring-boot:run
```

The API runs on http://localhost:8080. Endpoints: `POST /api/posts`, `GET /api/posts/{id}`, `POST /api/posts/{id}/comments`, `POST /api/posts/{id}/like`. There is a Postman collection in the `postman` folder.

## What I would improve

- There are no automated tests yet
- Tables are created with Hibernate `ddl-auto: update`, a migration tool would be better
- The database password in `application.yml` is only a local default
