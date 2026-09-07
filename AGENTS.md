# GEMINI.md - Requerimientos Técnicos Semanales del Sistema Móvil

## Misión de la Inteligencia Artificial
La inteligencia artificial actuará como desarrollador móvil senior encargado de codificar, refactorizar y verificar cada componente técnico de la aplicación. Su responsabilidad consiste en cumplir rigurosamente los siguientes requerimientos técnicos organizados cronológicamente a lo largo de las dieciséis semanas del ciclo académico. Debe marcar con una equis cada casilla completada de forma verificable en el código fuente.

---

## Semana 1: Ecosistema Android y Configuración Base

- [ ] REQ-SEM01-01: Configurar el proyecto en Android Studio con Kotlin moderno y compatibilidad para compilación en dispositivos móviles Android.
- [ ] REQ-SEM01-02: Configurar Gradle Kotlin DSL y estructurar el árbol de paquetes respetando separación de responsabilidades.
- [ ] REQ-SEM01-03: Implementar políticas técnicas de optimización en el uso de CPU y memoria desde la concepción del software para cumplir con el consumo responsable de recursos según el Objetivo de Desarrollo Sostenible 12.

---

## Semana 2: Entorno de Desarrollo y Fundamentos Declarativos

- [ ] REQ-SEM02-01: Configurar el catálogo centralizado de dependencias y versiones mediante el archivo libs.versions.toml.
- [ ] REQ-SEM02-02: Habilitar Jetpack Compose en el archivo de compilación del módulo de la aplicación.
- [ ] REQ-SEM02-03: Implementar la función de entrada principal mediante ComponentActivity y la llamada al método setContent.
- [ ] REQ-SEM02-04: Crear los primeros componentes composables verificando su correcta renderización sin errores de compilación.

---

## Semana 3: Interfaz de Usuario con Material Design 3

- [ ] REQ-SEM03-01: Implementar el sistema de diseño Material Design 3 con esquemas de colores para modo claro y modo oscuro.
- [ ] REQ-SEM03-02: Diseñar la tipografía y formas estandarizadas aplicadas uniformemente en toda la interfaz gráfica.
- [ ] REQ-SEM03-03: Gestionar estados inmutables dentro de la interfaz mediante remember y mutableStateOf para evitar recomposiciones innecesarias.
- [ ] REQ-SEM03-04: Implementar Compose Navigation mediante NavHost y composables para la navegación modular entre pantallas con paso de argumentos.
- [ ] REQ-SEM03-05: Construir componentes modulares reutilizables como tarjetas, botones elevados, barras de navegación y campos de texto estilizados.

---

## Semana 4: Ciclo de Vida, Intents y Validaciones

- [ ] REQ-SEM04-01: Administrar adecuadamente los eventos del ciclo de vida de componentes y actividades de Android previniendo fugas de memoria.
- [ ] REQ-SEM04-02: Implementar Intents explícitos para navegación interna e Intents implícitos para invocar servicios del sistema como marcado telefónico, cámara o navegador web.
- [ ] REQ-SEM04-03: Solicitar y comprobar permisos básicos en tiempo de ejecución con flujos de solicitud reglamentarios.
- [ ] REQ-SEM04-04: Implementar validación reactiva de formularios evaluando en tiempo real campos obligatorios, formatos de correo electrónico, números y longitudes de texto.
- [ ] REQ-SEM04-05: Aplicar directrices técnicas de accesibilidad móvil asegurando descripciones de contenido para lectores de pantalla y áreas táctiles mínimas reglamentarias.

---

## Semana 5: Persistencia Local con DataStore y Room

- [ ] REQ-SEM05-01: Implementar Jetpack DataStore Preferences para almacenar configuraciones del sistema y preferencias de usuario de forma asíncrona.
- [ ] REQ-SEM05-02: Implementar la base de datos relacional local con Room ORM sobre el motor SQLite.
- [ ] REQ-SEM05-03: Definir entidades de base de datos con anotaciones de Room, identificadores autogenerados, nombres de columnas explícitos e índices.
- [ ] REQ-SEM05-04: Implementar interfaces Data Access Object con operaciones de creación, lectura, actualización y eliminación de registros.
- [ ] REQ-SEM05-05: Exponer consultas reactivas desde el Data Access Object utilizando flujos observables mediante el tipo Flow de corrutinas.
- [ ] REQ-SEM05-06: Configurar migraciones de base de datos seguras para evitar la pérdida de información ante cambios de esquema.

---

## Semana 6: Arquitectura MVVM y Evaluación T1

