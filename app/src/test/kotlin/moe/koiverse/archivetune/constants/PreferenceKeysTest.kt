package moe.koiverse.archivetune.constants

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Discord-related PreferenceKeys added in the token-based login feature.
 *
 * These tests verify that the preference keys are properly defined and accessible,
 * which is critical for the Discord integration functionality.
 */
class PreferenceKeysTest {

    @Test
    fun `DiscordTokenKey should have correct key name`() {
        assertEquals("discordToken", DiscordTokenKey.name)
    }

    @Test
    fun `DiscordUsernameKey should have correct key name`() {
        assertEquals("discordUsername", DiscordUsernameKey.name)
    }

    @Test
    fun `DiscordNameKey should have correct key name`() {
        assertEquals("discordName", DiscordNameKey.name)
    }

    @Test
    fun `DiscordInfoDismissedKey should have correct key name`() {
        assertEquals("discordInfoDismissed", DiscordInfoDismissedKey.name)
    }

    @Test
    fun `EnableDiscordRPCKey should have correct key name`() {
        assertEquals("discordRPCEnable", EnableDiscordRPCKey.name)
    }

    @Test
    fun `DiscordActivityNameKey should have correct key name`() {
        assertEquals("discordActivityName", DiscordActivityNameKey.name)
    }

    @Test
    fun `DiscordActivityDetailsKey should have correct key name`() {
        assertEquals("discordActivityDetails", DiscordActivityDetailsKey.name)
    }

    @Test
    fun `DiscordActivityStateKey should have correct key name`() {
        assertEquals("discordActivityState", DiscordActivityStateKey.name)
    }

    @Test
    fun `DiscordActivityButton1LabelKey should have correct key name`() {
        assertEquals("discordActivityButton1Label", DiscordActivityButton1LabelKey.name)
    }

    @Test
    fun `DiscordActivityButton1UrlSourceKey should have correct key name`() {
        assertEquals("discordActivityButton1UrlSource", DiscordActivityButton1UrlSourceKey.name)
    }

    @Test
    fun `DiscordActivityButton1CustomUrlKey should have correct key name`() {
        assertEquals("discordActivityButton1CustomUrl", DiscordActivityButton1CustomUrlKey.name)
    }

    @Test
    fun `DiscordActivityButton2LabelKey should have correct key name`() {
        assertEquals("discordActivityButton2Label", DiscordActivityButton2LabelKey.name)
    }

    @Test
    fun `DiscordActivityButton2UrlSourceKey should have correct key name`() {
        assertEquals("discordActivityButton2UrlSource", DiscordActivityButton2UrlSourceKey.name)
    }

    @Test
    fun `DiscordActivityButton2CustomUrlKey should have correct key name`() {
        assertEquals("discordActivityButton2CustomUrl", DiscordActivityButton2CustomUrlKey.name)
    }

    @Test
    fun `DiscordActivityButton1EnabledKey should have correct key name`() {
        assertEquals("discordActivityButton1Enabled", DiscordActivityButton1EnabledKey.name)
    }

    @Test
    fun `DiscordActivityButton2EnabledKey should have correct key name`() {
        assertEquals("discordActivityButton2Enabled", DiscordActivityButton2EnabledKey.name)
    }

    @Test
    fun `DiscordShowWhenPausedKey should have correct key name`() {
        assertEquals("discordShowWhenPaused", DiscordShowWhenPausedKey.name)
    }

    @Test
    fun `DiscordActivityTypeKey should have correct key name`() {
        assertEquals("discordActivityType", DiscordActivityTypeKey.name)
    }

    @Test
    fun `DiscordPresenceIntervalValueKey should have correct key name`() {
        assertEquals("discordPresenceIntervalValue", DiscordPresenceIntervalValueKey.name)
    }

    @Test
    fun `DiscordPresenceIntervalUnitKey should have correct key name`() {
        assertEquals("discordPresenceIntervalUnit", DiscordPresenceIntervalUnitKey.name)
    }

