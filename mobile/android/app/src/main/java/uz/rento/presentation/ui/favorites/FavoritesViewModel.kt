package uz.rento.presentation.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.FavoriteItem
import uz.rento.domain.usecase.favorite.GetFavoritesUseCase
import uz.rento.domain.usecase.favorite.ToggleFavoriteUseCase
import javax.inject.Inject

/**
 * FavoritesViewModel — sevimlilar ekrani holati
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getFavoritesUseCase(page = 1, perPage = 50)
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            favorites = page.items,
                            total = page.total,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Xatolik yuz berdi"
                        )
                    }
                }
        }
    }

    fun removeFavorite(listingId: String) {
        viewModelScope.launch {
            toggleFavoriteUseCase(listingId)
                .onSuccess { result ->
                    if (!result.isFavorite) {
                        // O'chirildi — ro'yxatdan olib tashlash
                        _uiState.update { state ->
                            state.copy(
                                favorites = state.favorites.filter { it.listingId != listingId },
                                total = state.total - 1
                            )
                        }
                    }
                }
        }
    }

    fun refresh() {
        loadFavorites()
    }
}

/**
 * FavoritesUiState — sevimlilar ekrani UI holati
 */
data class FavoritesUiState(
    val isLoading: Boolean = false,
    val favorites: List<FavoriteItem> = emptyList(),
    val total: Int = 0,
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && favorites.isEmpty() && error == null
}
