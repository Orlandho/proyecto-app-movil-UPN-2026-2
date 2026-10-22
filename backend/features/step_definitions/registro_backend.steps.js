const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('chai');
const { makeRegister } = require('../../src/controllers/authController');

// Se prueba el controlador de registro REAL. Las dependencias externas
// (prisma y bcrypt) entran por la fábrica makeRegister, así que no hace falta
// base de datos ni reimplementar la lógica dentro del test.

function crearRes() {
    const res = { statusCode: null, body: null };

    res.status = (code) => {
        res.statusCode = code;
        return { json: (data) => { res.body = data; return res; } };
    };
    res.json = (data) => { res.statusCode = res.statusCode || 200; res.body = data; return res; };

    return res;
}

function crearPrisma({ usuarioExistente = null } = {}) {
    return {
        user: {
            findUnique: async () => usuarioExistente,
            create: async ({ data }) => ({ id: 99, ...data })
        }
    };
}

const bcryptFalso = {
    genSalt: async () => 'salt-de-prueba',
    hash: async (password) => `hash(${password})`
};

Given(
    'los datos de entrada son validos con nombre {string}, email {string}, telefono {string}, password {string} y rol {string}',
    function (nombre, email, telefono, password, rol) {
        this.req = { body: { nombre, email, telefono, password, rol } };
    }
);

Given(
    'los datos de entrada son validos con nombre {string}, email {string}, telefono {string}, password {string}',
    function (nombre, email, telefono, password) {
        this.req = { body: { nombre, email, telefono, password } };
    }
);

Given('los datos de entrada solo contienen el nombre {string}', function (nombre) {
    this.req = { body: { nombre } };
});

Given('los datos de entrada son validos pero el telefono es {string}', function (telefono) {
    this.req = { body: { nombre: 'Juan', email: 'ok@test.com', telefono, password: '123' } };
});

Given('el correo no existe previamente en la base de datos', function () {
    this.prisma = crearPrisma({ usuarioExistente: null });
});

Given('el correo ya esta registrado en el sistema', function () {
    this.prisma = crearPrisma({ usuarioExistente: { id: 1, email: this.req.body.email } });
});

When('se ejecuta el controlador de registro', async function () {
    this.res = crearRes();
    this.usuarioCreado = null;

    const prisma = this.prisma || crearPrisma();
    const originalCreate = prisma.user.create;
    prisma.user.create = async (args) => {
        this.usuarioCreado = args.data;
        return originalCreate(args);
    };

    const register = makeRegister({ prisma, bcrypt: bcryptFalso });
    await register(this.req, this.res);
});

Then('el sistema devuelve un codigo de estado HTTP {int}', function (codigo) {
    expect(this.res.statusCode).to.equal(codigo);
});

Then('el JSON de respuesta contiene el mensaje {string}', function (mensaje) {
    expect(this.res.body.message).to.equal(mensaje);
});

Then('el JSON de respuesta contiene el error de campos obligatorios', function () {
    expect(this.res.body.error).to.equal('Todos los campos son obligatorios.');
});

Then('el JSON de respuesta contiene el error de telefono invalido', function () {
    expect(this.res.body.error).to.equal('El teléfono debe tener exactamente 9 dígitos.');
});

Then('el JSON de respuesta contiene el error con codigo {string}', function (codigo) {
    expect(this.res.body.code).to.equal(codigo);
});

Then('el usuario se crea con el rol {string}', function (rol) {
    expect(this.usuarioCreado.rol).to.equal(rol);
});

Then('la contrasena se guarda cifrada', function () {
    expect(this.usuarioCreado.password).to.equal('hash(123)');
    expect(this.usuarioCreado.password).to.not.equal('123');
});
