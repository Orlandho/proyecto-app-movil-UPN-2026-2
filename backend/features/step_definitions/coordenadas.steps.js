const { When, Then } = require('@cucumber/cucumber');
const { expect } = require('chai');
const { validarCoordenadas } = require('../../src/domain/coordenadas');

// Se prueba el validador REAL que usan restaurantController y addressController.
// Una celda vacia del ejemplo equivale a un campo que no llega en el cuerpo.

When('se validan las coordenadas {string} y {string}', function (lat, lng) {
    this.resultado = validarCoordenadas(lat === '' ? undefined : lat, lng === '' ? undefined : lng);
});

Then('las coordenadas se aceptan: {word}', function (esperado) {
    expect(this.resultado.valid).to.equal(esperado === 'true');
});

Then('el error es {string}', function (esperado) {
    expect(this.resultado.error ?? '').to.equal(esperado);
});
