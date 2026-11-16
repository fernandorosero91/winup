# WINUP

Plataforma de apuestas deportivas en vivo para un proyecto académico. Los partidos, marcadores y cuotas se generan con un motor simulado; los depósitos y retiros pasan por una pasarela de pagos real en modo sandbox.

Lema del producto: **CADA SEGUNDO IMPORTA**.

- Web: Angular 22 con dos temas, cartera, apuestas en vivo y panel de administración.
- Android: Kotlin y Jetpack Compose con sesión persistente y notificaciones.
- Backend: un único proyecto Spring Boot con dominio rico, ledger de doble entrada y mensajería.

## Alcance

Es un simulador académico. No hay API deportiva real: los partidos se generan con `random`, `replay` o el panel de administración, y siempre se identifican como simulados. El dinero de la cartera se mueve con la pasarela de pagos configurada, por defecto Mercado Pago en sandbox.

## Funcionalidades

Usuario:

- Registro, login JWT, logout y perfil.
- Partidos en vivo por deporte, con cuotas que se mueven.
- Cupón de apuesta con importe, cuota y retorno potencial.
- Historial de apuestas y resultados.
- Cartera con saldo disponible, saldo retenido y movimientos.
- Depósito con Checkout Pro y retiro con retención.
- Notificaciones en web y Android.

Administración:

- Panel con métricas y estado del ledger.
- Gestión de usuarios y roles.
- Creación de partidos, ajuste de cuotas y generación de datos.
- Promociones y envío de correos de prueba.
- Aprobación y rechazo de retiros.

## Arquitectura

```text
Angular 22 ──┐
             ├── HTTP/JSON + WebSocket ── Spring Boot 3.5 ── PostgreSQL 16
Android ─────┘                                  │
                                                ├── Kafka 3.9 (KRaft)
                                                └── RabbitMQ 4.1
```

Un solo backend con capas y dependencias en una dirección:

```text
controller → service → repository → entity
service → model
```

`model` es dominio puro: no importa Spring, Kafka, Rabbit, JPA, HTTP ni JSON.

### Paquetes del backend

| Paquete | Responsabilidad |
|---|---|
| `model` | Agregados, estados y value objects del dominio |
| `entity` | Entidades JPA |
| `repository` | Spring Data JPA |
| `service` | Casos de uso, motor, pagos, ledger y mensajería |
| `controller` | REST, administración, webhooks y WebSocket |
| `dto` y `mapper` | Contratos de entrada y salida |
| `config` | Seguridad, Kafka, Rabbit, WebSocket y propiedades |
| `security` | JWT, filtros y rate limit |
| `exception` | Errores uniformes con `code`, `message` y `traceId` |

### Puertos y adaptadores

- `PaymentGateway`: `SimulatorGateway` y `MercadoPagoGateway` para cobrar.
- `PayoutGateway`: `SimulatorPayoutGateway` y `MercadoPagoPayoutGateway` para dispersar.
- `EmailPort`: `SmtpEmailAdapter`.
- `MobilePushPort`: `RealtimeMobilePushAdapter` y `FcmMobilePushAdapter`.

El proveedor se elige por configuración; los controladores y la UI no cambian.

## Dominio

- `MatchAggregate` con estados creado, abierto, cerrado y liquidado.
- Cuotas y liquidación con `BigDecimal`; nunca `double`.
- `Wallet` como proyección de saldo disponible y retenido.
- `LedgerEntry` de doble entrada: cada movimiento tiene débito y crédito por el mismo importe.
- Cartera en COP, sin conversión de moneda.

Cuentas contables:

```text
PROVIDER_CLEARING:<proveedor>
USER_AVAILABLE:<usuario>
USER_HELD:<usuario>
```

Reglas de dinero:

- Importes como `NUMERIC(19,2)` en PostgreSQL.
- `BigDecimal` en Java y Kotlin.
- Strings en JSON, con `HALF_EVEN` a dos decimales.
- Depósito acreditado una sola vez, incluso con webhooks repetidos.
- Retiro retenido al crearse y liberado si falla o se rechaza.

## Pagos

