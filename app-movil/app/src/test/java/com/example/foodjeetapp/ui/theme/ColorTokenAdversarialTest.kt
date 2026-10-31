package com.example.foodjeetapp.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Adversarial Challenge Test Suite for Milestone 1:
 * Validates exact ARGB token values, backward compatibility,
 * downstream resolution, and edge case color scheme overrides.
 */
class ColorTokenAdversarialTest {

    // Helper to extract 32-bit unsigned ARGB hex from Compose Color (which stores ARGB in upper 32 bits)
    private fun Color.argbHex(): Long = (this.value shr 32).toLong() and 0xFFFFFFFFL

    // =========================================================================
    // 1. Exact ARGB Token Assertions
    // =========================================================================

    @Test
    fun test_01_brand_tokens_exact_argb_values() {
        assertEquals("FoodJetPrimary must match #F4511E exactly", 0xFFF4511EL, FoodJetPrimary.argbHex())
        assertEquals("FoodJetPrimaryDark must match #D94310 exactly", 0xFFD94310L, FoodJetPrimaryDark.argbHex())
        assertEquals("FoodJetPrimaryLight must match #FCF0E4 exactly", 0xFFFCF0E4L, FoodJetPrimaryLight.argbHex())
        assertEquals("FoodJetDark must match #1C1C1C exactly", 0xFF1C1C1CL, FoodJetDark.argbHex())
        assertEquals("FoodJetGray must match #6B6B66 exactly", 0xFF6B6B66L, FoodJetGray.argbHex())
        assertEquals("FoodJetLightGray must match #F8F8F4 exactly", 0xFFF8F8F4L, FoodJetLightGray.argbHex())
        assertEquals("FoodJetBorder must match #E6E4DC exactly", 0xFFE6E4DCL, FoodJetBorder.argbHex())

        // Verify exact RGB floating point decomposition
        assertEquals(0xF4 / 255f, FoodJetPrimary.red, 0.001f)
        assertEquals(0x51 / 255f, FoodJetPrimary.green, 0.001f)
        assertEquals(0x1E / 255f, FoodJetPrimary.blue, 0.001f)
        assertEquals(1.0f, FoodJetPrimary.alpha, 0.001f)
    }

    @Test
    fun test_02_new_mobile_tokens_exact_argb_values() {
        assertEquals("FoodJetYellow must match #FCD460 exactly", 0xFFFCD460L, FoodJetYellow.argbHex())
        assertEquals("FoodJetSuccessSoft must match #ECF4EC exactly", 0xFFECF4ECL, FoodJetSuccessSoft.argbHex())
        assertEquals("FoodJetSuccessDeep must match #1E7B4F exactly", 0xFF1E7B4FL, FoodJetSuccessDeep.argbHex())

        assertEquals(0xFC / 255f, FoodJetYellow.red, 0.001f)
        assertEquals(0xD4 / 255f, FoodJetYellow.green, 0.001f)
        assertEquals(0x60 / 255f, FoodJetYellow.blue, 0.001f)
    }

    @Test
    fun test_03_functional_tokens_exact_argb_values() {
        assertEquals("FoodJetSuccess must match #28A745 exactly", 0xFF28A745L, FoodJetSuccess.argbHex())
        assertEquals("FoodJetSuccessContainer must match #D4EDDA exactly", 0xFFD4EDDAL, FoodJetSuccessContainer.argbHex())
        assertEquals("FoodJetSuccessText must match #155724 exactly", 0xFF155724L, FoodJetSuccessText.argbHex())
        assertEquals("FoodJetWarning must match #FFC107 exactly", 0xFFFFC107L, FoodJetWarning.argbHex())
        assertEquals("FoodJetWarningContainer must match #FFF3CD exactly", 0xFFFFF3CDL, FoodJetWarningContainer.argbHex())
        assertEquals("FoodJetDanger must match #DC3545 exactly", 0xFFDC3545L, FoodJetDanger.argbHex())
        assertEquals("FoodJetDangerContainer must match #F8D7DA exactly", 0xFFF8D7DAL, FoodJetDangerContainer.argbHex())
        assertEquals("FoodJetInfo must match #0DCAF0 exactly", 0xFF0DCAF0L, FoodJetInfo.argbHex())
    }

