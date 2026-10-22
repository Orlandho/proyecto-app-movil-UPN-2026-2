require('dotenv').config();
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/authRoutes');
const productRoutes = require('./routes/productRoutes');
const orderRoutes = require('./routes/orderRoutes');
const favoriteRoutes = require('./routes/favoriteRoutes');

const reviewRoutes = require('./routes/reviewRoutes');

const userRoutes = require('./routes/userRoutes');
const addressRoutes = require('./routes/addressRoutes');
const restaurantRoutes = require('./routes/restaurantRoutes');
const configRoutes = require('./routes/configRoutes');
const { startJobs } = require('./jobs/orderJobs');
const app = express();

// Middlewares globales
app.use(cors());
app.use(express.json());

// Rutas
app.use('/api/auth', authRoutes);
app.use('/api/products', productRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/reviews', reviewRoutes);
app.use('/api/favorites', favoriteRoutes);

app.use('/api/users', userRoutes);
app.use('/api/addresses', addressRoutes);
app.use('/api/restaurants', restaurantRoutes);
app.use('/api/config', configRoutes);
// Manejo de errores global
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Ocurrió un error inesperado en el servidor.' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`🚀 Servidor ejecutándose en http://localhost:${PORT}`);
    startJobs();
});
