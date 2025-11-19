# Notification Service API

Documentación funcional de la API HTTP expuesta por `notification-service`. Resume autenticación, endpoints, contratos JSON y lineamientos de integración basados en el código vigente.

## Autenticación y seguridad
- **Esquema general:** Resource Server JWT (Auth0). Cada request debe incluir `Authorization: Bearer <token>` cuando el perfil `auth` está activo.
- **Validaciones del JWT:** emisor (`iss`) y audiencia (`aud`) obligatorios; se rechazan tokens sin el _claim_ configurado en `JWT_USER_ID_CLAIM` (o `user_id`/`sub` como _fallback_).
- **Identidad del usuario:** se deriva como UUID estable (directo o `nameUUID`). Se usa para personalizar contactos y deduplicación.
- **Webhooks:** `/api/v1/webhooks/email` no usa JWT; exige header `X-Webhook-Token` coincidente con `notification.webhooks.email.token` **o** IP remota incluida en `notification.webhooks.email.allowed-ips`.
- **Permisos / scopes:** el servicio no exige _authorities_ específicos hoy; validar en API Gateway si se necesitan restricciones adicionales.

## Catálogo de endpoints
| Método | Path | Descripción | Autenticación |
| ------ | ---- | ----------- | ------------- |
| GET | `/api/v1/contact` | Obtiene la preferencia de contacto asociada al usuario autenticado | JWT Bearer |
| POST | `/api/v1/contact` | Crea o actualiza la preferencia de contacto del usuario autenticado | JWT Bearer |
| POST | `/api/v1/notify/alert` | Encola un email de alerta; respeta deduplicación y horario de silencio | JWT Bearer |
| POST | `/api/v1/notify/recommendation` | Encola una recomendación por email | JWT Bearer |
| POST | `/api/v1/webhooks/email` | Registra eventos del proveedor SMTP y ajusta el `EmailStatus` | Token/IP (ver arriba) |

Las rutas de documentación (`/docs/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/openapi/**`) son públicas.

## Contratos por endpoint

### GET `/api/v1/contact`
Obtiene los datos de contacto. Si no existe registro retorna 404 con Problem+JSON.

#### Response 200
```json
{
  "userId": "7ab02c59-8f45-4ef9-9a5b-b1b0d19cb7d2",
  "primaryEmail": "user@example.com",
  "emailStatus": "VERIFIED",
  "locale": "es-AR",
  "channels": {
    "email": true,
    "sms": false
  },
  "quietHours": {
    "start": "22:00",
    "end": "07:00",
    "timezone": "America/Argentina/Buenos_Aires"
  },
  "version": 3,
  "updatedAt": "2024-06-12T18:34:21.512Z",
  "createdAt": "2023-11-04T14:53:10.102Z"
}
```

