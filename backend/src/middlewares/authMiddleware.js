const jwt = require('jsonwebtoken');

const authMiddleware = (req, res, next) => {
    // 1. SOLUCIÓN: Dejar pasar las peticiones previas de seguridad (CORS Preflight)
    if (req.method === 'OPTIONS') {
        return next();
    }

    // 2. Obtener el token del header (formato: "Bearer [token]")
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ error: 'Acceso denegado. Token no proporcionado.' });
    }

    const token = authHeader.split(' ')[1];

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded; // ej: { userId: 1, rol: 'cliente' }
        next();
    } catch (error) {
        return res.status(403).json({ error: 'Token inválido o expirado.' });
    }
};

const adminMiddleware = (req, res, next) => {
    // Dejar pasar OPTIONS también para rutas de administrador
    if (req.method === 'OPTIONS') {
        return next();
    }

    if (!req.user || req.user.rol !== 'admin') {
        return res.status(403).json({ error: 'Acceso denegado. Se requieren permisos de administrador.' });
    }
    next();
};

/**
 * Restringe una ruta a los roles indicados.
 *
 * Compara en minúsculas a propósito: el rol convive en tres grafías según de
 * dónde venga el usuario ('Cliente' por el default del esquema, 'cliente' por
 * el registro, 'Administrador' por el script SQL heredado).
 */
const requireRole = (...roles) => (req, res, next) => {
    if (req.method === 'OPTIONS') {
        return next();
    }

    const rol = String(req.user?.rol || '').toLowerCase();
    const permitidos = roles.map((r) => String(r).toLowerCase());

    if (!permitidos.includes(rol)) {
        return res.status(403).json({ error: 'Acceso denegado. No tienes permisos para esta operación.' });
    }
    next();
};

module.exports = { authMiddleware, adminMiddleware, requireRole };

