package moe.koiverse.archivetune.extensions.system.ui

import kotlinx.serialization.Serializable

@Serializable
enum class UIMode {
    replace,
    overlay
}

@Serializable
sealed class UINode {
    @Serializable
    data class Column(val children: List<UINode>) : UINode()
    @Serializable
    data class Row(val children: List<UINode>) : UINode()
    @Serializable
    data class Text(val text: String) : UINode()
    @Serializable
    data class Image(val path: String, val widthDp: Int? = null, val heightDp: Int? = null) : UINode()
    @Serializable
    data class Button(val text: String, val icon: String? = null, val action: String? = null) : UINode()
}

@Serializable
data class UIConfig(
    val mode: UIMode = UIMode.replace,
    val root: UINode
)

