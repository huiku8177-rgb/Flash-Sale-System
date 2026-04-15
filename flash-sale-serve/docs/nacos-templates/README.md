# Nacos YAML Templates

These files map to the Data IDs recommended in `docs/nacos-config-guide.md`.

Suggested setup:

- Namespace: your project namespace
- Group: `FLASH_SALE`
- Format: `YAML`

Recommended import order in each service:

- `flash-sale-common.yaml`
- shared infra config such as MySQL / Redis / RabbitMQ / JWT
- service specific config such as `gateway.yaml`

Available service templates:

- `auth-service.yaml`
- `product-service.yaml`
- `seckill-service.yaml`
- `order-service.yaml`
- `gateway.yaml`
- `ai-service.yaml`

You can paste each file into the Nacos console as-is and then adjust the values for your environment.

## OpenAPI / Swagger

`gateway.yaml` contains the Swagger aggregation configuration.

Gateway docs entry:

- `http://localhost:8080/swagger-ui.html`

Aggregated OpenAPI JSON routes:

- `/v3/api-docs/auth-service`
- `/v3/api-docs/product-service`
- `/v3/api-docs/seckill-service`
- `/v3/api-docs/order-service`
- `/v3/api-docs/ai-service`

`ai-service.yaml` enables the downstream AI service docs:

- direct Swagger UI: `http://localhost:8085/swagger-ui.html`
- direct OpenAPI JSON: `http://localhost:8085/v3/api-docs`

The gateway auth exclude list must keep `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, and `/v3/api-docs/**` public so the docs page can load without a token.

## AI service secrets

Do not paste the real DashScope API key into `ai-service.yaml`.

Use the runtime environment variable instead:

- `FLASH_SALE_AI_API_KEY=your-secret-key`
