package dev.xexanos.mealie.core.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ColorSchemeTest {

    @Test
    fun `when light scheme created then primary matches E58325 tonal palette`() {
        assertEquals(Color(0xFF8B5000), LightColorScheme.primary)
    }

    @Test
    fun `when dark scheme created then primary matches E58325 tonal palette`() {
        assertEquals(Color(0xFFFFB95A), DarkColorScheme.primary)
    }

    @Test
    fun `when light scheme created then not default Material 3 purple`() {
        assertNotEquals(Color(0xFF6750A4), LightColorScheme.primary)
    }

    @Test
    fun `when dark scheme created then not default Material 3 purple`() {
        assertNotEquals(Color(0xFFD0BCFF), DarkColorScheme.primary)
    }

    @Test
    fun `when light scheme created then error color is standard M3 red`() {
        assertEquals(Color(0xFFBA1A1A), LightColorScheme.error)
    }

    @Test
    fun `when light scheme created then on-primary is white for contrast`() {
        assertEquals(Color(0xFFFFFFFF), LightColorScheme.onPrimary)
    }
}
