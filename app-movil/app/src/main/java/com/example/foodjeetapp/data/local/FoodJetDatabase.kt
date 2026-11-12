package com.example.foodjeetapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodjeetapp.data.local.dao.ProductDao
import com.example.foodjeetapp.data.local.entity.ProductEntity

/**
 * Base de datos relacional local Room sobre motor SQLite.
 * Cumple con REQ-SEM05-INF-02.
 */
@Database(
    entities = [ProductEntity::class],
    // 2: descarta la caché de la versión 1, que podía contener productos de
    // relleno inventados por la app cuando el backend no respondía.
    version = 2,
    exportSchema = false
)
abstract class FoodJetDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: FoodJetDatabase? = null

        fun getInstance(context: Context): FoodJetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodJetDatabase::class.java,
                    "foodjet_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
