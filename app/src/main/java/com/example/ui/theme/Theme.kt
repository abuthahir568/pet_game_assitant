package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoOnContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = CoralOnSecondaryDark,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = CoralOnContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = MintOnTertiaryDark,
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = MintOnContainerDark,
    background = NightBackgroundDark,
    onBackground = OnSurfaceDark,
    surface = NightSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = NightSurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = CoralSecondaryLight,
    onSecondary = CoralOnSecondaryLight,
    secondaryContainer = CoralContainerLight,
    onSecondaryContainer = CoralOnContainerLight,
    tertiary = MintTertiaryLight,
    onTertiary = MintOnTertiaryLight,
    tertiaryContainer = MintContainerLight,
    onTertiaryContainer = MintOnContainerLight,
    background = CreamBackgroundLight,
    onBackground = OnSurfaceLight,
    surface = CreamSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = CreamSurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

val PetPalShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun PetPalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PetPalShapes,
        content = content
    )
}
