# Seguridad y Alineación Académica — FoodJet Android

Documento técnico que consolida las cinco correcciones de seguridad, ciclo de vida y empaquetado `release` implementadas sobre la rama `fix/seguridad-y-cumplimiento-academico` del proyecto **FoodJet** (curso *Desarrollo de Aplicaciones Móviles — UPN*).

---

## 1. Resumen de alineación académica

| Semana | Tema del curso | Hallazgo atendido | Estado técnico |
| :--- | :--- | :--- | :--- |
| **Semana 2** | Ciclo de vida de Activities y estado | Preservación de estado no sensible ante rotación, detención/reanudación de sondeo en `ON_STOP`/`ON_START` y prevención de re-navegación por `Intent` tras `recreate()`. | Verificado en emulador (`LifecycleAndRotationInstrumentedTest`, 10/10). |
| **Semana 3** | Navegación, Intents y configuración | Conservación de ruta activa en `NavHost`, filtros/paginación en `HomeScreen`, entrega en `CheckoutScreen` (excluyendo tarjeta de `SavedInstanceState`) y liberación limpia de `WebView`. | Verificado en emulador (`ActivityScenario.recreate()`). |
| **Semana 11** | Optimización y empaquetado `release` | Activación de R8 (full mode) y reducción de recursos en `release` compatible con AGP 9 + reglas mínimas en `proguard-rules.pro`. | Verificado (`app-debug.apk` 65.77 MB → `app-release.apk` 4.04 MB). |
| **Semana 12** | Seguridad, cifrado, respaldos y red | Cifrado autenticado AES-GCM con Android Keystore para JWT y PII de sesión, exclusión de respaldos y transferencias D2D, y restricción de tráfico HTTP cleartext mediante `networkSecurityConfig`. | Verificado con pruebas unitarias e instrumentadas en Android Keystore y `NetworkSecurityPolicy`. |
| **Semana 15** | Firma digital y preparación de publicación | Configuración segura de `signingConfigs` mediante archivo local ignorado o variables de entorno, variante académica `localRelease`, refuerzo de `.gitignore` y verificación de APK firmado con `apksigner`. | Verificado localmente con keystore temporal (eliminado tras la prueba). |

---

## 2. Detalle técnico de las correcciones

### 2.1 Corrección 01 — Almacenamiento seguro del JWT y datos de sesión (Prioridad Alta · Semana 12)

- **Problema original:** `SessionDataStore` guardaba el token JWT (`jwt_token`), correo (`user_email`) y teléfono (`user_phone`) en texto plano dentro de Jetpack DataStore Preferences. Además, `RetrofitClient` creaba una segunda instancia de `SessionDataStore` independiente de `ServiceLocator`.
- **Solución implementada:**
  - Se creó `SessionTokenCrypto.kt` (`SessionCipher`, `AesGcmPayloadCodec` y `AndroidKeystoreAesGcmCipher`) usando **Android Keystore** con clave simétrica AES-256 en modo `AES/GCM/NoPadding`, `setRandomizedEncryptionRequired(true)`, IV aleatorio único de 12 bytes por cada cifrado y etiqueta de autenticación de 128 bits.
  - Se actualizó `SessionDataStore.kt` para persistir el JWT en `jwt_token_enc` y, adicionalmente, cifrar PII de sesión (`user_email_enc` y `user_phone_enc`) como defensa en profundidad.
  - **Migración y limpieza:** al leer sesiones legadas en texto plano (`jwt_token`, `user_email`, `user_phone`), el almacén las cifra con la clave de Android Keystore y elimina de inmediato las claves legadas sin cifrar.
  - **Resiliencia:** ante datos corruptos, alteración del ciphertext/tag GCM o invalidación/eliminación de la clave en Keystore (`KeyPermanentlyInvalidatedException` / `AEADBadTagException`), `SessionDataStore` limpia de forma controlada la sesión afectada y cierra la sesión sin provocar excepciones no controladas ni exponer tokens en logs o mensajes de error.
  - **Instancia única:** `RetrofitClient` reutiliza la instancia administrada por `ServiceLocator.sessionDataStore` para evitar concurrencia entre dos instancias de `DataStore` sobre el mismo archivo.
- **Archivos:** `SessionTokenCrypto.kt`, `SessionDataStore.kt`, `RetrofitClient.kt`, `ServiceLocator.kt`, `SessionDataStoreSecurityTest.kt`, `SessionDataStoreKeystoreInstrumentedTest.kt`.

### 2.2 Corrección 02 — Protección de datos en respaldos de Android (Prioridad Alta · Semana 12)