#### JSON Schema (response)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["userId", "primaryEmail", "emailStatus", "channels", "version", "updatedAt", "createdAt"],
  "properties": {
    "userId": { "type": "string", "format": "uuid" },
    "primaryEmail": { "type": "string", "format": "email" },
    "emailStatus": { "type": "string", "enum": ["VERIFIED", "UNVERIFIED", "BOUNCED", "UNSUB"] },
    "locale": { "type": ["string", "null"], "maxLength": 10 },
    "channels": {
      "type": "object",
      "additionalProperties": { "type": "boolean" },
      "minProperties": 1
    },
    "quietHours": {
      "type": ["object", "null"],
      "required": ["start", "end"],
      "properties": {
        "start": { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" },
        "end": { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" },
        "timezone": { "type": ["string", "null"], "maxLength": 40 }
      },
      "additionalProperties": false
    },
    "version": { "type": "integer", "minimum": 0 },
    "updatedAt": { "type": "string", "format": "date-time" },
    "createdAt": { "type": "string", "format": "date-time" }
  },
  "additionalProperties": false
}
```

#### Response 404
```json
{
  "type": "urn:problem:user-contact:user-contact-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "User contact not found",
  "instance": "/api/v1/contact",
  "traceId": "UNKNOWN // TODO: instrumentar traceId"
}
```

### POST `/api/v1/contact`
Crea o actualiza la preferencia del usuario autenticado. El backend calcula `userId` y `primaryEmail` mediante el JWT y sobrescribe `createdAt/updatedAt/version`.

#### Request body
```json
{
  "emailStatus": "VERIFIED",
  "locale": "es-AR",
  "channels": {
    "email": true,
    "sms": false
  },
  "quietHours": {
    "start": "22:00",
    "end": "07:00",
    "timezone": "America/Argentina/Buenos_Aires"
  }
}
```

#### JSON Schema (request)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["emailStatus", "channels"],
  "properties": {
    "emailStatus": { "type": "string", "enum": ["VERIFIED", "UNVERIFIED", "BOUNCED", "UNSUB"] },
    "locale": { "type": ["string", "null"], "maxLength": 10 },
    "channels": {
      "type": "object",
      "additionalProperties": { "type": "boolean" },
      "minProperties": 1
    },
    "quietHours": {
      "type": ["object", "null"],
      "required": ["start", "end"],
      "properties": {
        "start": { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" },
        "end": { "type": "string", "pattern": "^([01]\\d|2[0-3]):[0-5]\\d$" },
        "timezone": { "type": ["string", "null"], "maxLength": 40 }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

#### Response 200
Idéntico al schema de `GET /api/v1/contact`.

### POST `/api/v1/notify/alert`
Encola un envío de alerta por email. Deduplica por `userId` + `fingerprint` dentro de la ventana configurada (`dedup.window-minutes`, por defecto 5 min). Las alertas con `severity = "CRITICAL"` omiten las horas silenciosas.

- **Enriquecimiento automático:** si `templateData.symbol` está presente, el servicio consulta Portfolio Service (`POST /internal/v1/holdings/search`) y añade el arreglo `holdings` al JSON pasado a la plantilla. Cuando no existen posiciones para el símbolo pedido, el arreglo queda vacío.

#### Request body
```json
{
  "userId": "7ab02c59-8f45-4ef9-9a5b-b1b0d19cb7d2",
  "fingerprint": "credit-risk-alert-123",
  "occurredAt": "2024-07-01T10:11:12Z",
  "severity": "HIGH",
  "templateKey": "alert-default",
  "templateData": {
    "title": "Límite excedido",
    "amount": 2400.75
  }
}
```

#### JSON Schema (request)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["userId", "fingerprint", "occurredAt", "severity", "templateKey", "templateData"],
  "properties": {
    "userId": { "type": "string", "format": "uuid" },
    "fingerprint": { "type": "string", "minLength": 1, "maxLength": 128 },
    "occurredAt": { "type": "string", "format": "date-time" },
    "severity": { "type": "string", "minLength": 1, "maxLength": 32 },
    "templateKey": { "type": "string", "minLength": 1, "maxLength": 64 },
    "templateData": {}
  },
  "additionalProperties": false
}
```

#### Response 200
```json
{
  "accepted": true,
  "jobId": "6b9311be-2726-48d6-83f9-3720f19de265",
  "scheduledAt": "2024-07-01T10:11:12Z"
}
```

#### JSON Schema (response)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["accepted"],
  "properties": {
    "accepted": { "type": "boolean" },
    "jobId": { "type": ["string", "null"], "format": "uuid" },
    "scheduledAt": { "type": ["string", "null"], "format": "date-time" },
    "reason": {
      "type": ["string", "null"],
      "enum": ["deduplicated", "contact_not_found", "email_channel_disabled", "email_status_blocked", null]
    }
  },
  "additionalProperties": false
}
```

#### Casos de rechazo
```json
{
  "accepted": false,
  "reason": "deduplicated"
}
```

Los posibles valores de `reason` provienen de `NotificationRejectReason` (convertidos a minúsculas).

### POST `/api/v1/notify/recommendation`
Mismo contrato que el endpoint de alertas, reemplazando el campo `severity` por `kind`.

#### Request body
```json
{
  "userId": "7ab02c59-8f45-4ef9-9a5b-b1b0d19cb7d2",
  "fingerprint": "investment-reco-202407",
  "occurredAt": "2024-07-01T10:11:12Z",
  "kind": "PORTFOLIO_TIP",
  "templateKey": "reco-default",
  "templateData": {
    "title": "Nueva alternativa de inversión",
    "score": 0.87
  }
}
```

El schema de respuesta es idéntico al de `/api/v1/notify/alert`.

### POST `/api/v1/webhooks/email`
Recibe eventos del proveedor SMTP (bounces, entregas, aperturas, etc.). Persiste el evento y, si viene `userId`, actualiza el `emailStatus`.

#### Request body
```json
{
  "eventId": "982117f2-e4ec-4dbd-bb80-37bcc6d58ef3",
  "userId": "7ab02c59-8f45-4ef9-9a5b-b1b0d19cb7d2",
  "email": "user@example.com",
  "kind": "DELIVERED",
  "providerReference": "smtp-12345",
  "occurredAt": "2024-07-01T10:05:30Z",
  "payload": {
    "smtpCode": "250 2.0.0 OK"
  }
}
```

#### JSON Schema (request)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "required": ["email", "kind", "occurredAt"],
  "properties": {
    "eventId": { "type": ["string", "null"], "format": "uuid" },
    "userId": { "type": ["string", "null"], "format": "uuid" },
    "email": { "type": "string", "format": "email" },
    "kind": {
      "type": "string",
      "enum": ["DELIVERED", "OPEN", "CLICK", "BOUNCE", "COMPLAINT", "UNSUBSCRIBE", "OTHER"]
    },
    "providerReference": { "type": ["string", "null"], "maxLength": 255 },
    "occurredAt": { "type": "string", "format": "date-time" },
    "payload": {}
  },
  "additionalProperties": false
}
```

