package com.tuapp.buscaya.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tuapp.buscaya.data.local.database.AppDatabase
import com.tuapp.buscaya.data.model.BusinessUiModel
import com.tuapp.buscaya.data.repository.BusinessRepository
import com.tuapp.buscaya.ui.viewmodel.BusinessSearchViewModel
import com.tuapp.buscaya.ui.viewmodel.BusinessUiState
import com.tuapp.buscaya.ui.viewmodel.BusinessViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Paleta de colores de la aplicación
private val YellowPrimary   = Color(0xFFFFC107)
private val YellowDark      = Color(0xFFF9A825)
private val BackgroundColor = Color(0xFFFFF8E1)
private val CardWhite       = Color(0xFFFFFFFF)
private val TextPrimary     = Color(0xFF212121)
private val TextSecondary   = Color(0xFF757575)
private val ErrorRed        = Color(0xFFD32F2F)
private val OfflineOrange   = Color(0xFFE65100)
private val OfflineOrangeBg = Color(0xFFFFE0B2)

/**
 * ============================================================================
 * PANTALLA PRINCIPAL: BÚSQUEDA Y LISTA DE COMERCIOS (Jetpack Compose)
 * ============================================================================
 *
 * CONCEPTOS DIDÁCTICOS CLAVE:
 * 1. collectAsStateWithLifecycle(): En Jetpack Compose es el equivalente oficial
 *    de repeatOnLifecycle() en Views XML. Pausa la recolección del StateFlow cuando
 *    la Activity pasa a segundo plano (STOPPED), liberando recursos y batería.
 * 2. Desacoplamiento de la API: La pantalla NO conoce a Retrofit ni a la API.
 *    Solo lee el estado emitido por el ViewModel (`uiState`).
 * 3. Patrón UDF (Unidirectional Data Flow):
 *    - El ViewModel emite el estado (`BusinessUiState`).
 *    - La UI emite eventos hacia el ViewModel (`onQueryChange`, `refrescar`, `clearSearch`).
 */
@Composable
fun BusinessSearchScreen(
    viewModel: BusinessSearchViewModel = viewModel(
        factory = BusinessViewModelFactory(
            BusinessRepository(AppDatabase.getInstance(LocalContext.current.applicationContext))
        )
    )
) {
    // Recolección consciente del ciclo de vida del estado sellado y de la búsqueda
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
    ) {
        // Cabecera con título y botón explícito de "Actualizar"
        BusinessSearchHeader(
            isRefreshing = isRefreshing,
            onRefreshClick = { viewModel.refrescar(forzar = true) }
        )

        // Barra de búsqueda que filtra en tiempo real sobre Room
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                keyboardController?.hide()
                viewModel.refrescar(forzar = true)
            },
            onClear = viewModel::clearSearch
        )

        // Contenedor principal que reacciona a los 4 estados sellados del ViewModel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (val state = uiState) {
                is BusinessUiState.Cargando -> {
                    LoadingIndicator()
                }

                is BusinessUiState.SinConexion -> {
                    // Muestra el banner offline destacado y a continuación la lista guardada en Room
                    Column(modifier = Modifier.fillMaxSize()) {
                        OfflineWarningBanner(
                            mensaje = state.mensaje,
                            onRetry = { viewModel.refrescar(forzar = true) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BusinessList(businesses = state.listaGuardada)
                    }
                }

                is BusinessUiState.Exito -> {
                    if (state.lista.isEmpty() && searchQuery.isNotBlank()) {
                        EmptyResults(query = searchQuery)
                    } else if (state.lista.isEmpty()) {
                        WelcomePlaceholder()
                    } else {
                        BusinessList(businesses = state.lista)
                    }
                }

                is BusinessUiState.Error -> {
                    ErrorMessage(
                        message = state.mensaje,
                        onRetry = { viewModel.refrescar(forzar = true) }
                    )
                }
            }
        }
    }
}

/**
 * Cabecera principal con información y botón de sincronización manual
 */
@Composable
private fun BusinessSearchHeader(
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(YellowPrimary)
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buscá negocios",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Comercios y farmacias (con soporte offline)",
                    fontSize = 13.sp,
                    color = TextPrimary.copy(alpha = 0.8f)
                )
            }

            // Botón de Actualizar (refrescar forzado = true)
            FilledTonalButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = YellowDark,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Actualizar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Banner que avisa visiblemente cuando no hay conexión y se leen datos de Room
 */
@Composable
private fun OfflineWarningBanner(
    mensaje: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = OfflineOrangeBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = "Sin conexión",
                tint = OfflineOrange,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Modo Offline",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OfflineOrange
                )
                Text(
                    text = mensaje,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = OfflineOrange)
            ) {
                Text("Reintentar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Campo de texto y botón de búsqueda
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Filtrar por nombre (ej: farmacia)...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowDark,
                unfocusedBorderColor = YellowPrimary,
                cursorColor = YellowDark,
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        Button(
            onClick = onSearch,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TextPrimary)
        }
    }
}

/**
 * Lista scrolleable con los comercios guardados en Room
 */
@Composable
private fun BusinessList(businesses: List<BusinessUiModel>) {
    Column {
        Text(
            text = "${businesses.size} resultado${if (businesses.size != 1) "s" else ""} guardado${if (businesses.size != 1) "s" else ""}",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(businesses, key = { it.id }) { business ->
                BusinessCard(business = business)
            }
        }
    }
}

/**
 * Tarjeta individual para cada comercio
 */
@Composable
private fun BusinessCard(business: BusinessUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del comercio con Coil o placeholder si no hay imagen
            if (business.image.isNotBlank()) {
                AsyncImage(
                    model = business.image,
                    contentDescription = business.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(YellowPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = business.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (business.address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = YellowDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = business.address,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (business.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = YellowDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = business.phone,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Indicador didáctico de fecha de sincronización guardada en Room
                if (business.fechaActualizacion > 0) {
                    val fechaFormateada = remember(business.fechaActualizacion) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        sdf.format(Date(business.fechaActualizacion))
                    }
                    Text(
                        text = "Guardado local: $fechaFormateada",
                        fontSize = 11.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = YellowDark, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Sincronizando comercios con Room...", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("⚠️", fontSize = 36.sp)
                Text(
                    text = "Algo salió mal",
                    fontWeight = FontWeight.Bold,
                    color = ErrorRed,
                    fontSize = 18.sp
                )
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reintentar", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyResults(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sin resultados para \"$query\"",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 15.sp
            )
            Text(
                text = "Probá con otro término de búsqueda",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun WelcomePlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏪", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Comercios y Farmacias",
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontSize = 16.sp
            )
            Text(
                text = "Los comercios se sincronizan automáticamente en tu dispositivo",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