- **Problema original:** `AndroidManifest.xml` mantenía `android:allowBackup="true"` y los archivos `backup_rules.xml` y `data_extraction_rules.xml` contenían comentarios de plantilla sin exclusiones efectivas.
- **Solución implementada:**
  - Se estableció `android:allowBackup="false"` en `AndroidManifest.xml`, conservando las referencias explícitas a `android:fullBackupContent="@xml/backup_rules"` y `android:dataExtractionRules="@xml/data_extraction_rules"`.
  - En `backup_rules.xml` (API 29–30) se excluyeron explícitamente `file/datastore`, `database/.`, `sharedpref/.` y `external/.`.
  - En `data_extraction_rules.xml` (API 31+) se configuraron exclusiones explícitas tanto en `<cloud-backup disableIfNoEncryptionCapabilities="true">` como en `<device-transfer>` (necesario en Android 12+, ya que `allowBackup="false"` no deshabilita transferencias dispositivo a dispositivo por sí solo).
- **Decisión técnica:** dado que la base Room (`foodjet_database`), DataStore de sesión y preferencias locales contienen datos de sesión/usuario o caché reconstruible desde el backend, deshabilitar el respaldo y excluir explícitamente DataStore, SQLite y SharedPreferences evita fugas de estado cifrado o PII entre dispositivos.
- **Archivos:** `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`, `BackupRulesConfigTest.kt`.

### 2.3 Corrección 03 — Restricción del tráfico HTTP sin cifrar (Prioridad Media · Semana 12)

- **Problema original:** `AndroidManifest.xml` declaraba `android:usesCleartextTraffic="true"` a nivel global, permitiendo tráfico HTTP en claro hacia cualquier destino en todas las variantes.
- **Solución implementada:**
  - Se reemplazó `android:usesCleartextTraffic="true"` por `android:usesCleartextTraffic="false"` y `android:networkSecurityConfig="@xml/network_security_config"`.
  - **Variante `release` (`src/main/res/xml/network_security_config.xml`):** política estricta que bloquea cleartext globalmente (`cleartextTrafficPermitted="false"`), confía únicamente en certificados del sistema (`<certificates src="system" />`) y bloquea explícitamente `localhost` y `127.0.0.1` en texto plano.
  - **Variantes `debug` y `localRelease` (`src/debug/res/xml/network_security_config.xml` y `src/localRelease/res/xml/network_security_config.xml`):** bloquean cleartext por defecto (`base-config cleartextTrafficPermitted="false"`) y habilitan HTTP **exclusivamente** para `10.0.2.2` (emulador) y `localhost` (dispositivo físico vía ADB Reverse), con `includeSubdomains="false"`.
  - **Endurecimiento del WebView de mapas (`MapaFoodJet.kt`):** `MIXED_CONTENT_NEVER_ALLOW`, `allowFileAccess = false`, `allowContentAccess = false`, y `shouldOverrideUrlLoading` bloqueando navegaciones arbitrarias externas no locales.
  - **Verificación de seed y cliente:** se confirmó que `backend/prisma/seed.js` y `seedProductsExtra.js` no contienen URLs `http://`, y se ampliaron pruebas negativas en `ServidorFoodJetTest` para puertos/hosts no autorizados.
- **Archivos:** `AndroidManifest.xml`, `src/main/res/xml/network_security_config.xml`, `src/debug/res/xml/network_security_config.xml`, `src/localRelease/res/xml/network_security_config.xml`, `MapaFoodJet.kt`, `ServidorFoodJetTest.kt`, `NetworkSecurityConfigTest.kt`, `NetworkSecurityInstrumentedTest.kt`.

### 2.4 Corrección 04 — Ciclo de vida y rotación de pantalla (Prioridad Media · Semanas 2 y 3)

- **Hallazgos reales detectados durante el diagnóstico:**
  1. `CheckoutScreen.kt` usaba `remember` en campos de entrega, dirección, referencia, coordenada seleccionada, método de pago y cupón, perdiendo todo el avance del usuario al rotar la pantalla.
  2. `HomeScreen.kt` usaba `remember` en búsqueda y filtros, y reiniciaba `currentPage = 1` en cada recomposición inicial tras rotar aunque los filtros no hubieran cambiado.
  3. `MainActivity.kt` confiaba únicamente en `intent.removeExtra(...)`, sin registrar en `savedInstanceState` si el `Intent` inicial ya fue atendido.
  4. `MapaFoodJet.kt` podía reforzar la limpieza de `WebView` en `onRelease` pausándolo, limpiando historial y desacoplándolo de su contenedor antes de `destroy()`.
