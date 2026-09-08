# GEMINI.md - Requerimientos Técnicos Semanales del Sistema Móvil

## Misión de la Inteligencia Artificial
La inteligencia artificial actuará como desarrollador móvil senior encargado de codificar, refactorizar y verificar cada componente técnico de la aplicación. Su responsabilidad consiste en cumplir rigurosamente los siguientes requerimientos técnicos organizados cronológicamente por semanas y estructurados en tres categorías fundamentales: Capa Visual y Experiencia de Usuario, Lógica de Negocio y Servicios de Arquitectura, y Persistencia, Infraestructura y Configuración. Debe marcar con una equis cada casilla completada de forma verificable en el código fuente.

---

## Semana 1: Ecosistema Android y Configuración Base

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM01-VIS-01: Diseñar los primeros esquemas de interfaz para pantallas móviles considerando principios de usabilidad y diseño limpio.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM01-LOG-01: Establecer las directrices técnicas de optimización en el uso de CPU y memoria desde la concepción del software para garantizar un consumo responsable de recursos según el Objetivo de Desarrollo Sostenible 12.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM01-INF-01: Configurar el proyecto inicial en Android Studio con soporte para Kotlin moderno y compatibilidad con dispositivos móviles Android.
- [x] REQ-SEM01-INF-02: Configurar Gradle Kotlin DSL y estructurar el árbol de paquetes respetando la separación de responsabilidades del sistema.

---

## Semana 2: Entorno de Desarrollo y Fundamentos Declarativos

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM02-VIS-01: Crear los primeros componentes composables con Jetpack Compose verificando su correcta renderización visual y previsualización.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM02-LOG-01: Implementar el punto de entrada de la interfaz mediante ComponentActivity y la invocación del método setContent.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM02-INF-01: Configurar el catálogo de versiones centralizado mediante el archivo libs.versions.toml para dependencias y complementos.
- [x] REQ-SEM02-INF-02: Habilitar las banderas de compilación para Jetpack Compose en el archivo build.gradle.kts del módulo principal.

---

## Semana 3: Interfaz de Usuario con Material Design 3

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM03-VIS-01: Implementar el sistema de diseño Material Design 3 configurando esquemas de color dinámicos para modo claro y modo oscuro.
- [x] REQ-SEM03-VIS-02: Diseñar la tipografía, formas y componentes visuales modulares como tarjetas, botones elevados y barras de navegación superior e inferior.
- [x] REQ-SEM03-VIS-03: Implementar Compose Navigation mediante NavHost y composables para la navegación modular entre pantallas con paso de argumentos.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM03-LOG-01: Gestionar estados inmutables dentro de la interfaz gráfica mediante remember y mutableStateOf para evitar recomposiciones innecesarias.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM03-INF-01: Configurar temas y estilos centralizados en el paquete de diseño de la aplicación para reutilización global en la interfaz.

---

## Semana 4: Ciclo de Vida, Intents y Validaciones

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM04-VIS-01: Diseñar interfaces de formularios con validación visual reactiva mostrando mensajes de error dinámicos bajo campos de texto y control de habilitación de botones.
- [x] REQ-SEM04-VIS-02: Aplicar directrices técnicas de accesibilidad móvil asegurando descripciones de contenido para lectores de pantalla y áreas táctiles mínimas reglamentarias.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM04-LOG-01: Administrar adecuadamente los eventos del ciclo de vida de componentes y actividades de Android para prevenir fugas de memoria.
- [ ] REQ-SEM04-LOG-02: Implementar Intents explícitos para navegación interna e Intents implícitos para interactuar con aplicaciones del sistema como cámara o navegador web.
- [x] REQ-SEM04-LOG-03: Implementar validación lógica reactiva evaluando campos obligatorios, estructuras de correo electrónico, números y límites de caracteres.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM04-INF-01: Declarar permisos básicos en el archivo AndroidManifest.xml y gestionar solicitudes dinámicas de permisos en tiempo de ejecución.
- [ ] REQ-SEM04-INF-02: Implementar medidas de eficiencia energética reduciendo la ejecución de tareas redundantes en segundo plano.

---

