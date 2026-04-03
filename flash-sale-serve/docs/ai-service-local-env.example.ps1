$env:FLASH_SALE_AI_ENABLED = "true"
$env:FLASH_SALE_AI_PORT = "8085"
$env:FLASH_SALE_AI_API_KEY = "replace-with-your-real-key"

# Optional overrides
$env:SPRING_PROFILES_ACTIVE = "local"
$env:FLASH_SALE_NACOS_CONFIG_ENABLED = "false"

# Example: after running this script in the current PowerShell window,
# start ai-service from the same window so only this project session uses the key.
# mvn -pl ai-service spring-boot:run
