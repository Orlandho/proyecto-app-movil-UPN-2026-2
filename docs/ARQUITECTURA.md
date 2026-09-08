# Arquitectura y Flujos del Sistema - FoodJet App (UPN 2026-2)

Este documento centraliza la arquitectura de software, los flujos de navegación, el ciclo de pedidos y la gobernanza del repositorio regulada por `.github/CODEOWNERS` para el proyecto **FoodJet App**.

Todos los diagramas vectoriales generados en formato SVG de alta resolución se encuentran en el directorio [`mermaid diagramas/`](../mermaid%20diagramas/).

---

## 1. Arquitectura Técnica MVVM y Capas del Sistema
El sistema implementa el patrón arquitectónico **Model-View-ViewModel (MVVM)** desacoplando la capa visual en Jetpack Compose, los ViewModels retenedores de estado ante giros de pantalla, el patrón Repository como única fuente de verdad, y las fuentes de persistencia local (Room, DataStore) y remotas (Retrofit 2, Firebase).

- **Archivo Fuente:** [`mermaid diagramas/01_arquitectura_mvvm_limpia.mmd`](../mermaid%20diagramas/01_arquitectura_mvvm_limpia.mmd)
- **Vector SVG:** [`mermaid diagramas/01_arquitectura_mvvm_limpia.svg`](../mermaid%20diagramas/01_arquitectura_mvvm_limpia.svg)

```mermaid
flowchart TB
    subgraph UI ["Capa Visual (Jetpack Compose & Material 3)"]
        Screens["Pantallas Composables\n(HomeScreen, FavoritesScreen, CartSheet, etc.)"]
        Collector["collectAsStateWithLifecycle()"]
    end

    subgraph Presentation ["Lógica de Presentación (ViewModels)"]
        HomeVM["HomeViewModel"]
        CartVM["CartViewModel"]
        OrderVM["OrderViewModel"]
        AuthVM["AuthViewModel"]
        
        StateFlows["Estados Inmutables\n(UiState<T>, StateFlow)"]
        SideEffects["Efectos Únicos\n(SharedFlow / Snackbar)"]
    end

    subgraph DomainData ["Capa de Negocio y Repositorios"]
        ProductRepo["ProductRepository"]
        OrderRepo["OrderRepository"]
        UserRepo["UserRepository"]
    end

    subgraph DataSources ["Fuentes de Datos Locales y Remotas"]
        RoomDB[("Room SQLite DB\n(DAOs & Flow)")]
        DataStore[("DataStore\n(Preferencias de Usuario)")]
        RetrofitClient["Retrofit 2 REST Client\n(Dispatchers.IO)"]
        FirebaseServices["Firebase Suite\n(Auth, Firestore, Storage)"]
    end

    Screens -->|Eventos de Usuario / Intents| HomeVM & CartVM & OrderVM & AuthVM
    HomeVM & CartVM & OrderVM & AuthVM --> StateFlows & SideEffects
    StateFlows --> Collector
    Collector --> Screens

    HomeVM --> ProductRepo
    CartVM --> ProductRepo & OrderRepo
    OrderVM --> OrderRepo
    AuthVM --> UserRepo

    ProductRepo --> RoomDB & RetrofitClient
    OrderRepo --> RoomDB & FirebaseServices
    UserRepo --> DataStore & FirebaseServices
```

---

## 2. Flujo de Navegación y Ciclo de Pantallas
Representa el árbol de navegación controlado mediante `Scaffold`, `FoodJetTopBar`, `FoodJetBottomBar` y modales de diálogo.

- **Archivo Fuente:** [`mermaid diagramas/02_flujo_navegacion_pantallas.mmd`](../mermaid%20diagramas/02_flujo_navegacion_pantallas.mmd)
- **Vector SVG:** [`mermaid diagramas/02_flujo_navegacion_pantallas.svg`](../mermaid%20diagramas/02_flujo_navegacion_pantallas.svg)

