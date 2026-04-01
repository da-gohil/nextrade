# Contributing to NexTrade

## Branch Naming

All branches should follow this pattern:

| Prefix   | Use for                              | Example                          |
|----------|--------------------------------------|----------------------------------|
| `feat/`  | New features                         | `feat/order-creation-endpoint`   |
| `fix/`   | Bug fixes                            | `fix/kafka-serialization-error`  |
| `chore/` | Maintenance, deps, config            | `chore/update-spring-boot-3.3`   |
| `docs/`  | Documentation only                   | `docs/api-reference`             |
| `test/`  | Adding or fixing tests               | `test/payment-integration-tests` |

All feature branches should be cut from `develop`, not `main`.

## Commit Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<optional scope>): <description>

feat(order): add order creation endpoint
fix(inventory): handle race condition in stock reservation
test(payment): add integration tests for refund flow
chore: update docker-compose with kafka healthcheck
docs(auth): document JWT refresh flow
```

Valid types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`

## Pull Request Process

1. Branch off `develop` and keep changes focused.
2. Ensure `mvn verify -P integration-tests` passes locally before opening a PR.
3. PR titles must follow Conventional Commits format (enforced by CI).
4. Target `develop` for all feature/fix branches; only `develop → main` merges trigger staging deploy.
5. Squash-merge preferred for feature branches; merge commit for `develop → main`.
6. At least one approval required before merge.

## Code Style

- Java: Google Java Format via Spotless (`mvn spotless:apply`).
- TypeScript/Angular: ESLint (`npm run lint`).
- Run `mvn spotless:check` locally before pushing — CI will fail on formatting errors.
