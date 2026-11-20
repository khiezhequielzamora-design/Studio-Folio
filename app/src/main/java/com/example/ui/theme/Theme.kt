package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SageMint,
    onPrimary = OnSageMint,
    primaryContainer = SageMintContainer,
    onPrimaryContainer = OnSageMintContainer,
    secondary = WarmCoral,
    onSecondary = OnWarmCoral,
    secondaryContainer = WarmCoralContainer,
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = SoftGold,
    onTertiary = Color(0xFF3A2700),
    tertiaryContainer = SoftGoldContainer,
    onTertiaryContainer = Color(0xFFFFE5AE),
    background = MidnightBackground,
    onBackground = ChalkInk,
    surface = MidnightSurface,
    onSurface = ChalkInk,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = ChalkSecondary,
    outline = OutlineDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = ScholarGreen,
    onPrimary = Color.White,
    primaryContainer = ScholarGreenContainer,
    onPrimaryContainer = OnScholarGreenContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = OnAmberContainer,
    background = ParchmentBackground,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = OutlineWarm,
    error = SemanticError,
    onError = Color.White
)

val StudyFolioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun StudyFolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudyFolioShapes,
        content = content
    )
}
