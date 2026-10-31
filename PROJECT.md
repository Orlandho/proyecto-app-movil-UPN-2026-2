# Project: FoodJet Mobile UI Integral Update

## Architecture
- **Platform**: Android (Kotlin 2.1, Jetpack Compose, Material 3, Android Gradle Plugin 8.9, JVM Toolchain 21)
- **Module boundaries**: `app-movil/app/src/main/java/com/example/foodjeetapp/ui/`
  - `ui/theme/`: Centralized design tokens (color palette, typography, light/dark themes).
  - `ui/components/`: Reusable Compose components (`FoodJetUi`, `FoodJetTopBar`, `FoodJetBottomBar`, `HeroCarousel`, `FilterSection`, `ProductCard`, `FeatureBadgesRow`, `StarRatingBar`, `MapaFoodJet`).
  - `ui/screens/`: Screen composables (`HomeScreen`, `CartSheet`, `CheckoutScreen`, `TrackingScreen`, `FavoritesScreen`, `OrderHistoryScreen`, `AdminDashboardScreen`, `Modals`).
  - `ui/FoodJetApp.kt`: Application navigation shell, backstack management, and modal lifecycle integration.
  - `ui/viewmodel/`: Presentation and state management (`HomeViewModel`, `CartViewModel`, `OrderViewModel`, `AuthViewModel`) - preserved as single source of truth.
- **Shared Interfaces & Contracts**:
  - UI strictly observes ViewModels via `collectAsStateWithLifecycle()` and triggers actions via lambdas.
  - No changes permitted to Gradle configuration, network layer, DTOs, database schemas, or backend services.

## Code Layout
```
app-movil/app/src/main/java/com/example/foodjeetapp/ui/
├── theme/
│   ├── Color.kt              (FoodJet brand palette & accessible token mappings)
│   ├── Theme.kt              (LightColorScheme & DarkColorScheme with WCAG AA compliance)
│   └── Type.kt               (Typography definitions)
├── components/
│   ├── FoodJetUi.kt          (Shared headers: FoodJetSectionHeader, FoodJetFooter) [NEW]
│   ├── FoodJetTopBar.kt      (Top bar with dynamic address fallback, brand logo, action buttons)
│   ├── FoodJetBottomBar.kt   (4-tab bottom navigation with active indicators & badges)
│   ├── HeroCarousel.kt       (Promotional hero carousel with indicators)
│   ├── FilterSection.kt      (Category and price filter pills)
│   ├── ProductCard.kt        (Product item card with student pricing, add button, no fake ratings)
│   ├── FeatureBadgesRow.kt   (Value proposition badge row)
│   ├── StarRatingBar.kt      (Review rating bar)
│   └── MapaFoodJet.kt        (Interactive WebView map with schematic canvas fallback)
├── screens/
│   ├── HomeScreen.kt         (Responsive 2-column product grid, hero, search, categories)
│   ├── CartSheet.kt          (Sliding bottom sheet, item counters, subtotal, single restaurant)
│   ├── CheckoutScreen.kt     (Address selector, map pin, payment methods, coupons, submit)
│   ├── TrackingScreen.kt     (Live map, status timeline, delivery info, cancel order, repartoEnVivo)
│   ├── FavoritesScreen.kt    (Favorites list with empty state)
│   ├── OrderHistoryScreen.kt (Order history cards, status pills, tracking & review actions)
│   ├── AdminDashboardScreen.kt (Metrics overview, status update controls, sales charts)
│   └── Modals.kt             (LoginDialog, RegisterDialog, QrPaymentDialog, ReviewDialog, MiCuentaDialog)
└── FoodJetApp.kt             (Navigation host, backstack preservation, modal integration)
```

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Centralized Theme Tokens | FoodJet palette (`#F4511E`, `#D94310`, `#FCF0E4`, etc.) and contrast | M1 | Survey (R1) |
| 2 | Accessible Color Schemes | WCAG AA compliant onPrimary, light & dark theme support | M1 | Survey (R1) |
| 3 | Shared UI Elements | FoodJetFooter and FoodJetSectionHeader in FoodJetUi.kt | M1 | Survey (R3) |
| 4 | FoodJetTopBar Header | Brand logo, dynamic address fallback ("Ingresa tu dirección"), badges | M2 | Survey (R2) |
| 5 | FoodJetBottomBar Navigation | 4 main destinations (Inicio, Explorar, Pedidos, Perfil) with badges | M2 | Survey (R2) |
| 6 | Hero Carousel & Banners | Promotional banners with auto-paging and indicators | M2 | Survey (R2) |
| 7 | Category Filters | Horizontal filter chips with selected states and clear action | M2 | Survey (R2) |
| 8 | Product Cards Grid | Responsive 2-column grid, student discount, add button, no fake stars | M2 | Survey (R2) |
| 9 | HomeScreen Experience | "Tu antojo. Sin escalas.", search box, favorites section, sections | M2 | Survey (R2) |
| 10 | Cart Management (CartSheet) | Slide-up modal, item steppers, subtotal, restaurant check, checkout CTA | M3 | Survey (R2) |
| 11 | Checkout Form & Delivery | Address picker, GPS button, reference text, "A un paso del antojo." | M3 | Survey (R2) |
| 12 | Payment & Coupon Logic | Cash, Card, Wallet/QR, coupon verification (`PruebaCupon`, `FOODJET20`) | M3 | Survey (R2) |
| 13 | Order Submission Protection | Financial breakdown (18% IGV, S/ 5 shipping), submit lock (`isSubmitting`) | M3 | Survey (R2) |
| 14 | Live Tracking & Telemetry | "¡Tu antojo va volando!", live map, timeline, ETA, `repartoEnVivo` preserved | M4 | Survey (R2) |
| 15 | Order Cancellation | Cancellation allowed only on eligible states (`order.estado.isCancelable`) | M4 | Survey (R2) |
| 16 | Favorites Screen | Saved favorite items list, quick add to cart, empty state | M4 | Survey (R3) |
| 17 | Order History Screen | Historical orders list, status badges, tracking button, review modal trigger | M4 | Survey (R3) |
| 18 | Admin Dashboard Screen | Live order advancement, daily/weekly charts, restaurant metrics | M4 | Survey (R3) |
| 19 | Modals & Dialog Integrity | Reused LoginDialog, RegisterDialog, QrPaymentDialog, ReviewDialog, MiCuenta | M4 | Survey (R3) |
| 20 | State Views (Loading/Error/Empty) | Standardized LoadingView, ErrorView with retry, and EmptyView | M4 | Survey (R3) |
| 21 | Compose Previews Coverage | Full light/dark previews across all components and screens | M1-M4 | Survey (R5) |
| 22 | 100% E2E Test Suite Pass | Opaque-box requirements test suite (Tiers 1-4) passed 100% | M5 | Survey (R5) |
| 23 | Adversarial Coverage Hardening | White-box edge cases (Tier 5) verified by Challengers | M5 | Survey (R5) |
| 24 | Visual Evidence Capture | Screenshots on AVD Pixel_7 (light/dark mode, 4 core flows) | M5 | Survey (R5) |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Design Tokens & Base Components | `ui/theme/` (Color, Theme, Type), `ui/components/FoodJetUi.kt`, base Previews | None | PLANNED |
| M2 | Navigation Shell & Catalog Presentation | `FoodJetTopBar`, `FoodJetBottomBar`, `HeroCarousel`, `FilterSection`, `ProductCard`, `HomeScreen`, `FoodJetApp` | M1 | PLANNED |
| M3 | Transactional Flows (Cart & Checkout) | `CartSheet`, `CheckoutScreen` (coupons, payments, addresses, IGV) | M1, M2 | PLANNED |
| M4 | Order Fulfillment, Complementary Screens & Modals | `TrackingScreen` (`repartoEnVivo`), `FavoritesScreen`, `OrderHistoryScreen`, `AdminDashboardScreen`, `Modals.kt` | M1, M2 | PLANNED |
| M5 | Final Milestone (E2E Pass & Visual Evidence) | Pass 100% E2E test suite (Tiers 1-4), Tier 5 hardening, AVD Pixel_7 evidence | M1, M2, M3, M4, TEST_READY.md | PLANNED |

