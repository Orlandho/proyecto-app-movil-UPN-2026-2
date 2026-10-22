/**
 * Reglas de configuración de claves de mapas (Google Maps).
 *
 * Módulo puro, sin dependencias de Express ni base de datos.
 */

/**
 * Resuelve la cascada de claves de Google Maps a partir de variables de entorno:
 * 1. Clave primaria (GOOGLE_MAPS_API_KEY)
 * 2. Clave secundaria de respaldo (GOOGLE_MAPS_API_KEY_SECUNDARIA)
 *
 * Elimina duplicados, limpia espacios y descarta valores vacíos o nulos.
 *
 * @param {Record<string, string|undefined>} [env=process.env]
 * @returns {{ googleMapsApiKey: string|null, googleMapsApiKeys: string[] }}
 */
function resolveGoogleMapsKeys(env = process.env) {
    const rawKeys = [
        env?.GOOGLE_MAPS_API_KEY,
        env?.GOOGLE_MAPS_API_KEY_SECUNDARIA
    ];

    const keys = [...new Set(rawKeys.map((k) => String(k ?? '').trim()).filter(Boolean))];

    return {
        googleMapsApiKey: keys[0] || null,
        googleMapsApiKeys: keys
    };
}

module.exports = {
    resolveGoogleMapsKeys
};
