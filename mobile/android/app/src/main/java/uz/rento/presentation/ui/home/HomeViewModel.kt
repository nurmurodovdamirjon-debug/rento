package uz.rento.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.DealType
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingFilter
import uz.rento.domain.model.ListingType
import uz.rento.domain.usecase.listing.GetListingsUseCase
import javax.inject.Inject

data class HomeUiState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val filter: ListingFilter = ListingFilter(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = "",
    val selectedCity: String? = null,
    val selectedType: ListingType? = null,
    val selectedDealType: DealType? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getListingsUseCase: GetListingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadListings()
    }

    fun loadListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = buildFilter(page = 1)
            getListingsUseCase(filter).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            listings = page.items,
                            isLoading = false,
                            currentPage = page.page,
                            totalPages = page.totalPages,
                            totalCount = page.total,
                            hasMore = page.page < page.totalPages,
                            filter = filter
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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            val nextPage = state.currentPage + 1
            val filter = buildFilter(page = nextPage)

            getListingsUseCase(filter).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            listings = it.listings + page.items,
                            isLoadingMore = false,
                            currentPage = page.page,
                            hasMore = page.page < page.totalPages
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun setCity(city: String?) {
        _uiState.update { it.copy(selectedCity = city) }
        loadListings()
    }

    fun setType(type: ListingType?) {
        _uiState.update { it.copy(selectedType = type) }
        loadListings()
    }

    fun setDealType(dealType: DealType?) {
        _uiState.update { it.copy(selectedDealType = dealType) }
        loadListings()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCity = null,
                selectedType = null,
                selectedDealType = null,
                searchQuery = ""
            )
        }
        loadListings()
    }

    fun refresh() {
        loadListings()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun buildFilter(page: Int): ListingFilter {
        val state = _uiState.value
        return ListingFilter(
            city = state.selectedCity,
            type = state.selectedType,
            dealType = state.selectedDealType,
            page = page,
            perPage = 20,
            sort = "newest"
        )
    }
}
