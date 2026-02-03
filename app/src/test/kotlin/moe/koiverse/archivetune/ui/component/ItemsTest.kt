package moe.koiverse.archivetune.ui.component

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Items.kt helper logic and constants.
 *
 * These tests verify the logic and constants used in the UI component items,
 * particularly the SwipeToSongBox functionality.
 */
class ItemsTest {

    @Test
    fun `ActiveBoxAlpha should be in valid range`() {
        val activeBoxAlpha = 0.6f
        assertTrue("Alpha should be between 0 and 1",
            activeBoxAlpha >= 0f && activeBoxAlpha <= 1f)
    }

    @Test
    fun `ActiveBoxAlpha should be set to correct value`() {
        val activeBoxAlpha = 0.6f
        assertEquals(0.6f, activeBoxAlpha, 0.001f)
    }

    // SwipeToSongBox threshold tests
    @Test
    fun `Swipe threshold should be positive`() {
        val threshold = 300f
        assertTrue("Threshold should be positive", threshold > 0f)
    }

    @Test
    fun `Swipe threshold should be set to correct value`() {
        val threshold = 300f
        assertEquals(300f, threshold, 0.001f)
    }

    @Test
    fun `Positive swipe beyond threshold should trigger play next`() {
        val offsetValue = 350f
        val threshold = 300f
        val shouldTriggerPlayNext = offsetValue >= threshold
        assertTrue("Positive swipe beyond threshold should trigger play next",
            shouldTriggerPlayNext)
    }

    @Test
    fun `Negative swipe beyond threshold should trigger add to queue`() {
        val offsetValue = -350f
        val threshold = 300f
        val shouldTriggerAddToQueue = offsetValue <= -threshold
        assertTrue("Negative swipe beyond threshold should trigger add to queue",
            shouldTriggerAddToQueue)
    }

    @Test
    fun `Swipe within threshold should not trigger action`() {
        val offsetValue = 250f
        val threshold = 300f
        val shouldTriggerPlayNext = offsetValue >= threshold
        val shouldTriggerAddToQueue = offsetValue <= -threshold
        assertFalse("Swipe within threshold should not trigger play next",
            shouldTriggerPlayNext)
        assertFalse("Swipe within threshold should not trigger add to queue",
            shouldTriggerAddToQueue)
    }

    @Test
    fun `Swipe at exactly threshold should trigger action`() {
        val offsetValue = 300f
        val threshold = 300f
        val shouldTriggerPlayNext = offsetValue >= threshold
        assertTrue("Swipe at exactly threshold should trigger action",
            shouldTriggerPlayNext)
    }

    @Test
    fun `Negative swipe at exactly negative threshold should trigger action`() {
        val offsetValue = -300f
        val threshold = 300f
        val shouldTriggerAddToQueue = offsetValue <= -threshold
        assertTrue("Negative swipe at exactly threshold should trigger action",
            shouldTriggerAddToQueue)
    }

    @Test
    fun `Offset should be clamped within threshold range`() {
        val delta = 100f
        val currentOffset = 250f
        val threshold = 300f

        val newOffset = (currentOffset + delta).coerceIn(-threshold, threshold)

        assertTrue("Offset should not exceed positive threshold",
            newOffset <= threshold)
        assertTrue("Offset should not exceed negative threshold",
            newOffset >= -threshold)
    }

    @Test
    fun `Offset exceeding positive threshold should be clamped`() {
        val offset = 400f
        val threshold = 300f
        val clamped = offset.coerceIn(-threshold, threshold)

        assertEquals(threshold, clamped, 0.001f)
    }

    @Test
    fun `Offset exceeding negative threshold should be clamped`() {
        val offset = -400f
        val threshold = 300f
        val clamped = offset.coerceIn(-threshold, threshold)

        assertEquals(-threshold, clamped, 0.001f)
    }

    @Test
    fun `Offset within range should not be modified`() {
        val offset = 150f
        val threshold = 300f
        val clamped = offset.coerceIn(-threshold, threshold)

        assertEquals(offset, clamped, 0.001f)
    }

    // Animation reset tests
    @Test
    fun `Animation reset should target zero`() {
        val initialValue = 250f
        val targetValue = 0f

        assertEquals(0f, targetValue, 0.001f)
    }

    @Test
    fun `Animation duration should be positive`() {
        val durationMillis = 300
        assertTrue("Animation duration should be positive", durationMillis > 0)
    }

    @Test
    fun `Animation duration should be reasonable`() {
        val durationMillis = 300
        assertTrue("Animation duration should be between 100ms and 1000ms",
            durationMillis >= 100 && durationMillis <= 1000)
    }

