/**
 * Validación de un par latitud/longitud.
 *
 * La comparten los locales y las direcciones de entrega: las dos puntas de la
 * ruta del reparto. Módulo puro, sin red ni base de datos.
 */

const viene = (v) => v !== undefined && v !== null && v !== '';

/**
 * Las coordenadas son opcionales, pero si vienen deben ser válidas y venir
 * las dos: media coordenada no ubica nada.
 * @returns {{valid: boolean, error?: string}}
 */
function validarCoordenadas(latitud, longitud) {
    const tieneLat = viene(latitud);
    const tieneLon = viene(longitud);

    if (tieneLat !== tieneLon) {
        return { valid: false, error: 'Indica latitud y longitud, o ninguna de las dos.' };
    }

    if (tieneLat) {
        const lat = Number(latitud);
        const lon = Number(longitud);

        if (Number.isNaN(lat) || lat < -90 || lat > 90) {
            return { valid: false, error: 'La latitud debe estar entre -90 y 90.' };
        }
        if (Number.isNaN(lon) || lon < -180 || lon > 180) {
            return { valid: false, error: 'La longitud debe estar entre -180 y 180.' };
        }
    }

    return { valid: true };
}

/** Convierte el valor recibido a número, o a null si no vino. */
function numeroONulo(v) {
    return viene(v) ? Number(v) : null;
}

module.exports = { validarCoordenadas, numeroONulo };
