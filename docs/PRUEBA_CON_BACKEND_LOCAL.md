# Probar la app con el backend de FoodJet

La app no tiene datos ni backend simulados: todo lo que muestra (catálogo,
pedidos, direcciones, seguimiento, panel admin) sale de la API de
[`Pagina-Foodjet`](https://github.com/Orlandho/Pagina-Foodjet). Puede trabajar
contra dos backends, y se elige dentro de la app:

| Servidor (en la app) | URL de la API | Cuándo usarlo |
|---|---|---|
| **Producción** (predeterminado) | `https://foodjet.asen.pe/api/` | Probar sin instalar nada. Solo funciona mientras el túnel de Cloudflare esté encendido. |
| **Local · emulador** | `http://10.0.2.2:8081/api/` | Backend en Docker en tu PC y la app en el emulador de Android Studio. |
| **Local · celular por USB** | `http://localhost:8081/api/` | Backend en Docker en tu PC y la app en un celular conectado por USB. |

Para cambiarlo: icono de cuenta → **Servidor** → elige uno. Cambiar de servidor
cierra la sesión, porque el token de un servidor no vale en otro.

Se usa siempre el puerto **8081** (el proxy Caddy), no el 3000: el proxy sirve
la API y también los archivos de la web que la app necesita, como los QR de
pago de cada local.

## 1. Levantar el backend en tu PC

Requisito: **Docker Desktop** para Windows
(<https://www.docker.com/products/docker-desktop/>). Al instalarlo te pide
activar WSL 2 y reiniciar; después abre Docker Desktop y espera a que diga
*Engine running*.

En una terminal, dentro de la carpeta de `Pagina-Foodjet`:

```bash
cp .env.example .env
docker compose build
docker compose up -d
```

La primera vez tarda unos minutos. El backend aplica las migraciones y siembra
los datos de demostración solo. Comprueba que responde:

```bash
curl http://localhost:8081/api/products
```

Debe devolver la lista de productos en JSON. La web queda en
<http://localhost:8081>.

Usuarios de prueba (los siembra el backend, ver el `AGENTS.md` de
Pagina-Foodjet): `cliente1@mail.com` / `Cliente123!` (estudiante),
`cliente2@mail.com` / `Cliente123!` y `admin@foodjet.com` / `Admin123!` (abre
el Panel de Operaciones). Cupones: `PruebaCupon` (10 %) y `FOODJET20` (20 %).

## 2. Conectar la app

**Emulador:** abre la app, toca el icono de cuenta, en **Servidor** elige
**Local · emulador** e inicia sesión. El emulador llega a tu PC por
`10.0.2.2`.

**Celular por USB:** con la depuración USB activada y el celular conectado,
ejecuta en tu PC:

```bash
adb reverse tcp:8081 tcp:8081
```

y en la app elige **Local · celular por USB**. Hay que repetir el
`adb reverse` cada vez que se desconecta el cable.

## 3. Qué probar

1. Inicia sesión como `cliente1@mail.com`, añade productos al carrito y ve al
   checkout.
2. Elige una dirección guardada, usa **Usar mi ubicación** (el emulador permite
   fijar la posición en *Extended controls → Location*) o mueve el mapa hasta
   tu puerta.
3. Confirma el pedido. El seguimiento muestra el local, tu dirección, la ruta y
   la hora de cada etapa. Con `SIMULAR_PEDIDOS=true` (lo normal) el pedido
   avanza solo y el repartidor recorre la ruta en unos 3 minutos.
4. Entra como `admin@foodjet.com` → Mi cuenta → Panel de Operaciones para ver
   y avanzar pedidos. Las cifras del panel salen de los pedidos reales.

## Mapa

El mapa usa la misma key de Google Maps que la web, que el backend entrega en
`GET /api/config` (`GOOGLE_MAPS_API_KEY` en el `.env` de Pagina-Foodjet). Esa
key está restringida a `https://foodjet.asen.pe/*` y `http://localhost:8081/*`,
y la app carga el mapa con uno de esos orígenes según el servidor elegido. Si
el `.env` local no tiene key, o Google la rechaza, la app muestra un esquema de
la ruta sin calles de fondo y todo lo demás sigue funcionando.

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| "No se pudo conectar con Local · emulador" | Docker no está levantado: `docker compose ps` en Pagina-Foodjet. |
| "Tu sesión expiró" al abrir la app | El token caducó (24 h) o era de otro servidor. Vuelve a iniciar sesión. |
| En el celular no conecta a local | Falta `adb reverse tcp:8081 tcp:8081`, o elegiste "Local · emulador" en vez de "por USB". |
| El mapa sale como esquema | El `.env` no tiene `GOOGLE_MAPS_API_KEY`, o la key no autoriza `http://localhost:8081/*`. |
