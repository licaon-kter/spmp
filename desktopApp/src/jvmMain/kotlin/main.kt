import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPlacement
import com.toasterofbread.composekit.platform.composable.onWindowBackPressed
import com.toasterofbread.spmp.model.settings.category.DesktopSettings
import com.toasterofbread.spmp.model.settings.category.ThemeSettings
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.ui.component.shortcut.trigger.KeyboardShortcutTrigger
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.getTextFieldFocusState
import com.toasterofbread.spmp.ui.layout.apppage.mainpage.isTextFieldFocused
import com.toasterofbread.spmp.model.appaction.shortcut.ShortcutState
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import kotlinx.coroutines.*
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.awt.Toolkit
import java.awt.Frame
import java.lang.reflect.Field

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    println("main() 1")

    Thread.setDefaultUncaughtExceptionHandler { _: Thread, error: Throwable ->
        error.printStackTrace()
        val dialog = ExceptionDialog(Frame(), error)
        dialog.isVisible = true
    }
    println("main() 2")

    val coroutine_scope: CoroutineScope = CoroutineScope(Job())
    println("main() 3")
    val context: AppContext = AppContext(SpMp.app_name, coroutine_scope)
    println("main() 4")

    SpMp.init(context)
    println("main() 5")

    val arguments: ProgramArguments = ProgramArguments.parse(args) ?: return
    println("main() 6")

    SpMp.onStart()
    println("main() 7")

    if (hostOs == OS.Linux) {
        try {
            // Set AWT class name of window
            val toolkit: Toolkit = Toolkit.getDefaultToolkit()
            val class_name_field: Field = toolkit.javaClass.getDeclaredField("awtAppClassName")
            class_name_field.isAccessible = true
            class_name_field.set(toolkit, SpMp.app_name.lowercase())
        }
        catch (_: Throwable) {}
    }
    println("main() 8")

    lateinit var window: ComposeWindow
    val enable_window_transparency: Boolean = ThemeSettings.Key.ENABLE_WINDOW_TRANSPARENCY.get(context.getPrefs())

    val shortcut_state: ShortcutState = ShortcutState()
    var player: PlayerState? = null
    println("main() 9")

    application {
    println("main() 10")
        val text_field_focus_state: Any = getTextFieldFocusState()
    println("main() 11")

        Window(
            title = SpMp.app_name,
            onCloseRequest = ::exitApplication,
            onKeyEvent = { event ->
                val shortcut_modifier = KeyboardShortcutTrigger.KeyboardModifier.ofKey(event.key)
                if (shortcut_modifier != null) {
                    if (event.type == KeyEventType.KeyDown) {
                        shortcut_state.onModifierDown(shortcut_modifier)
                    }
                    else {
                        shortcut_state.onModifierUp(shortcut_modifier)
                    }
                    return@Window false
                }

                if (event.type != KeyEventType.KeyUp) {
                    return@Window false
                }

                player?.also {
                    return@Window shortcut_state.onKeyPress(event, isTextFieldFocused(text_field_focus_state), it)
                }

                return@Window false
            },
            state = rememberWindowState(
                size = DpSize(1280.dp, 720.dp)
            ),
            undecorated = enable_window_transparency,
            transparent = enable_window_transparency
        ) {
    println("main() 12")
            LaunchedEffect(Unit) {
    println("main() 13")
                window = this@Window.window

                if (enable_window_transparency) {
                    window.background = java.awt.Color(0, 0, 0, 0)
                }

                val startup_command: String = DesktopSettings.Key.STARTUP_COMMAND.get()
                if (startup_command.isBlank()) {
                    return@LaunchedEffect
                }

                withContext(Dispatchers.IO) {
                    try {
                        val process_builder: ProcessBuilder =
                            when (hostOs) {
                                OS.Linux -> ProcessBuilder("bash", "-c", startup_command)
                                OS.Windows -> TODO()
                                else -> return@withContext
                            }

                        process_builder.inheritIO().start()
                    }
                    catch (e: Throwable) {
                        RuntimeException("Execution of startup command failed", e).printStackTrace()
                    }
                }
    println("main() 14")
            }
    println("main() 15")

            SpMp.App(
                arguments,
                shortcut_state,
                Modifier.onPointerEvent(PointerEventType.Press) { event ->
                    val index: Int = event.button?.index ?: return@onPointerEvent
                    player?.also {
                        shortcut_state.onButtonPress(index, it)
                    }
                },
                window_fullscreen_toggler = {
                    if (window.placement == WindowPlacement.Fullscreen) {
                        window.placement = WindowPlacement.Floating
                    }
                    else {
                        window.placement = WindowPlacement.Fullscreen
                    }
                },
                onPlayerCreated = {
                    player = it
                }
            )
    println("main() 16")
        }
    }
    println("main() 17")

    coroutine_scope.cancel()
    println("main() 18")

    SpMp.onStop()
    SpMp.release()
    println("main() 19")
}