Depósitos con Mercado Pago Checkout Pro:

1. `POST /api/v1/payments/deposits` con `Idempotency-Key`.
2. El backend crea una preferencia y devuelve `checkoutUrl`.
3. El comprador paga en el checkout alojado.
4. El webhook consulta el pago y verifica importe, moneda y referencia.
5. Se acredita la cartera, se escriben asientos y se registra el evento.

Retornos:

- Web: `https://winup.site/cartera`, mismo origen, sin perder la sesión.
- Android: `winup://payment/return` mediante la página pública de retorno.

Retiros:

- Retención inmediata del saldo.
- Hasta 200.000 COP se pagan al instante con el adaptador simulado.
- Por encima del umbral quedan en `PENDING_APPROVAL` hasta que un administrador decide.
- El adaptador de Mercado Pago usa `POST /v1/payouts` y requiere el producto Payouts habilitado.

Variables principales:

```dotenv
WINUP_PAYMENTS_PROVIDER=mercadopago
WINUP_PAYOUT_PROVIDER=simulator
WINUP_MERCADOPAGO_ACCESS_TOKEN=
WINUP_MERCADOPAGO_PUBLIC_KEY=
WINUP_MERCADOPAGO_SANDBOX=true
WINUP_MERCADOPAGO_NOTIFICATION_URL=
WINUP_MERCADOPAGO_WEBHOOK_SECRET=
WINUP_WITHDRAWALS_APPROVAL_THRESHOLD=200000.00
```

## Mensajería

Kafka:

- Tema `match_events` con clave `matchId`.
- Consumidores idempotentes con inbox (`processed_event`).

RabbitMQ:

- Exchange diferido `match.delayed` para simular el partido.
- Exchange `notifications` y fanout para web, móvil, correo e inbox.
- Colas `q.email.win`, `q.email.loss`, `q.email.promo`, `q.notify.web`, `q.notify.mobile`.
- Dead letter exchange `winup.dlx` y cola `winup.dlq`.

WebSocket:

- Endpoint `/ws/events`.
- Eventos de partido y avisos de notificaciones nuevas.

## API

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Registro |
| POST | `/api/v1/auth/login` | Login |
| POST | `/api/v1/auth/logout` | Cierre de sesión |
| GET | `/api/v1/matches` | Partidos por estado, deporte y página |
| GET | `/api/v1/matches/{id}` | Detalle de partido |
| GET | `/api/v1/sports` | Resumen por deporte |
| GET | `/api/v1/bets` | Apuestas del usuario |
| POST | `/api/v1/bets` | Registrar apuesta |
| GET | `/api/v1/wallet` | Saldo, retenido y movimientos |
| GET | `/api/v1/notifications` | Notificaciones |
| POST | `/api/v1/notifications/read` | Marcar como leídas |
| GET | `/api/v1/me` | Datos del usuario |
| POST | `/api/v1/devices` | Registrar token móvil |
| POST | `/api/v1/payments/deposits` | Crear depósito |
| GET | `/api/v1/payments/intents/{id}` | Estado del depósito |
| POST | `/api/v1/payments/withdrawals` | Crear retiro |
| GET | `/api/v1/payments/withdrawals` | Retiros del usuario |
| POST | `/api/v1/webhooks/mercadopago` | Webhook de pagos |
| GET | `/api/v1/payments/return` | Página de retorno |
| GET | `/api/v1/admin/**` | Panel de administración |
| GET | `/api/v1/health` | Estado del servicio |

Los `POST` críticos exigen `Idempotency-Key`.

## Seguridad

- JWT HS256 con secreto de al menos 32 bytes.
- Contraseñas con BCrypt coste 12.
- Roles `USER` y `ADMIN`; `/api/v1/admin/**` protegido.
- Rate limit en login y registro.
- Token web en `sessionStorage`; Android en `EncryptedSharedPreferences`.
- Sin secretos en el repositorio: todo por variables de entorno.
- Validación en el borde (DTO) y en el dominio.

## Web

