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

You can paste each file into the Nacos console as-is and then adjust the values for your environment.