```mermaid
flowchart TD
    AppLaunch(["Apertura de la Aplicación"]) --> Splash["Pantalla Splash / Carga Inicial"]
    Splash --> AuthCheck{¿Sesión Activa?}

    AuthCheck -- No --> GuestView["Modo Invitado / Acceso Público"]
    AuthCheck -- Sí --> UserView["Sesión Autenticada (Estudiante / Cliente)"]

    GuestView & UserView --> MainScaffold["Scaffold Principal (FoodJetApp)"]

    subgraph TopAndBottomNavigation ["Barras de Navegación"]
        MainScaffold --> TopBar["FoodJetTopBar (Perfil, Carrito, Favoritos)"]
        MainScaffold --> BottomBar["FoodJetBottomBar (Home, Menú, Pedidos, Favoritos)"]
    end

    TopBar -->|Click Carrito| CartSheet["CartSheet (Modal Bottom Sheet)"]
    TopBar -->|Click Perfil| ProfileDecision{¿Autenticado?}
    ProfileDecision -- Sí --> AccountDialog["Diálogo Mi Cuenta"]
    ProfileDecision -- No --> LoginModal["Modal Login / Registro"]

    BottomBar -->|Ruta home / menu| HomeScreen["HomeScreen (Promos, Catálogo, Filtros)"]
    BottomBar -->|Ruta favorites| FavScreen["FavoritesScreen (Listado de Favoritos)"]
    BottomBar -->|Ruta orders| OrdersScreen["OrderHistoryScreen (Historial de Pedidos)"]
    BottomBar -->|Ruta admin| AdminScreen["AdminDashboardScreen (Panel de Gestión)"]

    CartSheet -->|Continuar Compra| Checkout["CheckoutScreen (Entrega, Método de Pago)"]
    Checkout -->|Confirmar Pedido| Tracking["TrackingScreen (Monitoreo en Tiempo Real)"]
```

---

## 3. Flujo de Carrito, Cupones y Checkout
Modela la lógica reactiva de validación de cupones, descuento automático a estudiantes universitarios y confirmación de la orden.

- **Archivo Fuente:** [`mermaid diagramas/03_flujo_checkout_y_pedidos.mmd`](../mermaid%20diagramas/03_flujo_checkout_y_pedidos.mmd)
- **Vector SVG:** [`mermaid diagramas/03_flujo_checkout_y_pedidos.svg`](../mermaid%20diagramas/03_flujo_checkout_y_pedidos.svg)

```mermaid
flowchart TD
    StartCart(["Artículos agregados al Carrito"]) --> OpenCart["Apertura de CartSheet"]
    OpenCart --> ReviewItems["Revisión de Cantidades y Subtotal"]
    
    ReviewItems --> ApplyCoupon{¿Ingresó Cupón?}
    ApplyCoupon -- Sí --> ValidateCoupon["Validar Cupón de Descuento"]
    ApplyCoupon -- No --> StudentCheck
    
    ValidateCoupon --> StudentCheck{¿Usuario es Estudiante UPN?}
    StudentCheck -- Sí --> ApplyStudentDiscount["Aplica Descuento Estudiantil"]
    StudentCheck -- No --> CalculateTotal["Cálculo de Total Final"]
    ApplyStudentDiscount --> CalculateTotal

    CalculateTotal --> ProceedCheckout["Navegar a CheckoutScreen"]
    ProceedCheckout --> SelectDelivery["Seleccionar Tipo de Entrega (Delivery / Retiro)"]
    SelectDelivery --> SelectPayment["Seleccionar Método de Pago (Yape, Plin, Tarjeta, Efectivo)"]
    
    SelectPayment --> ValidateForm{¿Campos Válidos?}
    ValidateForm -- No --> ShowFormErrors["Mostrar Errores Reactivos"]
    ShowFormErrors --> SelectPayment
    
    ValidateForm -- Sí --> CreateOrder["Generar Registro de Pedido (OrderRecord)"]
    CreateOrder --> SaveLocal["Persistir en Repositorio Local"]
    SaveLocal --> SyncRemote["Enviar Orden a Servidor / Firestore"]
    SyncRemote --> OpenTracking["Navegar a TrackingScreen con Número de Orden"]
```

