package uz.rento.presentation.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingStats
import uz.rento.domain.repository.ListingRepository
import javax.inject.Inject

data class DetailUiState(
    val listing: Listing? = null,
    val stats: ListingStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOwner: Boolean = false,
    val showGallery: Boolean = false,
    val galleryStartIndex: Int = 0
)

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val listingId: String = savedStateHandle.get<String>("id") ?: ""

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        if (listingId.isNotEmpty()) {
            loadListing()
        }
    }

    fun loadListing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            listingRepository.getListing(listingId).fold(
                onSuccess = { listing ->
                    _uiState.update {
                        it.copy(
                            listing = listing,
                            isLoading = false
                        )
                    }
                    // Statsni ham yuklab olamiz
                    loadStats()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "E'lonni olishda xatolik"
                        )
                    }
                }
            )
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            listingRepository.getStats(listingId).fold(
                onSuccess = { stats ->
                    _uiState.update { it.copy(stats = stats) }
                },
                onFailure = { /* Statsni yuklashda xato — e'tiborga olmaymiz */ }
            )
        }
    }

    fun openGallery(startIndex: Int = 0) {
        _uiState.update { it.copy(showGallery = true, galleryStartIndex = startIndex) }
    }

    fun closeGallery() {
        _uiState.update { it.copy(showGallery = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
