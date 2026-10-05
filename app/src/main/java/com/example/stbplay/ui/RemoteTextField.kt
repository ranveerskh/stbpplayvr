package com.example.stbplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.example.stbplay.isAndroidTvDevice

/** TV fields are navigation targets until explicitly selected for editing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() }
) {
    val context = LocalContext.current
    val television = remember(context) { context.isAndroidTvDevice() }
    if (!television) {
        BasicTextField(
            value = value, onValueChange = onValueChange, modifier = modifier,
            singleLine = singleLine, textStyle = textStyle,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            decorationBox = decorationBox
        )
        return
    }

    var editing by remember { mutableStateOf(false) }
    var restoreBrowseFocus by remember { mutableStateOf(false) }
    var editorHadFocus by remember { mutableStateOf(false) }
    var keyboardWasVisible by remember { mutableStateOf(false) }
    val browseRequester = remember { FocusRequester() }
    val editorRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun finishEditing() {
        keyboard?.hide()
        restoreBrowseFocus = true
        editing = false
    }
    fun beginEditing() {
        keyboardWasVisible = false
        editorHadFocus = false
        editing = true
    }

    LaunchedEffect(editing) {
        if (editing) {
            editorRequester.requestFocus()
            keyboard?.show()
        } else if (restoreBrowseFocus) {
            browseRequester.requestFocus()
            restoreBrowseFocus = false
        }
    }
    LaunchedEffect(imeVisible, editing) {
        if (editing && imeVisible) keyboardWasVisible = true
        else if (editing && keyboardWasVisible) finishEditing()
    }
    BackHandler(enabled = editing) { finishEditing() }

    val keyModifier = Modifier.onPreviewKeyEvent { event ->
        val select = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
        if (select) {
            if (event.type == KeyEventType.KeyUp) {
                if (editing) finishEditing() else beginEditing()
            }
            true
        } else false
    }
    Box(
        modifier = keyModifier.then(modifier).then(
            if (!editing) Modifier.focusRequester(browseRequester).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { beginEditing() }
            ) else Modifier
        )
    ) {
        if (editing) {
            BasicTextField(
                value = value, onValueChange = onValueChange,
                modifier = Modifier.fillMaxSize().focusRequester(editorRequester)
                    .onFocusChanged {
                        if (it.isFocused) editorHadFocus = true
                        else if (editorHadFocus) {
                            keyboard?.hide()
                            editing = false
                        }
                    },
                singleLine = singleLine, textStyle = textStyle,
                keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { finishEditing() },
                    onNext = { finishEditing() }
                ),
                decorationBox = decorationBox
            )
        } else {
            decorationBox { BasicText(value, style = textStyle, maxLines = if (singleLine) 1 else Int.MAX_VALUE) }
        }
    }
}