## Semana 5: Persistencia Local con DataStore y Room

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM05-VIS-01: Construir pantallas de listado y formularios de captura vinculados a la visualización de datos almacenados localmente.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM05-LOG-01: Implementar interfaces Data Access Object con métodos para inserción, consulta, actualización y eliminación de registros.
- [x] REQ-SEM05-LOG-02: Exponer consultas reactivas desde el Data Access Object utilizando flujos observables mediante el tipo Flow de corrutinas de Kotlin.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM05-INF-01: Implementar Jetpack DataStore Preferences para la persistencia asíncrona de configuraciones y preferencias del usuario.
- [x] REQ-SEM05-INF-02: Implementar la base de datos relacional local con Room ORM sobre el motor SQLite con entidades, llaves primarias autogeneradas e índices.
- [x] REQ-SEM05-INF-03: Configurar migraciones de base de datos automáticas y controladas para prevenir la pérdida de datos ante cambios en las tablas.

---

## Semana 6: Arquitectura MVVM y Evaluación T1

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM06-VIS-01: Vincular las pantallas de Jetpack Compose al estado inmutable expuesto por el ViewModel mediante collectAsStateWithLifecycle.
- [x] REQ-SEM06-VIS-02: Renderizar de forma condicional vistas de carga, vistas de contenido, vistas de error con botón de reintento y vistas para datos vacíos.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM06-LOG-01: Implementar formalmente el patrón arquitectónico Model View ViewModel separando la interfaz de la lógica de negocio y del acceso a datos.
- [x] REQ-SEM06-LOG-02: Implementar clases ViewModel que extiendan de Android ViewModel para retener estado ante cambios de configuración del dispositivo como giros de pantalla.
- [x] REQ-SEM06-LOG-03: Implementar el patrón Repository como mediador y única fuente de verdad entre la capa de datos local y la interfaz.
- [x] REQ-SEM06-LOG-04: Modelar el estado de la pantalla mediante clases selladas de Kotlin y exponerlo usando StateFlow y SharedFlow para eventos únicos.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM06-INF-01: Ejecutar pruebas técnicas de verificación sobre la persistencia local en Room y la estabilidad del flujo MVVM para el hito evaluativo T1.

---

## Semana 7: Fundamentos de Servicios Web y Contratos REST

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM07-VIS-01: Diseñar componentes visuales preparados para el consumo y despliegue de catálogos de datos remotos.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM07-LOG-01: Diseñar el contrato de la API REST especificando rutas de endpoints, métodos HTTP, cabeceras y estructuras de carga útil.
- [x] REQ-SEM07-LOG-02: Definir clases de datos inmutables fuertemente tipadas para serialización y deserialización sin pérdida de precisión.
- [x] REQ-SEM07-LOG-03: Aplicar políticas de consumo responsable de datos de red reduciendo campos innecesarios en las peticiones.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM07-INF-01: Configurar biblioteca de serialización JSON como Kotlinx Serialization o Gson en el entorno de compilación.

---

## Semana 8: Consumo de APIs REST con Retrofit y Corrutinas

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM08-VIS-01: Diseñar componentes visuales de retroalimentación de red con indicadores de progreso, barras de estado y avisos de pérdida de conectividad.

### Lógica de Negocio y Servicios de Arquitectura
- [x] REQ-SEM08-LOG-01: Integrar el cliente HTTP Retrofit versión 2 configurado con el motor OkHttp para llamadas a servicios web.
- [x] REQ-SEM08-LOG-02: Diseñar la interfaz de servicio de Retrofit con métodos GET, POST, PUT y DELETE vinculados a funciones de suspensión de corrutinas.
- [x] REQ-SEM08-LOG-03: Ejecutar llamadas de red en segundo plano utilizando el despachador de entrada y salida Dispatchers.IO.
- [x] REQ-SEM08-LOG-04: Implementar manejo estructurado de respuestas capturando códigos de éxito doscientos, errores de cliente cuatrocientos y errores de servidor quinientos.

### Persistencia, Infraestructura y Configuración
- [x] REQ-SEM08-INF-01: Implementar interceptores de OkHttp para registro controlado de peticiones, inyección de encabezados de autorización y tiempos de espera.
- [x] REQ-SEM08-INF-02: Diseñar mecanismo de almacenamiento en caché local combinando Room y Retrofit para permitir lectura de datos esenciales sin conexión a internet.