---

## 4. Estrategia de Persistencia Offline-First
Estrategia para garantizar que la aplicación presente datos locales de inmediato (Zero-Delay) mediante Room SQLite, sincronizando en paralelo con la API REST o Cloud Firestore en hilos secundarios (`Dispatchers.IO`).

- **Archivo Fuente:** [`mermaid diagramas/04_sincronizacion_datos_offline_first.mmd`](../mermaid%20diagramas/04_sincronizacion_datos_offline_first.mmd)
- **Vector SVG:** [`mermaid diagramas/04_sincronizacion_datos_offline_first.svg`](../mermaid%20diagramas/04_sincronizacion_datos_offline_first.svg)

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuario
    participant UI as Composable UI
    participant VM as HomeViewModel
    participant Repo as ProductRepository
    participant DB as Room SQLite Local
    participant Remote as REST API / Cloud Firestore

    User->>UI: Abre pantalla de Catálogo
    UI->>VM: loadProducts()
    VM->>Repo: getProductsFlow()
    Repo->>DB: queryAllProducts()
    DB-->>Repo: Emite catálogo almacenado localmente
    Repo-->>VM: Emite UiState.Success(cachedProducts)
    VM-->>UI: Renderiza catálogo instantáneamente (Zero Delay)

    rect rgb(240, 248, 255)
        Note over Repo,Remote: Tarea en segundo plano con Dispatchers.IO
        Repo->>Remote: fetchRemoteCatalog()
        alt Conexión Exitosa (HTTP 200)
            Remote-->>Repo: Lista actualizada de productos
            Repo->>DB: upsertProducts(updatedList)
            DB-->>Repo: Dispara nueva emisión en Flow
            Repo-->>VM: Emite UiState.Success(updatedList)
            VM-->>UI: Recompone lista suavemente con datos frescos
        else Falla de Red / Sin Conexión
            Remote-->>Repo: Timeout / IOException
            Repo-->>VM: Emite evento de estado Offline
            VM-->>UI: Muestra aviso no intrusivo 'Modo sin conexión'
        end
    end
```

---

## 5. Gobernanza y Flujo de Revisión con CODEOWNERS
Políticas de revisión obligatoria de código asignadas a `@Orlandho` a través del motor `.github/CODEOWNERS`.

- **Archivo Fuente:** [`mermaid diagramas/05_flujo_codeowners_y_pr_review.mmd`](../mermaid%20diagramas/05_flujo_codeowners_y_pr_review.mmd)
- **Vector SVG:** [`mermaid diagramas/05_flujo_codeowners_y_pr_review.svg`](../mermaid%20diagramas/05_flujo_codeowners_y_pr_review.svg)

```mermaid
flowchart TD
    Dev["Desarrollador / Contribuidor"] --> Commit["Commit y Push a rama feature/..."]
    Commit --> CreatePR["Abre Pull Request hacia main"]
    
    CreatePR --> Engine["GitHub CODEOWNERS Engine"]
    Engine --> CheckRules["Evalúa rutas modificadas contra .github/CODEOWNERS"]
    CheckRules --> Assign["Asigna obligatoriamente a @Orlandho como Revisor"]
    
    Assign --> Checks["Ejecución de Verificaciones Automatizadas (Gradle Build / Tests)"]
    
    Checks -- Falla --> DevFix["Desarrollador corrige errores"]
    DevFix --> Commit
    
    Checks -- Exitoso --> ReviewProcess{"Revisión de Código por @Orlandho"}
    
    ReviewProcess -- Solicita Cambios --> Feedback["Feedback técnico en el PR"]
    Feedback --> DevFix
    
    ReviewProcess -- Aprobado (Approved) --> MergeReady["PR listo para Merge"]
    MergeReady --> SquashMerge["Merge a rama main"]
