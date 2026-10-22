FROM node:20-alpine

# Prisma necesita openssl: sin él el motor de consultas falla al arrancar con
# un error críptico sobre bibliotecas compartidas que no menciona openssl.
RUN apk add --no-cache openssl

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY prisma ./prisma
RUN npx prisma generate

COPY . .

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/docker-entrypoint.sh && chmod +x /usr/local/bin/docker-entrypoint.sh

# El servicio corre como uid 1000 para que los archivos que genere Prisma en el
# bind mount (prisma/migrations/) pertenezcan al usuario del host, no a root.
RUN chown -R node:node /app

EXPOSE 3000

ENTRYPOINT ["docker-entrypoint.sh"]
