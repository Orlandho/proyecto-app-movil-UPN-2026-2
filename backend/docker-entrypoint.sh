#!/bin/sh
set -e

# El cliente generado vive en node_modules, dentro de la imagen, y el bind
# mount solo trae src y prisma: sin esto, un cambio de esquema no llega al
# contenedor en marcha y las consultas fallan por columnas desconocidas.
echo "⏳ Generando cliente de Prisma..."
npx prisma generate

echo "⏳ Aplicando migraciones..."
npx prisma migrate deploy

if [ "$RUN_SEED" = "true" ]; then
    echo "🌱 Ejecutando seed..."
    npx prisma db seed
fi

echo "🚀 Arrancando API..."
exec npx nodemon src/index.js