---

## Semana 9: Integración de la Suite Firebase

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM09-VIS-01: Construir pantallas de autenticación para inicio de sesión, registro de nuevos usuarios y recuperación de credenciales.
- [x] REQ-SEM09-VIS-02: Diseñar selectores visuales de archivos e imágenes con barras de progreso de carga y descarga en la interfaz.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM09-LOG-01: Implementar Firebase Authentication para registro de cuentas, validación segura de accesos y persistencia de tokens de sesión.
- [ ] REQ-SEM09-LOG-02: Implementar Cloud Firestore para base de datos documental en tiempo real con sincronización bidireccional de colecciones y documentos.
- [ ] REQ-SEM09-LOG-03: Implementar Firebase Storage para la carga, resguardo y descarga segura de archivos binarios e imágenes del proyecto.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM09-INF-01: Configurar el archivo google-services.json y los complementos de compilación de Firebase en el proyecto.
- [ ] REQ-SEM09-INF-02: Definir reglas de seguridad en Firebase para autenticación previa y restricción de lectura y escritura por identificador de usuario.

---

## Semana 10: Geolocalización y Mapas con Evaluación T2

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM10-VIS-01: Renderizar mapas interactivos de Google Maps con marcadores personalizados, áreas circulares y ventanas de información en Compose.
- [ ] REQ-SEM10-VIS-02: Diseñar controles táctiles sobre el mapa para centrado de cámara, acercamiento y visualización de rutas sobre el terreno.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM10-LOG-01: Implementar el cliente FusedLocationProviderClient para obtener coordenadas de latitud y longitud del dispositivo en tiempo real con precisión equilibrada.
- [ ] REQ-SEM10-LOG-02: Solicitar y validar dinámicamente en tiempo de ejecución los permisos ACCESS_FINE_LOCATION y ACCESS_COARSE_LOCATION con explicaciones contextuales.
- [ ] REQ-SEM10-LOG-03: Implementar algoritmos de cálculo de distancias geográficas y trazado de trayectorias entre múltiples puntos.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM10-INF-01: Configurar la clave de acceso de Google Maps SDK en local.properties y enlazarla de manera segura a través de BuildConfig.
- [ ] REQ-SEM10-INF-02: Consolidar la integración técnica de servicios web REST, Firebase y geolocalización para la entrega del hito evaluativo T2.

---

## Semana 11: Integración de Módulos, Pruebas y Transición a Flutter

### Capa Visual y Experiencia de Usuario
- [x] REQ-SEM11-VIS-01: Integrar todas las vistas de la aplicación en una experiencia unificada con navegación fluida y sin bloqueos de la interfaz.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM11-LOG-01: Unificar la interacción entre almacenamiento Room, cliente Retrofit, base documental Firestore y geolocalización.
- [ ] REQ-SEM11-LOG-02: Prevenir fugas de memoria cancelando corrutinas activas y desuscribiendo flujos cuando los componentes visuales se destruyan.
- [ ] REQ-SEM11-LOG-03: Implementar políticas de compresión de imágenes antes de su transferencia hacia la red para reducir el consumo de datos y batería.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM11-INF-01: Implementar registro controlado de errores y excepciones sin registrar información sensible del usuario.
- [ ] REQ-SEM11-INF-02: Configurar el entorno base del proyecto Flutter con lenguaje Dart y modo de seguridad de nulos estricto.

---

## Semana 12: Desarrollo Multiplataforma con Widgets de Flutter

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM12-VIS-01: Construir interfaces declarativas modulares en Flutter combinando adecuadamente StatelessWidget y StatefulWidget.
- [ ] REQ-SEM12-VIS-02: Aplicar componentes y temas de Material Design en Flutter coherentes con los estilos definidos en la versión nativa.
- [ ] REQ-SEM12-VIS-03: Diseñar componentes personalizados reutilizables respetando el árbol de widgets y optimizando el redibujado de la pantalla.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM12-LOG-01: Implementar el sistema de navegación declarativa entre pantallas pasando parámetros fuertemente tipados en Flutter.
- [ ] REQ-SEM12-LOG-02: Administrar el ciclo de vida de los widgets mediante los métodos initState, didUpdateWidget y dispose.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM12-INF-01: Configurar el archivo pubspec.yaml gestionando dependencias y activos visuales para el entorno Flutter.

