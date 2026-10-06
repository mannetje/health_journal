package nl.healthjournal.app.ui.medication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R

private const val APOTHEEK_URL = "https://www.apotheek.nl/"
private const val THUISARTS_URL = "https://www.thuisarts.nl/"

/**
 * The not-a-medical-device notice and the neutral Sources list. The app gives no advice, so the sources are
 * links only, with no summary and no recommendation. Shown on the first open of the pillbox and from Profile.
 */
@Composable
fun MedicationNoticeDialog(confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit = onConfirm) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medication_notice_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.medication_notice_text), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.medication_sources_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
                TextButton(
                    onClick = { uriHandler.openUri(APOTHEEK_URL) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.medication_source_apotheek)) }
                TextButton(
                    onClick = { uriHandler.openUri(THUISARTS_URL) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.medication_source_thuisarts)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) { Text(confirmLabel) }
        }
    )
}
