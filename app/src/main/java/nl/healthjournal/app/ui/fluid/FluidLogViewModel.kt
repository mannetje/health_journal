package nl.healthjournal.app.ui.fluid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.healthjournal.app.ui.common.UiText
import nl.healthjournal.app.ui.common.toUiText
import nl.healthjournal.domain.metrics.command.ObserveFluidHistoryQuery
import nl.healthjournal.domain.metrics.command.ObserveFluidLimitsQuery
import nl.healthjournal.domain.metrics.command.RecordFluidIntakeCommand
import nl.healthjournal.domain.metrics.command.SetFluidLimitsCommand
import nl.healthjournal.domain.metrics.model.DrinkPortions
import nl.healthjournal.domain.metrics.model.FluidDayTotals
import nl.healthjournal.domain.metrics.model.FluidDrink
import nl.healthjournal.domain.metrics.model.FluidIntakeEntry
import nl.healthjournal.domain.metrics.model.FluidLimits
import nl.healthjournal.domain.metrics.model.FluidPosition
import nl.healthjournal.domain.metrics.model.fluidPosition
import nl.healthjournal.domain.profile.command.GetActiveProfileQuery
import nl.healthjournal.domain.profile.model.Profile
import nl.healthjournal.domain.shared.command.DeleteEntryCommand
import nl.healthjournal.domain.shared.command.Dispatcher
import nl.healthjournal.domain.shared.command.DomainResult
import nl.healthjournal.domain.shared.command.EntryKind
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What the Fluid tab shows. All totals are derived in `:domain`; nothing here assesses anything. */
data class FluidUiState(
    val activeProfile: Profile? = null,
    val portions: DrinkPortions = DrinkPortions(),
    /** How many portions of each drink the next tap adds; one unless the person steps it up. */
    val counts: Map<FluidDrink, Int> = emptyMap(),
    val todayEntries: List<FluidIntakeEntry> = emptyList(),
    val todayMl: Int = 0,
    val limits: FluidLimits = FluidLimits.NONE,
    val position: FluidPosition = FluidPosition.NONE,
    /** The entry the Undo message would remove: the last tap. */
    val lastAdded: FluidIntakeEntry? = null,
    val message: UiText? = null
) {
    fun countOf(drink: FluidDrink): Int = counts[drink] ?: 1
}

@HiltViewModel
class FluidLogViewModel(
    private val dispatcher: Dispatcher,
    private val clock: Clock,
    private val zone: () -> ZoneId
) : ViewModel() {

    @Inject
    constructor(dispatcher: Dispatcher) : this(dispatcher, Clock.systemDefaultZone(), { ZoneId.systemDefault() })

    private val _uiState = MutableStateFlow(FluidUiState())
    val uiState: StateFlow<FluidUiState> = _uiState.asStateFlow()

    private var observing: Job? = null
    private var allEntries: List<FluidIntakeEntry> = emptyList()

    init {
        loadProfile()
    }

    /** Also called after a backup restore or a profile switch. */
    fun loadProfile() {
        viewModelScope.launch {
            val profile = dispatcher.query(GetActiveProfileQuery).getOrNull()
            _uiState.value = _uiState.value.copy(activeProfile = profile)
            observing?.cancel()
            if (profile == null) {
                allEntries = emptyList()
                _uiState.value = _uiState.value.copy(todayEntries = emptyList(), todayMl = 0, limits = FluidLimits.NONE, position = FluidPosition.NONE)
                return@launch
            }
            observing = launch {
                launch {
                    dispatcher.observe(ObserveFluidHistoryQuery(profile.id)).collect { result ->
                        result.getOrNull()?.let { allEntries = it; refreshToday() }
                    }
                }
                launch {
                    dispatcher.observe(ObserveFluidLimitsQuery(profile.id)).collect { result ->
                        result.getOrNull()?.let { limits ->
                            _uiState.value = _uiState.value.copy(limits = limits)
                            refreshToday()
                        }
                    }
                }
            }
        }
    }

    private fun refreshToday() {
        val z = zone()
        val today: LocalDate = clock.instant().atZone(z).toLocalDate()
        val entries = allEntries.filter { it.timestamp.atZone(z).toLocalDate() == today }.sortedBy { it.timestamp }
        val total = FluidDayTotals.forDay(allEntries, today, z)
        val s = _uiState.value
        _uiState.value = s.copy(
            todayEntries = entries,
            todayMl = total,
            position = if (entries.isEmpty()) FluidPosition.NONE else fluidPosition(total, s.limits)
        )
    }

    fun stepCount(drink: FluidDrink, delta: Int) {
        val s = _uiState.value
        _uiState.value = s.copy(counts = s.counts + (drink to (s.countOf(drink) + delta).coerceIn(1, 10)))
    }

    /** One tap: one entry of [FluidIntakeEntry.drink] with the count times the drink's own portion. */
    fun add(drink: FluidDrink) {
        val s = _uiState.value
        val profile = s.activeProfile ?: return
        val ml = s.portions.of(drink) * s.countOf(drink)
        viewModelScope.launch {
            when (val result = dispatcher.dispatch(RecordFluidIntakeCommand(profile.id, drink, ml))) {
                is DomainResult.Success ->
                    _uiState.value = _uiState.value.copy(lastAdded = result.value, counts = _uiState.value.counts - drink, message = null)
                is DomainResult.Failure -> _uiState.value = _uiState.value.copy(message = result.error.toUiText())
            }
        }
    }

    fun undo() {
        val last = _uiState.value.lastAdded ?: return
        viewModelScope.launch {
            dispatcher.dispatch(DeleteEntryCommand(EntryKind.FLUID_INTAKE, last.id))
            _uiState.value = _uiState.value.copy(lastAdded = null)
        }
    }

    fun dismissUndo() {
        _uiState.value = _uiState.value.copy(lastAdded = null)
    }

    /** Null clears a number. The numbers are only ever what the person typed. */
    fun saveLimits(minMl: Int?, maxMl: Int?, onSaved: () -> Unit = {}) {
        val profile = _uiState.value.activeProfile ?: return
        viewModelScope.launch {
            when (val result = dispatcher.dispatch(SetFluidLimitsCommand(profile.id, minMl, maxMl))) {
                is DomainResult.Success -> { _uiState.value = _uiState.value.copy(message = null); onSaved() }
                is DomainResult.Failure -> _uiState.value = _uiState.value.copy(message = result.error.toUiText())
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
