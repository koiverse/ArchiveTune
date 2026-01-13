package moe.koiverse.archivetune.extensions.system.ui

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
enum class UIMode {
    replace,
    overlay,
    prepend,
    append,
    wrap,
    inject
}

@Serializable
enum class UIAlignment {
    start,
    center,
    end,
    spaceBetween,
    spaceAround,
    spaceEvenly
}

@Serializable
enum class UIArrangement {
    start,
    center,
    end,
    spaceBetween,
    spaceAround,
    spaceEvenly,
    top,
    bottom
}

@Serializable
data class UIModifier(
    val width: String? = null,
    val height: String? = null,
    val padding: Int? = null,
    val paddingHorizontal: Int? = null,
    val paddingVertical: Int? = null,
    val paddingStart: Int? = null,
    val paddingEnd: Int? = null,
    val paddingTop: Int? = null,
    val paddingBottom: Int? = null,
    val margin: Int? = null,
    val backgroundColor: String? = null,
    val cornerRadius: Int? = null,
    val cornerRadiusTopStart: Int? = null,
    val cornerRadiusTopEnd: Int? = null,
    val cornerRadiusBottomStart: Int? = null,
    val cornerRadiusBottomEnd: Int? = null,
    val borderWidth: Int? = null,
    val borderColor: String? = null,
    val elevation: Int? = null,
    val alpha: Float? = null,
    val clickable: Boolean? = null,
    val enabled: Boolean? = null,
    val visible: Boolean? = null,
    val weight: Float? = null,
    val fillMaxWidth: Boolean? = null,
    val fillMaxHeight: Boolean? = null,
    val fillMaxSize: Boolean? = null,
    val wrapContentWidth: Boolean? = null,
    val wrapContentHeight: Boolean? = null,
    val aspectRatio: Float? = null,
    val scrollable: Boolean? = null,
    val horizontalScroll: Boolean? = null,
    val clip: Boolean? = null
)

@Serializable
data class UITextStyle(
    val fontSize: Int? = null,
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val color: String? = null,
    val textAlign: String? = null,
    val textDecoration: String? = null,
    val letterSpacing: Float? = null,
    val lineHeight: Float? = null,
    val maxLines: Int? = null,
    val overflow: String? = null
)

@Serializable
sealed class UINode {
    abstract val modifier: UIModifier?
    abstract val id: String?
    abstract val action: String?
    abstract val visible: Boolean?
    abstract val condition: String?

