/**
 * Validación de los datos de registro.
 *
 * Estaba dentro de authController.js, un módulo que abre una conexión a la
 * base de datos al importarse. Extraerla permite probarla tal cual.
 */

/**
 * @param {Object} data Cuerpo de la petición de registro
 * @returns {{isValid: boolean, status?: number, error?: string}}
 */
function validateRegistrationData(data) {
    const { nombre, email, telefono, password } = data || {};

    if (!nombre || !email || !telefono || !password) {
        return { isValid: false, status: 400, error: 'Todos los campos son obligatorios.' };
    }

    if (!/^\d{9}$/.test(telefono)) {
        return { isValid: false, status: 400, error: 'El teléfono debe tener exactamente 9 dígitos.' };
    }

    return { isValid: true };
}

module.exports = { validateRegistrationData };
