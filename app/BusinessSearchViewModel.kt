package com.tuapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.data.model.Business
import com.tuapp.data.repository.BusinessRepository
import com.tuapp.data.repository.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// Estado de la pantalla (UI State)
// ─────────────────────────────────────────────
data class BusinessSearchUiState(
    val searchQuery: String          = "",
    val businesses: List<Business>   = emptyList(),
    val isLoading: Boolean           = false,
    val errorMessage: String?        = null,
    val hasSearched: Boolean         = false   // para distinguir "inicio" de "sin resultados"
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────
class BusinessSearchViewModel(
    private val repository: BusinessRepository = BusinessRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSearchUiState())
    val uiState: StateFlow<BusinessSearchUiState> = _uiState.asStateFlow()

    /** El usuario escribe en el TextField */
    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery, errorMessage = null) }
    }

    /** El usuario presiona "Buscar" */
    fun searchBusinesses() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresá un término para buscar") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = repository.searchBusinesses(query)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            businesses   = result.data,
                            hasSearched  = true,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading    = false,
                            businesses   = emptyList(),
                            hasSearched  = true,
                            errorMessage = result.message
                        )
                    }
                }
                NetworkResult.Loading -> Unit   // No se usa directamente aquí
            }
        }
    }

    /** Limpia resultados y búsqueda */
    fun clearSearch() {
        _uiState.update { BusinessSearchUiState() }
    }
}
