package uz.rento.presentation.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.NearbyFilter
import uz.rento.domain.model.NearbyListing
import uz.rento.domain.repository.ListingRepository
import javax.inject.Inject

/**
 * MapUiState — xarita ekrani holati
 */
data class MapUiState(
    val nearbyListings: List<NearbyListing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val centerLat: Double = 41.2995,   // Toshkent markazi
    val centerLng: Double = 69.2401,
    val radiusKm: Int = 5,
    val totalCount: Int = 0,
    val selectedListingId: String? = null
)

/**
 * MapViewModel — xarita ekrani uchun ViewModel
 *
 * Yaqin atrofdagi e'lonlarni PostGIS orqali yuklaydi.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadNearby()
    }

    /**
     * Yaqin atrofdagi e'lonlarni yuklash
     */
    fun loadNearby() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val filter = NearbyFilter(
                lat = state.centerLat,
                lng = state.centerLng,
                radiusKm = state.radiusKm
            )

            listingRepository.getNearbyListings(filter).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            nearbyListings = page.items,
                            isLoading = false,
                            totalCount = page.total
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Yaqin e'lonlarni yuklashda xatolik"
                        )
                    }
                }
            )
        }
    }

    /**
     * Xarita markazi o'zgarganda
     */
    fun onMapCenterChanged(lat: Double, lng: Double) {
        _uiState.update { it.copy(centerLat = lat, centerLng = lng) }
        loadNearby()
    }

    /**
     * Radius o'zgartirish
     */
    fun onRadiusChange(radiusKm: Int) {
        _uiState.update { it.copy(radiusKm = radiusKm) }
        loadNearby()
    }

    /**
     * E'lon tanlash (xaritada marker bosilganda)
     */
    fun onListingSelected(listingId: String?) {
        _uiState.update { it.copy(selectedListingId = listingId) }
    }
}
