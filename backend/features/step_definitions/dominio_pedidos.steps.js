const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('chai');
const {
    normalizeEstado,
    isTerminal,
    nextEstado,
    isValidTransition
} = require('../../src/domain/orderStatus');
const {
    isMetodoValido,
    resolvePaymentStatus,
    resolveInitialOrderStatus
} = require('../../src/domain/payment');

// Se prueban los modulos de dominio REALES que usa orderController.

Given('un pedido en estado {string}', function (estado) {
    this.estadoActual = estado;
});

When('se intenta cambiar el estado a {string}', function (estado) {
    this.estadoSiguiente = estado;
    this.permitida = isValidTransition(this.estadoActual, estado);
});

Then('la transicion es {word}', function (esperado) {
    expect(this.permitida).to.equal(esperado === 'true');
});

Then('el estado normalizado del pedido es {string}', function (esperado) {
    expect(normalizeEstado(this.estadoActual)).to.equal(esperado);
});

Then('el pedido se considera terminal', function () {
    expect(isTerminal(this.estadoActual)).to.equal(true);
});

Then('no hay siguiente estado', function () {
    expect(nextEstado(this.estadoActual)).to.equal(null);
});

Given('el metodo de pago es {string}', function (metodo) {
    this.metodo = metodo;
});

Then('el estado del pago resultante es {string}', function (esperado) {
    expect(resolvePaymentStatus(this.metodo)).to.equal(esperado);
});

Then('el estado inicial del pedido es {string}', function (esperado) {
    expect(resolveInitialOrderStatus(resolvePaymentStatus(this.metodo))).to.equal(esperado);
});

Then('el metodo se considera {word}', function (esperado) {
    expect(isMetodoValido(this.metodo)).to.equal(esperado === 'true');
});
