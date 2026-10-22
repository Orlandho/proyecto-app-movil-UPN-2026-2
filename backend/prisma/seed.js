/**
 * Seed de datos de demostración.
 *
 * Reemplaza a los INSERT de "Script base de datos/create database Foodjet.sql",
 * que no son utilizables tal cual por tres motivos:
 *   1. Las contraseñas están en texto plano ('hashed_pwd_admin'), así que
 *      bcrypt.compare falla siempre y ningún usuario puede iniciar sesión.
 *   2. El rol se escribe 'Administrador', pero adminMiddleware exige 'admin'.
 *   3. Los estados se escriben 'En preparación' / 'Entregado', mientras el
 *      código compara contra los valores canónicos en snake_case.
 *
 * Es idempotente: todo se escribe con upsert sobre claves fijas, así que
 * volver a ejecutarlo no duplica filas.
 */
const bcrypt = require('bcryptjs');
const prisma = require('../src/config/db');

/**
 * QR de cobro por restaurante.
 *
 * Son ficticios y se sirven desde el propio sitio (frontend/img/qr/), no desde
 * un dominio ajeno: antes los tres restaurantes compartían un QR genérico
 * alojado en Wikipedia, así que el pago con billetera mostraba siempre la
 * misma imagen y dependía de que ese enlace externo siguiera vivo.
 *
 * Cada uno codifica una cadena tipo Yape/Plin que identifica al comercio y
 * lleva la marca DEMO-NO-VALIDO, para que quede claro al escanearlo que no
 * corresponde a una cuenta real.
 */
const QR_POR_RESTAURANTE = {
    1: '/img/qr/burger-king.svg',
    2: '/img/qr/pizza-hut.svg',
    3: '/img/qr/sushi-club.svg'
};

async function seedUsers() {
    const usuarios = [
        { id: 1, nombre: 'Admin', email: 'admin@foodjet.com', password: 'Admin123!', rol: 'admin', telefono: '123456789', es_estudiante: false },
        { id: 2, nombre: 'Cliente 1', email: 'cliente1@mail.com', password: 'Cliente123!', rol: 'cliente', telefono: '987654321', es_estudiante: true },
        { id: 3, nombre: 'Cliente 2', email: 'cliente2@mail.com', password: 'Cliente123!', rol: 'cliente', telefono: '456123789', es_estudiante: false }
    ];

    for (const { password, ...datos } of usuarios) {
        const hash = await bcrypt.hash(password, 10);
        await prisma.user.upsert({
            where: { id: datos.id },
            update: { ...datos, password: hash },
            create: { ...datos, password: hash }
        });
    }
    return usuarios.length;
}

async function seedRestaurants() {
    const restaurantes = [
        { id: 1, nombre: 'Burger King', descripcion: 'Las mejores hamburguesas a la parrilla', direccion: 'Av. Larco 345, Miraflores', latitud: -12.1219, longitud: -77.0297, estado_afiliacion: 'activo', calificacion_promedio: 4.5, qr_pago: QR_POR_RESTAURANTE[1], tiempo_entrega: '30 minutos' },
        { id: 2, nombre: 'Pizza Hut', descripcion: 'Pizzas recién horneadas con los mejores ingredientes', direccion: 'Av. Arequipa 4545, Miraflores', latitud: -12.1114, longitud: -77.0325, estado_afiliacion: 'activo', calificacion_promedio: 4.2, qr_pago: QR_POR_RESTAURANTE[2], tiempo_entrega: '1 hora' },
        { id: 3, nombre: 'Sushi Club', descripcion: 'El sushi más fresco y delicioso de la ciudad', direccion: 'Av. Conquistadores 500, San Isidro', latitud: -12.0977, longitud: -77.0365, estado_afiliacion: 'activo', calificacion_promedio: 4.8, qr_pago: QR_POR_RESTAURANTE[3], tiempo_entrega: 'Más de 1 hora' }
    ];

    for (const r of restaurantes) {
        await prisma.restaurant.upsert({ where: { id: r.id }, update: r, create: r });
    }
    return restaurantes.length;
}

