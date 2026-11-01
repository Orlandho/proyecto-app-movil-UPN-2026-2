# Pruebas: app móvil + backend MySQL (pagos y seguimiento)

Prueba de la app de `app-movil/` contra el backend de `backend/` (Node.js +
Express + Prisma + MySQL 8), con el servidor **"Backend MySQL · Emulador
(3000)"** de la app. Fecha: 1 de noviembre de 2026.

## Resultado

| Qué se probó | Resultado |
|---|---|
| Backend arranca con MySQL, migración y seed | ✅ tras corregir la migración (ver abajo) |
| Login desde la app (`cliente1@mail.com`) | ✅ |
| Compra con **efectivo** | ✅ pedido `pendiente`, pago `pendiente`; al entregar, pago `completado` (corregido) |
| Compra con **tarjeta** | ✅ pedido `confirmado`, pago `completado`; ya no se acepta con datos vacíos (corregido) |
| Compra con **Yape/Plin** | ✅ QR del local, pedido `confirmado`, pago `completado`; el QR ya no queda en blanco la primera vez (corregido) |
| Método de pago no soportado (`bitcoin`) | ✅ 400 |
| Cupón FOODJET20 | ✅ el backend recalcula: S/ 25.06 → S/ 21.05 |
| Dirección de otro usuario | ✅ 403 |
| Simulación del seguimiento | ✅ pendiente → confirmado → en preparación → en camino (ruta OSRM de 278 puntos, avance y metros restantes) → entregado, en unos 3 min con `SIM_ESCALA_TIEMPO=10` |
| Totales app = base de datos | ✅ p. ej. S/ 23.05 (2 × 7.65 + IGV 18 % + envío 5) |
| Pruebas del backend (`npm test`) | ✅ 50 escenarios |
| Pruebas de la app (`gradlew testDebugUnitTest`) | ✅ 354 pruebas |

## Fallos encontrados y corregidos en esta rama

1. **La migración de MySQL no se aplicaba.** `prisma/migrations/20261022000000_init_mysql/migration.sql`
   empezaba con un BOM de UTF-8 y MySQL respondía `ERROR 1064` en la primera
   línea. Con el `Dockerfile` (`prisma migrate deploy`) el backend no
   arrancaba. Se quitó el BOM.
2. **El pago en efectivo se quedaba `pendiente` para siempre**, aun con el
   pedido entregado. Ahora `services/estadoPedido.js` lo marca `completado`
   al entregar, con la regla pura `estadoPagoTrasCambio` de
   `domain/payment.js` y el escenario PAG-003 en `features/pago.feature`.
3. **`npm test` del backend no funcionaba**: apuntaba a un `package.json` en
   la raíz del repositorio que no existe. Ahora corre Cucumber desde
   `backend/` (con `prisma generate` antes).
4. **`.env.example` no traía `SIMULAR_PEDIDOS=true`**, y sin esa variable los
   pedidos no avanzan y el seguimiento se queda en el primer estado. Se
   añadieron las variables de la simulación.
5. **Tarjeta sin validar en la app**: se podía confirmar con número, fecha y
   CVC vacíos, y el pedido entraba como pagado. Ahora se exige número válido
   (Luhn), fecha MM/AA no vencida y CVC de 3 o 4 dígitos
   (`data/model/ValidacionTarjeta.kt`, con 6 pruebas). Los datos de la
   tarjeta nunca se envían al backend; el cobro sigue siendo simulado.
6. **QR de Yape en blanco la primera vez**: el primer WebView de la app
   tarda en arrancar y la cuenta de 5 s terminaba antes de verse el QR.
   Ahora la cuenta empieza cuando el QR cargó (con un tope de 10 s).

## Cómo repetir la prueba con Docker

No hace falta instalar MySQL ni Node: basta Docker Desktop.

```bash
docker network create foodjet-movil-net
docker run -d --name foodjet-mysql --network foodjet-movil-net -e MYSQL_ROOT_PASSWORD=foodjet_root -e MYSQL_DATABASE=foodjet -p 127.0.0.1:3307:3306 mysql:8.0
cd backend
docker build -t foodjet-backend-movil .
docker run -d --name foodjet-backend-movil --network foodjet-movil-net -p 127.0.0.1:3000:3000 -e DATABASE_URL="mysql://root:foodjet_root@foodjet-mysql:3306/foodjet" -e JWT_SECRET=cambia-esto -e RUN_SEED=true -e SIMULAR_PEDIDOS=true -e SIM_ESCALA_TIEMPO=10 foodjet-backend-movil
curl http://localhost:3000/api/products
```

Espera unos 20 s a que MySQL arranque antes de lanzar el backend. En la app,
el servidor predeterminado ya es **Backend MySQL · Emulador (3000)**
(`http://10.0.2.2:3000/api/`). Usuarios: `cliente1@mail.com` /
`Cliente123!`, `cliente2@mail.com` / `Cliente123!`, `admin@foodjet.com` /
`Admin123!`. Para la tarjeta usa un número de prueba público como
`4242 4242 4242 4242`, fecha futura y CVC `123`.

Para apagar todo:

```bash
docker rm -f foodjet-backend-movil foodjet-mysql
docker network rm foodjet-movil-net
```

## Observaciones sin corregir

- La key de Google Maps no estaba configurada, así que el mapa se vio como
  esquema de respaldo (con la ruta real). Con `GOOGLE_MAPS_API_KEY` en el
  entorno del backend se mostraría Google Maps.
- Los pedidos en efectivo entregados **antes** de esta corrección conservan
  el pago `pendiente`; la corrección rige para las entregas nuevas.
