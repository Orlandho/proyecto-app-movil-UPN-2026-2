# Probar la app con el backend de FoodJet

La app consume la API REST de FoodJet para todo su funcionamiento (catálogo, pedidos, direcciones, seguimiento y panel de administración).

La única API permitida es la de `backend/` en este repositorio, ejecutándose en el puerto 3000. La app tiene dos rutas de acceso según el dispositivo; ninguna conecta con producción, Docker/Caddy ni un segundo backend.

| Servidor (en la app) | URL de la API | Cuándo usarlo |
|---|---|---|
| **Backend MySQL · Emulador (3000)** *(Predeterminado)* | `http://10.0.2.2:3000/api/` | Backend Node.js de este repo (`backend/`) corriendo en tu PC con MySQL, visto desde el emulador de Android. |
| **Backend MySQL · Celular USB (3000)** | `http://localhost:3000/api/` | Backend Node.js de este repo (`backend/`) con MySQL en tu PC y la app en un celular físico por USB con `adb reverse tcp:3000 tcp:3000`. |

> [!NOTE]
> Para seleccionar entre **Emulador** y **Celular USB** usa **Conexión local** en **Iniciar Sesión** o **Mi Cuenta**. Ambas opciones acceden al mismo backend Node.js de este repositorio; cambiar de modo conserva la sesión porque las dos rutas usan el mismo backend. Si la preferencia antigua apuntaba a otro backend, la migración descarta su JWT y restablece el modo local predeterminado.

---

## 1. Levantar el Backend Node.js con MySQL en tu PC

### Paso A: Asegurar que el servicio de MySQL esté corriendo
En Windows PowerShell:
```powershell
Get-Service MySQL80
# Si no está en ejecución, inícialo con:
Start-Service MySQL80
```

### Paso B: Verificar que el puerto 3000 esté libre
Si anteriormente levantaste contenedores Docker de FoodJet, asegúrate de detener el contenedor que usa el puerto 3000 para evitar conflictos `EADDRINUSE`:
```powershell
docker stop foodjet-api
```

### Paso C: Iniciar el servidor Express en modo desarrollo
En una terminal PowerShell o CMD:

```powershell
cd backend
npm run dev
```

*Verifica que responda:*
```powershell
curl http://localhost:3000/api/products
```

> [!TIP]
> Si alguna vez necesitas re-sincronizar las tablas o re-poblar los datos de prueba, ejecuta en `backend/`:
> `npx prisma db push` y luego `npm run seed`.
> Para más detalles de configuración avanzada y visualización con Prisma Studio, consulta [`backend/README.md`](../backend/README.md).

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
3. En la app (en Iniciar Sesión o Mi Cuenta), selecciona **Backend del repositorio · Celular USB**.
4. Inicia sesión.
