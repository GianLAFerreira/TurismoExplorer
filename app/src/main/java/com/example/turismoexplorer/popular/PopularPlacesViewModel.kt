package com.example.turismoexplorer.popular


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.turismoexplorer.data.places.NetworkModule
import com.example.turismoexplorer.data.places.PlacesRepository
import com.example.turismoexplorer.domain.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PopularPlacesViewModel : ViewModel() {
    private val repository = PlacesRepository(
        api = NetworkModule.googlePlacesApi,
        apiKey = NetworkModule.apiKey
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun load(city: String) {
        if (city.isBlank()) {
            _uiState.value = UiState.Error("Informe uma cidade")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                repository.popularByCity(city)
            }.onSuccess { places ->
                _uiState.value = if (places.isEmpty()) {
                    UiState.Empty("Nenhum resultado para “$city”. Tente outra cidade.")
                } else {
                    UiState.Success(places)
                }
            }.onFailure { e ->
                _uiState.value = UiState.Error(e.message ?: "Erro ao carregar")
            }
        }
    }

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Success(val data: List<Place>) : UiState
        data class Empty(val message: String) : UiState
        data class Error(val message: String) : UiState
    }
}
