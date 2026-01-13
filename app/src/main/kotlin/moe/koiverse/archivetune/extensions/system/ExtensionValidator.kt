package moe.koiverse.archivetune.extensions.system

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

object ExtensionValidator {
    private val json = Json { ignoreUnknownKeys = true }
    private val idRegex = Regex("""^[a-zA-Z0-9_.-]{2,64}$""")

    fun loadManifest(file: File): ExtensionManifest {
        val text = file.readText()
        return json.decodeFromString(text)
    }

    fun validateManifest(context: Context, manifest: ExtensionManifest, baseDir: File): ValidationResult {
        val errors = mutableListOf<String>()
        if (!idRegex.matches(manifest.id)) {
            errors.add("Invalid id")
        }
        if (manifest.name.isBlank()) errors.add("Missing name")
        if (manifest.version.isBlank()) errors.add("Missing version")
        if (manifest.author.isBlank()) errors.add("Missing author")
        if (manifest.entry.isBlank()) errors.add("Missing entry")

        val entryExists = baseDir.resolve(manifest.entry).exists()
        if (!entryExists) errors.add("Entry not found")

        val seenKeys = mutableSetOf<String>()
        manifest.settings.forEach { s ->
            if (s.key.isBlank()) errors.add("Empty setting key")
            if (!Regex("""^[a-zA-Z0-9_.-]{1,64}$""").matches(s.key)) errors.add("Invalid setting key")
            if (!seenKeys.add(s.key)) errors.add("Duplicate setting key")
            when (s.type) {
                SettingType.toggle -> {
                    if (s.defaultBoolean == null) errors.add("Toggle missing defaultBoolean")
                }
                SettingType.slider -> {
                    if (s.defaultNumber == null) errors.add("Slider missing defaultNumber")
                    val min = s.min ?: 0
                    val max = s.max ?: 100
                    val def = s.defaultNumber ?: 0
                    if (min > max) errors.add("Slider min > max")
                    if (def < min || def > max) errors.add("Slider default out of range")
                }
                SettingType.text -> {
                    if (s.defaultString == null) errors.add("Text missing defaultString")
                }
                SettingType.select -> {
                    val opts = s.options ?: emptyList()
                    if (opts.isEmpty()) errors.add("Select missing options")
                    val def = s.defaultString
                    if (def != null && def !in opts) errors.add("Select defaultString not in options")
                }
                SettingType.button -> {
                    // Button doesn't require any specific validation
                }
                SettingType.checkbox -> {
                    if (s.defaultBoolean == null) errors.add("Checkbox missing defaultBoolean")
                }
                SettingType.radio -> {
                    val opts = s.options ?: emptyList()
                    if (opts.isEmpty()) errors.add("Radio missing options")
                    val def = s.defaultString
                    if (def != null && def !in opts) errors.add("Radio defaultString not in options")
                }
                SettingType.multiSelect -> {
                    val opts = s.options ?: emptyList()
                    if (opts.isEmpty()) errors.add("MultiSelect missing options")
                }
                SettingType.color -> {
                    if (s.defaultColor == null) errors.add("Color missing defaultColor")
                }
                SettingType.date -> {
                    if (s.defaultString == null) errors.add("Date missing defaultString")
                }
                SettingType.time -> {
                    if (s.defaultString == null) errors.add("Time missing defaultString")
                }
                SettingType.section -> {
                    // Section doesn't require validation
                }
                SettingType.divider -> {
                    // Divider doesn't require validation
                }
                SettingType.group -> {
                    // Group doesn't require specific validation
                }
                SettingType.card -> {
                    // Card doesn't require specific validation
                }
                SettingType.image -> {
                    // Image doesn't require specific validation
                }
                SettingType.link -> {
                    // Link doesn't require specific validation
                }
                SettingType.password -> {
                    if (s.defaultString == null) errors.add("Password missing defaultString")
                }
                SettingType.number -> {
                    if (s.defaultNumber == null) errors.add("Number missing defaultNumber")
                }
                SettingType.textarea -> {
                    if (s.defaultString == null) errors.add("Textarea missing defaultString")
                }
                SettingType.chip -> {
                    if (s.chipSelectable == null) errors.add("Chip missing chipSelectable")
                }
                SettingType.chipGroup -> {
                    val opts = s.options ?: emptyList()
                    if (opts.isEmpty()) errors.add("ChipGroup missing options")
                }
                SettingType.segmented -> {
                    val opts = s.options ?: emptyList()
                    if (opts.isEmpty()) errors.add("Segmented missing options")
                    val def = s.defaultString
                    if (def != null && def !in opts) errors.add("Segmented defaultString not in options")
                }
                SettingType.stepper -> {
                    if (s.defaultNumber == null) errors.add("Stepper missing defaultNumber")
                    val min = s.min ?: Int.MIN_VALUE
                    val max = s.max ?: Int.MAX_VALUE
                    val def = s.defaultNumber ?: 0
                    if (min > max) errors.add("Stepper min > max")
                    if (def < min || def > max) errors.add("Stepper default out of range")
                }
                SettingType.rating -> {
                    if (s.defaultNumber == null) errors.add("Rating missing defaultNumber")
                }
                SettingType.progress -> {
                    // Progress doesn't require specific validation
                }
                SettingType.custom -> {
                } else -> {
                    errors.add("Unknown setting type: ${s.type}")
                }
            }
        }

        val unknownPermissions = manifest.permissions.filterNot {
            runCatching { ExtensionPermission.valueOf(it) }.isSuccess
        }
        if (unknownPermissions.isNotEmpty()) {
            errors.add("Unknown permissions: ${unknownPermissions.joinToString(",")}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}

data class ValidationResult(val valid: Boolean, val errors: List<String>)

