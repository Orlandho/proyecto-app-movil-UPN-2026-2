const prisma = require('../config/db');

exports.verifyStudent = async (req, res) => {
    try {
        const userId = req.user.userId;

        // Fetch user to check if already verified
        const user = await prisma.user.findUnique({ where: { id: userId } });
        if (!user) {
            return res.status(404).json({ error: 'Usuario no encontrado.' });
        }

        if (user.es_estudiante) {
            return res.status(400).json({ error: 'Su estado de estudiante ya está verificado' });
        }

        // Simulate 3rd party API delay (1.5 seconds)
        await new Promise(resolve => setTimeout(resolve, 1500));

        // Update user
        const updatedUser = await prisma.user.update({
            where: { id: userId },
            data: { es_estudiante: true }
        });

        res.json({ message: 'Estudiante verificado exitosamente', es_estudiante: true });
    } catch (error) {
        console.error('Error in verifyStudent:', error);
        res.status(500).json({ error: 'Error en el servidor al verificar estudiante.' });
    }
};
