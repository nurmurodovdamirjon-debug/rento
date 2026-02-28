package uz.rento.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.rento.domain.model.DealType
import uz.rento.domain.model.ListingType
import uz.rento.presentation.components.EmptyStateView
import uz.rento.presentation.components.ErrorView
import uz.rento.presentation.components.NetworkErrorView
import uz.rento.presentation.components.ShimmerList

/**
 * HomeScreen — asosiy e'lonlar ro'yxati
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCreate: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Infinite scroll: oxirgi element ko'ringanda loadMore
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= uiState.listings.size - 3 && uiState.hasMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Rento",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.totalCount > 0) {
                            Text(
                                "${uiState.totalCount} ta e'lon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips
                FilterChipsRow(
                    selectedType = uiState.selectedType,
                    selectedDealType = uiState.selectedDealType,
                    selectedCity = uiState.selectedCity,
                    onTypeSelected = { viewModel.setType(it) },
                    onDealTypeSelected = { viewModel.setDealType(it) },
                    onCitySelected = { viewModel.setCity(it) }
                )

                when {
                    uiState.isLoading && uiState.listings.isEmpty() -> {
                        ShimmerList(count = 4)
                    }

                    uiState.error != null && uiState.listings.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.error?.contains("internet", ignoreCase = true) == true ||
                                uiState.error?.contains("network", ignoreCase = true) == true ||
                                uiState.error?.contains("connection", ignoreCase = true) == true) {
                                NetworkErrorView(onRetry = { viewModel.refresh() })
                            } else {
                                ErrorView(
                                    message = uiState.error ?: "Xatolik yuz berdi",
                                    onRetry = { viewModel.refresh() }
                                )
                            }
                        }
                    }

                    uiState.listings.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                message = "E'lonlar topilmadi",
                                icon = Icons.Default.SearchOff,
                                description = "Filtrlarni o'zgartirib ko'ring",
                                actionText = "Filtrni tozalash",
                                onAction = { viewModel.refresh() }
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = uiState.listings,
                                key = { it.id }
                            ) { listing ->
                                ListingCard(
                                    listing = listing,
                                    onClick = { onNavigateToDetail(listing.id) },
                                    isFavorite = listing.id in uiState.favoriteIds,
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }

                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * FilterChipsRow — filtr chiplari qatori
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedType: ListingType?,
    selectedDealType: DealType?,
    selectedCity: String?,
    onTypeSelected: (ListingType?) -> Unit,
    onDealTypeSelected: (DealType?) -> Unit,
    onCitySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Deal type chips
        items(DealType.entries.toList()) { dealType ->
            FilterChip(
                selected = selectedDealType == dealType,
                onClick = {
                    onDealTypeSelected(if (selectedDealType == dealType) null else dealType)
                },
                label = { Text(dealType.displayName) }
            )
        }

        // Type chips
        items(ListingType.entries.toList()) { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = {
                    onTypeSelected(if (selectedType == type) null else type)
                },
                label = { Text(type.displayName) }
            )
        }

        // City chips
        val cities = listOf("tashkent", "samarkand", "bukhara", "namangan", "fergana")
        val cityNames = mapOf(
            "tashkent" to "Toshkent",
            "samarkand" to "Samarqand",
            "bukhara" to "Buxoro",
            "namangan" to "Namangan",
            "fergana" to "Farg'ona"
        )
        items(cities) { city ->
            FilterChip(
                selected = selectedCity == city,
                onClick = {
                    onCitySelected(if (selectedCity == city) null else city)
                },
                label = { Text(cityNames[city] ?: city) }
            )
        }
    }
}
