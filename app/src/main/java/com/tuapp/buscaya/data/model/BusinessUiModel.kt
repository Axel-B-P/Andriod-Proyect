package com.tuapp.buscaya.data.model

import com.tuapp.buscaya.data.local.entity.BusinessEntity

/**
 * ============================================================================
 * MODELO DE UI (Capa de Presentación)
 * ============================================================================
 * Este modelo contiene exclusivamente los datos formateados y listos para ser
 * consumidos y renderizados por los componentes de Jetpack Compose.
 *
 * VENTAJAS DIDÁCTICAS:
 * - Desacopla la vista de la base de datos y de la red.
 * - Asegura valores no nulos y preparados para el usuario.
 * - Permite agregar lógica de formato (como la fecha legible) sin ensuciar la entidad.
 */
data class BusinessUiModel(
    val id: Long = 0L,
    val name: String,
    val address: String,
    val phone: String,
    val image: String,
    val fechaActualizacion: Long = 0L
)

/**
 * ============================================================================
 * FUNCIONES DE MAPEO (MAPPERS)
 * ============================================================================
 * Funciones de extensión que transforman los datos de una capa a otra de forma
 * segura, pura e inmutable.
 */

/**
 * Mapeo: DTO de la API (Business) -> Entidad de Room (BusinessEntity)
 * Normaliza campos nulos o vacíos provenientes del servicio web JSON.
 */
fun Business.toEntity(timestamp: Long = System.currentTimeMillis()): BusinessEntity {
    return BusinessEntity(
        id = 0L, // Al ser 0L con autoGenerate, Room asignará el ID al insertar
        name = this.name.orEmpty().trim(),
        address = this.address.orEmpty().trim(),
        phone = this.phone.orEmpty().trim(),
        image = this.image.orEmpty().trim(),
        fechaActualizacion = timestamp
    )
}

/**
 * Mapeo: Entidad de Room (BusinessEntity) -> Modelo de UI (BusinessUiModel)
 */
fun BusinessEntity.toUiModel(): BusinessUiModel {
    return BusinessUiModel(
        id = this.id,
        name = if (this.name.isNotBlank()) this.name else "Sin nombre",
        address = this.address,
        phone = this.phone,
        image = this.image,
        fechaActualizacion = this.fechaActualizacion
    )
}

/**
 * Mapeos de conveniencia para listas
 */
fun List<Business>.toEntityList(timestamp: Long = System.currentTimeMillis()): List<BusinessEntity> {
    return this.map { it.toEntity(timestamp) }
}

fun List<BusinessEntity>.toUiModelList(): List<BusinessUiModel> {
    return this.map { it.toUiModel() }
}
