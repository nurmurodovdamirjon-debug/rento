package uz.rento.presentation.ui.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingFilter
import uz.rento.domain.model.ListingStatus
import uz.rento.domain.repository.ListingRepository
import javax.inject.Inject

data class MyListingsUiState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: ListingStatus? = null,
    val totalCount: Int = 0
)

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyListingsUiState())
    val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

    init {
        loadMyListings()
    }

    fun loadMyListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = ListingFilter(page = 1, perPage = 50)
            listingRepository.getMyListings(filter).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            listings = page.items,
                            isLoading = false,
                            totalCount = page.total
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Xatolik yuz berdi"
                        )
                    }
                }
            )
        }
    }

    fun selectTab(status: ListingStatus?) {
        _uiState.update { it.copy(selectedTab = status) }
    }

    fun deleteListing(id: String) {
        viewModelScope.launch {
            listingRepository.deleteListing(id).fold(
                onSuccess = { loadMyListings() },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "O'chirishda xatolik")
                    }
                }
            )
        }
    }

    fun refresh() = loadMyListings()
}
