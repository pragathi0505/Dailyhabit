package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = MintTeal,
    onPrimary = OnMintTeal,
    primaryContainer = DeepTealContainer,
    onPrimaryContainer = OnDeepTealContainer,
    secondary = WarmGold,
    onSecondary = OnWarmGold,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = OnAmberContainerDark,
    tertiary = Periwinkle,
    onTertiary = OnPeriwinkle,
    tertiaryContainer = IndigoContainerDark,
    onTertiaryContainer = OnIndigoContainerDark,
    background = ObsidianTeal,
    onBackground = IvoryText,
    surface = DarkCard,
    onSurface = IvoryText,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = MutedAquaText,
    outline = DarkTealOutline,
    outlineVariant = DarkTealOutline,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForestTeal,
    onPrimary = OnForestTeal,
    primaryContainer = SageMist,
    onPrimaryContainer = OnSageMist,
    secondary = MorningAmber,
    onSecondary = OnMorningAmber,
    secondaryContainer = HoneyGlow,
    onSecondaryContainer = OnHoneyGlow,
    tertiary = ScholarIndigo,
    onTertiary = OnScholarIndigo,
    tertiaryContainer = LavenderMist,
    onTertiaryContainer = OnLavenderMist,
    background = WarmIvory,
    onBackground = InkCharcoal,
    surface = WarmWhite,
    onSurface = InkCharcoal,
    surfaceVariant = SandSurface,
    onSurfaceVariant = SlateMuted,
    outline = SandOutline,
    outlineVariant = SandOutline,
  )

val VitalPrepShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
  )

@Composable
fun VitalPrepTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = VitalPrepShapes,
    content = content,
  )
}
