package com.tuapp.buscaya.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ============================================================================
 * ENTIDAD DE ROOM (Persistencia Local en SQLite)
 * ============================================================================
 * Representa la tabla "negocios" en la base de datos SQLite local de Android.
 *
 * CONCEPTOS DIDÁCTICOS CLAVE:
 * 1. Separación de Responsabilidades: Mantenemos la entidad de base de datos
 *    separada del DTO (Data Transfer Object) de la API (Business) y del modelo de UI.
 *    Esto evita que cambios en la estructura del servidor backend afecten la persistencia
 *    local o viceversa.
 * 2. @Entity(tableName = "negocios"): Indica a Room que esta clase modela una tabla
 *    de base de datos relacional.
 * 3. @PrimaryKey(autoGenerate = true): Llave primaria requerida por Room. Con autoGenerate,
 *    SQLite asigna automáticamente un ID único incremental (1, 2, 3...) a cada registro.
 * 4. @ColumnInfo: Define el nombre de la columna en la tabla SQLite, desacoplándolo
 *    del nombre de la propiedad en Kotlin si fuera necesario.
 * 5. fechaActualizacion: Columna solicitada tipo Long (milisegundos) que registra
 *    el momento exacto en que los datos fueron sincronizados desde la API.
 */
@Entity(tableName = "negocios")
data class BusinessEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "nombre")
    val name: String,

    @ColumnInfo(name = "ubicacion")
    val address: String,

    @ColumnInfo(name = "telefono")
    val phone: String,

    @ColumnInfo(name = "imagen")
    val image: String,

    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: Long = System.currentTimeMillis()
)
