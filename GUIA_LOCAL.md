# Guia local - notification-service

## Que es
Servicio de notificaciones: intake de alertas/recomendaciones, deduplicacion y envio de email.

## Prerrequisitos
- Docker + Docker Compose, o Java 21
- PostgreSQL
- SMTP (Mailpit recomendado en local)

## Opcion A: correr standalone con este repo
Usa `notification-service/docker-compose.yml`.

```bash
docker compose up -d
```

Por configuracion del repo:
- app: `http://localhost:8084` (si `.env` mantiene `APP_PORT=8084` y `PORT=8084`)
- db: host definido por `DB_PORT` en `.env`
- Mailpit UI: `http://localhost:8025`

Apagar:

```bash
docker compose down
```

## Opcion B: correr en stack completo
Desde `gateway-api/`:

```bash
docker compose up -d notification-servicedb mailpit notification-service
```

En stack completo:
- app: `http://localhost:8084`
- db: `localhost:5434`
- Mailpit UI: `http://localhost:8025`

## Correr por Gradle

```bash
set -a
source .env
set +a
./gradlew bootRun
```

## Uso directo

```bash
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:8084/api/v1/contact"
```

## Uso via gateway

```bash
curl -H "Authorization: Bearer <jwt>" \
  "http://localhost:8080/api/notification/api/v1/contact"
```

## Referencias
- `README_API.md`
- `docs/application-overview.md`
- `docs/frontend-endpoints.md`
