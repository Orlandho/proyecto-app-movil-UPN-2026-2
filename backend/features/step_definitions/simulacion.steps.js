const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('chai');
const {
    leerConfigSimulacion,
    crearPlan,
    duracionRepartoSegundos,
    estadoVencido,
    progresoDelPedido
} = require('../../src/domain/simulacion');
const { simplificarRuta } = require('../../src/domain/geo');

// Se prueban los modulos REALES que usan el simulador y el seguimiento.

const AHORA = new Date('2026-10-06T20:00:00Z').getTime();

Given('la simulación con escala {int}, confirmación {int} min, preparación {int} min, reparto máximo {int} min y {int} km\\/h',
    function (escala, conf, prep, max, kmh) {
        this.config = leerConfigSimulacion({
            SIMULAR_PEDIDOS: 'true',
            SIM_ESCALA_TIEMPO: String(escala),
            SIM_CONFIRMACION_MIN: String(conf),
            SIM_PREPARACION_MIN: String(prep),
            SIM_REPARTO_MAX_MIN: String(max),
            REPARTIDOR_VELOCIDAD_KMH: String(kmh)
        });
    });

Given('la simulación está apagada', function () {
    this.config = { ...this.config, activa: false };
});

When('se crea el plan de un pedido nuevo', function () {
    this.plan = crearPlan(this.config);
});

Then('el estado {string} dura {int} segundos', function (estado, segundos) {
    expect(this.plan[estado]).to.equal(segundos);
});

Then('el reparto aún no tiene duración', function () {
    expect(this.plan.en_camino).to.equal(null);
});

Then('no hay plan', function () {
    expect(this.plan).to.equal(null);
});

When('se calcula la duración de un reparto de {int} metros', function (metros) {
    this.duracion = duracionRepartoSegundos(metros, this.config);
});

Then('el reparto dura {int} segundos', function (segundos) {
    expect(this.duracion).to.equal(segundos);
});

Given('un pedido en {string} desde hace {int} segundos con un plan de {int} segundos para ese estado',
    function (estado, transcurridos, duracion) {
        this.vencido = estadoVencido(estado, new Date(AHORA - transcurridos * 1000), { [estado]: duracion }, AHORA);
    });

Given('un pedido en {string} desde hace {int} segundos sin duración de reparto en el plan', function (estado, transcurridos) {
    this.vencido = estadoVencido(estado, new Date(AHORA - transcurridos * 1000), { pendiente: 12, en_camino: null }, AHORA);
});

Then('el estado está vencido: {word}', function (esperado) {
    expect(this.vencido).to.equal(esperado === 'true');
});

// Una ruta recta hacia el norte: 1000 m son ~0.008993 grados de latitud.
Given('un pedido en camino por una ruta recta de {int} metros que dura {int} segundos', function (metros, segundos) {
    const grados = metros / 111195;
    this.pedido = {
        estado: 'en_camino',
        ruta: [[-12, -77], [-12 + grados, -77]],
        envio_iniciado_en: new Date(AHORA).toISOString(),
        plan_tiempos: { en_camino: segundos }
    };
});

Given('el pedido ya figura como entregado', function () {
    this.pedido.estado = 'entregado';
});

When('han pasado {int} segundos desde que salió', function (segundos) {
    this.progreso = progresoDelPedido(this.pedido, AHORA + segundos * 1000);
});

Then('el repartidor lleva el {int} por ciento del recorrido', function (porcentaje) {
    expect(Math.round(this.progreso.avance * 100)).to.equal(porcentaje);
});

Then('le faltan {int} metros', function (metros) {
    expect(Math.abs(this.progreso.restante_metros - metros)).to.be.at.most(1);
});

Given('una ruta de {int} puntos', function (n) {
    this.ruta = Array.from({ length: n }, (_, i) => [-12 - i * 0.0005, -77 + Math.sin(i / 20) * 0.001]);
});

When('se simplifica a {int} puntos como máximo', function (max) {
    this.simplificada = simplificarRuta(this.ruta, max);
    this.max = max;
});

Then('quedan como mucho {int} puntos', function (max) {
    expect(this.simplificada.length).to.be.at.most(max);
    expect(this.simplificada.length).to.be.above(max * 0.8);
});

Then('conserva el primer y el último punto', function () {
    expect(this.simplificada[0]).to.deep.equal(this.ruta[0]);
    expect(this.simplificada.at(-1)).to.deep.equal(this.ruta.at(-1));
});
