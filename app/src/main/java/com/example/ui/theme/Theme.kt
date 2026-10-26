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
    primary = SoftSageDarkPrimary,
    onPrimary = OnSoftSageDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = OnSageContainerDark,
    secondary = SoftTerracottaDark,
    onSecondary = OnSoftSageDark,
    secondaryContainer = DarkCedarContainer,
    onSecondaryContainer = CreamInkDark,
    tertiary = WarmAmberDark,
    background = EveningMossBg,
    onBackground = CreamInkDark,
    surface = DarkCedarSurface,
    onSurface = CreamInkDark,
    surfaceVariant = DarkCedarContainer,
    onSurfaceVariant = MutedMossDark,
    outline = DarkHerbariumOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SageForestPrimary,
    onPrimary = OnSagePrimary,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = OnSageContainerLight,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = OnTerracottaContainerLight,
    tertiary = AmberOchreTertiary,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    background = ParchmentCream,
    onBackground = CharcoalInk,
    surface = WarmMilkSurface,
    onSurface = CharcoalInk,
    surfaceVariant = SoftLinenContainer,
    onSurfaceVariant = WarmBarkMuted,
    outline = HerbariumOutline
)

val BotanicalShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BotanicalShapes,
        content = content
    )
}
