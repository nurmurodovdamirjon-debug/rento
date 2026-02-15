package uz.rento.presentation.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.rento.domain.model.DealType
import uz.rento.domain.model.ListingType

/**
 * FilterSheet — qidiruv filtrlari BottomSheet
 *
 * Foydalanuvchi mulk turi, shartnoma turi, xonalar, narx va
 * qulayliklar bo'yicha filtr qo'yishi mumkin.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    sheetState: SheetState,
    selectedType: ListingType?,
    selectedDealType: DealType?,
    roomsMin: Int?,
    roomsMax: Int?,
    priceMin: Double?,
    priceMax: Double?,
    currency: String?,
    hasFurniture: Boolean?,
    hasParking: Boolean?,
    allowsPets: Boolean?,
    onApply: (
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
    ) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    // Lokal state
    var localType by remember(selectedType) { mutableStateOf(selectedType) }
    var localDealType by remember(selectedDealType) { mutableStateOf(selectedDealType) }
    var localRoomsMin by remember(roomsMin) { mutableStateOf(roomsMin) }
    var localRoomsMax by remember(roomsMax) { mutableStateOf(roomsMax) }
    var localCurrency by remember(currency) { mutableStateOf(currency ?: "UZS") }
    var localHasFurniture by remember(hasFurniture) { mutableStateOf(hasFurniture) }
    var localHasParking by remember(hasParking) { mutableStateOf(hasParking) }
    var localAllowsPets by remember(allowsPets) { mutableStateOf(allowsPets) }

    // Narx diapazoni (slider uchun)
    val maxPrice = if (localCurrency == "USD") 5000f else 50_000_000f
    var localPriceRange by remember(priceMin, priceMax) {
        mutableStateOf(
            (priceMin?.toFloat() ?: 0f)..(priceMax?.toFloat() ?: maxPrice)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sarlavha
            Text(
                "Filtrlar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ===== MULK TURI =====
            Text(
                "Mulk turi",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ListingType.entries.forEach { type ->
                    FilterChip(
                        selected = localType == type,
                        onClick = { localType = if (localType == type) null else type },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== SHARTNOMA TURI =====
            Text(
                "Shartnoma turi",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DealType.entries.forEach { deal ->
                    FilterChip(
                        selected = localDealType == deal,
                        onClick = { localDealType = if (localDealType == deal) null else deal },
                        label = { Text(deal.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== XONALAR SONI =====
            Text(
                "Xonalar soni",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3, 4, 5).forEach { rooms ->
                    FilterChip(
                        selected = localRoomsMin == rooms && localRoomsMax == rooms,
                        onClick = {
                            if (localRoomsMin == rooms && localRoomsMax == rooms) {
                                localRoomsMin = null
                                localRoomsMax = null
                            } else {
                                localRoomsMin = rooms
                                localRoomsMax = rooms
                            }
                        },
                        label = { Text("$rooms xona") }
                    )
                }
                FilterChip(
                    selected = localRoomsMin != null && localRoomsMin!! >= 6,
                    onClick = {
                        if (localRoomsMin != null && localRoomsMin!! >= 6) {
                            localRoomsMin = null
                            localRoomsMax = null
                        } else {
                            localRoomsMin = 6
                            localRoomsMax = null
                        }
                    },
                    label = { Text("6+") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== VALYUTA =====
            Text(
                "Valyuta",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                listOf("UZS", "USD").forEach { curr ->
                    FilterChip(
                        selected = localCurrency == curr,
                        onClick = {
                            localCurrency = curr
                            // Narx diapazonini qayta sozlash
                            val newMax = if (curr == "USD") 5000f else 50_000_000f
                            localPriceRange = 0f..newMax
                        },
                        label = { Text(curr) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== NARX DIAPAZONI =====
            Text(
                "Narx diapazoni",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            val currentMax = if (localCurrency == "USD") 5000f else 50_000_000f
            Text(
                "${formatPrice(localPriceRange.start)} — ${formatPrice(localPriceRange.endInclusive)} $localCurrency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RangeSlider(
                value = localPriceRange,
                onValueChange = { localPriceRange = it },
                valueRange = 0f..currentMax,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ===== QULAYLIKLAR =====
            Text(
                "Qulayliklar",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = localHasFurniture == true,
                    onClick = { localHasFurniture = if (localHasFurniture == true) null else true },
                    label = { Text("Mebel") }
                )
                FilterChip(
                    selected = localHasParking == true,
                    onClick = { localHasParking = if (localHasParking == true) null else true },
                    label = { Text("Parking") }
                )
                FilterChip(
                    selected = localAllowsPets == true,
                    onClick = { localAllowsPets = if (localAllowsPets == true) null else true },
                    label = { Text("Hayvonlar ruxsat") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== TUGMALAR =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tozalash")
                }

                Button(
                    onClick = {
                        onApply(
                            localType,
                            localDealType,
                            localRoomsMin,
                            localRoomsMax,
                            if (localPriceRange.start > 0) localPriceRange.start.toDouble() else null,
                            if (localPriceRange.endInclusive < currentMax) localPriceRange.endInclusive.toDouble() else null,
                            localCurrency,
                            localHasFurniture,
                            localHasParking,
                            localAllowsPets
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Qo'llash")
                }
            }
        }
    }
}

/**
 * Narxni formatlash
 */
private fun formatPrice(value: Float): String {
    return if (value >= 1_000_000) {
        String.format("%.1fM", value / 1_000_000)
    } else if (value >= 1_000) {
        String.format("%.0fK", value / 1_000)
    } else {
        String.format("%.0f", value)
    }
}
