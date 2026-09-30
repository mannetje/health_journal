package nl.healthjournal.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A message a ViewModel wants to show. ViewModels must not hold pre-translated text, because the app
 * language can change; the screen resolves it against the current locale.
 */
sealed interface UiText {
    data class Res(@param:StringRes val id: Int, val args: List<Any>) : UiText {
        constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
    }

    /** Text that is already final, such as a message coming from the domain layer. */
    data class Plain(val value: String) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> stringResource(id, *args.map { if (it is UiText) it.asString() else it }.toTypedArray())
}

/** The exception's own message when it has one, otherwise the given fallback resource. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText =
    (this as? UiTextException)?.text ?: message?.let { UiText.Plain(it) } ?: UiText.Res(fallback)

/** Input validation failure whose message is localized when shown. */
class UiTextException(val text: UiText) : IllegalArgumentException()