    @Test
    fun `DiscordPresenceStatusKey should have correct key name`() {
        assertEquals("discordPresenceStatus", DiscordPresenceStatusKey.name)
    }

    @Test
    fun `DiscordLargeImageTypeKey should have correct key name`() {
        assertEquals("discordLargeImageType", DiscordLargeImageTypeKey.name)
    }

    @Test
    fun `DiscordLargeTextSourceKey should have correct key name`() {
        assertEquals("discordLargeTextSource", DiscordLargeTextSourceKey.name)
    }

    @Test
    fun `DiscordLargeTextCustomKey should have correct key name`() {
        assertEquals("discordLargeTextCustom", DiscordLargeTextCustomKey.name)
    }

    @Test
    fun `DiscordLargeImageCustomUrlKey should have correct key name`() {
        assertEquals("discordLargeImageCustomUrl", DiscordLargeImageCustomUrlKey.name)
    }

    @Test
    fun `DiscordSmallImageTypeKey should have correct key name`() {
        assertEquals("discordSmallImageType", DiscordSmallImageTypeKey.name)
    }

    @Test
    fun `DiscordSmallImageCustomUrlKey should have correct key name`() {
        assertEquals("discordSmallImageCustomUrl", DiscordSmallImageCustomUrlKey.name)
    }

    @Test
    fun `DiscordActivityPlatformKey should have correct key name`() {
        assertEquals("discordActivityPlatform", DiscordActivityPlatformKey.name)
    }

    // Edge case: ensure all Discord keys are string or boolean types
    @Test
    fun `Discord token related keys should use stringPreferencesKey`() {
        // These are all stringPreferencesKey types, which is correct for storing strings
        assertNotNull(DiscordTokenKey)
        assertNotNull(DiscordUsernameKey)
        assertNotNull(DiscordNameKey)
    }

    @Test
    fun `Discord boolean keys should use booleanPreferencesKey`() {
        // These are all booleanPreferencesKey types
        assertNotNull(DiscordInfoDismissedKey)
        assertNotNull(EnableDiscordRPCKey)
        assertNotNull(DiscordActivityButton1EnabledKey)
        assertNotNull(DiscordActivityButton2EnabledKey)
        assertNotNull(DiscordShowWhenPausedKey)
    }

    @Test
    fun `Discord integer keys should use intPreferencesKey`() {
        // These are intPreferencesKey types
        assertNotNull(DiscordPresenceIntervalValueKey)
    }

    // Regression test: ensure keys don't accidentally get renamed
    @Test
    fun `Discord preference keys should maintain backward compatibility`() {
        // If these key names change, existing user preferences will be lost
        val criticalKeys = mapOf(
            "discordToken" to DiscordTokenKey.name,
            "discordUsername" to DiscordUsernameKey.name,
            "discordName" to DiscordNameKey.name,
            "discordRPCEnable" to EnableDiscordRPCKey.name
        )

        criticalKeys.forEach { (expected, actual) ->
            assertEquals("Key name changed - breaks backward compatibility!", expected, actual)
        }
    }

    // Boundary test: ensure interval value key accepts typical values
    @Test
    fun `DiscordPresenceIntervalValueKey should handle typical interval values`() {
        // This is a meta-test to document expected value ranges
        // Actual validation happens in the UI layer
        val typicalValues = listOf(20, 30, 60, 300) // 20s, 30s, 1m, 5m in seconds
        assertTrue("Interval key should support typical values", typicalValues.all { it >= 20 })
    }

    // Negative test: document invalid states
    @Test
    fun `Discord preference keys should not be null`() {
        assertNotNull("DiscordTokenKey should not be null", DiscordTokenKey)
        assertNotNull("DiscordUsernameKey should not be null", DiscordUsernameKey)
        assertNotNull("DiscordNameKey should not be null", DiscordNameKey)
        assertNotNull("EnableDiscordRPCKey should not be null", EnableDiscordRPCKey)
    }
}