- [ ] REQ-SEM06-01: Implementar formalmente el patrón arquitectónico Model View ViewModel separando la interfaz de la lógica de presentación y del modelo de datos.
- [ ] REQ-SEM06-02: Implementar clases ViewModel que hereden de Android ViewModel para retener estado ante cambios de configuración del dispositivo como giros de pantalla.
- [ ] REQ-SEM06-03: Implementar el patrón Repository como única fuente de verdad para mediar entre fuentes de datos locales y remotas.
- [ ] REQ-SEM06-04: Modelar el estado de la interfaz mediante clases selladas o interfaces selladas representando estados de carga, éxito, error y datos vacíos.
- [ ] REQ-SEM06-05: Exponer el estado de la interfaz utilizando StateFlow y gestionar eventos únicos mediante SharedFlow.
- [ ] REQ-SEM06-06: Realizar pruebas técnicas de la persistencia local y la navegación de la aplicación para el hito evaluativo T1.

---

## Semana 7: Fundamentos de Servicios Web y Contratos REST

- [ ] REQ-SEM07-01: Diseñar el contrato de la API REST especificando rutas de endpoints, métodos HTTP, cabeceras y estructuras de carga útil.
- [ ] REQ-SEM07-02: Definir modelos de datos fuertemente tipados para serialización y deserialización sin pérdida de precisión.
- [ ] REQ-SEM07-03: Configurar biblioteca de serialización JSON como Kotlinx Serialization o Gson para el mapeo automático de objetos.
- [ ] REQ-SEM07-04: Implementar políticas de consumo responsable de datos móviles reduciendo el tamaño de las cargas útiles transmitidas por la red.

---

## Semana 8: Consumo de APIs REST con Retrofit y Corrutinas

- [ ] REQ-SEM08-01: Integrar el cliente HTTP Retrofit versión 2 configurado con el motor OkHttp.
- [ ] REQ-SEM08-02: Diseñar la interfaz de servicio de Retrofit con métodos GET, POST, PUT y DELETE vinculados a funciones de suspensión de corrutinas.
- [ ] REQ-SEM08-03: Ejecutar todas las llamadas de red en segundo plano utilizando el despachador de entrada y salida Dispatchers.IO.
- [ ] REQ-SEM08-04: Implementar interceptores de OkHttp para registro de peticiones, inyección de encabezados de autorización y configuración de tiempos de espera.
- [ ] REQ-SEM08-05: Implementar manejo estructurado de respuestas HTTP distinguiendo códigos de éxito de la serie doscientos, errores de cliente de la serie cuatrocientos y fallos de servidor de la serie quinientos.
- [ ] REQ-SEM08-06: Diseñar un mecanismo de sincronización y almacenamiento en caché local coordinando Room y Retrofit para permitir lectura sin conexión a internet.

---

## Semana 9: Integración de la Suite Firebase

- [ ] REQ-SEM09-01: Integrar el SDK de Firebase en la aplicación mediante el archivo de configuración google-services.json y los complementos de compilación correspondientes.
- [ ] REQ-SEM09-02: Implementar Firebase Authentication para registro de cuentas, inicio de sesión seguro y gestión de sesión con correo y contraseña.
- [ ] REQ-SEM09-03: Implementar Cloud Firestore para almacenamiento documental en tiempo real con sincronización remota de colecciones y documentos.
- [ ] REQ-SEM09-04: Implementar Firebase Storage para la carga, resguardo y descarga de archivos binarios e imágenes del proyecto.
- [ ] REQ-SEM09-05: Configurar reglas de seguridad en Firebase para autenticación previa y restricción granular de acceso a lectura y escritura.

---

## Semana 10: Geolocalización y Mapas con Evaluación T2

- [ ] REQ-SEM10-01: Integrar el SDK de Google Maps para Android gestionando la clave de API de forma segura en local.properties y referenciada mediante BuildConfig.
- [ ] REQ-SEM10-02: Implementar el cliente FusedLocationProviderClient para obtener coordenadas de latitud y longitud del dispositivo en tiempo real.
- [ ] REQ-SEM10-03: Solicitar y validar dinámicamente en tiempo de ejecución los permisos de localización precisa ACCESS_FINE_LOCATION y localización aproximada ACCESS_COARSE_LOCATION.
- [ ] REQ-SEM10-04: Renderizar mapas interactivos con marcadores dinámicos, ventanas informativas personalizadas y control de cámara.
- [ ] REQ-SEM10-05: Implementar cálculo de distancias geográficas y trazado de trayectorias sobre el mapa interactivo.
- [ ] REQ-SEM10-06: Consolidar la integración de servicios web REST, Firebase y geolocalización para el hito evaluativo T2.

---

## Semana 11: Integración de Módulos, Pruebas y Transición a Flutter

