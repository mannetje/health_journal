package nl.healthjournal.app.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R

/**
 * Shown on every tab while the database could not be encrypted, so the user does not have to open
 * the Profile tab to find out. The user can dismiss it for this session and carry on.
 */
@Composable
fun EncryptionNotActiveBanner(
    onOpenProfile: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.encryption_not_active_banner_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.encryption_not_active_banner_body),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onOpenProfile) {
                    Text(stringResource(R.string.encryption_not_active_banner_open_profile))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.encryption_not_active_banner_dismiss))
                }
            }
        }
    }
}
