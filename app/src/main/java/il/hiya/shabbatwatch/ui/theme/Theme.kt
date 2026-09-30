package il.hiya.shabbatwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val ShabbatColors = Colors(
    primary = Color(0xFFE8E8E8),
    primaryVariant = Color(0xFFBDBDBD),
    secondary = Color(0xFF9E9E9E),
    background = Color.Black,
    surface = Color(0xFF1A1A1A),
    error = Color(0xFFFF6E6E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
)

@Composable
fun ShabbatWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = ShabbatColors, content = content)
}
