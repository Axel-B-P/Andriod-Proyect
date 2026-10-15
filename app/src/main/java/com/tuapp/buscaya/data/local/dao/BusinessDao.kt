package com.tuapp.buscaya.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tuapp.buscaya.data.local.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

/**
 * ============================================================================
 * DAO (Data Access Object) DE ROOM
 * ============================================================================
 * Define las operaciones de lectura y escritura en la base de datos SQLite.
 *
 * CONCEPTOS DIDÁCTICOS CLAVE:
 * 1. Reactividad con Flow: Los métodos que devuelven Flow<List<...>> son reactivos.
 *    Room emite una nueva lista automáticamente cada vez que cualquier operación
 *    modifica la tabla "negocios". Además, la consulta se traslada automáticamente
 *    a un hilo de fondo (background thread), protegiendo el hilo principal (Main Thread).
 * 2. Operaciones suspend: Todas las funciones de escritura (insertar, actualizar,
 *    eliminar, borrarTodos) son "suspend", obligando a que se invoquen dentro de
 *    una Coroutine en un dispatcher de entrada/salida (Dispatchers.IO).
 * 3. LIKE parametrizado: En Room NUNCA debemos concatenar strings con interpolación
 *    manual (ej: "LIKE '%$query%'") porque abre brechas de seguridad (SQL Injection).
 *    Usamos LIKE '%' || :query || '%' donde Room sustituye el valor como un parámetro '?'
 *    seguro de SQLite.
 * 4. OnConflictStrategy.REPLACE: Si se intenta insertar un registro con un ID ya existente,
 *    Room lo reemplaza automáticamente.
 */
@Dao
interface BusinessDao {

    /**
     * Consulta reactiva de todos los comercios ordenados alfabéticamente por nombre.
     */
    @Query("SELECT * FROM negocios ORDER BY nombre ASC")
    fun obtenerTodos(): Flow<List<BusinessEntity>>

    /**
     * Búsqueda por nombre utilizando LIKE parametrizado de forma segura.
     * Retorna un Flow reactivo para actualizar la lista en tiempo real.
     */
    @Query("SELECT * FROM negocios WHERE nombre LIKE '%' || :query || '%' ORDER BY nombre ASC")
    fun buscarPorNombre(query: String): Flow<List<BusinessEntity>>

    /**
     * Cuenta la cantidad de registros en la tabla "negocios".
     * Permite verificar eficientemente si la caché local está vacía.
     */
    @Query("SELECT COUNT(*) FROM negocios")
    suspend fun contar(): Int

    /**
     * Inserta una lista completa de comercios con estrategia de reemplazo en conflictos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(negocios: List<BusinessEntity>)

    /**
     * Actualiza un comercio existente según su @PrimaryKey.
     */
    @Update
    suspend fun actualizar(negocio: BusinessEntity)

    /**
     * Elimina un comercio específico según su @PrimaryKey.
     */
    @Delete
    suspend fun eliminar(negocio: BusinessEntity)

    /**
     * Elimina todos los registros de la tabla para refrescar la caché por completo.
     */
    @Query("DELETE FROM negocios")
    suspend fun borrarTodos()
}
