package nl.healthjournal.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.healthjournal.app.R
import nl.healthjournal.domain.model.metrics.EntryComment

/**
 * The optional single-line comment field shared by the logging form and the edit dialog.
 * The caller keeps the text within [EntryComment.MAX_LENGTH]; the counter shows how much is used.
 */
@Composable
fun CommentField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.comment_label)) },
        supportingText = {
            Text(stringResource(R.string.comment_counter, value.length, EntryComment.MAX_LENGTH))
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