#### Response 202
Sin body (`application/json` vacío).

#### Response 401
Cuando el token/IP no coincide:
```json
{
  "timestamp": "2024-07-01T10:05:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "path": "/api/v1/webhooks/email",
  "message": "Unauthorized webhook request"
}
```

## Paginación
No hay endpoints con colecciones paginadas en esta versión.

## Límite de tasa
`UNKNOWN // TODO: acordar política de rate limit y encabezados informativos con la plataforma`.

## Idempotencia
- **Notificaciones:** deduplicación interna por `(userId, fingerprint, bucket)` con un bucket configurable (`dedup.window-minutes`). Enviar el mismo `fingerprint` dentro de la ventana produce respuesta `accepted=false` con `reason=deduplicated`.
- **Contactos:** la operación `POST /contact` es _upsert_; evite reintentos concurrentes con versiones antiguas. No hay _ETag_ ni `If-Match`.
- **Webhooks:** el endpoint acepta reintentos; si `eventId` se repite el repositorio debe manejar la idempotencia (**verificar implementación en base de datos: TODO**).

## Manejo de errores
- **Problem+JSON** (RFC 7807) para errores de aplicación (404, 400, validaciones). Campos: `type`, `title`, `status`, `detail`, `instance`, `traceId`.
- **Autenticación JWT:** respuestas 401 con estructura `AuthenticationErrorHandler.ErrorResponse`:
  ```json
  {
    "error": "invalid_token",
    "message": "The provided JWT is invalid",
    "details": "Make sure the token is well-formed and not expired",
    "timestamp": "2024-07-01T10:15:00.000Z",
    "status": 401
  }
  ```
- **Validaciones (Jakarta Bean Validation):** Spring devolverá 400 con detalle estándar; se recomienda mapearlo a errores de negocio a futuro (**TODO**).

## Guía mínima de integración
### Flujo 1: Sincronizar preferencias de contacto
1. Obtener token JWT con el claim `JWT_USER_ID_CLAIM`.
2. `GET /api/v1/contact` para verificar estado actual.
3. Aplicar cambios con `POST /api/v1/contact`.
4. Registrar `version` para auditoría en el consumidor.

**Mapeo de campos**
| Campo | Fuente sugerida | Notas |
| ----- | ---------------- | ----- |
| `primaryEmail` | Email principal del usuario | Obligatorio |
| `emailStatus` | Estado interno (`VERIFIED`, `UNVERIFIED`, etc.) | Conviene alinear con proveedor |
| `channels` | Flags por canal (`email`, `sms`, etc.) | Se almacenan como JSON dinámico |
| `quietHours` | Ventana horaria local | Usar formato `HH:mm` y `tz` IANA |

**Errores y reintentos:** Reintentar ante errores 5xx con _exponential backoff_. No reintentar 4xx salvo 409 (no implementado aún) o `unauthorized`.

### Flujo 2: Encolar notificaciones transaccionales
1. Resolver `userId` (UUID). Si el front solo tiene identificadores externos, solicitar al servicio de identidad.
2. Preparar `fingerprint` único por evento para deduplicación.
3. Enviar a `/api/v1/notify/alert` o `/api/v1/notify/recommendation` según corresponda.
4. Si `accepted=false` revisar `reason`:
   - `deduplicated`: descartar reintentos.
   - `contact_not_found`: sincronizar contacto y reintentar.
   - `email_channel_disabled` o `email_status_blocked`: aplicar fallback (push, SMS).
5. Si `accepted=true`, almacenar `jobId` para seguimiento.

**Reintentos:** Para 5xx usar _retry_ con jitter. Si el problema es `contact_not_found`, crear contacto antes de reintentar.

### Flujo 3: Ingesta de webhooks del proveedor SMTP
1. Configurar el proveedor para enviar eventos a `/api/v1/webhooks/email`.
2. Incluir header `X-Webhook-Token` con el secreto acordado.
3. Reintentar on failure con backoff exponencial. El servicio es idempotente por `eventId` (revisar persistencia: **TODO confirmar upsert en DB**).
4. Escuchar cambios en `EmailStatus` mediante consultas posteriores o eventos downstream (**TODO: no implementado**).

## Información adicional
- Host base: `UNKNOWN // TODO: definir URL base por entorno`.
- Versionado del contrato: `v1`. Cambios incompatibles deben publicarse como `/api/v2`.
- Observabilidad: el servicio emite logs JSON (WARN/ERROR) y métricas New Relic (requiere agente). Exponer `traceId` en responses aún es **TODO**.
