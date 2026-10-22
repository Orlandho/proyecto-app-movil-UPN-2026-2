# Instrucciones de Prisma (MySQL)

Este documento explica los comandos básicos de Prisma para el backend de FoodJet configurado con **MySQL**.

**Nota importante:** Todos los comandos de Prisma deben ejecutarse siempre dentro de la carpeta `backend` (donde se encuentran `package.json` y la carpeta `prisma`).

## 1. Generar el cliente de Prisma

```bash
npx prisma generate
```

**¿Cuándo ejecutarlo?**
- Inmediatamente después de clonar el proyecto y ejecutar `npm install`.
- Cada vez que hagas alguna modificación en tu archivo `prisma/schema.prisma`.
- Si encuentras el error `Cannot find module '.prisma/client'` al intentar iniciar el servidor.

**¿Qué hace?**
Lee la estructura de base de datos definida en `schema.prisma` y genera el cliente tipado para MySQL en `node_modules/@prisma/client`.

## 2. Aplicar cambios a la base de datos (Migraciones)

```bash
npx prisma migrate dev --name describe_el_cambio
```

**¿Cuándo ejecutarlo?**
- Cuando agregas, modificas o eliminas modelos (tablas) o campos en tu archivo `schema.prisma` y necesitas que tu base de datos **MySQL** se actualice guardando el historial de migración.

## 3. Empujar el esquema a la base de datos (Sincronización directa)

```bash
npx prisma db push
```

**¿Cuándo ejecutarlo?**
- Para sincronizar de forma inmediata la estructura en MySQL sin crear nuevos archivos de migración.

## 4. Sembrar datos de prueba (Seed)

```bash
node prisma/seed.js
```

**¿Qué hace?**
Puebla la base de datos MySQL con usuarios de prueba (Admin, Clientes), restaurantes, categorías, productos, direcciones y cupones.

## 5. Visualizar los datos (Prisma Studio)

```bash
npx prisma studio
```

**¿Cuándo ejecutarlo?**
- Cuando quieras abrir una interfaz web interactiva en el navegador (`http://localhost:5555`) para inspeccionar y gestionar las tablas en MySQL.
