package com.squalor.consecutor.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {
    @Test
    fun `contrast of black on white is 21`() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.000001)
        assertEquals(21.0, contrastRatio(0xFFFFFFFF, 0xFF000000), 0.000001)
        assertEquals(1.0, contrastRatio(0xFF18242D, 0xFF18242D), 0.000001)
    }

    @Test
    fun `dark palette text pairs meet 4_5 to 1`() {
        with(DarkPalette) {
            assertTextContrast("onPrimary on primary", onPrimary, primary)
            assertTextContrast("onSurface on surface", onSurface, surface)
            assertTextContrast("onBackground on background", onBackground, background)
            assertTextContrast("onSurfaceVariant on surfaceVariant", onSurfaceVariant, surfaceVariant)
            assertTextContrast("tertiary on surface", tertiary, surface)
            assertTextContrast("onPrimaryContainer on primaryContainer", onPrimaryContainer, primaryContainer)
            assertTextContrast("onSecondaryContainer on secondaryContainer", onSecondaryContainer, secondaryContainer)
            assertTextContrast("onSurface on surfaceContainerHighest", onSurface, surfaceContainerHighest)
            assertTextContrast("onSurface on surfaceContainerHigh", onSurface, surfaceContainerHigh)
            assertTextContrast("onError on error", onError, error)
            assertTextContrast("onErrorContainer on errorContainer", onErrorContainer, errorContainer)
            assertTextContrast("onSecondary on secondary", onSecondary, secondary)
            assertTextContrast("onTertiary on tertiary", onTertiary, tertiary)
            assertTextContrast("onTertiaryContainer on tertiaryContainer", onTertiaryContainer, tertiaryContainer)
            assertTextContrast("tertiary on surfaceContainer", tertiary, surfaceContainer)
            assertTextContrast("tertiary on surfaceContainerHigh", tertiary, surfaceContainerHigh)
            assertTextContrast("tertiary on surfaceContainerHighest", tertiary, surfaceContainerHighest)
            assertTextContrast("inverseOnSurface on inverseSurface", inverseOnSurface, inverseSurface)
            assertTextContrast("inversePrimary on inverseSurface", inversePrimary, inverseSurface)
            assertTextContrast("onPrimaryFixed on primaryFixed", onPrimaryFixed, primaryFixed)
            assertTextContrast("onPrimaryFixed on primaryFixedDim", onPrimaryFixed, primaryFixedDim)
            assertTextContrast("onPrimaryFixedVariant on primaryFixed", onPrimaryFixedVariant, primaryFixed)
            assertTextContrast("onPrimaryFixedVariant on primaryFixedDim", onPrimaryFixedVariant, primaryFixedDim)
            assertTextContrast("onSecondaryFixed on secondaryFixed", onSecondaryFixed, secondaryFixed)
            assertTextContrast("onSecondaryFixed on secondaryFixedDim", onSecondaryFixed, secondaryFixedDim)
            assertTextContrast("onSecondaryFixedVariant on secondaryFixed", onSecondaryFixedVariant, secondaryFixed)
            assertTextContrast("onSecondaryFixedVariant on secondaryFixedDim", onSecondaryFixedVariant, secondaryFixedDim)
            assertTextContrast("onTertiaryFixed on tertiaryFixed", onTertiaryFixed, tertiaryFixed)
            assertTextContrast("onTertiaryFixed on tertiaryFixedDim", onTertiaryFixed, tertiaryFixedDim)
            assertTextContrast("onTertiaryFixedVariant on tertiaryFixed", onTertiaryFixedVariant, tertiaryFixed)
            assertTextContrast("onTertiaryFixedVariant on tertiaryFixedDim", onTertiaryFixedVariant, tertiaryFixedDim)
        }
    }

    @Test
    fun `light palette text pairs meet 4_5 to 1`() {
        with(LightPalette) {
            assertTextContrast("onPrimary on primary", onPrimary, primary)
            assertTextContrast("onSurface on surface", onSurface, surface)
            assertTextContrast("onBackground on background", onBackground, background)
            assertTextContrast("onSurfaceVariant on surfaceVariant", onSurfaceVariant, surfaceVariant)
            assertTextContrast("tertiary on surface", tertiary, surface)
            assertTextContrast("onPrimaryContainer on primaryContainer", onPrimaryContainer, primaryContainer)
            assertTextContrast("onSecondaryContainer on secondaryContainer", onSecondaryContainer, secondaryContainer)
            assertTextContrast("onSurface on surfaceContainerHighest", onSurface, surfaceContainerHighest)
            assertTextContrast("onSurface on surfaceContainerHigh", onSurface, surfaceContainerHigh)
            assertTextContrast("onError on error", onError, error)
            assertTextContrast("onErrorContainer on errorContainer", onErrorContainer, errorContainer)
            assertTextContrast("onSecondary on secondary", onSecondary, secondary)
            assertTextContrast("onTertiary on tertiary", onTertiary, tertiary)
            assertTextContrast("onTertiaryContainer on tertiaryContainer", onTertiaryContainer, tertiaryContainer)
            assertTextContrast("tertiary on surfaceContainer", tertiary, surfaceContainer)
            assertTextContrast("tertiary on surfaceContainerHigh", tertiary, surfaceContainerHigh)
            assertTextContrast("tertiary on surfaceContainerHighest", tertiary, surfaceContainerHighest)
            assertTextContrast("inverseOnSurface on inverseSurface", inverseOnSurface, inverseSurface)
            assertTextContrast("inversePrimary on inverseSurface", inversePrimary, inverseSurface)
            assertTextContrast("onPrimaryFixed on primaryFixed", onPrimaryFixed, primaryFixed)
            assertTextContrast("onPrimaryFixed on primaryFixedDim", onPrimaryFixed, primaryFixedDim)
            assertTextContrast("onPrimaryFixedVariant on primaryFixed", onPrimaryFixedVariant, primaryFixed)
            assertTextContrast("onPrimaryFixedVariant on primaryFixedDim", onPrimaryFixedVariant, primaryFixedDim)
            assertTextContrast("onSecondaryFixed on secondaryFixed", onSecondaryFixed, secondaryFixed)
            assertTextContrast("onSecondaryFixed on secondaryFixedDim", onSecondaryFixed, secondaryFixedDim)
            assertTextContrast("onSecondaryFixedVariant on secondaryFixed", onSecondaryFixedVariant, secondaryFixed)
            assertTextContrast("onSecondaryFixedVariant on secondaryFixedDim", onSecondaryFixedVariant, secondaryFixedDim)
            assertTextContrast("onTertiaryFixed on tertiaryFixed", onTertiaryFixed, tertiaryFixed)
            assertTextContrast("onTertiaryFixed on tertiaryFixedDim", onTertiaryFixed, tertiaryFixedDim)
            assertTextContrast("onTertiaryFixedVariant on tertiaryFixed", onTertiaryFixedVariant, tertiaryFixed)
            assertTextContrast("onTertiaryFixedVariant on tertiaryFixedDim", onTertiaryFixedVariant, tertiaryFixedDim)
        }
    }

    private fun assertTextContrast(pair: String, foreground: Long, background: Long) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$pair has contrast $ratio, expected at least 4.5", ratio >= 4.5)
    }
}
