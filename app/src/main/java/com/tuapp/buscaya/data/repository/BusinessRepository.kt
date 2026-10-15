package com.tuapp.buscaya.data.repository

import androidx.room.withTransaction
import com.tuapp.buscaya.data.local.dao.BusinessDao
import com.tuapp.buscaya.data.local.database.AppDatabase
import com.tuapp.buscaya.data.local.entity.BusinessEntity
import com.tuapp.buscaya.data.model.toEntityList
import com.tuapp.buscaya.data.network.BusinessApiService
import com.tuapp.buscaya.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * ============================================================================
 * REPOSITORIO (Patrón Repository - Fuente Única de Verdad / SSOT)
 * ============================================================================
 * Actúa como mediador entre los datos remotos (API con Retrofit) y los datos
 * locales (Room con SQLite).
 *
 * PRINCIPIO ARQUITECTÓNICO "OFFLINE-FIRST":
 * 1. Single Source of Truth (SSOT): Room es la única fuente de verdad para la UI.
 *    La interfaz gráfica nunca observa directamente la respuesta de Retrofit,
 *    sino el Flow que emite Room.
 * 2. Flujo unidireccional de datos:
 *    Retrofit -> (DTOs) -> Mapeo -> Room (Entidades) -> Flow -> UI.
 * 3. Atomicidad con withTransaction: Al refrescar, el borrado de datos viejos y
 *    la inserción de los nuevos se realiza dentro de una transacción. Si ocurre un
 *    error en el proceso, SQLite revierte automáticamente todo cambio.
 * 4. Resiliencia ante desconexión: Si no hay Internet, la excepción se captura en
 *    un bloque try/catch; Room no se toca y los datos cacheados siguen estando
 *    disponibles para la vista.
 * 5. Hilo no bloqueante: Toda operación de acceso a disco o red se asegura
 *    en Dispatchers.IO para no congelar jamás el Main Thread (hilo de UI).
 */
class BusinessRepository(
    private val apiService: BusinessApiService = RetrofitClient.businessApiService,
    private val dao: BusinessDao,
    private val database: AppDatabase
) {

    /**
     * Constructor secundario de conveniencia para instanciar el repositorio
     * fácilmente a partir de la base de datos Room.
     */
    constructor(database: AppDatabase) : this(
        apiService = RetrofitClient.businessApiService,
        dao = database.businessDao(),
        database = database
    )

    /**
     * Expone el flujo reactivo de todos los comercios almacenados en Room.
     * Room ejecuta la consulta internamente en un hilo secundario y emite
     * una nueva lista cada vez que se actualiza la tabla "negocios".
     */
    fun obtenerTodos(): Flow<List<BusinessEntity>> = dao.obtenerTodos()

    /**
     * Búsqueda reactiva filtrando por nombre en Room.
     * Si la consulta está vacía, devuelve todos los registros.
     */
    fun buscarPorNombre(query: String): Flow<List<BusinessEntity>> {
        return if (query.isBlank()) {
            dao.obtenerTodos()
        } else {
            dao.buscarPorNombre(query.trim())
        }
    }

    /**
     * Sincroniza los datos con el servidor remoto.
     *
     * @param forzar Si es true, ignora el contenido actual de Room y solicita
     *               datos frescos a la API. Si es false, solo consulta la API
     *               si Room se encuentra completamente vacío (primera apertura).
     * @param query Término de búsqueda para la API (por defecto "farmacia").
     *
     * @return Result.success si se sincronizó correctamente o si no hacía falta refrescar.
     *         Result.failure si hubo un error de red o de servidor (sin borrar Room).
     */
    suspend fun refrescar(forzar: Boolean = false, query: String = "farmacia"): Result<Unit> {
        return withContext(Dispatchers.IO) {
            val cantidadLocal = dao.contar()

            // Si Room ya tiene datos guardados y el usuario no forzó la recarga,
            // evitamos el consumo innecesario de datos móviles y batería.
            if (cantidadLocal > 0 && !forzar) {
                return@withContext Result.success(Unit)
            }

            try {
                // Llamada a la API mediante Retrofit (ejecutada en background)
                val response = apiService.searchBusinesses(query.trim())

                if (response.isSuccessful) {
                    val dtoList = response.body()?.results ?: emptyList()
                    val entities = dtoList.toEntityList()

                    // withTransaction: Operación atómica de Room.
                    // Si ocurre cualquier falla, se hace rollback y no se pierden los datos.
                    database.withTransaction {
                        dao.borrarTodos()
                        dao.insertarTodos(entities)
                    }

                    Result.success(Unit)
                } else {
                    Result.failure(
                        Exception("Error del servidor: código ${response.code()} ${response.message()}")
                    )
                }
            } catch (e: Exception) {
                // ANTE FALTA DE CONEXIÓN O ERROR DE RED:
                // Capturamos el error y NO borramos nada de Room.
                // De esta manera, el usuario continúa viendo la lista guardada en la app.
                Result.failure(e)
            }
        }
    }

    /**
     * Funciones de escritura auxiliares del DAO delegadas en Dispatchers.IO
     */
    suspend fun actualizar(negocio: BusinessEntity) = withContext(Dispatchers.IO) {
        dao.actualizar(negocio)
    }

    suspend fun eliminar(negocio: BusinessEntity) = withContext(Dispatchers.IO) {
        dao.eliminar(negocio)
    }

    suspend fun contar(): Int = withContext(Dispatchers.IO) {
        dao.contar()
    }
}
