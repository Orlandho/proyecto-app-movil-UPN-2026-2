# 🚀 Integración con el Backend FoodJet (Node.js / PostgreSQL)

Para consultar la guía técnica completa de arquitectura, configuración de red, permisos de tráfico sin cifrar, dependencias de Retrofit, DTOs y reemplazo de datos simulados en repositorios según los requerimientos de [AGENTS.md](AGENTS.md), revisa:

👉 **[docs/INTEGRACION_BACKEND_FOODJET.md](docs/INTEGRACION_BACKEND_FOODJET.md)**

### Resumen de los 4 Puntos Clave de Trabajo:
1. **Networking y Base URL:** Usar `http://10.0.2.2:3000/api/` en el emulador de Android Studio para comunicarse con el servidor Node.js en la máquina física.
2. **Seguridad y Cleartext Traffic:** Habilitar `android:usesCleartextTraffic="true"` o `network_security_config.xml` en `AndroidManifest.xml` para permitir peticiones HTTP locales.
3. **Mapeo de Campos JSON:** Configurar `@SerializedName` en los DTOs para transformar los campos `snake_case` del backend (`restaurante_id`, `tipo_comida`, `imagen_url`, `descuento_estudiante`) a los modelos Kotlin `camelCase` en `FoodJetModels.kt`.
4. **Repositorios Reales (Cero Mocks):** Sustituir los datos simulados por llamadas suspendidas de Retrofit 2 (`Dispatchers.IO`), inyección de cabecera `Authorization: Bearer <token>` mediante un `Interceptor` de OkHttp conectado a `DataStore`, y almacenamiento local en `Room`.

*Diagrama de Arquitectura:* [`mermaid diagramas/06_integracion_backend_foodjet_api.svg`](mermaid%20diagramas/06_integracion_backend_foodjet_api.svg)
