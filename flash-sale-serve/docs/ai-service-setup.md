# AI Service Local Setup

## API docs

`ai-service` exposes OpenAPI through SpringDoc.

Direct service access:

- Swagger UI: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

Gateway aggregation:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- AI Service OpenAPI JSON through gateway: `http://localhost:8080/v3/api-docs/ai-service`

Gateway routing requirements:

- `/ai/**` routes to `lb://ai-service`
- `/v3/api-docs/ai-service` routes to `lb://ai-service` and rewrites to `/v3/api-docs`
- `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, and `/v3/api-docs/**` must be in the gateway auth exclude list

Main API groups:

- `AI 问答`: chat, session list/detail/delete, product resolution
- `知识管理`: knowledge sync, sync task status, knowledge stats

## Project-only secret setup

Do not configure `FLASH_SALE_AI_API_KEY` as a system-wide environment variable unless you want every project on this computer to inherit it.

Use one of these project-scoped approaches instead:

1. PowerShell session only

Run the example script below in the terminal you will use to start `ai-service`:

- `flash-sale-serve/docs/ai-service-local-env.example.ps1`

This affects only the current PowerShell window. Close the window and the variables disappear.

2. IDE run configuration

Set these environment variables in the run configuration for `ai-service` only:

- `FLASH_SALE_AI_ENABLED=true`
- `FLASH_SALE_AI_API_KEY=your-real-key`
- `FLASH_SALE_AI_PORT=8085`

This is usually the cleanest option if you start Spring Boot from IntelliJ IDEA.

## Recommended practice

- Keep `application-local.yml` free of real keys.
- Keep `ai.enabled` defaulted to `false` in git-tracked config.
- Enable AI only in the terminal session or IDE run configuration where you need it.
- In local/dev profiles, embedding can fall back to a local hash vector when DashScope is unavailable. This keeps local startup and basic chat testing unblocked, but it is not a production-quality semantic embedding.

## Nacos template

If you want a service-specific Nacos config template for `ai-service`, use:

- `flash-sale-serve/docs/nacos-templates/ai-service.yaml`

The service-specific Nacos config should include:

- `springdoc.api-docs.enabled=true`
- `springdoc.swagger-ui.path=/swagger-ui.html`
- `ai.enabled`
- `ai.base-url`
- `ai.chat-model`
- `ai.embedding-model`
- `ai.product-service-url`
- `ai.seckill-service-url`
- retrieval, history, confidence, cleanup, and rule document settings

Do not put the real DashScope key in Nacos or git-tracked files. Keep using:

- `FLASH_SALE_AI_API_KEY=your-real-key`
