package uz.rento.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.DealType
import uz.rento.domain.model.Listing
import uz.rento.domain.model.ListingType
import uz.rento.domain.model.SearchFilter
import uz.rento.domain.usecase.listing.SearchListingsUseCase
import javax.inject.Inject

/**
 * SearchUiState — qidiruv ekrani holati
 */
data class SearchUiState(
    val query: String = "",
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    // Filtr holatlari
    val selectedType: ListingType? = null,
    val selectedDealType: DealType? = null,
    val roomsMin: Int? = null,
    val roomsMax: Int? = null,
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val currency: String? = null,
    val hasFurniture: Boolean? = null,
    val hasParking: Boolean? = null,
    val allowsPets: Boolean? = null,
    val sort: String = "relevance",
    val showFilterSheet: Boolean = false,
    val activeFilterCount: Int = 0
)

/**
 * SearchViewModel — qidiruv ekrani ViewModel
 *
 * Elasticsearch orqali matnli qidiruv va filtrlar bilan ishlaydi.
 * 300ms debounce bilan avtomatik qidirish.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchListingsUseCase: SearchListingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Qidiruv so'zi o'zgarganda (debounce 300ms)
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // debounce
                search(resetPage = true)
            }
        } else if (query.isEmpty()) {
            _uiState.update {
                it.copy(listings = emptyList(), totalCount = 0, currentPage = 1, hasMore = false)
            }
        }
    }

    /**
     * Qidirish tugmasi bosilganda
     */
    fun onSearch() {
        searchJob?.cancel()
        search(resetPage = true)
    }

    /**
     * Keyingi sahifani yuklash (infinite scroll)
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        search(resetPage = false)
    }

    /**
     * Filtrlarni qo'llash
     */
    fun applyFilters(
        type: ListingType?,
        dealType: DealType?,
        roomsMin: Int?,
        roomsMax: Int?,
        priceMin: Double?,
        priceMax: Double?,
        currency: String?,
        hasFurniture: Boolean?,
        hasParking: Boolean?,
        allowsPets: Boolean?
    ) {
        _uiState.update {
            it.copy(
                selectedType = type,
                selectedDealType = dealType,
                roomsMin = roomsMin,
                roomsMax = roomsMax,
                priceMin = priceMin,
                priceMax = priceMax,
                currency = currency,
                hasFurniture = hasFurniture,
                hasParking = hasParking,
                allowsPets = allowsPets,
                showFilterSheet = false,
                activeFilterCount = countActiveFilters(
                    type, dealType, roomsMin, roomsMax, priceMin, priceMax,
                    currency, hasFurniture, hasParking, allowsPets
                )
            )
        }
        search(resetPage = true)
    }

    /**
     * Filtrlarni tozalash
     */
    fun resetFilters() {
        _uiState.update {
            it.copy(
                selectedType = null,
                selectedDealType = null,
                roomsMin = null,
                roomsMax = null,
                priceMin = null,
                priceMax = null,
                currency = null,
                hasFurniture = null,
                hasParking = null,
                allowsPets = null,
                showFilterSheet = false,
                activeFilterCount = 0
            )
        }
        search(resetPage = true)
    }

    /**
     * Saralash o'zgartirish
     */
    fun onSortChange(sort: String) {
        _uiState.update { it.copy(sort = sort) }
        search(resetPage = true)
    }

    /**
     * Filter sheet ko'rsatish/yashirish
     */
    fun toggleFilterSheet(show: Boolean) {
        _uiState.update { it.copy(showFilterSheet = show) }
    }

    /**
     * Xatolikni tozalash
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Asosiy qidiruv funksiyasi
     */
    private fun search(resetPage: Boolean) {
        val state = _uiState.value
        val page = if (resetPage) 1 else state.currentPage + 1

        viewModelScope.launch {
            _uiState.update {
                if (resetPage) it.copy(isLoading = true, error = null)
                else it.copy(isLoadingMore = true)
            }

            val filter = SearchFilter(
                q = state.query.ifBlank { null },
                city = null,
                district = null,
                type = state.selectedType,
                dealType = state.selectedDealType,
                roomsMin = state.roomsMin,
                roomsMax = state.roomsMax,
                priceMin = state.priceMin,
                priceMax = state.priceMax,
                currency = state.currency,
                hasFurniture = state.hasFurniture,
                hasParking = state.hasParking,
                allowsPets = state.allowsPets,
                sort = state.sort,
                page = page,
                perPage = 20
            )

            searchListingsUseCase(filter).fold(
                onSuccess = { result ->
                    _uiState.update {
                        val newListings = if (resetPage) result.items
                        else it.listings + result.items
                        it.copy(
                            listings = newListings,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = result.page,
                            totalPages = result.totalPages,
                            totalCount = result.total,
                            hasMore = result.page < result.totalPages
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "Qidiruvda xatolik"
                        )
                    }
                }
            )
        }
    }

    /**
     * Faol filtrlar sonini hisoblash
     */
    private fun countActiveFilters(
        type: ListingType?,
        dealType: DealType?,
        roomsMin: Int?,
        roomsMax: Int?,
        priceMin: Double?,
        priceMax: Double?,
        currency: String?,
        hasFurniture: Boolean?,
        hasParking: Boolean?,
        allowsPets: Boolean?
    ): Int {
        var count = 0
        if (type != null) count++
        if (dealType != null) count++
        if (roomsMin != null || roomsMax != null) count++
        if (priceMin != null || priceMax != null) count++
        if (currency != null) count++
        if (hasFurniture == true) count++
        if (hasParking == true) count++
        if (allowsPets == true) count++
        return count
    }
}