    @Test
    fun test_04_legacy_alias_tokens_exact_argb_values() {
        assertEquals("FoodJetPrimaryLegacy must match #F5AF69", 0xFFF5AF69L, FoodJetPrimaryLegacy.argbHex())
        assertEquals("FoodJetPrimaryDarkLegacy must match #F39D4A", 0xFFF39D4AL, FoodJetPrimaryDarkLegacy.argbHex())
        assertEquals("FoodJetPrimaryLightLegacy must match #FEF3E8", 0xFFFEF3E8L, FoodJetPrimaryLightLegacy.argbHex())
        assertEquals("FoodJetDarkLegacy must match #1A1A1A", 0xFF1A1A1AL, FoodJetDarkLegacy.argbHex())
        assertEquals("FoodJetGrayLegacy must match #6C757D", 0xFF6C757DL, FoodJetGrayLegacy.argbHex())
        assertEquals("FoodJetLightGrayLegacy must match #F8F9FA", 0xFFF8F9FAL, FoodJetLightGrayLegacy.argbHex())
        assertEquals("FoodJetBorderLegacy must match #DEE2E6", 0xFFDEE2E6L, FoodJetBorderLegacy.argbHex())
    }

    @Test
    fun test_05_all_tokens_fully_opaque_alpha_channel() {
        val allTokens = listOf(
            FoodJetPrimary, FoodJetPrimaryDark, FoodJetPrimaryLight,
            FoodJetDark, FoodJetGray, FoodJetLightGray, FoodJetBorder,
            FoodJetYellow, FoodJetSuccessSoft, FoodJetSuccessDeep,
            FoodJetSuccess, FoodJetSuccessContainer, FoodJetSuccessText,
            FoodJetWarning, FoodJetWarningContainer, FoodJetDanger,
            FoodJetDangerContainer, FoodJetInfo,
            FoodJetPrimaryLegacy, FoodJetPrimaryDarkLegacy, FoodJetPrimaryLightLegacy,
            FoodJetDarkLegacy, FoodJetGrayLegacy, FoodJetLightGrayLegacy, FoodJetBorderLegacy
        )

        for (token in allTokens) {
            assertEquals("Alpha channel must be 1.0f (fully opaque)", 1.0f, token.alpha, 0.001f)
            val argbInt = token.toArgb()
            val alphaByte = (argbInt ushr 24) and 0xFF
            assertEquals("ARGB int alpha byte must be 0xFF", 0xFF, alphaByte)
        }
    }

    // =========================================================================
    // 2. Downstream Resolution & Deprecation Check
    // =========================================================================

    @Test
    fun test_06_tokens_are_not_deprecated() {
        val colorClass = Class.forName("com.example.foodjeetapp.ui.theme.ColorKt")
        val activeTokenNames = listOf(
            "getFoodJetPrimary", "getFoodJetPrimaryDark", "getFoodJetPrimaryLight",
            "getFoodJetDark", "getFoodJetGray", "getFoodJetLightGray", "getFoodJetBorder",
            "getFoodJetYellow", "getFoodJetSuccessSoft", "getFoodJetSuccessDeep",
            "getFoodJetSuccess", "getFoodJetWarning", "getFoodJetDanger", "getFoodJetInfo"
        )

        for (getterName in activeTokenNames) {
            val getter = colorClass.getMethod(getterName)
            assertNull(
                "Active token $getterName should not be annotated with @Deprecated",
                getter.getAnnotation(Deprecated::class.java)
            )
        }
    }

    @Test
    fun test_07_legacy_aliases_resolve_as_distinct_colors_from_active_tokens() {
        assertNotEquals("Primary updated should differ from Legacy", FoodJetPrimary.value, FoodJetPrimaryLegacy.value)
        assertNotEquals("Dark updated should differ from Legacy", FoodJetDark.value, FoodJetDarkLegacy.value)
        assertNotEquals("Gray updated should differ from Legacy", FoodJetGray.value, FoodJetGrayLegacy.value)
    }

    // =========================================================================
    // 3. Color Scheme Overrides & Dark Mode Switching
    // =========================================================================

    private fun getScheme(name: String): ColorScheme {
        val field = Class.forName("com.example.foodjeetapp.ui.theme.ThemeKt").getDeclaredField(name)
        field.isAccessible = true
        return field.get(null) as ColorScheme
    }

