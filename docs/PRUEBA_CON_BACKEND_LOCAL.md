# Probar la app con el backend de FoodJet

La app consume la API REST de FoodJet para todo su funcionamiento (catálogo, pedidos, direcciones, seguimiento y panel de administración).

Puedes alternar entre los siguientes servidores según tu entorno de trabajo:

| Servidor (en la app) | URL de la API | Cuándo usarlo |
|---|---|---|
| **Backend MySQL · Emulador (3000)** *(Predeterminado)* | `http://10.0.2.2:3000/api/` | Backend Node.js de este repo (`backend/`) corriendo en tu PC con MySQL, visto desde el emulador de Android. |
| **Backend MySQL · Celular USB (3000)** | `http://localhost:3000/api/` | Backend Node.js de este repo (`backend/`) con MySQL en tu PC y la app en un celular físico por USB con `adb reverse tcp:3000 tcp:3000`. |
| **Producción** | `https://foodjet.asen.pe/api/` | Servidor remoto oficial de FoodJet a través del túnel de Cloudflare. |
| **Docker Caddy · Emulador (8081)** | `http://10.0.2.2:8081/api/` | Contenedores Docker de `Pagina-Foodjet` desde el emulador. |
| **Docker Caddy · Celular por USB (8081)** | `http://localhost:8081/api/` | Contenedores Docker de `Pagina-Foodjet` por cable USB con `adb reverse tcp:8081 tcp:8081`. |

> [!NOTE]
> Para cambiar de servidor: Toca el selector **Servidor** visible en el diálogo de **Iniciar Sesión** o en **Mi Cuenta**. Cambiar de servidor cierra la sesión automáticamente, ya que los tokens JWT pertenecen al servidor respectivo.

---

## 1. Levantar el Backend Node.js con MySQL en tu PC

En una terminal PowerShell o CMD:

```powershell
cd "C:\Users\Orlando\Desktop\proyecto app movil UPN 2026-2\backend"
npm run dev
```

*Verifica que responda:*
```powershell
curl http://localhost:3000/api/products
```

**Credenciales de prueba sembradas en MySQL:**
- Cliente: `cliente1@mail.com` / `Cliente123!` (estudiante verificado, aplica descuentos).
- Cliente 2: `cliente2@mail.com` / `Cliente123!` (cuenta regular).
- Administrador: `admin@foodjet.com` / `Admin123!` (acceso al Panel de Operaciones).

---

## 2. Conectar la App Móvil

### Desde el Emulador de Android Studio:
1. Abre el proyecto Android ubicado en `app-movil`.
2. Inicia el emulador y ejecuta la app.
3. El servidor predeterminado es **Backend MySQL · Emulador (3000)** (`10.0.2.2:3000`).
4. Inicia sesión con cualquiera de las cuentas de prueba.

### Desde un Celular Físico por USB:
1. Conecta el celular con Depuración USB habilitada.
2. Ejecuta en tu PC:
   ```powershell
   adb reverse tcp:3000 tcp:3000
   ```
3. En la app (en Iniciar Sesión o Mi Cuenta), selecciona **Backend MySQL · Celular USB (3000)**.
4. Inicia sesión.
