/**
 * Restablece la contraseña de un usuario.
 *
 *   docker compose exec backend node src/scripts/reset-password.js <email> [nueva]
 *
 * Si no se indica contraseña, genera una aleatoria y la muestra.
 *
 * Las contraseñas se guardan cifradas con bcrypt: un UPDATE directo por SQL
 * con el texto plano dejaría al usuario sin poder iniciar sesión, porque
 * bcrypt.compare nunca daría positivo. Este script aplica el mismo cifrado
 * que usa el registro (authController), así que el resultado es válido.
 */
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const prisma = require('../config/db');

const LONGITUD_MINIMA = 8;

function generarPassword() {
    // base64url sobre 12 bytes: 16 caracteres sin ambigüedades de escapado.
    return crypto.randomBytes(12).toString('base64url');
}

async function main() {
    const [email, passwordIndicada] = process.argv.slice(2);

    if (!email) {
        console.error('Uso: node src/scripts/reset-password.js <email> [nueva contraseña]');
        process.exit(1);
    }

    const usuario = await prisma.user.findUnique({ where: { email } });

    if (!usuario) {
        console.error(`✖ No existe ningún usuario con el correo ${email}`);
        const existentes = await prisma.user.findMany({ select: { email: true }, orderBy: { id: 'asc' } });
        console.error('  Correos registrados:', existentes.map((u) => u.email).join(', '));
        process.exit(1);
    }

    const password = passwordIndicada || generarPassword();

    if (password.length < LONGITUD_MINIMA) {
        console.error(`✖ La contraseña debe tener al menos ${LONGITUD_MINIMA} caracteres.`);
        process.exit(1);
    }

    const salt = await bcrypt.genSalt(10);
    const hash = await bcrypt.hash(password, salt);

    await prisma.user.update({ where: { id: usuario.id }, data: { password: hash } });

    console.log(`✔ Contraseña restablecida para ${usuario.nombre} <${usuario.email}> (rol: ${usuario.rol})`);

    if (!passwordIndicada) {
        console.log(`  Nueva contraseña: ${password}`);
        console.log('  Anótala: no se puede recuperar, solo volver a restablecerla.');
    }

    // Los tokens emitidos antes siguen siendo válidos hasta que caduquen (24 h):
    // el JWT no guarda la contraseña, así que cambiarla no cierra las sesiones
    // abiertas. Para invalidarlas todas habría que rotar JWT_SECRET.
    console.log('  Nota: las sesiones ya abiertas siguen activas hasta 24 h.');
}

main()
    .catch((e) => {
        console.error('✖ Error al restablecer la contraseña:', e.message);
        process.exit(1);
    })
    .finally(async () => {
        await prisma.$disconnect();
    });
