/**
 * Cálculos geográficos para el reparto.
 *
 * Módulo puro: sin red ni base de datos, así que se importa tal cual desde las
 * pruebas. Todas las coordenadas son pares [latitud, longitud] en grados.
 */

const RADIO_TIERRA_M = 6371000;

/** Velocidad media de un repartidor urbano en moto, en km/h. */
const VELOCIDAD_KMH_POR_DEFECTO = 25;

const grados = (v) => (v * Math.PI) / 180;

/**
 * Distancia en metros entre dos puntos sobre la superficie terrestre.
 * Fórmula del haversine: para las distancias de un reparto urbano el error
 * frente a modelos elipsoidales es despreciable.
 */
function distanciaMetros([lat1, lon1], [lat2, lon2]) {
    const dLat = grados(lat2 - lat1);
    const dLon = grados(lon2 - lon1);

    const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(grados(lat1)) * Math.cos(grados(lat2)) * Math.sin(dLon / 2) ** 2;

    return 2 * RADIO_TIERRA_M * Math.asin(Math.min(1, Math.sqrt(a)));
}

/** Longitud total de una ruta, sumando sus tramos. */
function longitudRuta(ruta) {
    if (!Array.isArray(ruta) || ruta.length < 2) return 0;

    let total = 0;
    for (let i = 1; i < ruta.length; i++) {
        total += distanciaMetros(ruta[i - 1], ruta[i]);
    }
    return total;
}

/**
 * Punto de la ruta situado a `metros` del inicio.
 *
 * Interpola dentro del tramo correspondiente, así que el repartidor avanza de
 * forma continua y no a saltos entre vértices. Antes del inicio devuelve el
 * origen; pasado el final, el destino.
 */
function puntoEnRuta(ruta, metros) {
    if (!Array.isArray(ruta) || ruta.length === 0) return null;
    if (ruta.length === 1 || metros <= 0) return ruta[0];

    let recorrido = 0;

    for (let i = 1; i < ruta.length; i++) {
        const tramo = distanciaMetros(ruta[i - 1], ruta[i]);

        if (recorrido + tramo >= metros) {
            // Proporción de avance dentro de este tramo.
            const t = tramo === 0 ? 0 : (metros - recorrido) / tramo;
            const [latA, lonA] = ruta[i - 1];
            const [latB, lonB] = ruta[i];

            return [latA + (latB - latA) * t, lonA + (lonB - lonA) * t];
        }

        recorrido += tramo;
    }

    return ruta[ruta.length - 1];
}

/** Minutos estimados para recorrer una distancia a una velocidad media. */
function etaMinutos(metros, velocidadKmh = VELOCIDAD_KMH_POR_DEFECTO) {
    if (!metros || metros <= 0 || velocidadKmh <= 0) return 0;

    const metrosPorMinuto = (velocidadKmh * 1000) / 60;
    return Math.max(1, Math.ceil(metros / metrosPorMinuto));
}

/**
 * Dónde está el repartidor y cuánto le queda, según el tiempo transcurrido.
 *
 * @param {Array} ruta Lista de [lat, lon]
 * @param {number} transcurridoMs Milisegundos desde que salió a reparto
 * @param {number} duracionMs Duración total prevista del trayecto
 */
function progresoDelReparto(ruta, transcurridoMs, duracionMs) {
    const total = longitudRuta(ruta);

    if (!total || !duracionMs || duracionMs <= 0) {
        return { posicion: puntoEnRuta(ruta, 0), avance: 0, restanteMetros: total, restanteMinutos: 0 };
    }

    const avance = Math.min(1, Math.max(0, transcurridoMs / duracionMs));
    const recorridos = total * avance;

    return {
        posicion: puntoEnRuta(ruta, recorridos),
        avance,
        restanteMetros: Math.max(0, total - recorridos),
        restanteMinutos: Math.max(0, Math.ceil(((duracionMs - transcurridoMs) / 1000) / 60))
    };
}

/**
 * Reduce una ruta a como mucho `maxPuntos`, conservando la primera y la
 * última posición.
 *
 * El seguimiento la envía en cada consulta: una ruta entre ciudades trae
 * miles de puntos que en el mapa no se distinguen. Se queda con un punto cada
 * (longitud total / maxPuntos) metros, así que las curvas cerradas de un
 * reparto urbano, que ya traen pocos puntos, quedan intactas.
 */
function simplificarRuta(ruta, maxPuntos = 300) {
    if (!Array.isArray(ruta) || ruta.length <= maxPuntos || maxPuntos < 2) return ruta;

    const paso = longitudRuta(ruta) / (maxPuntos - 1);
    const resultado = [ruta[0]];
    let acumulado = 0;

    for (let i = 1; i < ruta.length - 1; i++) {
        acumulado += distanciaMetros(ruta[i - 1], ruta[i]);
        if (acumulado >= paso) {
            resultado.push(ruta[i]);
            acumulado = 0;
        }
    }

    resultado.push(ruta[ruta.length - 1]);
    if (resultado.length <= maxPuntos) return resultado;

    // Por redondeo puede sobrar alguno: recorte uniforme por posición.
    const salto = (resultado.length - 1) / (maxPuntos - 1);
    return Array.from({ length: maxPuntos }, (_, k) => resultado[Math.round(k * salto)]);
}

module.exports = {
    RADIO_TIERRA_M,
    VELOCIDAD_KMH_POR_DEFECTO,
    distanciaMetros,
    longitudRuta,
    puntoEnRuta,
    etaMinutos,
    progresoDelReparto,
    simplificarRuta
};
