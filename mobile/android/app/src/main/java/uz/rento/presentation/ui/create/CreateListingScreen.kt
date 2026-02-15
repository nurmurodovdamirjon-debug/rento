package uz.rento.presentation.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.rento.presentation.components.RentoButton

/**
 * CreateListingScreen — ko'p bosqichli e'lon yaratish ekrani
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // E'lon yaratilganda — detail sahifasiga o'tish
    LaunchedEffect(uiState.createdListing) {
        uiState.createdListing?.let { listing ->
            onCreated(listing.id)
        }
    }

    // Xatolik snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val stepTitles = listOf(
        "Asosiy ma'lumotlar",
        "Joylashuv",
        "Parametrlar",
        "Qulayliklar"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("E'lon yaratish")
                        Text(
                            "${uiState.currentStep + 1}/${uiState.totalSteps}: ${stepTitles[uiState.currentStep]}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep > 0) viewModel.previousStep()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1).toFloat() / uiState.totalSteps },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                when (uiState.currentStep) {
                    0 -> Step1BasicInfo(uiState, viewModel)
                    1 -> Step2Location(uiState, viewModel)
                    2 -> Step3Parameters(uiState, viewModel)
                    3 -> Step4Amenities(uiState, viewModel)
                }
            }

            // Navbatdagi/Yaratish tugmasi
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (uiState.currentStep == uiState.totalSteps - 1) {
                    RentoButton(
                        text = "E'lon yaratish",
                        onClick = { viewModel.submitListing() },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    RentoButton(
                        text = "Keyingi",
                        onClick = { viewModel.nextStep() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Step1BasicInfo(
    state: CreateListingUiState,
    viewModel: CreateListingViewModel
) {
    SectionTitle("Mulk turi")
    Spacer(modifier = Modifier.height(8.dp))

    val types = listOf(
        "apartment" to "Kvartira",
        "house" to "Uy",
        "office" to "Ofis",
        "shop" to "Do'kon",
        "warehouse" to "Ombor"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (value, label) ->
            FilterChip(
                selected = state.type == value,
                onClick = { viewModel.updateType(value) },
                label = { Text(label) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SectionTitle("Shartnoma turi")
    Spacer(modifier = Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.dealType == "rent",
            onClick = { viewModel.updateDealType("rent") },
            label = { Text("Ijara") }
        )
        FilterChip(
            selected = state.dealType == "daily",
            onClick = { viewModel.updateDealType("daily") },
            label = { Text("Kunlik") }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.title,
        onValueChange = { viewModel.updateTitle(it) },
        label = { Text("Sarlavha *") },
        supportingText = {
            Text(
                state.titleError ?: "${state.title.length}/200",
                color = if (state.titleError != null)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        isError = state.titleError != null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.description,
        onValueChange = { viewModel.updateDescription(it) },
        label = { Text("Tavsif") },
        supportingText = { Text("${state.description.length}/5000") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6
    )
}

@Composable
private fun Step2Location(
    state: CreateListingUiState,
    viewModel: CreateListingViewModel
) {
    SectionTitle("Shahar")
    Spacer(modifier = Modifier.height(8.dp))

    val cities = listOf(
        "tashkent" to "Toshkent",
        "samarkand" to "Samarqand",
        "bukhara" to "Buxoro",
        "namangan" to "Namangan",
        "fergana" to "Farg'ona",
        "andijan" to "Andijon",
        "nukus" to "Nukus"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.take(4).forEach { (value, label) ->
            FilterChip(
                selected = state.city == value,
                onClick = { viewModel.updateCity(value) },
                label = { Text(label) }
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.drop(4).forEach { (value, label) ->
            FilterChip(
                selected = state.city == value,
                onClick = { viewModel.updateCity(value) },
                label = { Text(label) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = state.district,
        onValueChange = { viewModel.updateDistrict(it) },
        label = { Text("Tuman") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.address,
        onValueChange = { viewModel.updateAddress(it) },
        label = { Text("Manzil") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = state.landmark,
        onValueChange = { viewModel.updateLandmark(it) },
        label = { Text("Mo'ljal") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun Step3Parameters(
    state: CreateListingUiState,
    viewModel: CreateListingViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.rooms,
            onValueChange = { viewModel.updateRooms(it) },
            label = { Text("Xonalar") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = state.floor,
            onValueChange = { viewModel.updateFloor(it) },
            label = { Text("Qavat") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        OutlinedTextField(
            value = state.totalFloors,
            onValueChange = { viewModel.updateTotalFloors(it) },
            label = { Text("Jami") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = state.areaSqm,
        onValueChange = { viewModel.updateAreaSqm(it) },
        label = { Text("Maydon (m²)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    SectionTitle("Narx")
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.price,
            onValueChange = { viewModel.updatePrice(it) },
            label = { Text("Narx *") },
            supportingText = {
                state.priceError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            },
            isError = state.priceError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(2f),
            singleLine = true
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.currency == "UZS",
                    onClick = { viewModel.updateCurrency("UZS") },
                    label = { Text("UZS") }
                )
                FilterChip(
                    selected = state.currency == "USD",
                    onClick = { viewModel.updateCurrency("USD") },
                    label = { Text("USD") }
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = state.priceNegotiable,
            onCheckedChange = { viewModel.updatePriceNegotiable(it) }
        )
        Text("Narx kelishiladi", style = MaterialTheme.typography.bodyMedium)
    }

    OutlinedTextField(
        value = state.depositAmount,
        onValueChange = { viewModel.updateDepositAmount(it) },
        label = { Text("Kafolat puli") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun Step4Amenities(
    state: CreateListingUiState,
    viewModel: CreateListingViewModel
) {
    SectionTitle("Qulayliklar")
    Spacer(modifier = Modifier.height(12.dp))

    val amenities = listOf(
        Triple("Mebel", state.hasFurniture) { v: Boolean -> viewModel.updateHasFurniture(v) },
        Triple("Maishiy texnika", state.hasAppliances) { v: Boolean -> viewModel.updateHasAppliances(v) },
        Triple("Internet", state.hasInternet) { v: Boolean -> viewModel.updateHasInternet(v) },
        Triple("Parking", state.hasParking) { v: Boolean -> viewModel.updateHasParking(v) },
        Triple("Konditsioner", state.hasConditioner) { v: Boolean -> viewModel.updateHasConditioner(v) },
        Triple("Hayvonlar ruxsat", state.allowsPets) { v: Boolean -> viewModel.updateAllowsPets(v) },
        Triple("Bolalar ruxsat", state.allowsChildren) { v: Boolean -> viewModel.updateAllowsChildren(v) },
        Triple("Kommunal xizmatlar kiritilgan", state.utilitiesIncluded) { v: Boolean -> viewModel.updateUtilitiesIncluded(v) }
    )

    amenities.forEach { (label, checked, onCheckedChange) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