async function seedCategories() {
    const categorias = [
        { nombre: 'Hamburguesas', descripcion: 'Comida rápida americana' },
        { nombre: 'Pizzas', descripcion: 'Especialidades italianas' },
        { nombre: 'Sushi', descripcion: 'Comida japonesa y asiática' },
        { nombre: 'Ensaladas', descripcion: 'Opciones saludables' },
        { nombre: 'Bebidas', descripcion: 'Refrescos y jugos' }
    ];

    for (const c of categorias) {
        await prisma.category.upsert({ where: { nombre: c.nombre }, update: c, create: c });
    }
    return categorias.length;
}

async function seedProducts() {
    const productos = [
        { id: 1, restaurante_id: 1, nombre: 'Hamburguesa Clásica', descripcion: 'Doble carne, queso cheddar, lechuga, tomate y salsa especial', precio: 8.5, imagen_url: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', descuento_estudiante: 10.0, disponibilidad: true, tipo_comida: 'Comida rápida' },
        { id: 2, restaurante_id: 2, nombre: 'Pizza Margarita', descripcion: 'Salsa de tomate casera, mozzarella fresca y albahaca', precio: 12.0, imagen_url: 'https://imag.bonviveur.com/pizza-margarita.jpg', descuento_estudiante: 0.0, disponibilidad: true, tipo_comida: 'Pizzas' },
        { id: 3, restaurante_id: 2, nombre: 'Pizza Pepperoni', descripcion: 'Salsa de tomate, mozzarella y abundante pepperoni', precio: 14.5, imagen_url: 'https://images.unsplash.com/photo-1628840042765-356cda07504e?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', descuento_estudiante: 15.0, disponibilidad: true, tipo_comida: 'Pizzas' },
        { id: 4, restaurante_id: 3, nombre: 'Sushi Roll California', descripcion: 'Cangrejo, aguacate, pepino y sésamo tostado', precio: 9.0, imagen_url: 'https://images.unsplash.com/photo-1579871494447-9811cf80d66c?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', descuento_estudiante: 20.0, disponibilidad: true, tipo_comida: 'Cena' },
        { id: 5, restaurante_id: 1, nombre: 'Ensalada César', descripcion: 'Lechuga romana, crutones, queso parmesano y aderezo César', precio: 7.0, imagen_url: 'https://images.unsplash.com/photo-1550304943-4f24f54ddde9?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', descuento_estudiante: 0.0, disponibilidad: true, tipo_comida: 'Cena' },
        { id: 6, restaurante_id: 1, nombre: 'Refresco de Cola', descripcion: 'Lata de 330ml', precio: 2.0, imagen_url: 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80', descuento_estudiante: 5.0, disponibilidad: true, tipo_comida: 'Bebidas' }
    ];

    for (const p of productos) {
        await prisma.product.upsert({ where: { id: p.id }, update: p, create: p });
    }
    return productos.length;
}

async function seedAddresses() {
    const direcciones = [
        { id: 1, usuario_id: 2, direccion_detallada: 'Av. Principal 123, Miraflores', referencia: 'Cerca al parque', latitud: -12.1310, longitud: -77.0220, es_predeterminada: true },
        { id: 2, usuario_id: 3, direccion_detallada: 'Calle Secundaria 456, San Isidro', referencia: 'Edificio rojo, piso 3', latitud: -12.0920, longitud: -77.0510, es_predeterminada: true }
    ];

    for (const d of direcciones) {
        await prisma.deliveryAddress.upsert({ where: { id: d.id }, update: d, create: d });
    }
    return direcciones.length;
}

async function seedCoupons() {
    // El HTML del checkout usa "PruebaCupon" como placeholder, pero el SQL no
    // sembraba ningún cupón, así que el campo no tenía nada válido que aceptar.
    const cupones = [
        { id: 1, codigo: 'PruebaCupon', porcentaje_descuento: 10.0, monto_maximo: 20.0, fecha_expiracion: new Date('2027-12-31T23:59:59Z'), estado: 'Activo' },
        { id: 2, codigo: 'FOODJET20', porcentaje_descuento: 20.0, monto_maximo: 30.0, fecha_expiracion: new Date('2027-12-31T23:59:59Z'), estado: 'Activo' }
    ];

    for (const c of cupones) {
        await prisma.discountCoupon.upsert({ where: { id: c.id }, update: c, create: c });
    }
    return cupones.length;
}

async function seedOrders() {
    // Los importes cuadran con el catálogo y con la fórmula del checkout
    // (subtotal + 18% IGV + S/5 de envío), para que la vista de detalle pueda
    // derivar el subtotal como total - impuestos - costo_envio.
    const pedidos = [
        {
            id: 1, user_id: 2, restaurante_id: 1, direccion_entrega_id: 1,
            estado: 'entregado', costo_envio: 5.0, impuestos: 2.79, total: 23.29,
            pago: { metodo_pago: 'card', estado_pago: 'completado' },
            items: [
                { id: 1, product_id: 1, cantidad: 1, precio_unitario: 8.5 },
                { id: 2, product_id: 5, cantidad: 1, precio_unitario: 7.0 }
            ]
        },
        {
            id: 2, user_id: 3, restaurante_id: 2, direccion_entrega_id: 2,
            estado: 'en_preparacion', costo_envio: 5.0, impuestos: 2.52, total: 21.52,
            pago: { metodo_pago: 'wallet', estado_pago: 'completado' },
            items: [
                { id: 3, product_id: 2, cantidad: 1, precio_unitario: 12.0 },
                { id: 4, product_id: 6, cantidad: 1, precio_unitario: 2.0 }
            ]
        }
    ];

    for (const { items, pago, ...pedido } of pedidos) {
        await prisma.order.upsert({ where: { id: pedido.id }, update: pedido, create: pedido });

        for (const item of items) {
            const fila = { ...item, order_id: pedido.id };
            await prisma.orderItem.upsert({ where: { id: item.id }, update: fila, create: fila });
        }

        // Sin transacción el historial no puede mostrar con qué se pagó.
        const transaccion = { ...pago, pedido_id: pedido.id, monto: pedido.total };
        await prisma.transaction.upsert({
            where: { pedido_id: pedido.id },
            update: transaccion,
            create: transaccion
        });
    }
    return pedidos.length;
}

/**
 * Las secuencias de autoincremento no avanzan cuando se insertan ids
 * explícitos, así que el siguiente INSERT sin id chocaría con una clave ya
 * ocupada. Las reposicionamos al máximo usado.
 */
async function resyncSequences() {
    const queries = [
        'SELECT setval(pg_get_serial_sequence(\'"User"\', \'id\'), COALESCE((SELECT MAX(id) FROM "User"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"Restaurant"\', \'id\'), COALESCE((SELECT MAX(id) FROM "Restaurant"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"Category"\', \'id\'), COALESCE((SELECT MAX(id) FROM "Category"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"Product"\', \'id\'), COALESCE((SELECT MAX(id) FROM "Product"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"DeliveryAddress"\', \'id\'), COALESCE((SELECT MAX(id) FROM "DeliveryAddress"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"DiscountCoupon"\', \'id\'), COALESCE((SELECT MAX(id) FROM "DiscountCoupon"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"Order"\', \'id\'), COALESCE((SELECT MAX(id) FROM "Order"), 1));',
        'SELECT setval(pg_get_serial_sequence(\'"OrderItem"\', \'id\'), COALESCE((SELECT MAX(id) FROM "OrderItem"), 1));'
    ];

    for (const query of queries) {
        try {
            await prisma.$executeRawUnsafe(query);
        } catch {
            // En MySQL, InnoDB gestiona AUTO_INCREMENT automaticamente
        }
    }
}

async function main() {
    console.log('🌱 Sembrando datos de demostración...');

    console.log(`   usuarios:      ${await seedUsers()}`);
    console.log(`   restaurantes:  ${await seedRestaurants()}`);
    console.log(`   categorías:    ${await seedCategories()}`);
    console.log(`   productos:     ${await seedProducts()}`);
    console.log(`   direcciones:   ${await seedAddresses()}`);
    console.log(`   cupones:       ${await seedCoupons()}`);
    console.log(`   pedidos:       ${await seedOrders()}`);

    await resyncSequences();

    console.log('✅ Seed completado. Credenciales de acceso:');
    console.log('   admin@foodjet.com  / Admin123!    (rol admin)');
    console.log('   cliente1@mail.com  / Cliente123!  (estudiante, con descuento)');
    console.log('   cliente2@mail.com  / Cliente123!');
}

main()
    .catch((e) => {
        console.error('❌ Error en el seed:', e);
        process.exit(1);
    })
    .finally(async () => {
        await prisma.$disconnect();
    });
