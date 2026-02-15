package uz.rento.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effekti — yuklanish vaqtida skeleton placeholder
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    height: Dp = 16.dp,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

/**
 * Listing card skeleton — shimmer placeholder
 */
@Composable
fun ListingCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rasm placeholder
        ShimmerEffect(
            height = 180.dp,
            shape = RoundedCornerShape(12.dp)
        )
        // Sarlavha
        ShimmerEffect(widthFraction = 0.7f, height = 20.dp)
        // Manzil
        ShimmerEffect(widthFraction = 0.5f, height = 14.dp)
        // Narx
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerEffect(
                widthFraction = 0.3f,
                height = 18.dp
            )
            ShimmerEffect(
                modifier = Modifier.size(32.dp),
                height = 32.dp,
                shape = RoundedCornerShape(50)
            )
        }
    }
}

/**
 * Chat item skeleton
 */
@Composable
fun ChatItemSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        ShimmerEffect(
            modifier = Modifier.size(48.dp),
            height = 48.dp,
            shape = RoundedCornerShape(50)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerEffect(widthFraction = 0.6f, height = 16.dp)
            ShimmerEffect(widthFraction = 0.9f, height = 12.dp)
        }
    }
}

/**
 * Shimmer ro'yxat — n ta skeleton element
 */
@Composable
fun ShimmerList(
    count: Int = 5,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { ListingCardSkeleton() }
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            content()
        }
    }
}
