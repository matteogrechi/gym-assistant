package it.matteogrechi.gymassystant.feature

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.ui.ButtonEmphasis
import it.matteogrechi.gymassystant.ui.GymButton

// GYM-006/017/026: risky transitions share native dismissal and initial Cancel focus.
@Composable
fun ConfirmDialog(title: String, message: String, confirmLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    AlertDialog(onDismissRequest = onDismiss,
        modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 380.dp).fillMaxWidth(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        text = { Text(message, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { GymButton(confirmLabel, onConfirm) },
        dismissButton = {
            GymButton(stringResource(R.string.cancel), onDismiss, Modifier.focusRequester(cancelFocus),
                emphasis = ButtonEmphasis.Text)
        })
    LaunchedEffect(Unit) { withFrameNanos { }; cancelFocus.requestFocus() }
}
