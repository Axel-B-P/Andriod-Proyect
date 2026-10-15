package com.tuapp.ui.screen

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tuapp.data.model.Business
import com.tuapp.ui.viewmodel.BusinessSearchViewModel

// ─────────────────────────────────────────────
// Colores del tema (igual que farmacias de turno)
// ─────────────────────────────────────────────
private val YellowPrimary    = Color(0xFFFFC107)
private val YellowDark       = Color(0xFFF9A825)
private val BackgroundColor  = Color(0xFFFFF8E1)
private val CardWhite        = Color(0xFFFFFFFF)
private val TextPrimary      = Color(0xFF212121)
private val TextSecondary    = Color(0xFF757575)
private val ErrorRed         = Color(0xFFD32F2F)

// ─────────────────────────────────────────────
// Pantalla principal
// ─────────────────────────────────────────────
@Composable
fun BusinessSearchScreen(
    viewModel: BusinessSearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
    ) {

        // ── Header ──────────────────────────────
        BusinessSearchHeader()

        // ── Barra de búsqueda ───────────────────
        SearchBar(
            query        = uiState.searchQuery,
            onQueryChange = viewModel::onQueryChange,
            onSearch     = {
                keyboardController?.hide()
                viewModel.searchBusinesses()
            },
            onClear      = viewModel::clearSearch
        )

        // ── Contenido dinámico ──────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()

                uiState.errorMessage != null -> ErrorMessage(uiState.errorMessage!!)

                uiState.hasSearched && uiState.businesses.isEmpty() -> EmptyResults(uiState.searchQuery)

                uiState.businesses.isNotEmpty() -> BusinessList(businesses = uiState.businesses)

                else -> WelcomePlaceholder()
            }
        }
    }
}

// ─────────────────────────────────────────────
// Header con fondo amarillo
// ─────────────────────────────────────────────
@Composable
private fun BusinessSearchHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(YellowPrimary)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Column {
            Text(
                text       = "Buscá negocios",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Text(
                text     = "Encontrá lo que necesitás cerca tuyo",
                fontSize = 14.sp,
                color    = TextPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Barra de búsqueda + botón Buscar
// ─────────────────────────────────────────────
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
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.weight(1f),
            placeholder   = { Text("Ej: ferretería, comida...") },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = YellowDark,
                unfocusedBorderColor = YellowPrimary,
                cursorColor          = YellowDark,
                focusedContainerColor   = CardWhite,
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
            shape   = RoundedCornerShape(12.dp),
            colors  = ButtonDefaults.buttonColors(containerColor = YellowPrimary),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TextPrimary)
        }
    }
}

// ─────────────────────────────────────────────
// Lista de negocios
// ─────────────────────────────────────────────
@Composable
private fun BusinessList(businesses: List<Business>) {
    Column {
        Text(
            text       = "${businesses.size} resultado${if (businesses.size != 1) "s" else ""} encontrado${if (businesses.size != 1) "s" else ""}",
            fontSize   = 13.sp,
            color      = TextSecondary,
            modifier   = Modifier.padding(bottom = 8.dp, top = 4.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding      = PaddingValues(bottom = 24.dp)
        ) {
            items(businesses, key = { it.id ?: it.name.orEmpty() }) { business ->
                BusinessCard(business = business)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Tarjeta individual de negocio
// ─────────────────────────────────────────────
@Composable
private fun BusinessCard(business: Business) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Imagen del negocio
            if (!business.image.isNullOrBlank()) {
                AsyncImage(
                    model             = business.image,
                    contentDescription = business.name,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                // Placeholder amarillo si no hay imagen
                Box(
                    modifier          = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(YellowPrimary),
                    contentAlignment  = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint   = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Información del negocio
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre
                Text(
                    text       = business.name ?: "Sin nombre",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = TextPrimary,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                // Dirección
                if (!business.address.isNullOrBlank()) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint     = YellowDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text     = business.address,
                            fontSize = 13.sp,
                            color    = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Teléfono
                if (!business.phone.isNullOrBlank()) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint     = YellowDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text     = business.phone,
                            fontSize = 13.sp,
                            color    = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Estados auxiliares de UI
// ─────────────────────────────────────────────
@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = YellowDark, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("Buscando negocios...", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier              = Modifier.padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", fontSize = 32.sp)
                Text(
                    text       = "Algo salió mal",
                    fontWeight = FontWeight.Bold,
                    color      = ErrorRed,
                    fontSize   = 16.sp
                )
                Text(
                    text     = message,
                    color    = TextSecondary,
                    fontSize = 13.sp
                )
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
                text       = "Sin resultados para \"$query\"",
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontSize   = 15.sp
            )
            Text(
                text     = "Probá con otro término de búsqueda",
                color    = TextSecondary,
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
                text       = "¡Empezá a buscar!",
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                fontSize   = 16.sp
            )
            Text(
                text     = "Ingresá el nombre de un negocio o rubro",
                color    = TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
