# FoodJet Android ↔ Backend interno Node.js/MySQL

> **Documento vigente.** La única API de negocio de FoodJet Android es la implementada en `backend/` de este repositorio. No se utiliza `Pagina-Foodjet`, Cloudflare ni el proxy Caddy de otro proyecto.

## Arquitectura

```text
app-movil/ (Kotlin, Jetpack Compose)
    UI → ViewModels → Repositories → Retrofit + OkHttp
                                   │
                                   ▼
                         backend/ (Express, :3000)
                                   │
                                   ▼
                         Prisma ORM → MySQL
```

La capa `data/local/` conserva DataStore (JWT, preferencias) y Room (caché de catálogo). Los datos y operaciones de negocio provienen de `backend/`. La conexión remota se utiliza por HTTP local durante el desarrollo.

## Únicos modos admitidos

| Modo | URL del API | Condición |
|---|---|---|
| Emulador Android | `http://10.0.2.2:3000/api/` | Express ejecutándose en el PC anfitrión |
| Celular físico USB | `http://localhost:3000/api/` | Ejecutar `adb reverse tcp:3000 tcp:3000` en el PC |

**Ambos modos utilizan el mismo backend del repositorio.** El selector «Conexión local» solo alterna cómo Android accede al PC; no se permiten servidores arbitrarios. Cambiar de modo no cierra la sesión. La migración de instalaciones anteriores elimina sesiones vinculadas a perfiles externos.

El cliente está implementado en:

- `app-movil/app/src/main/java/com/example/foodjeetapp/data/remote/ServidorFoodJet.kt`
- `app-movil/app/src/main/java/com/example/foodjeetapp/data/remote/RetrofitClient.kt`
- `app-movil/app/src/main/java/com/example/foodjeetapp/data/local/SessionDataStore.kt`

`RetrofitClient` rechaza hosts o puertos diferentes y no sigue redirecciones HTTP, para evitar comunicación accidental con API externas. Recursos de negocio como códigos QR se resuelven en el mismo backend interno.

## Levantar y probar el backend

Desde `backend/`:

```bash
npm ci
npx prisma generate
# Configurar .env a partir de .env.example, con MySQL local disponible
npx prisma db push
npm run seed
npm run dev
```

Para comprobar la respuesta de la API:

```bash
curl http://localhost:3000/api/products
```

Abre `app-movil/` en Android Studio y ejecuta la app en el emulador, o utiliza un teléfono USB con `adb reverse`. Consulta también [`PRUEBA_CON_BACKEND_LOCAL.md`](PRUEBA_CON_BACKEND_LOCAL.md) y [`backend/README.md`](../backend/README.md).

## Contratos conservados

La API de `backend/src/index.js` publica rutas `/api/auth`, `/api/products`, `/api/orders`, `/api/reviews`, `/api/favorites`, `/api/users`, `/api/addresses`, `/api/restaurants` y `/api/config`. Retrofit usa `FoodJetApiService` con los DTO ya existentes y autenticación JWT mediante `AuthInterceptor`.

Los mapas siguen siendo Google Maps JavaScript en WebView. Sus claves se consultan a `GET /api/config` del backend interno; Google Maps es un proveedor cartográfico, **no** un segundo backend de negocio. Para que la key funcione, deben estar autorizados los orígenes HTTP locales utilizados por el WebView, o se mostrará el mapa esquemático de respaldo.

## Validación y restricciones

- Ejecutar `./gradlew :app:assembleDebug` y `./gradlew :app:testDebugUnitTest` desde `app-movil/`.
- Verificar que las opciones de la interfaz sean exclusivamente Emulador y USB.
- Verificar que la API rechace solicitudes a hosts ajenos, puertos distintos de 3000 o rutas fuera de `/api/`.
- No reintroducir URL de producción como plantilla o fallback, ni operaciones de sincronización automática con otros repositorios.
- No modificar Gradle, DTO ni esquema SQL para realizar este aislamiento salvo que una prueba demuestre que es imprescindible.

**Seguridad:** `android:usesCleartextTraffic=true` permite tráfico HTTP para desarrollo local; no debe tratarse como una configuración de producción segura fuera del PC de pruebas.