---

## Semana 13: Persistencia y Servicios Web en Flutter y Evaluación T3

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM13-VIS-01: Implementar vistas asíncronas con widgets FutureBuilder o StreamBuilder para reflejar estados de carga, datos obtenidos o errores de conexión.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM13-LOG-01: Consumir servicios web REST externos utilizando el paquete http de Dart decodificando respuestas JSON y gestionando excepciones.
- [ ] REQ-SEM13-LOG-02: Implementar la lógica de manipulación de registros locales con métodos de inserción, actualización, consulta y borrado en Dart.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM13-INF-01: Implementar persistencia de datos local en Flutter utilizando la biblioteca sqflite con sentencias relacionales estructuradas sobre SQLite.
- [ ] REQ-SEM13-INF-02: Preparar la evidencia técnica del producto funcional integrado al cien por ciento para la entrega de práctica de campo y evaluación T3.

---

## Semana 14: Preparación para Distribución y Empaquetado

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM14-VIS-01: Configurar el icono adaptable de la aplicación con capas independientes de fondo y primer plano cumpliendo normativas de diseño de tiendas.
- [ ] REQ-SEM14-VIS-02: Preparar capturas de pantalla de la interfaz gráfica optimizadas para teléfonos y tabletas requeridas por la plataforma de distribución.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM14-LOG-01: Configurar el esquema de versionamiento semántico con versionCode incremental y versionName descriptivo en los archivos de compilación.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM14-INF-01: Generar y custodiar el almacén de llaves criptográficas keystore para la firma digital de la versión de producción.
- [ ] REQ-SEM14-INF-02: Configurar el bloque signingConfigs en Gradle para aplicar la firma digital automatizada en la variante de compilación release.
- [ ] REQ-SEM14-INF-03: Habilitar optimización de código, reducción de recursos y ofuscación mediante R8 y reglas de ProGuard.
- [ ] REQ-SEM14-INF-04: Generar el archivo binario Android App Bundle con extensión aab mediante la tarea bundleRelease de Gradle.

---

## Semana 15: Publicación Controlada y Monitoreo

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM15-VIS-01: Diseñar pantallas y cuadros de diálogo de consentimiento informado sobre términos de uso y políticas de privacidad dentro de la aplicación.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM15-LOG-01: Implementar mecanismos de telemetría y captura de registros de fallos para la mejora continua del sistema en producción.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM15-INF-01: Configurar la consola Google Play Console estructurando la ficha de tienda y cumpliendo los cuestionarios de contenido y privacidad.
- [ ] REQ-SEM15-INF-02: Crear el lanzamiento en la pista de pruebas internas o cerradas con usuarios evaluadores designados.
- [ ] REQ-SEM15-INF-03: Redactar notas de versión técnicas y descriptivas que acompañen el despliegue del paquete aab.

---

## Semana 16: Evaluación Final y Sustentación Técnica

### Capa Visual y Experiencia de Usuario
- [ ] REQ-SEM16-VIS-01: Verificar la fluidez de las animaciones, transiciones de pantalla y respuesta táctil en tiempo real en dispositivos físicos.

### Lógica de Negocio y Servicios de Arquitectura
- [ ] REQ-SEM16-LOG-01: Ejecutar una auditoría exhaustiva del código fuente verificando limpieza, tipado estricto, resiliencia ante errores y cumplimiento de patrones de diseño.
- [ ] REQ-SEM16-LOG-02: Demostrar en vivo el funcionamiento integrado de persistencia local, consumo de servicios web REST, Firebase, geolocalización y módulos Flutter.

### Persistencia, Infraestructura y Configuración
- [ ] REQ-SEM16-INF-01: Confirmar el cumplimiento del cien por ciento de las casillas técnicas del presente documento para la defensa y sustentación de la Evaluación Final.
