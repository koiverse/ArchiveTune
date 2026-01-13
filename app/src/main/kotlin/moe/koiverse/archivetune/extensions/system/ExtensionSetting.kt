package moe.koiverse.archivetune.extensions.system

import kotlinx.serialization.Serializable

@Serializable
enum class SettingType {
    toggle,
    slider,
    text,
    select,
    button,
    checkbox,
    radio,
    multiSelect,
    color,
    date,
    time,
    section,
    divider,
    group,
    card,
    image,
    link,
    password,
    number,
    textarea,
    chip,
    chipGroup,
    segmented,
    stepper,
    rating,
    progress,
    custom
}

@Serializable
enum class SettingVisibility {
    visible,
    hidden,
    disabled,
    conditional
}

@Serializable
enum class SettingValidation {
    none,
    required,
    email,
    url,
    phone,
    number,
    regex,
    custom
}

@Serializable
data class SettingCondition(
    val dependsOn: String,
    val operator: String = "equals",
    val value: String? = null,
    val values: List<String>? = null
)

@Serializable
data class SettingAction(
    val type: String,
    val target: String? = null,
    val payload: String? = null,
    val route: String? = null,
    val url: String? = null,
    val confirm: Boolean = false,
    val confirmMessage: String? = null
)

@Serializable
data class SettingStyle(
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val iconColor: String? = null,
    val borderColor: String? = null,
    val borderWidth: Int? = null,
    val cornerRadius: Int? = null,
    val padding: Int? = null,
    val margin: Int? = null,
    val elevation: Int? = null,
    val fontSize: Int? = null,
    val fontWeight: String? = null,
    val textAlign: String? = null,
    val width: String? = null,
    val height: String? = null,
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null
)

@Serializable
data class SettingDefinition(
    val key: String,
    val type: SettingType,
    val label: String,
    val description: String? = null,
    val icon: String? = null,
    val placeholder: String? = null,
    val hint: String? = null,
    val defaultBoolean: Boolean? = null,
    val defaultNumber: Int? = null,
    val defaultFloat: Float? = null,
    val defaultString: String? = null,
    val defaultList: List<String>? = null,
    val defaultColor: String? = null,
    val options: List<String>? = null,
    val optionLabels: List<String>? = null,
    val optionIcons: List<String>? = null,
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null,
    val minFloat: Float? = null,
    val maxFloat: Float? = null,
    val stepFloat: Float? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val lines: Int? = null,
    val maxLines: Int? = null,
    val regex: String? = null,
    val regexMessage: String? = null,
    val visibility: SettingVisibility = SettingVisibility.visible,
    val validation: SettingValidation = SettingValidation.none,
    val condition: SettingCondition? = null,
    val action: SettingAction? = null,
    val style: SettingStyle? = null,
    val children: List<SettingDefinition>? = null,
    val imageUrl: String? = null,
    val imagePath: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val linkUrl: String? = null,
    val linkText: String? = null,
    val sectionTitle: String? = null,
    val sectionSubtitle: String? = null,
    val groupCollapsed: Boolean = false,
    val cardElevation: Int? = null,
    val chipSelectable: Boolean = false,
    val chipMultiple: Boolean = false,
    val ratingMax: Int = 5,
    val ratingAllowHalf: Boolean = false,
    val progressIndeterminate: Boolean = false,
    val progressValue: Float? = null,
    val customRenderer: String? = null,
    val customData: String? = null,
    val order: Int = 0,
    val category: String? = null,
    val tags: List<String>? = null,
    val searchable: Boolean = true,
    val exportable: Boolean = true,
    val restartRequired: Boolean = false,
    val experimental: Boolean = false,
    val deprecated: Boolean = false,
    val deprecationMessage: String? = null
)
