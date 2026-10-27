package nl.healthjournal.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.ui.theme.BrandNavy

/** The navy bar with the screen title, the Pillbox and Mindfulness buttons and the logo (which goes to Log). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onOpenPillbox: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenLog: () -> Unit
) {
    val pillboxDescription = stringResource(R.string.nav_pillbox_desc)
    val mindfulnessDescription = stringResource(R.string.nav_mindfulness_desc)
    val logoDescription = stringResource(R.string.nav_logo_desc)
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onOpenPillbox, modifier = Modifier.size(48.dp)) {
                Text("💊", modifier = Modifier.semantics { contentDescription = pillboxDescription })
            }
            IconButton(onClick = onOpenMindfulness, modifier = Modifier.size(48.dp)) {
                Text("🪷", modifier = Modifier.semantics { contentDescription = mindfulnessDescription })
            }
            Image(
                painter = painterResource(R.drawable.logo_health_journal_on_navy),
                contentDescription = logoDescription,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable(role = Role.Button, onClick = onOpenLog)
                    .minimumInteractiveComponentSize()
                    .height(40.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BrandNavy,
            titleContentColor = Color.White
        )
    )
}

/** The bottom bar. [selected] is null while a screen over the tabs (Pillbox, Mindfulness) is open. */
@Composable
fun AppNavigationBar(selected: AppNavDestination?, onSelect: (AppNavDestination) -> Unit) {
    NavigationBar {
        AppNavDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                label = { Text(stringResource(destination.tabRes)) },
                icon = { Text(destination.icon) }
            )
        }
    }
}

/** Explains why notifications are needed before the system asks. Both buttons count as having asked. */
@Composable
fun ReminderPermissionDialog(onAllow: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.reminder_permission_title)) },
        text = { Text(stringResource(R.string.reminder_permission_body)) },
        confirmButton = { TextButton(onClick = onAllow) { Text(stringResource(R.string.reminder_permission_allow)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.reminder_permission_later)) } }
    )
}
