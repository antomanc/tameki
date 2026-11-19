package dev.antomanc.tameki.core.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.antomanc.tameki.core.model.coroutines.runCatchingCancellable

/**
 * The app's motion vocabulary. Motion is one authored gesture, not a dozen small effects: things
 * that appear settle with a spring, amounts roll, and nothing moves when the system asks for calm.
 */
@Immutable
data class TamekiMotion(
    /** Presses, selection and other immediate feedback. */
    val snap: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 900f),
    /** Entrances and layout changes. The default. */
    val spatial: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 420f),
    /** Larger surfaces, sheets and full-width transforms. */
    val gentle: SpringSpec<Float> = spring(dampingRatio = 0.86f, stiffness = 260f),
    /** Non-spatial changes: colour, alpha. */
    val effects: TweenSpec<Float> = tween(durationMillis = 180, easing = FastOutSlowInEasing),
    /** The odometer roll of a changing amount. */
    val amountRoll: SpringSpec<Float> = spring(dampingRatio = 0.9f, stiffness = 520f),
)

/** The motion tokens for the current theme. Read them through [MaterialTheme.motion]. */
val LocalTamekiMotion = staticCompositionLocalOf { TamekiMotion() }

/**
 * False when the system "remove animations" setting is on (animator duration scale 0). Set by
 * [TamekiTheme] and readable by every component: motion is never the only way to understand a
 * change, so components simply snap when this is false.
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

/** Whether motion should play right now. */
@Composable
fun rememberMotionEnabled(): Boolean = LocalMotionEnabled.current

/**
 * Reads the system-wide animator duration scale and keeps listening for changes. Compose's own
 * animations already scale with it; this exists so components can also skip work that only serves
 * an animation (like swapping a value cell by cell).
 */
@Composable
fun rememberSystemMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun current(): Boolean = runCatchingCancellable {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)

    var enabled by remember(resolver) { mutableStateOf(current()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = current()
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}