- **Solución implementada:**
  - `MainActivity.kt`: guarda `foodjet_intent_inicial_atendido` en `onSaveInstanceState` para garantizar que `recreate()` nunca repita la navegación de un `Intent` explícito ya atendido, manteniendo `onNewIntent` funcional.
  - `CheckoutScreen.kt`: migra campos **no sensibles** (`name`, `phone`, `address`, `reference`, `punto` con `listSaver`, `precisionGps`, `direccionGuardadaId`, `textoEditadoAMano`, `avisoUbicacion`, `mostrarExplicacionPermiso`, `paymentMethod`, `couponCode`, `couponAppliedId`, `couponDiscountPercent`, `couponMessage`) a `rememberSaveable`, y mantiene deliberadamente `cardNumber`, `cardExpiryCampo` y `cardCvc` en memoria volátil (`remember`) para **no serializar datos de tarjeta** en `SavedInstanceState`.
  - `HomeScreen.kt`: preserva búsqueda, filtros y página actual ante rotación mediante `rememberSaveable` y clave de filtros `ultimaClaveFiltros`.
  - `MapaFoodJet.kt`: refuerza `onRelease` con `onPause()`, `removeJavascriptInterface("Android")`, `stopLoading()`, `clearHistory()`, remoción del padre y `destroy()`.
  - `OrderViewModel.kt`: expone propiedades internas de solo lectura (`isTrackingPollingActive`, `currentTrackedOrderId`) para verificación determinista del sondeo en pruebas instrumentadas.
- **Archivos:** `MainActivity.kt`, `CheckoutScreen.kt`, `HomeScreen.kt`, `MapaFoodJet.kt`, `OrderViewModel.kt`, `LifecycleAndRotationInstrumentedTest.kt`.

### 2.5 Corrección 05 — Compilación `release` y firma digital (Prioridad Media · Semanas 11 y 15)

- **Problema original:** `app/build.gradle.kts` mantenía la optimización desactivada (`optimization { enable = false }`), no existía `proguard-rules.pro` ni `signingConfigs` externo, y no se diferenciaba una compilación académica local optimizada de una compilación `release` estricta.
- **Solución implementada:**
  - `app/build.gradle.kts`:
    - Habilita R8 (`isMinifyEnabled = true`) y reducción optimizada de recursos (`isShrinkResources = true`, con optimized resource shrinking automático en AGP 9) usando `proguard-android-optimize.txt` + `proguard-rules.pro`.
    - Agrega configuración de firma `signingConfigs.release` leída exclusivamente desde `app-movil/keystore.properties` (ignorado por Git) o desde variables de entorno (`FOODJET_KEYSTORE_PATH`, `FOODJET_KEYSTORE_PASSWORD`, `FOODJET_KEY_ALIAS`, `FOODJET_KEY_PASSWORD`). Si no están presentes, `release` compila sin firma de forma segura sin fallar ni incluir secretos.
    - Crea la variante `localRelease` (`applicationIdSuffix = ".local"`, `versionNameSuffix = "-academic-local"`) que hereda R8 y usa el `network_security_config.xml` restringido a `10.0.2.2` / `localhost`, preservando intacta la variante `release` estricta HTTPS.
  - `app/proguard-rules.pro`: reglas mínimas y justificadas para `@JavascriptInterface` (`PuenteMapa` del WebView de Google Maps), campos `@SerializedName` en DTOs de FoodJet y el constructor por reflexión de Room 2.6.1 bajo R8 full mode en AGP 9 (`FoodJetDatabase` y `FoodJetDatabase_Impl { public <init>(); }`, hallazgo detectado y corregido durante la prueba en vivo de `app-localRelease.apk`), evitando reglas amplias redundantes con las *consumer rules* de Retrofit 2.11, Gson 2.11, Coroutines y AndroidX.
  - `.gitignore` y `app-movil/.gitignore`: ignoran `keystore.properties`, `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.bks`, `*.apk` y `*.aab`.

---

## 3. Guía de compilación `release` y firma digital segura

> **Regla de seguridad:** nunca subir archivos `.jks`, `.keystore`, `.p12`, `keystore.properties` ni contraseñas al repositorio Git.

### Opción A — Mediante archivo local ignorado (`app-movil/keystore.properties`)

Crear `app-movil/keystore.properties` únicamente en la máquina local (está ignorado por `.gitignore`):

```properties
storeFile=C:/ruta/segura/fuera-del-repo/foodjet-release.jks
storePassword=CONTRASENA_KEYSTORE
keyAlias=foodjet_release
keyPassword=CONTRASENA_CLAVE
```

### Opción B — Mediante variables de entorno (recomendada para CI/CD)

