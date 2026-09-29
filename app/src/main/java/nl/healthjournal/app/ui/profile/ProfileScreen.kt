package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.AppLanguage
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onLanguageChange: (AppLanguage) -> Unit = {}
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
            text = stringResource(if (state.activeProfile != null) R.string.profile_title_edit else R.string.profile_title_create),
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
            label = { Text(stringResource(R.string.profile_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = birthDateInput,
            onValueChange = { birthDateInput = it },
            label = { Text(stringResource(R.string.profile_dob_label)) },
            placeholder = { Text(stringResource(R.string.profile_dob_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = heightInput,
            onValueChange = { heightInput = it },
            label = { Text(stringResource(R.string.profile_height_label)) },
            placeholder = { Text(stringResource(R.string.profile_height_placeholder)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.profile_sex_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf<Sex?>(null, Sex.FEMALE, Sex.MALE)
                val labels = listOf(
                    stringResource(R.string.common_not_set),
                    stringResource(R.string.profile_sex_female),
                    stringResource(R.string.profile_sex_male)
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = sexInput == option,
                        onClick = { sexInput = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.profile_language_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.DUTCH)
                val labels = listOf(
                    stringResource(R.string.profile_language_system),
                    stringResource(R.string.profile_language_english),
                    stringResource(R.string.profile_language_dutch)
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = currentLanguage == option,
                        onClick = { onLanguageChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], maxLines = 1, softWrap = false)
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
            Text(stringResource(if (state.activeProfile != null) R.string.profile_update_button else R.string.profile_title_create))
        }

        if (state.activeProfile != null) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.profile_summary_title),
                style = MaterialTheme.typography.titleMedium
            )
            val notSet = stringResource(R.string.common_not_set)
            Text(stringResource(R.string.profile_summary_name, state.activeProfile?.name.orEmpty()))
            Text(stringResource(R.string.profile_summary_dob, state.activeProfile?.dateOfBirth.toString()))
            val heightText = state.activeProfile?.height?.value?.let {
                stringResource(R.string.profile_height_value, it.toString())
            } ?: notSet
            Text(stringResource(R.string.profile_summary_height, heightText))
            val sexText = when (state.activeProfile?.sex) {
                Sex.FEMALE -> stringResource(R.string.profile_sex_female)
                Sex.MALE -> stringResource(R.string.profile_sex_male)
                null -> notSet
            }
            Text(stringResource(R.string.profile_summary_sex, sexText))
        }
    }
}
