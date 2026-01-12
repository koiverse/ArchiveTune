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

