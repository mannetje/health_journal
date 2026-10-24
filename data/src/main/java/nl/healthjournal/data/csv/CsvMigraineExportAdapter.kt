package nl.healthjournal.data.csv

import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.port.secondary.MigraineExportPort
import nl.healthjournal.domain.port.secondary.MigraineRepositoryPort

internal const val MIGRAINES_HEADER =
    "start_timestamp,woke_up_with_it,end_timestamp,intensity,areas,pain_character,type,symptoms,triggers," +
        "took_something,medication_text,second_dose,intensity_after_2h,relief_methods,custom_triggers," +
        "custom_relief_methods,medication_helped_me,relief_helped_me,activities,comment"

/**
 * Writes the migraine file. Stable English codes joined by `|`, never translated labels; personal tags by their
 * label (also archived ones, so old entries keep theirs).
 */
class CsvMigraineExportAdapter(
    private val repository: MigraineRepositoryPort
) : MigraineExportPort {

    override suspend fun exportMigraineCsv(profileId: ProfileId): String {
        val labels = repository.getMigraineTags(profileId).associate { it.id to it.label }
        fun tags(ids: Set<MigraineTagId>) = ids.mapNotNull { labels[it] }.sortedBy { it.lowercase() }.joinToString("|")
        fun codes(values: Set<Enum<*>>) = values.map { it.name }.sorted().joinToString("|")

        val sb = StringBuilder(MIGRAINES_HEADER).append('\n')
        for (e in repository.getMigraineHistory(profileId).sortedBy { it.start }) {
            sb.append(
                listOf(
                    e.start.toString(),
                    e.wokeUpWithIt.toString(),
                    e.end?.toString().orEmpty(),
                    e.intensity?.toString().orEmpty(),
                    codes(e.areas),
                    codes(e.painCharacters),
                    e.type?.name.orEmpty(),
                    codes(e.symptoms),
                    codes(e.triggers),
                    e.tookSomething.toString(),
                    CsvQuoting.cell(e.medicationText),
                    e.secondDose.toString(),
                    e.intensityAfter2Hours?.toString().orEmpty(),
                    codes(e.reliefMethods),
                    CsvQuoting.cell(tags(e.customTriggerIds)),
                    CsvQuoting.cell(tags(e.customReliefIds)),
                    e.medicationHelpedMe.toString(),
                    e.reliefHelpedMe.toString(),
                    e.activities?.name.orEmpty(),
                    CsvQuoting.cell(e.comment?.text)
                ).joinToString(",")
            ).append('\n')
        }
        return sb.toString()
    }
}
