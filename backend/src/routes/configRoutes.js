const express = require('express');
const { resolveGoogleMapsKeys } = require('../domain/mapsConfig');
const router = express.Router();

/**
 * Configuración pública que necesita el navegador.
 *
 * Las keys de Google Maps viven en .env y no en el código: así cada entorno usa
 * las suyas y no quedan en el historial del repositorio. Entregarlas aquí no las
 * expone más de lo que ya está: Google exige cargarlas en el navegador, y lo
 * que las protege es la restricción por dominio configurada en Google Cloud.
 *
 * Se entrega tanto googleMapsApiKey (compatibilidad hacia atrás) como el
 * arreglo en orden de prioridad googleMapsApiKeys (cascada doble).
 */
router.get('/', (req, res) => {
    res.json(resolveGoogleMapsKeys(process.env));
});

module.exports = router;