## Interface Contracts
### `ui/theme/` ↔ All Screens & Components
- `MaterialTheme.colorScheme.primary` resolves to `FoodJetPrimary` (`#F4511E`).
- `MaterialTheme.colorScheme.onPrimary` resolves to `Color.White` with contrast ratio > 4.5:1.
- `MaterialTheme.colorScheme.surface` and `background` adapt dynamically to light/dark themes.
- Custom tokens exposed via `FoodJetColors` or constants in `Color.kt` (`FoodJetPrimaryLight`, `FoodJetDark`, `FoodJetBorder`, etc.).

### `FoodJetUi.kt` ↔ Screen Composables
- `FoodJetSectionHeader(icon: ImageVector, title: String, modifier: Modifier = Modifier)`
- `FoodJetFooter(modifier: Modifier = Modifier)`

### `FoodJetTopBar.kt` ↔ `FoodJetApp.kt` & `HomeScreen.kt`
- Signature:
  ```kotlin
  @Composable
  fun FoodJetTopBar(
      cartCount: Int,
      favoritesCount: Int,
      isLoggedIn: Boolean,
      userName: String?,
      onLogoClick: () -> Unit,
      onCartClick: () -> Unit,
      onFavoritesClick: () -> Unit,
      onProfileClick: () -> Unit,
      modifier: Modifier = Modifier,
      address: String = "Ingresa tu dirección"
  )
  ```
- Guarantee: Default address MUST be neutral ("Ingresa tu dirección"), NEVER a hardcoded fake location.

### `TrackingScreen.kt` ↔ Unit Tests (`RepartoEnVivoTest.kt`)
- `repartoEnVivo(order: Order, ahoraServidorMs: Long): DatosReparto` MUST remain accessible and preserve exact return values and interpolation mathematics.

### `Modals.kt` ↔ `FoodJetApp.kt`
- `LoginDialog`, `RegisterDialog`, `QrPaymentDialog`, `ReviewDialog`, `MiCuentaDialog`, and `SelectorServidor` must retain all exact parameters and callbacks.

## Baseline Quality Metrics
- Git commit: `38818c7` on `feat/UI-update`
- `:app:assembleDebug`: SUCCESSFUL (1m 45s), APK generated.
- `:app:testDebugUnitTest`: 61/61 PASSED (100%), 0 failures, 0 skipped across 12 test classes.
- `:app:lintDebug`: 0 errors, 58 pre-existing warnings.
- Execution Environment: AVD `Pixel_7` (Android 37.1 x86_64) booted & connected via ADB as `emulator-5554`.
