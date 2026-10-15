package com.tuapp.buscaya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tuapp.buscaya.data.local.database.AppDatabase
import com.tuapp.buscaya.data.repository.BusinessRepository
import com.tuapp.buscaya.ui.screen.BusinessSearchScreen
import com.tuapp.buscaya.ui.viewmodel.BusinessSearchViewModel
import com.tuapp.buscaya.ui.viewmodel.BusinessViewModelFactory

/**
 * ============================================================================
 * ACTIVIDAD PRINCIPAL (MainActivity)
 * ============================================================================
 * Punto de entrada de la aplicación en Android.
 *
 * ARQUITECTURA DIDÁCTICA:
 * 1. Inicialización de dependencias:
 *    - Obtenemos la instancia singleton de Room (AppDatabase).
 *    - Instanciamos BusinessRepository proveyéndole la base de datos y su DAO.
 *    - Creamos BusinessViewModelFactory para permitir que Android cree el
 *      ViewModel con su repositorio correspondiente sin necesidad de librerías
 *      adicionales complejas de inyección (como Hilt).
 * 2. Composable UI:
 *    - Pasamos el ViewModel instanciado a la pantalla BusinessSearchScreen.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instancia singleton de la base de datos Room
        val database = AppDatabase.getInstance(applicationContext)

        // Repositorio con Room como única fuente de verdad (SSOT)
        val repository = BusinessRepository(database)

        // Fábrica para construir el ViewModel
        val factory = BusinessViewModelFactory(repository)

        setContent {
            MaterialTheme {
                val searchViewModel: BusinessSearchViewModel = viewModel(factory = factory)
                BusinessSearchScreen(viewModel = searchViewModel)
            }
        }
    }
}