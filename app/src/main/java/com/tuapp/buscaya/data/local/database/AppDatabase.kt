package com.tuapp.buscaya.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tuapp.buscaya.data.local.dao.BusinessDao
import com.tuapp.buscaya.data.local.entity.BusinessEntity

/**
 * ============================================================================
 * BASE DE DATOS ROOM (Punto de acceso principal a la persistencia SQLite)
 * ============================================================================
 * Esta clase abstracta sirve como capa de acceso principal sobre la base de datos
 * SQLite subyacente de Android. KSP generará la implementación concreta en tiempo
 * de compilación.
 *
 * CONCEPTOS DIDÁCTICOS CLAVE:
 * 1. @Database: Declara las entidades que forman las tablas (BusinessEntity) y la
 *    versión del esquema (versión 1).
 * 2. Patrón Singleton (@Volatile + synchronized): La creación de una base de datos
 *    es una operación muy costosa en memoria y CPU. Se utiliza una única instancia
 *    compartida para toda la aplicación. La anotación @Volatile garantiza que cualquier
 *    escritura en INSTANCE sea inmediatamente visible para todos los hilos.
 * 3. fallbackToDestructiveMigration(dropAllTables = true):
 *    ¡ATENCIÓN! Esta configuración es EXCLUSIVAMENTE PARA DESARROLLO.
 *    Le indica a Room que si se incrementa la versión de la base de datos sin
 *    especificar un objeto Migration formal, descarte y recree todas las tablas,
 *    provocando la pérdida definitiva de los datos. En una aplicación en producción,
 *    se deben escribir migraciones manuales (Migration(1, 2) { ... }) para
 *    preservar los datos del usuario.
 */
@Database(
    entities = [BusinessEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun businessDao(): BusinessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buscaya_database"
                )
                    // Configuración para desarrollo: recrea la base si cambia el esquema sin migración
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