    @Serializable
    data class Column(
        val children: List<UINode>,
        val verticalArrangement: UIArrangement? = null,
        val horizontalAlignment: UIAlignment? = null,
        val spacing: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Row(
        val children: List<UINode>,
        val horizontalArrangement: UIArrangement? = null,
        val verticalAlignment: UIAlignment? = null,
        val spacing: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Box(
        val children: List<UINode>,
        val contentAlignment: UIAlignment? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class LazyColumn(
        val children: List<UINode>,
        val verticalArrangement: UIArrangement? = null,
        val horizontalAlignment: UIAlignment? = null,
        val spacing: Int? = null,
        val contentPadding: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class LazyRow(
        val children: List<UINode>,
        val horizontalArrangement: UIArrangement? = null,
        val verticalAlignment: UIAlignment? = null,
        val spacing: Int? = null,
        val contentPadding: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Card(
        val children: List<UINode>,
        val elevation: Int? = null,
        val shape: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Surface(
        val children: List<UINode>,
        val elevation: Int? = null,
        val shape: String? = null,
        val color: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Text(
        val text: String,
        val style: UITextStyle? = null,
        val selectable: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Image(
        val path: String,
        val widthDp: Int? = null,
        val heightDp: Int? = null,
        val contentScale: String? = null,
        val contentDescription: String? = null,
        val placeholder: String? = null,
        val error: String? = null,
        val crossfade: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class NetworkImage(
        val url: String,
        val widthDp: Int? = null,
        val heightDp: Int? = null,
        val contentScale: String? = null,
        val contentDescription: String? = null,
        val placeholder: String? = null,
        val error: String? = null,
        val crossfade: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Icon(
        val name: String,
        val size: Int? = null,
        val tint: String? = null,
        val contentDescription: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Button(
        val text: String,
        val icon: String? = null,
        val style: String? = null,
        val enabled: Boolean? = null,
        val loading: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class IconButton(
        val icon: String,
        val size: Int? = null,
        val tint: String? = null,
        val enabled: Boolean? = null,
        val contentDescription: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class FloatingActionButton(
        val icon: String,
        val text: String? = null,
        val extended: Boolean? = null,
        val containerColor: String? = null,
        val contentColor: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class TextField(
        val key: String,
        val value: String? = null,
        val label: String? = null,
        val placeholder: String? = null,
        val leadingIcon: String? = null,
        val trailingIcon: String? = null,
        val singleLine: Boolean? = null,
        val maxLines: Int? = null,
        val enabled: Boolean? = null,
        val readOnly: Boolean? = null,
        val isError: Boolean? = null,
        val supportingText: String? = null,
        val keyboardType: String? = null,
        val imeAction: String? = null,
        val visualTransformation: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class OutlinedTextField(
        val key: String,
        val value: String? = null,
        val label: String? = null,
        val placeholder: String? = null,
        val leadingIcon: String? = null,
        val trailingIcon: String? = null,
        val singleLine: Boolean? = null,
        val maxLines: Int? = null,
        val enabled: Boolean? = null,
        val readOnly: Boolean? = null,
        val isError: Boolean? = null,
        val supportingText: String? = null,
        val keyboardType: String? = null,
        val imeAction: String? = null,
        val visualTransformation: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Switch(
        val key: String,
        val checked: Boolean? = null,
        val enabled: Boolean? = null,
        val thumbContent: UINode? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Checkbox(
        val key: String,
        val checked: Boolean? = null,
        val enabled: Boolean? = null,
        val label: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class RadioButton(
        val key: String,
        val value: String,
        val selected: Boolean? = null,
        val enabled: Boolean? = null,
        val label: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class RadioGroup(
        val key: String,
        val options: List<String>,
        val optionLabels: List<String>? = null,
        val selected: String? = null,
        val orientation: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Slider(
        val key: String,
        val value: Float? = null,
        val min: Float? = null,
        val max: Float? = null,
        val steps: Int? = null,
        val enabled: Boolean? = null,
        val showValue: Boolean? = null,
        val valueFormat: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class RangeSlider(
        val key: String,
        val valueStart: Float? = null,
        val valueEnd: Float? = null,
        val min: Float? = null,
        val max: Float? = null,
        val steps: Int? = null,
        val enabled: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class DropdownMenu(
        val key: String,
        val options: List<String>,
        val optionLabels: List<String>? = null,
        val selected: String? = null,
        val label: String? = null,
        val placeholder: String? = null,
        val enabled: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Chip(
        val text: String,
        val leadingIcon: String? = null,
        val trailingIcon: String? = null,
        val selected: Boolean? = null,
        val enabled: Boolean? = null,
        val style: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class ChipGroup(
        val key: String,
        val chips: List<String>,
        val chipLabels: List<String>? = null,
        val selected: List<String>? = null,
        val multiSelect: Boolean? = null,
        val style: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class ProgressIndicator(
        val progress: Float? = null,
        val indeterminate: Boolean? = null,
        val linear: Boolean? = null,
        val color: String? = null,
        val trackColor: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Divider(
        val thickness: Int? = null,
        val color: String? = null,
        val startIndent: Int? = null,
        val endIndent: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Spacer(
        val width: Int? = null,
        val height: Int? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Badge(
        val content: String? = null,
        val containerColor: String? = null,
        val contentColor: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class ListItem(
        val headlineText: String,
        val supportingText: String? = null,
        val overlineText: String? = null,
        val leadingContent: UINode? = null,
        val trailingContent: UINode? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class NavigationItem(
        val label: String,
        val icon: String,
        val selectedIcon: String? = null,
        val route: String,
        val badge: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class TabRow(
        val tabs: List<String>,
        val tabIcons: List<String>? = null,
        val selectedIndex: Int? = null,
        val scrollable: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class TopAppBar(
        val title: String,
        val subtitle: String? = null,
        val navigationIcon: String? = null,
        val actions: List<UINode>? = null,
        val style: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class BottomSheet(
        val content: UINode,
        val peekHeight: Int? = null,
        val skipHalfExpanded: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Dialog(
        val title: String? = null,
        val content: UINode? = null,
        val confirmButton: UINode? = null,
        val dismissButton: UINode? = null,
        val icon: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Snackbar(
        val message: String,
        val actionLabel: String? = null,
        val duration: String? = null,
        val withDismissAction: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class AlertDialog(
        val title: String,
        val text: String,
        val confirmButtonText: String,
        val dismissButtonText: String? = null,
        val icon: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Scaffold(
        val topBar: UINode? = null,
        val bottomBar: UINode? = null,
        val floatingActionButton: UINode? = null,
        val content: UINode,
        val floatingActionButtonPosition: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class AnimatedVisibility(
        val child: UINode,
        val visible: Boolean? = null,
        val enter: String? = null,
        val exit: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Pager(
        val pages: List<UINode>,
        val orientation: String? = null,
        val initialPage: Int? = null,
        val pageSpacing: Int? = null,
        val userScrollEnabled: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class SwipeRefresh(
        val content: UINode,
        val refreshing: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class PullRefresh(
        val content: UINode,
        val refreshing: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class SegmentedButton(
        val key: String,
        val options: List<String>,
        val optionLabels: List<String>? = null,
        val optionIcons: List<String>? = null,
        val selected: String? = null,
        val multiSelect: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class DatePicker(
        val key: String,
        val value: Long? = null,
        val label: String? = null,
        val minDate: Long? = null,
        val maxDate: Long? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class TimePicker(
        val key: String,
        val hour: Int? = null,
        val minute: Int? = null,
        val is24Hour: Boolean? = null,
        val label: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class ColorPicker(
        val key: String,
        val value: String? = null,
        val label: String? = null,
        val showAlpha: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Rating(
        val key: String,
        val value: Float? = null,
        val max: Int? = null,
        val allowHalf: Boolean? = null,
        val size: Int? = null,
        val activeColor: String? = null,
        val inactiveColor: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Stepper(
        val key: String,
        val value: Int? = null,
        val min: Int? = null,
        val max: Int? = null,
        val step: Int? = null,
        val label: String? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class WebView(
        val url: String? = null,
        val html: String? = null,
        val javaScriptEnabled: Boolean? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Custom(
        val type: String,
        val data: JsonElement? = null,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()

    @Serializable
    data class Placeholder(
        val key: String,
        override val modifier: UIModifier? = null,
        override val id: String? = null,
        override val action: String? = null,
        override val visible: Boolean? = null,
        override val condition: String? = null
    ) : UINode()
}

@Serializable
data class UIConfig(
    val mode: UIMode = UIMode.replace,
    val root: UINode,
    val version: Int = 1,
    val targetRoute: String? = null,
    val position: String? = null,
    val priority: Int = 0,
    val animations: Boolean = true,
    val cacheEnabled: Boolean = true
)

@Serializable
data class UIStateBinding(
    val key: String,
    val source: String,
    val transform: String? = null,
    val defaultValue: String? = null
)

@Serializable
data class UIEventHandler(
    val event: String,
    val action: String,
    val target: String? = null,
    val payload: String? = null
)