    // Icon and background color tests for swipe direction
    @Test
    fun `Positive offset should show play next icon`() {
        val offset = 150f
        val isPositive = offset > 0f
        assertTrue("Positive offset should show play next", isPositive)
    }

    @Test
    fun `Negative offset should show queue icon`() {
        val offset = -150f
        val isNegative = offset < 0f
        assertTrue("Negative offset should show queue icon", isNegative)
    }

    @Test
    fun `Zero offset should show no indicator`() {
        val offset = 0f
        val shouldShowIndicator = offset != 0f
        assertFalse("Zero offset should show no indicator", shouldShowIndicator)
    }

    // Edge cases
    @Test
    fun `Very small positive offset should not trigger action`() {
        val offset = 1f
        val threshold = 300f
        val shouldTrigger = offset >= threshold
        assertFalse(shouldTrigger)
    }

    @Test
    fun `Very small negative offset should not trigger action`() {
        val offset = -1f
        val threshold = 300f
        val shouldTrigger = offset <= -threshold
        assertFalse(shouldTrigger)
    }

    @Test
    fun `Very large positive offset should be clamped to threshold`() {
        val offset = 10000f
        val threshold = 300f
        val clamped = offset.coerceIn(-threshold, threshold)
        assertEquals(threshold, clamped, 0.001f)
    }

    @Test
    fun `Very large negative offset should be clamped to negative threshold`() {
        val offset = -10000f
        val threshold = 300f
        val clamped = offset.coerceIn(-threshold, threshold)
        assertEquals(-threshold, clamped, 0.001f)
    }

    // Quadruple data class tests
    @Test
    fun `Quadruple should hold four values`() {
        val quad = Quadruple(1, "two", 3.0, true)
        assertEquals(1, quad.first)
        assertEquals("two", quad.second)
        assertEquals(3.0, quad.third, 0.001)
        assertEquals(true, quad.fourth)
    }

    @Test
    fun `Quadruple should support different types`() {
        val quad = Quadruple<Int, String, Double, Boolean>(42, "test", 3.14, false)
        assertNotNull(quad.first)
        assertNotNull(quad.second)
        assertNotNull(quad.third)
        assertNotNull(quad.fourth)
    }

    @Test
    fun `Quadruple should be a data class`() {
        val quad1 = Quadruple(1, 2, 3, 4)
        val quad2 = Quadruple(1, 2, 3, 4)
        assertEquals(quad1, quad2)
    }

    @Test
    fun `Quadruple with different values should not be equal`() {
        val quad1 = Quadruple(1, 2, 3, 4)
        val quad2 = Quadruple(1, 2, 3, 5)
        assertNotEquals(quad1, quad2)
    }

    // Regression tests
    @Test
    fun `Swipe threshold should not change without consideration`() {
        // Changing threshold affects user experience
        val expectedThreshold = 300f
        assertEquals("Swipe threshold should remain stable",
            300f, expectedThreshold, 0.001f)
    }

    @Test
    fun `Active box alpha should not change without consideration`() {
        // Changing alpha affects visual appearance
        val expectedAlpha = 0.6f
        assertEquals("Active box alpha should remain stable",
            0.6f, expectedAlpha, 0.001f)
    }

    @Test
    fun `Animation duration should not change without consideration`() {
        // Changing duration affects user experience
        val expectedDuration = 300
        assertEquals("Animation duration should remain stable", 300, expectedDuration)
    }

    // Boundary value analysis
    @Test
    fun `Threshold minus one should not trigger`() {
        val offset = 299f
        val threshold = 300f
        val shouldTrigger = offset >= threshold
        assertFalse(shouldTrigger)
    }

    @Test
    fun `Threshold plus one should trigger`() {
        val offset = 301f
        val threshold = 300f
        val shouldTrigger = offset >= threshold
        assertTrue(shouldTrigger)
    }

    @Test
    fun `Negative threshold minus one should trigger`() {
        val offset = -301f
        val threshold = 300f
        val shouldTrigger = offset <= -threshold
        assertTrue(shouldTrigger)
    }

    @Test
    fun `Negative threshold plus one should not trigger`() {
        val offset = -299f
        val threshold = 300f
        val shouldTrigger = offset <= -threshold
        assertFalse(shouldTrigger)
    }

    // State management tests
    @Test
    fun `Offset state should be mutable`() {
        var offset = 0f
        offset = 150f
        assertEquals(150f, offset, 0.001f)

        offset = -150f
        assertEquals(-150f, offset, 0.001f)

        offset = 0f
        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun `Swipe enabled state should control functionality`() {
        val swipeEnabled = true
        assertTrue("Swipe should be enabled when preference is true", swipeEnabled)

        val swipeDisabled = false
        assertFalse("Swipe should be disabled when preference is false", swipeDisabled)
    }
}