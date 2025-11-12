package moe.koiverse.archivetune

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape

class DebugActivity : ComponentActivity() {
    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stack = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available"

        setContent {
            MaterialTheme {
                DebugScreen(stack = stack, activity = this)
            }
        }
    }
}

@Composable
private fun DebugScreen(stack: String, activity: Activity) {
    val previewText = remember(stack) { stack.lineSequence().firstOrNull() ?: stack }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = previewText,
                color = Color.White,
                modifier = Modifier.padding(12.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = stack, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                clipboard.setText(AnnotatedString(stack))
            }, modifier = Modifier.weight(1f)) {
                Text("Copy")
            }

            Button(onClick = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, stack)
                }
                context.startActivity(Intent.createChooser(share, "Share crash log"))
            }, modifier = Modifier.weight(1f)) {
                Text("Share")
            }

            Button(onClick = {
                activity.finishAffinity()
            }, modifier = Modifier.weight(1f)) {
                Text("Close")
            }
        }
    }
}
