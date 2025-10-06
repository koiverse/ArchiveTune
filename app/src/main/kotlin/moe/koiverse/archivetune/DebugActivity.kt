package moe.koiverse.archivetune

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding

class DebugActivity : Activity() {
    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stack = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No stack trace available"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }

        val scroll = ScrollView(this)
        val tv = TextView(this).apply {
            text = stack
            setTextIsSelectable(true)
        }
        scroll.addView(tv)

        val btnCopy = Button(this).apply {
            text = "Copy"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("stack", stack))
            }
        }

        val btnShare = Button(this).apply {
            text = "Share"
            setOnClickListener {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, stack)
                }
                startActivity(Intent.createChooser(share, "Share crash log"))
            }
        }

        val btnClose = Button(this).apply {
            text = "Close"
            setOnClickListener { finishAffinity() }
        }

        root.addView(scroll, LinearLayout.LayoutParams.MATCH_PARENT, 0,)
        root.addView(btnCopy)
        root.addView(btnShare)
        root.addView(btnClose)

        setContentView(root)
    }
}
