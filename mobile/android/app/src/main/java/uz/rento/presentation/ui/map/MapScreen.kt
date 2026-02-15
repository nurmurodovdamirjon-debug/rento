package uz.rento.presentation.ui.map

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * MapScreen — xarita ekrani (Yandex Maps WebView)
 *
 * Yaqin atrofdagi e'lonlarni xaritada ko'rsatadi.
 * WebView orqali Yandex Maps API ishlatiladi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xarita") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga"
                        )
                    }
                },
                actions = {
                    Text(
                        "${uiState.totalCount} ta e'lon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Yandex Maps WebView
            val mapHtml = remember(uiState.nearbyListings, uiState.centerLat, uiState.centerLng) {
                buildYandexMapHtml(
                    centerLat = uiState.centerLat,
                    centerLng = uiState.centerLng,
                    listings = uiState.nearbyListings
                )
            }

            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onMarkerClick(listingId: String) {
                                    viewModel.onListingSelected(listingId)
                                }
                            },
                            "Android"
                        )

                        loadDataWithBaseURL(
                            "https://api-maps.yandex.ru",
                            mapHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        "https://api-maps.yandex.ru",
                        mapHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading indikator
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .size(32.dp),
                    strokeWidth = 3.dp
                )
            }

            // Radius tanlash chiplari
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(1, 3, 5, 10).forEach { km ->
                    FilterChip(
                        selected = uiState.radiusKm == km,
                        onClick = { viewModel.onRadiusChange(km) },
                        label = { Text("${km} km") }
                    )
                }
            }

            // Tanlangan e'lon kartasi (pastda)
            uiState.selectedListingId?.let { selectedId ->
                val selected = uiState.nearbyListings.find { it.listing.id == selectedId }
                if (selected != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        onClick = { onNavigateToDetail(selectedId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    selected.listing.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    selected.listing.formattedPrice,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    selected.formattedDistance,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Yandex Maps HTML generatsiya (WebView uchun)
 */
private fun buildYandexMapHtml(
    centerLat: Double,
    centerLng: Double,
    listings: List<uz.rento.domain.model.NearbyListing>
): String {
    val markersJs = listings.mapNotNull { nearby ->
        val lat = nearby.latitude ?: return@mapNotNull null
        val lng = nearby.longitude ?: return@mapNotNull null
        val price = nearby.listing.formattedPrice.replace("'", "\\'")
        val title = nearby.listing.title.replace("'", "\\'").take(40)
        val id = nearby.listing.id
        val isPremium = nearby.listing.isPremium
        val color = if (isPremium) "#FF6B00" else "#1976D2"

        """
        var pm_${'$'}{$id.replace('-','')} = new ymaps.Placemark(
            [$lat, $lng],
            {
                balloonContentHeader: '$title',
                balloonContentBody: '<b>$price</b>',
                hintContent: '$price'
            },
            {
                preset: 'islands#dotIcon',
                iconColor: '$color'
            }
        );
        pm_${'$'}{$id.replace('-','')}.events.add('click', function() {
            Android.onMarkerClick('$id');
        });
        myMap.geoObjects.add(pm_${'$'}{$id.replace('-','')});
        """.trimIndent()
    }.joinToString("\n")

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }
        </style>
        <script src="https://api-maps.yandex.ru/2.1/?apikey=&lang=uz_UZ" type="text/javascript"></script>
    </head>
    <body>
        <div id="map"></div>
        <script type="text/javascript">
            ymaps.ready(function() {
                var myMap = new ymaps.Map('map', {
                    center: [$centerLat, $centerLng],
                    zoom: 13,
                    controls: ['zoomControl', 'geolocationControl']
                });
                
                $markersJs
            });
        </script>
    </body>
    </html>
    """.trimIndent()
}