```powershell
$env:FOODJET_KEYSTORE_PATH = "C:\ruta\segura\foodjet-release.jks"
$env:FOODJET_KEYSTORE_PASSWORD = "CONTRASENA_KEYSTORE"
$env:FOODJET_KEY_ALIAS = "foodjet_release"
$env:FOODJET_KEY_PASSWORD = "CONTRASENA_CLAVE"

.\gradlew.bat :app:assembleRelease
```

### Verificación de firma con `apksigner`

```powershell
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

---

## 4. Resultados reales de verificación

Todas las verificaciones se ejecutaron sobre la rama `fix/seguridad-y-cumplimiento-academico` en Windows con Gradle 9.8.0, Kotlin 2.2.10, `compileSdk = 35`, `minSdk = 29` y emulador `Pixel_7(AVD) - 17` (`emulator-5554`):

| Verificación | Resultado | Detalle |
| :--- | :--- | :--- |
| `:app:testDebugUnitTest` | **EXITOSO** | **376 passed, 0 failed, 0 skipped** (incluye `SessionDataStoreSecurityTest`, `BackupRulesConfigTest`, `NetworkSecurityConfigTest`, `ServidorFoodJetTest` y toda la suite previa). |
| `:app:lintDebug` | **EXITOSO** | 0 errores y 0 advertencias sobre `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`, `network_security_config.xml` o `WebView`. |
| `:app:assembleDebug` | **EXITOSO** | `app-debug.apk` generado (65.77 MB). |
| `:app:connectedDebugAndroidTest` | **EXITOSO** | **18 passed, 0 failed, 0 skipped** en `Pixel_7(AVD) - 17` (10 escenarios de `LifecycleAndRotationInstrumentedTest`, 2 de `SessionDataStoreKeystoreInstrumentedTest`, 2 de `NetworkSecurityInstrumentedTest`, 3 de `MapaFoodJetInstrumentedTest` contra backend local, 1 de `ExampleInstrumentedTest`). |
| `:app:assembleRelease` (sin firma) | **EXITOSO** | R8 + optimized resource shrinking + `lintVitalRelease` exitosos (`app-release-unsigned.apk`). |
| `:app:assembleLocalRelease` | **EXITOSO** | Variante académica R8 generada (`app-localRelease-unsigned.apk`, 4.03 MB). |
| `:app:assembleRelease` (firmado temporal) + `apksigner verify` | **EXITOSO** | `app-release.apk` (4.04 MB, reducción de ~93.9% frente a debug) verificado con **APK Signature Scheme v2** (`CN=FoodJet Local Test Only - Not For Production`, RSA 2048). El keystore temporal en `%TEMP%` fue eliminado inmediatamente tras la verificación. |
| Ejecución en vivo `app-localRelease.apk` (R8 full mode) | **EXITOSO** | Instalado en `Pixel_7(AVD) - 17` contra backend local (`10.0.2.2:3000`). Detectó y corrigió `NoSuchMethodException` en `FoodJetDatabase_Impl.<init>()` bajo R8 full mode. Tras la corrección: arranque en frío en 785 ms, catálogo de 106 productos (`1–12 de 106`), login demo + persistencia cifrada AES-256-GCM en Android Keystore, recuperación automática de sesión tras `am force-stop` en 457 ms, **0 crashes** y **0 fugas de JWT en Logcat**. APK y keystore temporal eliminados tras la prueba. |

---

## 5. Limitaciones pendientes y alcance

- **Backend local sin TLS:** el servidor académico (`backend/`) opera localmente por HTTP (`10.0.2.2:3000` / `localhost:3000`). En consecuencia, la variante `release` estricta requiere que un eventual despliegue productivo utilice un backend HTTPS con certificado válido emitido por una CA confiable del sistema.
- **Dispositivo físico por ADB Reverse:** el flujo para dispositivo físico (`http://localhost:3000/api/` mediante `ServidorFoodJet.NODE_LOCAL_USB` y excepción explícita de `localhost` en `debug`/`localRelease`) fue verificado mediante pruebas unitarias y de política de seguridad (`ServidorFoodJetTest`, `NetworkSecurityConfigTest`, `NetworkSecurityInstrumentedTest`), pero no sobre un teléfono físico conectado por USB en esta sesión (solo se contó con emulador `Pixel_7(AVD) - 17`).
- **Ausencia de keystore de producción:** el repositorio no contiene ni gestiona claves de firma productivas; para distribución en Google Play se requiere generar y custodiar un Upload Keystore bajo Play App Signing fuera de Git.
- **Verificación de alcance:** los resultados confirman la corrección de los cinco hallazgos académicos y de seguridad identificados en el entorno controlado de pruebas; no constituyen una auditoría externa de penetración ni certificación para producción comercial.
