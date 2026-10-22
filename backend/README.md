# FoodJet Backend (Node.js + Express + Prisma ORM + MySQL)

Este módulo contiene el backend oficial y la API REST del ecosistema **FoodJet**, optimizado para ejecutarse localmente con **MySQL** y ser consumido tanto por la aplicación móvil Android ([`app-movil/`](../app-movil/)) como por la plataforma web.

---

## 📋 Requisitos Previos

1. **Node.js**: Versión 18 o superior.
2. **MySQL Server**: Servicio local activo (ej. `MySQL80` en el puerto `3306`).
3. **npm**: Gestor de paquetes incluido con Node.js.

---

## ⚙️ Configuración Inicial

### 1. Variables de Entorno (`.env`)

Crea o verifica el archivo `.env` en la raíz de `backend/`:

```env
PORT=3000
DATABASE_URL="mysql://root:TU_PASSWORD@localhost:3306/foodjet"
JWT_SECRET="foodjet_super_secret_jwt_key_2026"
```

> [!NOTE]
> En este entorno local de Windows, la contraseña de MySQL configurada es `67.testeable!#`. En la URL de conexión se codifica como:
> `DATABASE_URL="mysql://root:67.testeable!%23@localhost:3306/foodjet"`

### 2. Instalación de Dependencias

```powershell
cd backend
npm install
```

### 3. Generación del Cliente Prisma y Base de Datos

1. Generar el cliente de Prisma para MySQL:
   ```powershell
   npx prisma generate
   ```

2. Sincronizar el esquema con la base de datos MySQL (crea la base `foodjet` y sus tablas si no existen):
   ```powershell
   npx prisma db push
   ```

3. Poblar la base de datos con los datos iniciales y usuarios de prueba:
   ```powershell
   npm run seed
   # o alternativamente:
   node prisma/seed.js
   ```

---

## 🚀 Cómo Iniciar el Backend

### Modo Desarrollo (con reinicio automático al guardar cambios):

```powershell
cd backend
npm run dev
```

### Modo Producción:

```powershell
cd backend
npm start
```

Al iniciar correctamente, verás en la consola:
```text
🚀 Servidor FoodJet ejecutándose en http://localhost:3000
📦 Entorno: development | Base de datos: MySQL
```

---

## 🔍 Verificación Rápida

Puedes comprobar que el backend responde ejecutando en PowerShell:

```powershell
# Obtener catálogo de productos
curl http://localhost:3000/api/products

# Probar endpoint de autenticación (Login)
curl -X POST http://localhost:3000/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"cliente1@mail.com","password":"Cliente123!"}'
```

---

## 👥 Cuentas de Prueba Pre-sembradas

| Rol | Correo Electrónico | Contraseña | Características |
|---|---|---|---|
| **Cliente Estudiante** | `cliente1@mail.com` | `Cliente123!` | Cuenta verificada como estudiante. Aplica 20% de descuento en productos seleccionados. |
| **Cliente Regular** | `cliente2@mail.com` | `Cliente123!` | Cuenta estándar sin beneficios de estudiante. |
| **Administrador** | `admin@foodjet.com` | `Admin123!` | Acceso completo al Panel de Operaciones / Dashboard de Pedidos. |

---

## 🛠️ Herramientas de Gestión y Diagnóstico

### Prisma Studio (Explorador Gráfico de Base de Datos):
Para inspeccionar, editar o consultar las tablas de MySQL en una interfaz web visual:
```powershell
npx prisma studio
```
Abre automáticamente tu navegador en `http://localhost:5555`.

### Servir Recursos Estáticos (Códigos QR de Pago):
El backend sirve automáticamente los códigos QR de Yape y Plin en:
- `http://localhost:3000/public/img/qr/qr-yape.svg`
- `http://localhost:3000/public/img/qr/qr-plin.svg`

---

## ⚠️ Solución de Problemas Frecuentes

### 1. Error: `EADDRINUSE: address already in use :::3000`
Significa que el puerto 3000 está ocupado (por ejemplo, por un contenedor Docker previo de `foodjet-api`).
- **Si usas Docker:**
  ```powershell
  docker stop foodjet-api
  ```
- **Si es otro proceso de Node o Windows:**
  ```powershell
  # Identificar el PID usando el puerto 3000:
  netstat -ano | findstr :3000
  # Detener el proceso por su PID:
  Stop-Process -Id <PID> -Force
  ```

### 2. Error: `Can't reach database server at localhost:3306`
El servicio local de MySQL no está corriendo.
- Inicia el servicio en Windows PowerShell (como Administrador o servicio estándar):
  ```powershell
  Start-Service MySQL80
  # o con net:
  net start MySQL80
  ```
- Verifica su estado:
  ```powershell
  Get-Service MySQL80
  ```
