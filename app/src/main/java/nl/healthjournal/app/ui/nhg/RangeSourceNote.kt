package nl.healthjournal.app.ui.nhg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R

private data class RangeSource(val label: Int, val url: String)

// Links only: no logos, no endorsement. NHG leads, the others are named for wording.
private val RANGE_SOURCES = listOf(
    RangeSource(R.string.ranges_link_nhg, "https://www.nhg.org"),
    RangeSource(R.string.ranges_link_thuisarts, "https://www.thuisarts.nl"),
    RangeSource(R.string.ranges_link_diabetesfonds, "https://www.diabetesfonds.nl"),
    RangeSource(R.string.ranges_link_dvn, "https://www.dvn.nl"),
    RangeSource(R.string.ranges_link_hartstichting, "https://www.hartstichting.nl/oorzaken/bloeddruk"),
    RangeSource(R.string.ranges_link_voedingscentrum, "https://www.voedingscentrum.nl")
)

/** A small "About these ranges" button that opens the source and not-a-diagnosis note. One per screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeSourceNote(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    TextButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.ranges_about), modifier = Modifier.padding(start = 8.dp))
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.ranges_about_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.ranges_about_body), style = MaterialTheme.typography.bodyMedium)
                RANGE_SOURCES.forEach { source ->
                    TextButton(onClick = { uriHandler.openUri(source.url) }) {
                        Text(stringResource(source.label))
                    }
                }
                TextButton(onClick = { open = false }) { Text(stringResource(R.string.ranges_close)) }
            }
        }
    }
}
