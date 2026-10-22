/**
 * Cálculo de la ruta que sigue el repartidor.
 *
 * Consulta OSRM, un servicio público de enrutamiento sobre OpenStreetMap, para
 * obtener el trazado real por calles. Si no responde a tiempo, o devuelve algo
 * inesperado, se cae a una línea recta entre los dos puntos: la simulación
 * sigue funcionando y solo pierde el detalle del callejero.
 *
 * La ruta se guarda en el pedido al salir a reparto, así que se pide una sola
 * vez. Una demostración no puede depender de que un servicio externo esté
 * disponible en ese preciso momento.
 *
 * Solo se envían las dos coordenadas; ningún dato del cliente sale de aquí.
 */
const { distanciaMetros, longitudRuta, etaMinutos } = require('../domain/geo');

const OSRM_BASE = process.env.OSRM_URL || 'https://router.project-osrm.org';
const TIMEOUT_MS = Number(process.env.OSRM_TIMEOUT_MS) || 5000;

/** Ruta en línea recta, el respaldo cuando no hay enrutamiento disponible. */
function rutaEnLineaRecta(origen, destino) {
    const ruta = [origen, destino];

    return {
        ruta,
        distancia_metros: longitudRuta(ruta),
        origen: 'linea_recta'
    };
}

/**
 * @param {[number, number]} origen [lat, lon] del local
 * @param {[number, number]} destino [lat, lon] de la entrega
 * @returns {Promise<{ruta: Array, distancia_metros: number, origen: string}>}
 */
async function calcularRuta(origen, destino) {
    if (!Array.isArray(origen) || !Array.isArray(destino)) {
        return rutaEnLineaRecta(origen, destino);
    }

    // OSRM espera lon,lat; el resto del proyecto usa lat,lon.
    const coords = `${origen[1]},${origen[0]};${destino[1]},${destino[0]}`;
    const url = `${OSRM_BASE}/route/v1/driving/${coords}?overview=full&geometries=geojson`;

    const control = new AbortController();
    const corte = setTimeout(() => control.abort(), TIMEOUT_MS);

    try {
        const respuesta = await fetch(url, { signal: control.signal });
        if (!respuesta.ok) throw new Error(`OSRM respondió ${respuesta.status}`);

        const datos = await respuesta.json();
        const tramo = datos?.routes?.[0];
        const puntos = tramo?.geometry?.coordinates;

        if (!Array.isArray(puntos) || puntos.length < 2) {
            throw new Error('OSRM no devolvió una geometría utilizable');
        }

        return {
            ruta: puntos.map(([lon, lat]) => [lat, lon]),
            distancia_metros: tramo.distance ?? longitudRuta(puntos.map(([lon, lat]) => [lat, lon])),
            origen: 'osrm'
        };
    } catch (error) {
        console.warn(`⚠️  Ruta por calles no disponible (${error.message}). Se usa línea recta.`);
        return rutaEnLineaRecta(origen, destino);
    } finally {
        clearTimeout(corte);
    }
}

/** Ruta + tiempo estimado, que es lo que se guarda en el pedido. */
async function planificarReparto(origen, destino, velocidadKmh) {
    const plan = await calcularRuta(origen, destino);

    return {
        ...plan,
        eta_minutos: etaMinutos(plan.distancia_metros, velocidadKmh)
    };
}

module.exports = { calcularRuta, planificarReparto, rutaEnLineaRecta, distanciaMetros };
