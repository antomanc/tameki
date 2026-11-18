package dev.antomanc.tameki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dev.antomanc.tameki.core.ui.gallery.ComponentGallery
import dev.antomanc.tameki.core.ui.theme.TamekiTheme

/**
 * Debug-only entry point for the core:ui component gallery. Launch it from a debug build with
 * `adb shell am start -n dev.antomanc.tameki.debug/dev.antomanc.tameki.GalleryActivity`.
 * It is excluded from release builds by living in `src/debug`.
 */
class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TamekiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ComponentGallery()
                }
            }
        }
    }
}
