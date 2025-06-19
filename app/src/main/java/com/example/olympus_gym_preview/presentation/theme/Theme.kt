// Theme.kt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

@Composable
fun OlympusGymTheme(
    content: @Composable () -> Unit
) {
    val wearColorPalette = Colors(
        primary = Color(0xFFD9303D),
        secondary = Color(0xFF232323),
        error = Color(0xFF55CD86),
        background = Color(0xFF2A2A2A),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onError = Color.White
    )

    MaterialTheme(
        colors = wearColorPalette,
        content = content
    )
}