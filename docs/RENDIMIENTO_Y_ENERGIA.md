# Directrices de CPU, memoria, red y batería

Requisitos REQ-SEM01-LOG-01, REQ-SEM04-LOG-01 y REQ-SEM04-INF-02, alineados con
el ODS 12 (producción y consumo responsables). Cada regla dice dónde se aplica.

## 1. No trabajar cuando nadie mira

- **El seguimiento solo consulta con la pantalla visible.** El sondeo de
  `GET /api/orders/:id` arranca en `ON_START` y se cancela en `ON_STOP` o al
  salir de la ruta (`SeguimientoMientrasVisible` en `ui/FoodJetApp.kt`).
- **El reloj del repartidor** avanza cada segundo con
  `repeatOnLifecycle(STARTED)` y solo mientras el pedido va en camino
  (`ui/screens/TrackingScreen.kt`).
- **Los WebView del mapa** se pausan con la Activity (`onPause`/`onResume`) y
  se destruyen al salir del árbol de Compose (`onRelease`), en
  `ui/components/MapaFoodJet.kt`.

## 2. Consultar al ritmo de los datos

- Cada 3 s cuando el repartidor va en camino, cada 8 s en los demás estados y
  nunca más cuando el pedido terminó (`OrderViewModel.intervaloDeSondeo`).
- Tras fallos de red la espera se duplica hasta 30 s: no se insiste contra un
  servidor caído.
- La posición del repartidor no se pide al servidor cada segundo: se deriva de
  la hora de salida y la duración del reparto, como hace la web.

## 3. Ubicación: una lectura, precisión equilibrada

- Se pide la ubicación solo cuando el cliente toca "Usar mi ubicación", una
  sola vez, y se cancela al salir de la pantalla (`LocationProvider`).
- Se prefiere el proveedor de red (wifi/antenas) al GPS: alcanza para ubicar
  una puerta en ciudad y gasta mucha menos batería.

## 4. Memoria

- `ServiceLocator` guarda solo el contexto de aplicación, nunca una Activity:
  retenerla la filtraría en cada giro de pantalla.
- Los ViewModel cancelan sus corrutinas en `onCleared` y usan
  `viewModelScope`; las pantallas leen estado con
  `collectAsStateWithLifecycle`, que deja de recolectar en segundo plano.
- Las listas largas usan `LazyColumn`, que solo compone lo visible.
- Los cálculos derivados del pedido (línea de tiempo, reparto, estadísticas
  del admin) se memorizan con `remember(claves)`.

## 5. Red y datos móviles

- El catálogo se guarda en Room y se sirve desde ahí sin conexión.
- Las keys del mapa se piden una vez por servidor (`ConfigMapa`).
- El registro HTTP es `BASIC` solo en builds de depuración y `NONE` en
  release; nunca registra cuerpos, así que tampoco contraseñas ni tokens.
- Los locales consultados para el QR de pago se guardan en memoria durante la
  sesión.

## 6. Al añadir código nuevo

1. Toda corrutina que se repite debe tener un dueño con ciclo de vida
   (`viewModelScope`, `LaunchedEffect`, `repeatOnLifecycle`).
2. Un `while (true)` con `delay` tiene que detenerse en segundo plano.
3. Una vista nativa dentro de Compose (`AndroidView`) se libera en
   `onRelease`.
4. Antes de añadir una consulta periódica, pensar si el dato se puede derivar
   en el teléfono.
