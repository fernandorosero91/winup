# Colección WINUP para Postman y Thunder Client

Prueba la API de producción en `https://winup.site` sin instalar nada más que Postman o la extensión Thunder Client de VS Code.

## Archivos

```text
postman/winup-produccion.postman_collection.json
postman/winup-produccion.postman_environment.json
```

La colección usa el formato Postman v2.1, que Thunder Client también importa.

## Importar en Postman

1. Abrir Postman.
2. `Import` → arrastrar los dos archivos JSON.
3. Seleccionar el environment **WINUP Producción** en la esquina superior derecha.
4. Editar `email` y `password` con una cuenta real. Si no existe, ejecutar **Registrar usuario** primero.
5. Ejecutar **Iniciar sesión**. El token se guarda solo en `token`.

## Importar en Thunder Client

1. Instalar la extensión **Thunder Client** en VS Code.
2. Abrir la pestaña Thunder Client → `Collections` → `Import`.
3. Seleccionar `winup-produccion.postman_collection.json`.
4. Ir a `Env` → `Import` y seleccionar `winup-produccion.postman_environment.json`.
5. Abrir el environment importado y elegir `Set Active`.
6. Editar `email` y `password`.

En Thunder Client, la versión gratuita no ejecuta los scripts de Postman. Después de **Iniciar sesión**, guardar el token manualmente:

1. Abrir la petición **Iniciar sesión** y pulsar `Send`.
2. Ir a la pestaña `Tests`.
3. En `Set Env Variable`, escribir a la izquierda `json.token` y a la derecha `{{token}}`.
4. Repetir con `json.user.id` y `{{userId}}`.

También se puede copiar el token de la respuesta y pegarlo en `Env` → `token`.

Todas las peticiones protegidas ya llevan el header `Authorization: Bearer {{token}}`. No hace falta configurar la pestaña `Auth` en cada una.

Con la versión de pago, el script de la colección funciona igual que en Postman.

## Variables principales

| Variable | Uso |
|---|---|
| `baseUrl` | `https://winup.site/api/v1` |
| `email` | correo de la cuenta de prueba |
| `password` | contraseña de 8 a 72 caracteres, con letra y número |
| `token` | JWT que se envía como Bearer |
| `matchId` | partido sobre el que se apuesta |
| `betId`, `depositId`, `withdrawalId`, `promoId` | ids de las operaciones creadas |
| `idempotencyBet`, `idempotencyDeposit`, `idempotencyWithdrawal` | claves de idempotencia |

El token dura 2 horas. Si responde 401, volver a ejecutar **Iniciar sesión**.

## Flujo recomendado

1. **Health**: confirmar que el backend responde.
2. **Registrar usuario** o **Iniciar sesión**.
3. **Listar partidos en vivo**: en Postman guarda el primer partido OPEN en `matchId`.
4. **Crear apuesta**: usa `matchId`, selección `LOCAL` y 1.000 COP.
5. **Cartera** y **Movimientos**: comprobar el descuento del saldo.
6. **Crear depósito**: la respuesta trae `checkoutUrl` de MercadoPago. Abrirla en el navegador y pagar con una tarjeta de prueba si el sandbox está activo.
7. **Mis apuestas**: ver el estado de la apuesta.
8. **Notificaciones**: revisar la bandeja.
9. **Administración**: solo con una cuenta ADMIN.

En Postman, el script de la colección guarda automáticamente `token`, `userId`, `matchId`, `betId`, `depositId`, `checkoutUrl`, `withdrawalId`, `notificationId` y `promoId` cuando la respuesta es correcta.

## Reglas de la API

- Apuesta: entre 500 y 500.000 COP.
- Depósito y retiro: entre 5.000 y 500.000 COP.
- Cuotas: entre 1.01 y 100.00.
- `selection`: `LOCAL`, `EMPATE` o `VISITANTE`. `EMPATE` solo en deportes con empate.
- El partido debe estar `OPEN` para aceptar apuestas.
- Retiros superiores a 200.000 COP quedan pendientes de aprobación.
- Login y registro: máximo 10 intentos por minuto y por IP. Si responde 429, esperar 60 segundos.
- Las promociones se envían a todos los usuarios. Úselas solo con autorización.

## Errores frecuentes

| Código | Causa |
|---|---|
| 401 | Token ausente, vencido o inválido |
| 403 | La cuenta no es ADMIN o no es dueña del recurso |
| 404 | El id no existe |
| 409 | Partido cerrado, saldo insuficiente o retiro no cancelable |
| 422 | Validación: importe, cuota, contraseña o selección |
| 429 | Demasiados intentos de login o registro |

## Solución de problemas

### La petición de partidos responde 401

Revisar la petición enviada: debe incluir la cabecera `Authorization: Bearer ...`. Si no aparece, la variable `token` está vacía o el environment no está activo.

1. Ejecutar **Iniciar sesión** y revisar que la respuesta sea 200.
2. Copiar el valor de `token` de la respuesta, sin comillas.
3. Pegarlo en el environment, variable `token`.
4. Confirmar que el environment **WINUP Producción** está en `Set Active`.
5. Volver a enviar la petición.

### Aparece "Login Success" y luego error

Ese mensaje suele ser el inicio de sesión de la cuenta de Thunder Client o de su sincronización en la nube. No tiene relación con WINUP. El login de la aplicación es la petición **Iniciar sesión** de la colección.

### El login responde 401

El correo o la contraseña no son correctos para `https://winup.site`. Verificar las variables `email` y `password`. Si la cuenta se creó con otro correo, usar ese.

### El login responde 429

Se superaron 10 intentos en un minuto desde la misma IP. Esperar 60 segundos.

## Notas

- No guardar tokens reales en Git. El environment incluido tiene valores de ejemplo.
- El WebSocket `/ws/events` no se prueba desde Postman ni Thunder Client. Para eso usar `wscat` o la web.
- La colección apunta a producción. Registrar usuarios, apostar o enviar promociones tiene efecto real en la base de datos.
- Para Thunder Client, si una petición falla por `{{matchId}}` vacío, ejecutar primero **Listar partidos en vivo** y copiar un `id` de la respuesta.
