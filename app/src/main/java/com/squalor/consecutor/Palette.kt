package com.squalor.consecutor.ui.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Opaque ARGB values, independent of Android and Compose for JVM contrast checks.
internal object DarkPalette {
    const val primary: Long = 0xFF6EE7D2
    const val onPrimary: Long = 0xFF0F1419
    const val primaryContainer: Long = 0xFF134E48
    const val onPrimaryContainer: Long = 0xFFC9F7EE
    const val inversePrimary: Long = 0xFF0F766E
    const val secondary: Long = 0xFFB4C0C8
    const val onSecondary: Long = 0xFF0F1419
    const val secondaryContainer: Long = 0xFF2C3F4C
    const val onSecondaryContainer: Long = 0xFFF4EFE7
    const val tertiary: Long = 0xFFF6C177
    const val onTertiary: Long = 0xFF0F1419
    const val tertiaryContainer: Long = 0xFF673B00
    const val onTertiaryContainer: Long = 0xFFFFDCB0
    const val background: Long = 0xFF0F1419
    const val onBackground: Long = 0xFFF4EFE7
    const val surface: Long = 0xFF18242D
    const val onSurface: Long = 0xFFF4EFE7
    const val surfaceVariant: Long = 0xFF223542
    const val onSurfaceVariant: Long = 0xFFB4C0C8
    const val surfaceTint: Long = 0xFF6EE7D2
    const val inverseSurface: Long = 0xFFF4EFE7
    const val inverseOnSurface: Long = 0xFF18242D
    const val error: Long = 0xFFFFB4AB
    const val onError: Long = 0xFF690005
    const val errorContainer: Long = 0xFF93000A
    const val onErrorContainer: Long = 0xFFFFDAD6
    const val outline: Long = 0xFF6B7D89
    const val outlineVariant: Long = 0xFF2F4250
    const val scrim: Long = 0xFF000000
    const val surfaceBright: Long = 0xFF2A3F4E
    const val surfaceDim: Long = 0xFF0F1419
    const val surfaceContainer: Long = 0xFF18242D
    const val surfaceContainerHigh: Long = 0xFF223542
    const val surfaceContainerHighest: Long = 0xFF2A3F4E
    const val surfaceContainerLow: Long = 0xFF141E26
    const val surfaceContainerLowest: Long = 0xFF0B1015
    const val primaryFixed: Long = 0xFFCCEFE9
    const val primaryFixedDim: Long = 0xFF6EE7D2
    const val onPrimaryFixed: Long = 0xFF0A3D38
    const val onPrimaryFixedVariant: Long = 0xFF134E48
    const val secondaryFixed: Long = 0xFFE3EAEE
    const val secondaryFixedDim: Long = 0xFFB4C0C8
    const val onSecondaryFixed: Long = 0xFF18242D
    const val onSecondaryFixedVariant: Long = 0xFF2C3F4C
    const val tertiaryFixed: Long = 0xFFF6C177
    const val tertiaryFixedDim: Long = 0xFFF0AD52
    const val onTertiaryFixed: Long = 0xFF3F2100
    const val onTertiaryFixedVariant: Long = 0xFF673B00
}

internal object LightPalette {
    const val primary: Long = 0xFF0F766E
    const val onPrimary: Long = 0xFFFFFFFF
    const val primaryContainer: Long = 0xFFCCEFE9
    const val onPrimaryContainer: Long = 0xFF0A3D38
    const val inversePrimary: Long = 0xFF6EE7D2
    const val secondary: Long = 0xFF5C6B75
    const val onSecondary: Long = 0xFFFFFFFF
    const val secondaryContainer: Long = 0xFFE3EAEE
    const val onSecondaryContainer: Long = 0xFF18242D
    const val tertiary: Long = 0xFFA14808
    const val onTertiary: Long = 0xFFFFFFFF
    const val tertiaryContainer: Long = 0xFFFFDCB0
    const val onTertiaryContainer: Long = 0xFF3F2100
    const val background: Long = 0xFFF7F4EF
    const val onBackground: Long = 0xFF18242D
    const val surface: Long = 0xFFFFFFFF
    const val onSurface: Long = 0xFF18242D
    const val surfaceVariant: Long = 0xFFE6ECEF
    const val onSurfaceVariant: Long = 0xFF5C6B75
    const val surfaceTint: Long = 0xFF0F766E
    const val inverseSurface: Long = 0xFF18242D
    const val inverseOnSurface: Long = 0xFFF4EFE7
    const val error: Long = 0xFFBA1A1A
    const val onError: Long = 0xFFFFFFFF
    const val errorContainer: Long = 0xFFFFDAD6
    const val onErrorContainer: Long = 0xFF410002
    const val outline: Long = 0xFF8A979F
    const val outlineVariant: Long = 0xFFD4DDE3
    const val scrim: Long = 0xFF000000
    const val surfaceBright: Long = 0xFFFFFFFF
    const val surfaceDim: Long = 0xFFE6E0D7
    const val surfaceContainer: Long = 0xFFF3EFE8
    const val surfaceContainerHigh: Long = 0xFFEDE8E0
    const val surfaceContainerHighest: Long = 0xFFE6E0D7
    const val surfaceContainerLow: Long = 0xFFFBF9F5
    const val surfaceContainerLowest: Long = 0xFFFFFFFF
    const val primaryFixed: Long = 0xFFCCEFE9
    const val primaryFixedDim: Long = 0xFF6EE7D2
    const val onPrimaryFixed: Long = 0xFF0A3D38
    const val onPrimaryFixedVariant: Long = 0xFF134E48
    const val secondaryFixed: Long = 0xFFE3EAEE
    const val secondaryFixedDim: Long = 0xFFB4C0C8
    const val onSecondaryFixed: Long = 0xFF18242D
    const val onSecondaryFixedVariant: Long = 0xFF2C3F4C
    const val tertiaryFixed: Long = 0xFFF6C177
    const val tertiaryFixedDim: Long = 0xFFF0AD52
    const val onTertiaryFixed: Long = 0xFF3F2100
    const val onTertiaryFixedVariant: Long = 0xFF673B00
}

/** WCAG contrast ratio for the opaque foreground and background colors. */
internal fun contrastRatio(foreground: Long, background: Long): Double {
    val foregroundLuminance = relativeLuminance(foreground)
    val backgroundLuminance = relativeLuminance(background)
    return (max(foregroundLuminance, backgroundLuminance) + 0.05) /
        (min(foregroundLuminance, backgroundLuminance) + 0.05)
}

private fun relativeLuminance(color: Long): Double {
    fun linearChannel(shift: Int): Double {
        val channel = ((color shr shift) and 0xFF).toDouble() / 255.0
        return if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linearChannel(16) + 0.7152 * linearChannel(8) + 0.0722 * linearChannel(0)
}
