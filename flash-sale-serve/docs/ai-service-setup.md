# AI Service Local Setup

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

## Nacos template

If you want a service-specific Nacos config template for `ai-service`, use:

- `flash-sale-serve/docs/nacos-templates/ai-service.yaml`
