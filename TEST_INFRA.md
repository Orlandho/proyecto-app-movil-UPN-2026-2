# TEST_INFRA.md — FoodJet Mobile Test Infrastructure & E2E Architecture

## 1. Test Philosophy & Principles

The FoodJet Mobile Test Infrastructure is designed around **opaque-box, requirement-driven verification**, derived strictly from user specifications and contracts (`ORIGINAL_REQUEST.md`, `PROJECT.md`, `AGENTS.md`) rather than internal implementation details.

### Core Principles
1. **Opaque-Box Independence**: Tests exercise public APIs, domain models, ViewModels, calculation contracts, and state transformations without depending on private member variables or fragile UI hierarchy bindings.
2. **Authoritative Output Derivation**: Every test assertion is derived from documented business specifications:
   - 18% IGV applied to taxable subtotal (post-discounts).
   - S/ 5.00 fixed delivery fee across all Lima delivery routes.
   - Peruvian Soles (S/) currency formatting.
   - Exact coupon discounts: `PruebaCupon` (10%), `FOODJET20` (20%).
   - Student discount benefit applied exclusively to eligible university users.
   - Order cancellation permitted strictly during `PENDIENTE` and `CONFIRMADO` phases.
3. **Deterministic & Isolated Execution**: Every test manages its own state, executes independently on JVM test runners, and leaves zero persistent side effects.
4. **Zero Flakiness**: All timing, coordinates, and telemetry computations utilize deterministic mocks or fixed timestamps (`Instant.parse()`).

---

## 2. Feature Inventory Mapping (24 Features Across Tiers 1–4)

| # | Feature | Tier 1 (Coverage >=5) | Tier 2 (Boundary >=5) | Tier 3 (Cross-Feature Pairwise) | Tier 4 (E2E User Journeys) |
|---|---------|----------------------|-----------------------|---------------------------------|---------------------------|
| 1 | Centralized Theme Tokens | Primary palette, containers, text & borders | Contrast ratios, dark mode alpha, missing tokens | F1 ↔ F2 (Token resolution in Dark Theme) | J1: Theme Switching Journey |
| 2 | Accessible Color Schemes | WCAG AA compliance, light & dark schemes | Min contrast ratios, high-contrast fallbacks | F2 ↔ F3 (Contrast in Section Headers) | J1: Accessibility Compliance |
| 3 | Shared UI Elements | FoodJetFooter & FoodJetSectionHeader | Long titles, empty texts, extreme paddings | F3 ↔ F9 (Header on Home sections) | J2: Catalog Browsing Journey |
| 4 | FoodJetTopBar Header | Badges, actions, neutral address fallback | 0 count badges, extreme badge numbers (>99) | F4 ↔ F10 (Cart badge sync with CartSheet) | J3: Order Checkout Journey |
| 5 | FoodJetBottomBar Navigation | 4 destinations (Inicio, Explorar, Pedidos, Perfil) | Active indicators, invalid routes, double taps | F5 ↔ F17 (BottomBar to Order History) | J4: Post-Order Tracking Journey |
| 6 | Hero Carousel & Banners | Slides collection, auto-paging, indicators | Single slide, empty slide list, long titles | F6 ↔ F9 (Banner tap navigation to Menu) | J2: Catalog Browsing Journey |
| 7 | Category Filters | Category pills, selection toggles, reset | Multi-category selection, non-matching filters | F7 ↔ F8 (Filters applied to Product Grid) | J2: Search & Filter Journey |
| 8 | Product Cards Grid | 2-column layout, image URL, student discount | Price = 0, discount = 0, discount = 100% | F8 ↔ F10 (Card "+" click to CartItem) | J3: Order Checkout Journey |
| 9 | HomeScreen Experience | "Tu antojo. Sin escalas.", search, favorites | Empty search results, unicode search queries | F9 ↔ F16 (Home favorites sync) | J2: Catalog Browsing Journey |
| 10 | Cart Management (CartSheet) | Add, increase, decrease, remove, clear | Decrease quantity 1 to 0 (auto-removal) | F10 ↔ F13 (Cart pricing into Checkout) | J3: Order Checkout Journey |
| 11 | Checkout Form & Delivery | Address input, GPS button, reference field | Address < 5 chars, Phone != 9 digits | F11 ↔ F12 (SavedAddress to Payment) | J3: Order Checkout Journey |
| 12 | Payment & Coupon Logic | Cash, Card, QR Wallet, `PruebaCupon`, `FOODJET20` | Invalid coupon, expired coupon, lowercase/uppercase | F12 ↔ F13 (Coupon applied to Taxable IGV) | J3: Order Checkout Journey |
| 13 | Order Submission Protection | Subtotal, 18% IGV, S/ 5 shipping, submit lock | `isSubmitting` = true blocks double-click | F13 ↔ F14 (Created order to Tracking) | J3: Order Checkout Journey |
| 14 | Live Tracking & Telemetry | Live map, timeline, ETA, `repartoEnVivo` | Server clock skew, 0 distance, arrival time | F14 ↔ F15 (Tracking status to Cancel button) | J4: Post-Order Tracking Journey |
| 15 | Order Cancellation | Cancel allowed in `PENDIENTE` & `CONFIRMADO` | Rejection in `EN_PREPARACION`, `EN_CAMINO`, `ENTREGADO` | F15 ↔ F17 (Cancelled badge in History) | J5: Order Cancellation Journey |
| 16 | Favorites Screen | Favorite items list, quick-add, toggle | Empty favorites state, toggling all items off | F16 ↔ F10 (Add from Favorites to Cart) | J6: Favorites Management Journey |
| 17 | Order History Screen | Historical cards, status badges, tracking CTA | Empty history, legacy order ID formats ("FJ-XX") | F17 ↔ F14 (History card to Tracking) | J7: Re-Order & Review Journey |
| 18 | Admin Dashboard Screen | Live order progression, daily/weekly charts | Zero orders state, large volume aggregation | F18 ↔ F15 (Admin status vs User Cancel) | J8: Admin Operations Journey |
| 19 | Modals & Dialog Integrity | LoginDialog, RegisterDialog, QrPaymentDialog | Form errors, dialog dismiss, blank credentials | F19 ↔ F11 (Login required dialog on Checkout) | J9: Authentication Flow Journey |
| 20 | State Views (Loading/Error/Empty) | LoadingView, ErrorView with retry, EmptyView | Error with retry lambda, network timeout states | F20 ↔ F9 (UiState Error on HomeScreen) | J10: Offline & Error Recovery |
| 21 | Compose Previews Coverage | Full light/dark previews across all screens | Font scaling 200%, narrow width (360dp) | F21 ↔ F1 (Theme preview fidelity) | J11: UI Responsiveness Journey |
| 22 | 100% E2E Test Suite Pass | Opaque-box requirements test suite pass rate | Edge matrix combinations, strict assertions | F22 ↔ All Features (Regression Suite) | J12: Full Quality Gate Verification |
| 23 | Adversarial Coverage Hardening | White-box edge cases, parameter poisoning | SQL-injection strings in address, negative prices | F23 ↔ F11 (Adversarial inputs in Checkout) | J12: Security & Integrity Gate |
| 24 | Visual Evidence Capture | AVD Pixel_7 flow verification, light/dark mode | Display orientation, high DPI assets | F24 ↔ F21 (Preview & Render Parity) | J1: Visual Evidence Audit |