- [ ] REQ-SEM11-01: Unificar la totalidad de los módulos nativos de la aplicación: persistencia Room, API REST, Firebase y Google Maps.
- [ ] REQ-SEM11-02: Prevenir fugas de memoria cancelando corrutinas activas y desuscribiendo flujos cuando los componentes de la interfaz se destruyan.
- [ ] REQ-SEM11-03: Implementar compresión de imágenes antes de su envío a servicios de red o almacenamiento en la nube para ahorrar ancho de banda y batería.
- [ ] REQ-SEM11-04: Implementar registro controlado de errores y excepciones sin comprometer información sensible del usuario.
- [ ] REQ-SEM11-05: Configurar el entorno inicial de Flutter con lenguaje Dart y seguridad de nulos obligatoria.

---

## Semana 12: Desarrollo Multiplataforma con Widgets de Flutter

- [ ] REQ-SEM12-01: Construir interfaces declarativas modulares en Flutter combinando adecuadamente StatelessWidget y StatefulWidget según la reactividad requerida.
- [ ] REQ-SEM12-02: Implementar el sistema de navegación declarativa entre pantallas pasando parámetros fuertemente tipados en Flutter.
- [ ] REQ-SEM12-03: Aplicar componentes y temas visuales de Material Design en Flutter manteniendo coherencia estética con la aplicación nativa.
- [ ] REQ-SEM12-04: Diseñar componentes personalizados reutilizables gestionando adecuadamente el ciclo de vida del widget.

---

## Semana 13: Persistencia y Servicios Web en Flutter y Evaluación T3

- [ ] REQ-SEM13-01: Implementar persistencia de datos local en Flutter utilizando la biblioteca sqflite con sentencias estructuradas sobre SQLite.
- [ ] REQ-SEM13-02: Implementar cliente de red en Flutter mediante el paquete http para consumir servicios web REST externos y decodificar respuestas JSON.
- [ ] REQ-SEM13-03: Manejar estados asíncronos en Flutter utilizando widgets como FutureBuilder o StreamBuilder para mostrar indicadores de carga, errores y datos procesados.
- [ ] REQ-SEM13-04: Presentar la solución técnica completa e integrada al cien por ciento para la evidencia de práctica de campo y evaluación T3.

---

## Semana 14: Preparación para Distribución y Empaquetado

- [ ] REQ-SEM14-01: Configurar el versionamiento semántico formal de la aplicación mediante versionCode incremental y versionName descriptivo en el archivo de compilación.
- [ ] REQ-SEM14-02: Generar y resguardar el almacén de llaves criptográficas keystore para la firma digital de la versión de producción.
- [ ] REQ-SEM14-03: Configurar el bloque signingConfigs en Gradle para aplicar la firma digital automatizada en la variante de compilación release.
- [ ] REQ-SEM14-04: Habilitar optimización de código, reducción de recursos y reglas de ofuscación mediante R8 y ProGuard.
- [ ] REQ-SEM14-05: Generar el archivo binario Android App Bundle con extensión aab mediante la tarea bundleRelease de Gradle.
- [ ] REQ-SEM14-06: Configurar el icono adaptable con capas separadas de fondo y primer plano cumpliendo los requerimientos visuales de la tienda.

---

## Semana 15: Publicación Controlada y Monitoreo

- [ ] REQ-SEM15-01: Estructurar la aplicación para cumplir con las políticas de privacidad, declaración de permisos y normativas de seguridad de Google Play Console.
- [ ] REQ-SEM15-02: Preparar la carga del paquete en la pista de pruebas internas o cerradas dentro de Google Play Console.
- [ ] REQ-SEM15-03: Redactar notas de versión técnicas y descriptivas para los evaluadores y usuarios de prueba.
- [ ] REQ-SEM15-04: Configurar mecanismos de monitoreo técnico para la recolección de trazas de fallos y métricas iniciales de estabilidad del sistema.

---

## Semana 16: Evaluación Final y Sustentación Técnica

- [ ] REQ-SEM16-01: Ejecutar una auditoría exhaustiva del código fuente verificando limpieza, tipado estricto y ausencia de advertencias críticas de compilación.
- [ ] REQ-SEM16-02: Verificar el funcionamiento sin fallos en vivo de todos los módulos: persistencia local, cliente REST, Firebase, geolocalización y módulos Flutter.
- [ ] REQ-SEM16-03: Demostrar la correcta aplicación de patrones arquitectónicos, resiliencia ante pérdida de conexión y eficiencia en el uso de recursos.
- [ ] REQ-SEM16-04: Confirmar la verificación y cumplimiento del cien por ciento de las casillas técnicas del presente documento para la defensa final del proyecto.
