# Repository Guidelines

## Project Structure & Module Organization

ShopCore is a Java 21, Spring Boot 4.1 modular monolith. The Maven application lives in `backend/`; there is no root Maven project.

- `backend/src/main/java/io/github/datnguyen2810/shopcore/`: feature packages `auth`, `rbac`, `user`, `category`, and `product`; shared auditing, exceptions, and responses live in `common`.
- `backend/src/main/resources/`: application configuration, startup banner, and Flyway SQL in `db/migration/`.
- `backend/src/test/java/`: tests mirroring the application package hierarchy and shared Testcontainers configuration.
- `backend/compose.yaml`: local PostgreSQL 17 service.
- `docs/`: architecture diagrams, interview notes, and supporting images.

Keep controllers, services, repositories, mappers, and DTOs within their feature package.

## Build, Test, and Development Commands

Install JDK 21 and start Docker Desktop. Run these commands from `backend/`:

- `docker volume create backend_shopcore-postgres-data`: create the required external database volume once.
- `.\mvnw.cmd spring-boot:run`: run the API; Spring Boot starts PostgreSQL through Docker Compose.
- `.\mvnw.cmd test`: run tests, including the container-backed context test.
- `.\mvnw.cmd clean verify`: clean, build, and run verification.
- `.\mvnw.cmd package -DskipTests`: build the executable JAR in `target/` without running tests.
- `docker compose down`: stop local services while retaining the database volume.

On macOS/Linux, replace `.\mvnw.cmd` with `./mvnw`. Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

## Coding Style & Naming Conventions

Use four-space Java indentation, `PascalCase` class names, `camelCase` members, and `UPPER_SNAKE_CASE` constants. Follow existing suffixes such as `Controller`, `ServiceImpl`, `Repository`, `Mapper`, and `Request`/`Response`. Prefer constructor injection and existing Lombok patterns. Keep business logic and transaction boundaries in services. No formatter or lint plugin is configured; match nearby code.

## Testing Guidelines

Use JUnit Jupiter and Spring Boot test support. Database tests use PostgreSQL Testcontainers and require Docker. Name test classes `*Test` or `*Tests`. Add focused regression coverage for changed behavior, especially authorization, token rotation, and catalog validation. The existing suite contains a context smoke test; no coverage threshold is configured.

## Commit & Pull Request Guidelines

Follow history's prefixes: `feat:`, `chore:`, and `docs:` with concise, imperative summaries. Keep commits focused. PRs should describe behavior changes, link relevant issues, report verification commands/results, and flag API, configuration, or migration changes.

## Security & Configuration

Never commit production credentials or tokens. Override the local JWT secret through `APP_JWT_SECRET` for deployment. Use new versioned Flyway migrations such as `V4__describe_change.sql`; preserve applied migrations and Hibernate's `ddl-auto: validate` setting.