---

## 3. Test Methodology & Minimum Test Threshold

### Multi-Tiered Architecture
- **Tier 1: Feature Coverage (>=5 test cases per feature)**
  - Direct functional requirements validation.
  - 24 features × 5 test cases = **120 test cases**.
- **Tier 2: Boundary & Corner Cases (>=5 test cases per feature)**
  - Extreme values, zero amounts, character limits, invalid formats, null/empty collections.
  - 24 features × 5 test cases = **120 test cases**.
- **Tier 3: Cross-Feature Interactions (Pairwise combinations)**
  - Co-dependent modules (Cart ↔ Pricing, Auth ↔ Checkout, Tracking ↔ Cancellation).
  - 24 features mapped to pairwise interactions = **24 test cases**.
- **Tier 4: Real-World Application Scenarios (End-to-end user journeys)**
  - Multi-step customer and admin journeys.
  - max(5, 24 / 2) = **12 test cases**.

### Test Count Mathematical Threshold
$$\text{Threshold} = 11 \times N + \max\left(5, \left\lfloor\frac{N}{2}\right\rfloor\right)$$
$$\text{For } N = 24: \quad 11 \times 24 + \max(5, 12) = 264 + 12 = \mathbf{276 \text{ test cases}}$$

Together with the pre-existing baseline of **61 tests**, the complete suite delivers **337+ automated tests**.

---

## 4. Test Suite Implementation Structure

Test files are housed in `app-movil/app/src/test/java/com/example/foodjeetapp/ui/e2e/`:

```
app-movil/app/src/test/java/com/example/foodjeetapp/ui/e2e/
├── Tier1FeatureCoveragePart1Test.kt     (Features 1–12: 60 test cases)
├── Tier1FeatureCoveragePart2Test.kt     (Features 13–24: 60 test cases)
├── Tier2BoundaryAndCornerTest.kt        (Features 1–24: 120 test cases)
├── Tier3CrossFeatureInteractionTest.kt  (Pairwise Interactions: 24 test cases)
└── Tier4RealWorldScenariosTest.kt       (Full User Journeys: 12 test cases)
```

---

## 5. Execution & Verification Command

Execute the complete test suite directly from the `app-movil` directory via the Gradle wrapper:

```powershell
cd app-movil
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

To run specifically the E2E test suite:
```powershell
cd app-movil
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.foodjeetapp.ui.e2e.*"
```
