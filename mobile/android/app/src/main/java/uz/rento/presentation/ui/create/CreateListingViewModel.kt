package uz.rento.presentation.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.rento.domain.model.Listing
import uz.rento.domain.usecase.listing.CreateListingUseCase
import javax.inject.Inject

data class CreateListingUiState(
    // Step tracking
    val currentStep: Int = 0,
    val totalSteps: Int = 4,

    // Step 1: Asosiy ma'lumotlar
    val type: String = "apartment",
    val dealType: String = "rent",
    val title: String = "",
    val description: String = "",

    // Step 2: Joylashuv
    val city: String = "tashkent",
    val district: String = "",
    val address: String = "",
    val landmark: String = "",

    // Step 3: Parametrlar
    val rooms: String = "",
    val floor: String = "",
    val totalFloors: String = "",
    val areaSqm: String = "",
    val price: String = "",
    val currency: String = "UZS",
    val priceNegotiable: Boolean = false,
    val depositAmount: String = "",

    // Step 4: Qulayliklar
    val hasFurniture: Boolean = false,
    val hasAppliances: Boolean = false,
    val hasInternet: Boolean = false,
    val hasParking: Boolean = false,
    val hasConditioner: Boolean = false,
    val allowsPets: Boolean = false,
    val allowsChildren: Boolean = true,
    val utilitiesIncluded: Boolean = false,

    // State
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdListing: Listing? = null,
    val titleError: String? = null,
    val priceError: String? = null
)

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val createListingUseCase: CreateListingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateListingUiState())
    val uiState: StateFlow<CreateListingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val state = _uiState.value
        if (state.currentStep < state.totalSteps - 1) {
            // Validatsiya
            when (state.currentStep) {
                0 -> {
                    if (state.title.length < 5) {
                        _uiState.update { it.copy(titleError = "Kamida 5 ta belgi") }
                        return
                    }
                }
                2 -> {
                    val price = state.price.toDoubleOrNull()
                    if (price == null || price <= 0) {
                        _uiState.update { it.copy(priceError = "Narxni kiriting") }
                        return
                    }
                }
            }
            _uiState.update { it.copy(currentStep = state.currentStep + 1) }
        }
    }

    fun previousStep() {
        val state = _uiState.value
        if (state.currentStep > 0) {
            _uiState.update { it.copy(currentStep = state.currentStep - 1) }
        }
    }

    // Step 1 updates
    fun updateType(type: String) = _uiState.update { it.copy(type = type) }
    fun updateDealType(dealType: String) = _uiState.update { it.copy(dealType = dealType) }
    fun updateTitle(title: String) = _uiState.update { it.copy(title = title, titleError = null) }
    fun updateDescription(desc: String) = _uiState.update { it.copy(description = desc) }

    // Step 2 updates
    fun updateCity(city: String) = _uiState.update { it.copy(city = city) }
    fun updateDistrict(district: String) = _uiState.update { it.copy(district = district) }
    fun updateAddress(address: String) = _uiState.update { it.copy(address = address) }
    fun updateLandmark(landmark: String) = _uiState.update { it.copy(landmark = landmark) }

    // Step 3 updates
    fun updateRooms(rooms: String) = _uiState.update { it.copy(rooms = rooms) }
    fun updateFloor(floor: String) = _uiState.update { it.copy(floor = floor) }
    fun updateTotalFloors(floors: String) = _uiState.update { it.copy(totalFloors = floors) }
    fun updateAreaSqm(area: String) = _uiState.update { it.copy(areaSqm = area) }
    fun updatePrice(price: String) = _uiState.update { it.copy(price = price, priceError = null) }
    fun updateCurrency(currency: String) = _uiState.update { it.copy(currency = currency) }
    fun updatePriceNegotiable(v: Boolean) = _uiState.update { it.copy(priceNegotiable = v) }
    fun updateDepositAmount(amount: String) = _uiState.update { it.copy(depositAmount = amount) }

    // Step 4 updates
    fun updateHasFurniture(v: Boolean) = _uiState.update { it.copy(hasFurniture = v) }
    fun updateHasAppliances(v: Boolean) = _uiState.update { it.copy(hasAppliances = v) }
    fun updateHasInternet(v: Boolean) = _uiState.update { it.copy(hasInternet = v) }
    fun updateHasParking(v: Boolean) = _uiState.update { it.copy(hasParking = v) }
    fun updateHasConditioner(v: Boolean) = _uiState.update { it.copy(hasConditioner = v) }
    fun updateAllowsPets(v: Boolean) = _uiState.update { it.copy(allowsPets = v) }
    fun updateAllowsChildren(v: Boolean) = _uiState.update { it.copy(allowsChildren = v) }
    fun updateUtilitiesIncluded(v: Boolean) = _uiState.update { it.copy(utilitiesIncluded = v) }

    fun submitListing() {
        val state = _uiState.value

        // Final validatsiya
        if (state.title.length < 5) {
            _uiState.update { it.copy(error = "Sarlavha kamida 5 ta belgi bo'lishi kerak") }
            return
        }
        val price = state.price.toDoubleOrNull()
        if (price == null || price <= 0) {
            _uiState.update { it.copy(error = "Narxni to'g'ri kiriting") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            createListingUseCase(
                type = state.type,
                dealType = state.dealType,
                city = state.city,
                district = state.district.ifBlank { null },
                address = state.address.ifBlank { null },
                landmark = state.landmark.ifBlank { null },
                latitude = null,
                longitude = null,
                rooms = state.rooms.toIntOrNull(),
                floor = state.floor.toIntOrNull(),
                totalFloors = state.totalFloors.toIntOrNull(),
                areaSqm = state.areaSqm.toDoubleOrNull(),
                price = price,
                currency = state.currency,
                priceNegotiable = state.priceNegotiable,
                hasFurniture = state.hasFurniture,
                hasAppliances = state.hasAppliances,
                hasInternet = state.hasInternet,
                hasParking = state.hasParking,
                hasConditioner = state.hasConditioner,
                allowsPets = state.allowsPets,
                allowsChildren = state.allowsChildren,
                utilitiesIncluded = state.utilitiesIncluded,
                depositAmount = state.depositAmount.toDoubleOrNull(),
                title = state.title,
                description = state.description.ifBlank { null }
            ).fold(
                onSuccess = { listing ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            createdListing = listing
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "E'lon yaratishda xatolik"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
