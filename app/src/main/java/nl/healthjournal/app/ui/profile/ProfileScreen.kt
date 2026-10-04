package nl.healthjournal.app.ui.profile

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.healthjournal.app.R
import nl.healthjournal.app.settings.AppLanguage
import nl.healthjournal.app.settings.AppRegion
import nl.healthjournal.app.settings.GlucoseUnitChoice
import nl.healthjournal.app.settings.UnitSystemChoice
import nl.healthjournal.app.ui.common.LocalDisplayUnits
import nl.healthjournal.app.ui.common.asString
import nl.healthjournal.app.ui.common.formatHeight
import nl.healthjournal.app.ui.theme.onSuccessContainerColor
import nl.healthjournal.app.ui.theme.successContainerColor
import nl.healthjournal.domain.model.common.UnitConversion
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
    currentLanguage: AppLanguage = AppLanguage.SYSTEM,
    onLanguageChange: (AppLanguage) -> Unit = {},
    currentRegion: AppRegion = AppRegion.SYSTEM,
    onRegionChange: (AppRegion) -> Unit = {},
    currentUnitSystem: UnitSystemChoice = UnitSystemChoice.SYSTEM,
    onUnitSystemChange: (UnitSystemChoice) -> Unit = {},
    currentGlucoseUnit: GlucoseUnitChoice = GlucoseUnitChoice.SYSTEM,
    onGlucoseUnitChange: (GlucoseUnitChoice) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val units = LocalDisplayUnits.current

    var nameInput by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    // Height is kept as centimetres (the stored unit). The feet and inches fields are a view on it and only
    // rewrite it when edited, so an untouched height never changes through unit rounding.
    var heightInput by remember { mutableStateOf("") }
    var feetInput by remember { mutableStateOf("") }
    var inchesInput by remember { mutableStateOf("") }
    var sexInput by remember { mutableStateOf<Sex?>(null) }

    LaunchedEffect(state.activeProfile) {
        state.activeProfile?.let {
            nameInput = it.name
            birthDate = it.dateOfBirth
            heightInput = it.height?.value?.toString() ?: ""
            sexInput = it.sex
        }
    }

    LaunchedEffect(state.activeProfile, units.isImperial) {
        val cm = heightInput.trim().toIntOrNull()
        if (cm != null) {
            val (feet, inches) = UnitConversion.cmToFeetInches(cm)
            feetInput = feet.toString()
            inchesInput = inches.toString()
        } else {
            feetInput = ""
            inchesInput = ""
        }
    }

    fun updateImperialHeight(feet: String, inches: String) {
        feetInput = feet
        inchesInput = inches
        val ft = feet.trim().toIntOrNull()
        val inch = inches.trim().toIntOrNull()
        heightInput = if (ft == null && inch == null) "" else UnitConversion.feetInchesToCm(ft ?: 0, inch ?: 0).toString()
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
                    text = error.asString(),
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
                    text = success.asString(),
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

        BirthDateField(value = birthDate, onChange = { birthDate = it })

        // A non-numeric height is an error, not "no height": saving would otherwise silently clear it.
        val heightInvalid = if (units.isImperial) {
            (feetInput.isNotBlank() && feetInput.trim().toIntOrNull() == null) ||
                (inchesInput.isNotBlank() && inchesInput.trim().toIntOrNull() == null)
        } else {
            heightInput.isNotBlank() && heightInput.trim().toIntOrNull() == null
        }

        if (units.isImperial) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.profile_height_label_imperial), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feetInput,
                        onValueChange = { updateImperialHeight(it, inchesInput) },
                        label = { Text(stringResource(R.string.profile_height_feet_label)) },
                        suffix = { Text("ft") },
                        isError = heightInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inchesInput,
                        onValueChange = { updateImperialHeight(feetInput, it) },
                        label = { Text(stringResource(R.string.profile_height_inches_label)) },
                        suffix = { Text("in") },
                        isError = heightInvalid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                if (heightInvalid) {
                    Text(
                        stringResource(R.string.profile_height_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text(stringResource(R.string.profile_height_label, "cm")) },
                placeholder = { Text(stringResource(R.string.profile_height_placeholder)) },
                isError = heightInvalid,
                supportingText = if (heightInvalid) {
                    { Text(stringResource(R.string.profile_height_invalid)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

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

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.profile_region_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(AppRegion.SYSTEM, AppRegion.NETHERLANDS, AppRegion.UNITED_STATES)
                val labels = listOf(
                    stringResource(R.string.profile_language_system),
                    stringResource(R.string.profile_region_netherlands),
                    stringResource(R.string.profile_region_us)
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = currentRegion == option,
                        onClick = { onRegionChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], maxLines = 1, softWrap = false)
                    }
                }
            }
            Text(
                stringResource(R.string.profile_region_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.profile_units_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(UnitSystemChoice.SYSTEM, UnitSystemChoice.METRIC, UnitSystemChoice.IMPERIAL)
                val labels = listOf(
                    stringResource(R.string.profile_language_system),
                    stringResource(R.string.profile_units_metric),
                    stringResource(R.string.profile_units_imperial)
                )
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = currentUnitSystem == option,
                        onClick = { onUnitSystemChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], maxLines = 1, softWrap = false)
                    }
                }
            }
            Text(stringResource(R.string.profile_glucose_unit_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(GlucoseUnitChoice.SYSTEM, GlucoseUnitChoice.MMOL, GlucoseUnitChoice.MGDL)
                val labels = listOf(stringResource(R.string.profile_language_system), "mmol/L", "mg/dL")
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = currentGlucoseUnit == option,
                        onClick = { onGlucoseUnitChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(labels[index], maxLines = 1, softWrap = false)
                    }
                }
            }
            Text(
                stringResource(R.string.profile_units_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = {
                birthDate?.let { dob ->
                    val height = heightInput.trim().toIntOrNull()
                    viewModel.saveProfile(nameInput.trim(), dob, height, sexInput)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && nameInput.isNotBlank() && birthDate != null && !heightInvalid
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
                stringResource(R.string.profile_height_value, units.formatHeight(it))
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

/**
 * Date-of-birth field: a read-only field that opens a Material date picker. The picker starts in
 * text-input mode (typing a birth date is faster than paging back through decades) and offers the
 * calendar as a toggle; only past dates from 1900 onwards are selectable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(value: LocalDate?, onChange: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) showPicker = true }

    OutlinedTextField(
        value = value?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.profile_dob_label)) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.profile_dob_pick))
            }
        },
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    if (showPicker) {
        val today = LocalDate.now()
        val todayUtcMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayMode = DisplayMode.Input,
            yearRange = 1900..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis < todayUtcMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedDateMillis != null,
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            onChange(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    }
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.common_cancel)) } }
        ) { DatePicker(state = pickerState) }
    }
}