- Angular 22 standalone, sin `NgModule`.
- Signals para estado y `computed` para derivados.
- Tailwind CSS 4 y tokens propios en `frontend/src/styles/tokens.css`.
- Iconografía Lucide para la interfaz y Material Icons Outlined para los deportes.
- Temas claro y oscuro con selector persistente.
- Rutas: `inicio`, `deportes`, `en-vivo`, `mis-apuestas`, `resultados`, `cartera`, `perfil`, `notificaciones`, `admin`.
- Proxy de desarrollo en `frontend/proxy.conf.json`.

## Android

- Kotlin 2.1 y Jetpack Compose (BOM 2025.04).
- Una sola actividad, navegación por estado, sin `NgModule`.
- Retrofit 2.11, OkHttp 4.12 y kotlinx.serialization.
- Sesión con `EncryptedSharedPreferences`.
- WebSocket para eventos en vivo y notificaciones.
- Iconografía con `material-icons-extended`.
- `minSdk 26`, `compileSdk 35`.
- URL por `BuildConfig.API_BASE_URL`; en local `http://127.0.0.1:8080/` con `adb reverse`.

## Datos

Tablas principales: `app_user`, `wallet`, `movements`, `match`, `bet`, `ledger_entry`, `payment_intent`, `payment_event`, `withdrawal`, `notification`, `notification_read`, `device_token`, `promo_campaign`, `outbox_event`, `processed_event`.

El esquema lo gestiona Hibernate con `ddl-auto=update`.

## Ejecución local

Requisitos: Docker, Java 21, Node 24 y Android SDK para el móvil.

```bash
cp .env.example .env
docker compose up -d --build
```

Servicios:

| Servicio | URL |
|---|---|
| Web | http://localhost:4200 |
| API | http://localhost:8080/api/v1/health |
| PostgreSQL | localhost:5433 |
| Kafka | localhost:9092 |
| RabbitMQ | localhost:15672 |

Desarrollo sin contenedores de aplicación:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
cd frontend && npm ci && npm start
```

Android:

```bash
cd android
./gradlew :app:assembleDebug
adb reverse tcp:8080 tcp:8080
./gradlew :app:installDebug
```

## Pruebas

```bash
./backend/mvnw -f backend/pom.xml verify
cd frontend && npx ng test --watch=false
cd android && ./gradlew testDebugUnitTest
```

Cobertura actual: 209 pruebas de backend con JUnit 5, Mockito y AssertJ, 38 pruebas web con Vitest y pruebas unitarias de Android.

El backend no usa `@SpringBootTest` en la suite unitaria.

## Despliegue

El repositorio incluye lo necesario para un VPS con Docker y HTTPS:

```bash
git clone <repo> winup && cd winup
cp .env.example .env
docker compose --profile proxy up -d --build
```

- `frontend/nginx.conf` sirve la aplicación y proxya `/api` y `/ws`.
- `deploy/Caddyfile` termina TLS para `winup.site` y redirige `www`.
- `deploy/deploy.sh` actualiza, reconstruye y verifica el health.
- Solo se publican `80` y `443`; el resto queda en la red interna o en `127.0.0.1`.

## Estructura

```text
backend/        Spring Boot, dominio, pagos, ledger y mensajería
frontend/       Angular, cartera, apuestas y panel admin
android/        Kotlin y Compose
deploy/         Caddy, script de despliegue y RabbitMQ
docker-compose.yml
.env.example
```

La documentación de diseño, las specs y los ADR se mantienen fuera de este repositorio por decisión del equipo.

## Limitaciones conocidas

- Los partidos son simulados; no hay proveedor deportivo real.
- Los retiros usan el adaptador simulado por defecto. El adaptador de Mercado Pago Payouts requiere habilitar el producto y su webhook asíncrono.
- El límite diario de retiros es configurable y viene desactivado; el de depósitos todavía no se aplica.
- Mercado Pago arranca en sandbox; producción exige credenciales y webhook con secreto.
- El APK de release debe firmarse aparte y apuntar a la URL pública.

## Uso académico

Proyecto final de programación orientada a objetos. No es una casa de apuestas real ni ofrece asesoría de apuestas. El dinero mostrado en la demo no representa saldo real salvo que se configuren credenciales de producción.
