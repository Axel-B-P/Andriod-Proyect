package com.tuapp.buscaya.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tuapp.buscaya.data.local.entity.BusinessEntity
import com.tuapp.buscaya.data.model.BusinessUiModel
import com.tuapp.buscaya.data.model.toUiModelList
import com.tuapp.buscaya.data.repository.BusinessRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * ESTADO SELLADO DE LA UI (State Pattern / UDF)
 * ============================================================================
 * Representa los 4 estados posibles de la pantalla solicitados en la consigna:
 *
 * 1. Cargando: Pantalla inicial mientras se leen o sincronizan datos.
 * 2. Exito(lista): Lista de comercios cargada exitosamente desde Room.
 * 3. SinConexion(listaGuardada): Falla la conexión a internet, pero Room conserva
 *    y entrega los datos guardados para que la app siga siendo funcional offline.
 * 4. Error(mensaje): Ocurrió una falla y no hay datos guardados en Room.
 */
sealed interface BusinessUiState {
    object Cargando : BusinessUiState
    data class Exito(val lista: List<BusinessUiModel>) : BusinessUiState
    data class SinConexion(
        val listaGuardada: List<BusinessUiModel>,
        val mensaje: String = "Sin conexión, mostrando datos guardados"
    ) : BusinessUiState
    data class Error(val mensaje: String) : BusinessUiState
}

/**
 * ============================================================================
 * VIEWMODEL (Capa de Presentación y Lógica de Negocio de la Pantalla)
 * ============================================================================
 * Maneja el estado de la UI y sobrevive a cambios de configuración (como rotar pantalla).
 *
 * CONCEPTOS DIDÁCTICOS CLAVE:
 * 1. StateFlow: Flujo caliente (hot flow) que emite siempre el último valor conocido a la UI.
 * 2. Room como SSOT: El ViewModel recolecta el Flow de Room (filtrado por la búsqueda).
 * 3. flatMapLatest: Al cambiar el término de búsqueda, cancela la consulta anterior
 *    en Room y se suscribe inmediatamente a la nueva consulta.
 * 4. Sincronización al iniciar: Llama a refrescar() en su bloque init.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BusinessSearchViewModel(
    private val repository: BusinessRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    /**
     * StateFlow que combina los datos de Room con el estado de red/sincronización.
     */
    val uiState: StateFlow<BusinessUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            repository.buscarPorNombre(query)
        },
        _isRefreshing,
        _isOffline,
        _error
    ) { entities: List<BusinessEntity>, isRefreshing: Boolean, isOffline: Boolean, error: String? ->
        val uiList = entities.toUiModelList()

        when {
            // Caso 1: Cargando datos por primera vez (Room vacío y petición en curso)
            isRefreshing && uiList.isEmpty() -> {
                BusinessUiState.Cargando
            }
            // Caso 2: Sin conexión pero CON datos almacenados en Room (Offline-First)
            isOffline && uiList.isNotEmpty() -> {
                BusinessUiState.SinConexion(
                    listaGuardada = uiList,
                    mensaje = "Sin conexión, mostrando datos guardados"
                )
            }
            // Caso 3: Error y NO hay datos en Room (ej: primer inicio sin internet)
            error != null && uiList.isEmpty() -> {
                BusinessUiState.Error(
                    error.ifBlank { "No se pudo conectar y no hay datos guardados" }
                )
            }
            // Caso 4: Error de red con datos previos existentes
            error != null && uiList.isNotEmpty() -> {
                BusinessUiState.SinConexion(
                    listaGuardada = uiList,
                    mensaje = "Sin conexión, mostrando datos guardados"
                )
            }
            // Caso 5: Inicializando pero aún sin datos ni error
            uiList.isEmpty() && isRefreshing -> {
                BusinessUiState.Cargando
            }
            // Caso 6: Éxito con los datos provenientes de Room
            else -> {
                BusinessUiState.Exito(lista = uiList)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BusinessUiState.Cargando
    )

    init {
        // Al iniciar, refresca datos solo si Room está vacío (forzar = false)
        refrescar(forzar = false)
    }

    /**
     * Actualiza la consulta de búsqueda que filtra localmente en Room.
     */
    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Limpia el campo de búsqueda.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Ejecuta la sincronización con el servidor remoto.
     * @param forzar Si es true, descarga desde la API independientemente de lo que haya en Room.
     */
    fun refrescar(forzar: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null

            // Usa la consulta actual o "farmacia" por defecto para enriquecer la base local
            val queryParam = _searchQuery.value.ifBlank { "farmacia" }
            val result = repository.refrescar(forzar = forzar, query = queryParam)

            result.fold(
                onSuccess = {
                    _isOffline.value = false
                    _error.value = null
                },
                onFailure = { throwable ->
                    _isOffline.value = true
                    _error.value = throwable.localizedMessage ?: "Error de conexión"
                }
            )

            _isRefreshing.value = false
        }
    }
}

/**
 * ============================================================================
 * VIEWMODEL FACTORY SIMPLE (Sin Hilt / Inyección Manual)
 * ============================================================================
 * Dado que BusinessSearchViewModel recibe BusinessRepository en su constructor,
 * Android no sabe crearlo con el constructor por defecto sin parámetros.
 * Esta fábrica le enseña al sistema cómo instanciar nuestro ViewModel con sus
 * dependencias correspondientes.
 */
class BusinessViewModelFactory(
    private val repository: BusinessRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusinessSearchViewModel::class.java)) {
            return BusinessSearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Clase de ViewModel desconocida: ${modelClass.name}")
    }
}
