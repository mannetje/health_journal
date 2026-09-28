package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var birthDateInput by remember { mutableStateOf("") } // YYYY-MM-DD
    var heightInput by remember { mutableStateOf("") }
    var sexInput by remember { mutableStateOf<Sex?>(null) }

    LaunchedEffect(state.activeProfile) {
        state.activeProfile?.let {
            nameInput = it.name
            birthDateInput = it.dateOfBirth.toString()
            heightInput = it.height?.value?.toString() ?: ""
            sexInput = it.sex
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (state.activeProfile != null) "Edit Profile" else "Create Profile",
            style = MaterialTheme.typography.headlineSmall
        )

        state.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        state.successMessage?.let { success ->
            Card(
                colors = CardDefaults.cardColors(containerColor = successContainerColor)
            ) {
                Text(
                    text = success,
                    color = onSuccessContainerColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Full Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = birthDateInput,
            onValueChange = { birthDateInput = it },
            label = { Text("Date of Birth (YYYY-MM-DD) *") },
            placeholder = { Text("e.g. 1990-05-15") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = heightInput,
            onValueChange = { heightInput = it },
            label = { Text("Height (cm, optional)") },
            placeholder = { Text("e.g. 180") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Sex (optional)", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf<Sex?>(null, Sex.FEMALE, Sex.MALE)
                val labels = listOf("Not set", "Female", "Male")
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = sexInput == option,
                        onClick = { sexInput = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index])
                    }
                }
            }
        }

        Button(
            onClick = {
                try {
                    val dob = LocalDate.parse(birthDateInput.trim())
                    val height = heightInput.trim().toIntOrNull()
                    viewModel.saveProfile(nameInput.trim(), dob, height, sexInput)
                } catch (e: Exception) {
                    // Let ViewModel / exception handle error
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && nameInput.isNotBlank() && birthDateInput.isNotBlank()
        ) {
            Text(if (state.activeProfile != null) "Update Profile" else "Create Profile")
        }

        if (state.activeProfile != null) {
            HorizontalDivider()
            Text(
                text = "Active Profile Summary",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Name: ${state.activeProfile?.name}")
            Text("Date of Birth: ${state.activeProfile?.dateOfBirth}")
            Text("Height: ${state.activeProfile?.height?.let { "${it.value} cm" } ?: "Not set"}")
            Text("Sex: ${state.activeProfile?.sex?.name ?: "Not set"}")
        }
    }
}
