package moe.koiverse.archivetune.extensions.system.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.io.File

@Composable
fun RenderUI(config: UIConfig, baseDir: File, onAction: (String) -> Unit = {}) {
    RenderNode(config.root, baseDir, onAction)
}

@Composable
private fun RenderNode(node: UINode, baseDir: File, onAction: (String) -> Unit) {
    when (node) {
        is UINode.Column -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                node.children.forEach { RenderNode(it, baseDir, onAction) }
            }
        }
        is UINode.Row -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                node.children.forEach { RenderNode(it, baseDir, onAction) }
            }
        }
        is UINode.Text -> {
            Text(node.text)
        }
        is UINode.Image -> {
            val context = LocalContext.current
            val file = baseDir.resolve(node.path)
            val model = ImageRequest.Builder(context).data(file).build()
            val w = node.widthDp?.let { it.dp } ?: 140.dp
            val h = node.heightDp?.let { it.dp } ?: 140.dp
            AsyncImage(model = model, contentDescription = null, modifier = Modifier.width(w).height(h))
        }
        is UINode.Button -> {
            Button(onClick = { node.action?.let(onAction) }) {
                if (node.icon != null) {
                    val resId = resolveIcon(node.icon)
                    if (resId != null) {
                        Icon(painterResource(resId), null)
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Text(node.text)
            }
        }
    }
}

private fun resolveIcon(name: String): Int? {
    return when (name) {
        "add" -> moe.koiverse.archivetune.R.drawable.add
        "settings" -> moe.koiverse.archivetune.R.drawable.settings
        "restore" -> moe.koiverse.archivetune.R.drawable.restore
        else -> null
    }
}