    @Test
    fun test_08_light_color_scheme_mapping_integrity() {
        val light = getScheme("LightColorScheme")
        assertEquals("Light primary must be FoodJetPrimary", FoodJetPrimary.value, light.primary.value)
        assertEquals("Light onPrimary must be White", Color.White.value, light.onPrimary.value)
        assertEquals("Light primaryContainer must be FoodJetPrimaryLight", FoodJetPrimaryLight.value, light.primaryContainer.value)
        assertEquals("Light onPrimaryContainer must be FoodJetDark", FoodJetDark.value, light.onPrimaryContainer.value)
        assertEquals("Light secondary must be FoodJetDark", FoodJetDark.value, light.secondary.value)
        assertEquals("Light onSecondary must be White", Color.White.value, light.onSecondary.value)
        assertEquals("Light background must be FoodJetLightGray", FoodJetLightGray.value, light.background.value)
        assertEquals("Light onBackground must be FoodJetDark", FoodJetDark.value, light.onBackground.value)
        assertEquals("Light surface must be White", Color.White.value, light.surface.value)
        assertEquals("Light onSurface must be FoodJetDark", FoodJetDark.value, light.onSurface.value)
        assertEquals("Light surfaceVariant must be FoodJetLightGray", FoodJetLightGray.value, light.surfaceVariant.value)
        assertEquals("Light onSurfaceVariant must be FoodJetGray", FoodJetGray.value, light.onSurfaceVariant.value)
        assertEquals("Light error must be FoodJetDanger", FoodJetDanger.value, light.error.value)
        assertEquals("Light outline must be FoodJetBorder", FoodJetBorder.value, light.outline.value)
    }

    @Test
    fun test_09_dark_color_scheme_mapping_integrity() {
        val dark = getScheme("DarkColorScheme")
        assertEquals("Dark primary must be FoodJetPrimary", FoodJetPrimary.value, dark.primary.value)
        assertEquals("Dark onPrimary must be White", Color.White.value, dark.onPrimary.value)
        assertEquals("Dark onPrimaryContainer must be FoodJetPrimaryLight", FoodJetPrimaryLight.value, dark.onPrimaryContainer.value)
        assertEquals("Dark secondary must be FoodJetPrimaryDark", FoodJetPrimaryDark.value, dark.secondary.value)
        assertEquals("Dark onSecondary must be White", Color.White.value, dark.onSecondary.value)
        assertEquals("Dark error must be FoodJetDanger", FoodJetDanger.value, dark.error.value)
        assertEquals("Dark onError must be White", Color.White.value, dark.onError.value)
    }

    @Test
    fun test_10_theme_switching_surface_contrast() {
        val light = getScheme("LightColorScheme")
        val dark = getScheme("DarkColorScheme")

        // Light mode contrast between surface and onSurface
        val lightSurfaceLum = calculateRelativeLuminance(light.surface)
        val lightOnSurfaceLum = calculateRelativeLuminance(light.onSurface)
        val lightSurfaceContrast = calculateContrastRatio(lightSurfaceLum, lightOnSurfaceLum)

        // Dark mode contrast between surface and onSurface
        val darkSurfaceLum = calculateRelativeLuminance(dark.surface)
        val darkOnSurfaceLum = calculateRelativeLuminance(dark.onSurface)
        val darkSurfaceContrast = calculateContrastRatio(darkSurfaceLum, darkOnSurfaceLum)

        assertTrue("Light surface contrast ratio ($lightSurfaceContrast) must exceed 7:1", lightSurfaceContrast > 7.0)
        assertTrue("Dark surface contrast ratio ($darkSurfaceContrast) must exceed 7:1", darkSurfaceContrast > 7.0)
    }

    @Test
    fun test_11_wcag_contrast_primary_button() {
        val light = getScheme("LightColorScheme")
        val primaryLum = calculateRelativeLuminance(light.primary)
        val onPrimaryLum = calculateRelativeLuminance(light.onPrimary)
        val contrastRatio = calculateContrastRatio(primaryLum, onPrimaryLum)

        // For large text, buttons, and UI components, WCAG AA requires >= 3.0:1
        assertTrue(
            "Primary button content contrast ratio ($contrastRatio) must meet WCAG AA UI Component standard (>= 3.0:1)",
            contrastRatio >= 3.0
        )
    }

    // Helper functions for WCAG relative luminance and contrast ratio
    private fun calculateRelativeLuminance(color: Color): Double {
        fun transform(channel: Float): Double {
            val c = channel.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = transform(color.red)
        val g = transform(color.green)
        val b = transform(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun calculateContrastRatio(l1: Double, l2: Double): Double {
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
