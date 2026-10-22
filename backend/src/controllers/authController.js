const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const prisma = require('../config/db');
const { validateRegistrationData } = require('../domain/userValidation');


/**
 * Fábrica del controlador de registro.
 *
 * Las dependencias entran por parámetro para que las pruebas puedan pasar
 * dobles de prisma y bcrypt. No se añaden como tercer argumento de register
 * porque Express reserva esa posición para next().
 */
exports.makeRegister = ({ prisma, bcrypt }) => async (req, res) => {
        try {
            // 1. Validación de formato
            const validation = validateRegistrationData(req.body);
            if (!validation.isValid) {
                return res.status(validation.status).json({ error: validation.error });
            }

            const { nombre, email, telefono, password } = req.body;

            // 2. Validación de reglas de negocio (Duplicidad)
            const existingUser = await prisma.user.findUnique({ where: { email } });
            if (existingUser) {
                return res.status(409).json({ error: 'El email ya está registrado.', code: 'EMAIL_ALREADY_EXISTS' });
            }

            // 3. Procesamiento y guardado
            const salt = await bcrypt.genSalt(10);
            const hashedPassword = await bcrypt.hash(password, salt);

            const newUser = await prisma.user.create({
                data: {
                    nombre,
                    email,
                    telefono,
                    password: hashedPassword,
                    // El rol NO se toma del body: aceptarlo permitía que
                    // cualquiera se registrase como administrador desde el
                    // formulario público. Las cuentas admin salen del seed.
                    rol: 'cliente',
                }
            });

            return res.status(201).json({ message: 'Usuario registrado exitosamente', userId: newUser.id });
        } catch (error) {
            console.error(error);
            return res.status(500).json({ error: 'Error en el servidor al registrar usuario.' });
        }
};

exports.register = exports.makeRegister({ prisma, bcrypt });

exports.login = async (req, res) => {
    try {
        const { email, password } = req.body;

        // Buscar el usuario
        const user = await prisma.user.findUnique({ where: { email } });

        // Mismo 401 y mismo mensaje tanto si el usuario no existe como si la
        // contraseña falla: distinguirlos permitía averiguar qué correos están
        // registrados probándolos uno a uno.
        const validPassword = user ? await bcrypt.compare(password, user.password) : false;
        if (!user || !validPassword) {
            return res.status(401).json({ error: 'Credenciales inválidas.' });
        }

        // Generar JWT
        const token = jwt.sign(
            { userId: user.id, rol: user.rol, nombre: user.nombre },
            process.env.JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.json({ message: 'Inicio de sesión exitoso', token, user: { id: user.id, nombre: user.nombre, email: user.email, rol: user.rol, telefono: user.telefono, es_estudiante: user.es_estudiante } });    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error en el servidor al iniciar sesión.' });
    }
};


exports.me = async (req, res) => {
    try {
        const user = await prisma.user.findUnique({ where: { id: req.user.userId } });
        if (!user) {
            return res.status(404).json({ error: 'Usuario no encontrado.' });
        }
        res.json({ user: { id: user.id, nombre: user.nombre, email: user.email, rol: user.rol, telefono: user.telefono, es_estudiante: user.es_estudiante } });
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Error en el servidor al obtener el perfil.' });
    }
};