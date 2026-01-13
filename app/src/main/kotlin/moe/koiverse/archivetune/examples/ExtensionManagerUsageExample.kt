package moe.koiverse.archivetune.examples

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import moe.koiverse.archivetune.utils.getExtensionManager
import moe.koiverse.archivetune.extensions.system.ExtensionManager

/**
 * Example demonstrating how to access ExtensionManager anywhere in the app
 */
object ExtensionManagerUsageExample {

    /**
     * Example of accessing ExtensionManager from a Composable function
     */
    @Composable
    fun AccessExtensionManagerInComposable() {
        val context = LocalContext.current
        val extensionManager = getExtensionManager(context)
        
        // Use the extension manager for various operations
        // extensionManager.enable("some_extension_id")
        // extensionManager.disable("some_extension_id")
        // extensionManager.onTrackPlay(metadata)
    }

    /**
     * Example of accessing ExtensionManager from a regular function with Context
     */
    fun accessExtensionManagerInFunction(context: Context) {
        val extensionManager = getExtensionManager(context)
        
        // Use the extension manager for various operations
        // extensionManager.discover()
        // extensionManager.reload("extension_id")
    }

    /**
     * Example of accessing ExtensionManager from a ViewModel or other classes with Context
     */
    class ExampleClass(private val context: Context) {
        private val extensionManager: ExtensionManager by lazy {
            getExtensionManager(context)
        }

        fun performExtensionOperation() {
            // Use extensionManager here
            // extensionManager.installFromZip(uri)
        }
    }
}