```

---

## 6. Arquitectura de Integración con el Backend FoodJet (API REST y PostgreSQL)
El sistema móvil se conecta directamente con los microservicios y base de datos relacional de FoodJet, permitiendo autenticación con JWT Bearer tokens, catálogo en tiempo real con soporte de descuento para estudiantes y ciclo de vida de pedidos. Para la especificación detallada de endpoints, configuración de red local y DTOs, consultar el documento técnico:

👉 **[Guía Técnica de Integración Backend](INTEGRACION_BACKEND_FOODJET.md)**

- **Archivo Fuente:** [`mermaid diagramas/06_integracion_backend_foodjet_api.mmd`](../mermaid%20diagramas/06_integracion_backend_foodjet_api.mmd)
- **Vector SVG:** [`mermaid diagramas/06_integracion_backend_foodjet_api.svg`](../mermaid%20diagramas/06_integracion_backend_foodjet_api.svg)

```mermaid
flowchart TD
    subgraph AndroidApp ["Dispositivo Móvil Android (Emulador / Físico)"]
        subgraph Presentation ["Capa de Presentación"]
            UI["Pantallas Jetpack Compose\n(Home, Cart, Orders, Auth)"]
            VM["ViewModels (HomeVM, AuthVM, OrderVM)\ncollectAsStateWithLifecycle()"]
        end

        subgraph Repositories ["Capa de Repositorios (Única Fuente de Verdad)"]
            ProductRepo["ProductRepositoryImpl"]
            UserRepo["UserRepositoryImpl"]
            OrderRepo["OrderRepositoryImpl"]
        end

        subgraph LocalPersistence ["Persistencia Local"]
            RoomDB[("Room Database (SQLite)\nCaché Offline de Productos\nREQ-SEM05-INF-02")]
            DataStore[("DataStore Preferences\nJWT Token & Sesión\nREQ-SEM05-INF-01")]
        end

        subgraph NetworkClient ["Cliente de Red (Retrofit 2 + OkHttp)"]
            AuthInterceptor["AuthInterceptor\n(Inyecta 'Authorization: Bearer token')\nREQ-SEM08-INF-01"]
            OkHttpClient["OkHttpClient\n(Timeouts + HttpLoggingInterceptor)"]
            RetrofitAPI["FoodJetApiService\n(Endpoints Suspendidos en Dispatchers.IO)\nREQ-SEM08-LOG-01 / REQ-SEM08-LOG-02"]
        end
    end

    subgraph Connectivity ["Capa de Conectividad y Seguridad"]
        CleartextPerm["AndroidManifest.xml\nandroid:usesCleartextTraffic='true'"]
        BaseUrl["Base URL:\nEmulador: http://10.0.2.2:3000/api/\nDispositivo LAN: http://192.168.x.x:3000/api/"]
    end

    subgraph HostBackend ["Backend Local FoodJet (PC Host)"]
        ExpressRouter["Express API Router\n(/api/auth, /api/products, /api/orders)"]
        AuthMid["authMiddleware\n(Verificación Bearer JWT)"]
        Controllers["Controladores\n(authController, productController, orderController)"]
        PrismaORM["Prisma Client ORM"]
        PostgresDB[("PostgreSQL Database (Docker)\nUsuarios, Productos, Pedidos Reales")]
    end

    UI --> VM
    VM --> Repositories
    ProductRepo --> RoomDB
    ProductRepo --> RetrofitAPI
    UserRepo --> DataStore
    UserRepo --> RetrofitAPI
    OrderRepo --> RetrofitAPI

    DataStore -. Lee Token .-> AuthInterceptor
    AuthInterceptor --> OkHttpClient
    OkHttpClient --> RetrofitAPI

    RetrofitAPI -. "Petición HTTP JSON" .-> BaseUrl
    BaseUrl -. "Atraviesa NAT / Puente de Red" .-> ExpressRouter

    ExpressRouter --> AuthMid
    AuthMid --> Controllers
    Controllers --> PrismaORM
    PrismaORM --> PostgresDB
    PrismaORM -. "Retorna Datos Reales" .-> Controllers
    Controllers -. "JSON Response (snake_case)" .-> RetrofitAPI
    RetrofitAPI -. "DTO Mapper" .-> ProductRepo & UserRepo & OrderRepo
```

