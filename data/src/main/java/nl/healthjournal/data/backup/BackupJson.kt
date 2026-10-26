package nl.healthjournal.data.backup

import nl.healthjournal.domain.model.backup.BackupDocument
import nl.healthjournal.domain.model.backup.BackupException
import nl.healthjournal.domain.model.backup.BackupMedication
import nl.healthjournal.domain.model.backup.BackupPreferences
import nl.healthjournal.domain.model.backup.BackupProfile
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.medication.DayPattern
import nl.healthjournal.domain.model.medication.Dosage
import nl.healthjournal.domain.model.medication.DoseUnit
import nl.healthjournal.domain.model.medication.Intake
import nl.healthjournal.domain.model.medication.IntakeId
import nl.healthjournal.domain.model.medication.IntakeStatus
import nl.healthjournal.domain.model.medication.Medication
import nl.healthjournal.domain.model.medication.MedicationForm
import nl.healthjournal.domain.model.medication.MedicationId
import nl.healthjournal.domain.model.medication.MedicationName
import nl.healthjournal.domain.model.medication.PillAppearance
import nl.healthjournal.domain.model.medication.PillColor
import nl.healthjournal.domain.model.medication.PillShape
import nl.healthjournal.domain.model.medication.Schedule
import nl.healthjournal.domain.model.medication.ScheduleVersion
import nl.healthjournal.domain.model.medication.Strength
import nl.healthjournal.domain.model.medication.StrengthUnit
import nl.healthjournal.domain.model.migraine.DailyActivities
import nl.healthjournal.domain.model.migraine.HeadArea
import nl.healthjournal.domain.model.migraine.MigraineEntry
import nl.healthjournal.domain.model.migraine.MigraineSymptom
import nl.healthjournal.domain.model.migraine.MigraineTag
import nl.healthjournal.domain.model.migraine.MigraineTagId
import nl.healthjournal.domain.model.migraine.MigraineTagKind
import nl.healthjournal.domain.model.migraine.MigraineTrigger
import nl.healthjournal.domain.model.migraine.MigraineType
import nl.healthjournal.domain.model.migraine.PainCharacter
import nl.healthjournal.domain.model.migraine.ReliefMethod
import nl.healthjournal.domain.model.metrics.ActivitySession
import nl.healthjournal.domain.model.metrics.BloodPressureEntry
import nl.healthjournal.domain.model.metrics.BloodPressureReading
import nl.healthjournal.domain.model.metrics.EntryComment
import nl.healthjournal.domain.model.metrics.GlucoseContext
import nl.healthjournal.domain.model.metrics.GlucoseEntry
import nl.healthjournal.domain.model.metrics.GlucoseLevel
import nl.healthjournal.domain.model.metrics.HeightCm
import nl.healthjournal.domain.model.metrics.WaistCircumferenceCm
import nl.healthjournal.domain.model.metrics.WaistCircumferenceEntry
import nl.healthjournal.domain.model.metrics.WeightEntry
import nl.healthjournal.domain.model.metrics.WeightKg
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import nl.healthjournal.domain.model.profile.Profile
import nl.healthjournal.domain.model.profile.Sex
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The JSON document inside a backup, exactly as `docs/backup-format/README.md` describes it.
 * Units are metric, instants are UTC, decimals are strings. Derived fields (bmi, categories) are written,
 * but the reader recomputes them with the domain rules and never trusts the file for them.
 */
object BackupJson {
    const val FORMAT = "healthjournal-backup"
    const val SCHEMA_VERSION = 1

    private val SNOOZE_MINUTES = setOf(10, 30, 60)

    // ---- write ----

    fun write(document: BackupDocument): ByteArray {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("createdAt", document.createdAt.toString())
        root.put("app", JSONObject().put("name", "Health Journal").put("version", nullable(document.appVersion)))
        document.preferences?.let { root.put("preferences", writePreferences(it)) }
        root.put("profiles", JSONArray().also { arr -> document.profiles.forEach { arr.put(writeProfile(it)) } })
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    private fun nullable(value: Any?): Any = value ?: JSONObject.NULL

    private fun writePreferences(p: BackupPreferences) = JSONObject()
        .put("language", p.language)
        .put("region", p.region)
        .put("unitSystem", p.unitSystem)
        .put("glucoseUnit", p.glucoseUnit)
        .put("theme", p.theme)
        .put("reminderSnoozeMinutes", p.reminderSnoozeMinutes)

    private fun writeProfile(bp: BackupProfile): JSONObject {
        val p = bp.profile
        return JSONObject()
            .put("id", p.id.toString())
            .put("name", p.name)
            .put("dateOfBirth", p.dateOfBirth.toString())
            .put("heightCm", nullable(p.height?.value))
            .put("sex", nullable(p.sex?.name))
            .put("isActive", bp.isActive)
            .put("weights", array(bp.weights.sortedBy { it.timestamp }) { e ->
                JSONObject().put("id", e.id.toString()).put("timestamp", e.timestamp.toString())
                    .put("weightKg", e.weight.value.toPlainString())
                    .put("bmi", nullable(e.bmi?.toPlainString()))
                    .put("comment", nullable(e.comment?.text))
            })
            .put("bloodPressures", array(bp.bloodPressures.sortedBy { it.timestamp }) { e ->
                JSONObject().put("id", e.id.toString()).put("timestamp", e.timestamp.toString())
                    .put("systolic", e.reading.systolic).put("diastolic", e.reading.diastolic)
                    .put("pulse", nullable(e.reading.pulse))
                    .put("category", e.category.name)
                    .put("comment", nullable(e.comment?.text))
            })
            .put("glucoses", array(bp.glucoses.sortedBy { it.timestamp }) { e ->
                JSONObject().put("id", e.id.toString()).put("timestamp", e.timestamp.toString())
                    .put("glucoseMmolL", e.glucose.valueInMmolL.toPlainString())
                    .put("context", e.context.name).put("category", e.category.name)
                    .put("comment", nullable(e.comment?.text))
            })
            .put("waistCircumferences", array(bp.waistCircumferences.sortedBy { it.timestamp }) { e ->
                JSONObject().put("id", e.id.toString()).put("timestamp", e.timestamp.toString())
                    .put("waistCm", decimal(e.waist.value))
                    .put("category", nullable(e.category?.name))
                    .put("comment", nullable(e.comment?.text))
            })
            .put("activities", array(bp.activities.sortedBy { it.startTime }) { e ->
                JSONObject().put("id", e.id.toString())
                    .put("startTime", e.startTime.toString()).put("endTime", e.endTime.toString())
                    .put("distanceMeters", decimal(e.distanceInMeters))
                    .put("comment", nullable(e.comment?.text))
            })
            .put("medications", array(bp.medications) { writeMedication(it) })
            .also { o ->
                // Written only when present, so a profile without Migraine data looks like it did before.
                if (bp.migraines.isNotEmpty()) o.put("migraines", array(bp.migraines.sortedBy { it.start }) { writeMigraine(it) })
                if (bp.migraineTags.isNotEmpty()) {
                    o.put("migraineTags", array(bp.migraineTags.sortedWith(compareBy({ it.kind.name }, { it.label.lowercase() }, { it.id.toString() }))) { t ->
                        JSONObject().put("id", t.id.toString()).put("kind", t.kind.name).put("label", t.label)
                            .put("archived", t.archived)
                    })
                }
            }
    }

    private fun writeMigraine(e: MigraineEntry): JSONObject = JSONObject()
        .put("id", e.id.toString())
        .put("start", e.start.toString())
        .put("wokeUpWithIt", e.wokeUpWithIt)
        .put("end", nullable(e.end?.toString()))
        .put("intensity", nullable(e.intensity))
        .put("areas", codes(e.areas))
        .put("painCharacters", codes(e.painCharacters))
        .put("type", nullable(e.type?.name))
        .put("symptoms", codes(e.symptoms))
        .put("triggers", codes(e.triggers))
        .put("customTriggers", ids(e.customTriggerIds))
        .put("tookSomething", e.tookSomething)
        .put("medicationText", nullable(e.medicationText))
        .put("secondDose", e.secondDose)
        .put("intensityAfter2Hours", nullable(e.intensityAfter2Hours))
        .put("medicationHelpedMe", e.medicationHelpedMe)
        .put("reliefMethods", codes(e.reliefMethods))
        .put("customReliefMethods", ids(e.customReliefIds))
        .put("reliefHelpedMe", e.reliefHelpedMe)
        .put("activities", nullable(e.activities?.name))
        .put("comment", nullable(e.comment?.text))

    private fun codes(values: Set<Enum<*>>): JSONArray = JSONArray().also { arr -> values.map { it.name }.sorted().forEach { arr.put(it) } }

    private fun ids(values: Set<MigraineTagId>): JSONArray =
        JSONArray().also { arr -> values.map { it.toString() }.sorted().forEach { arr.put(it) } }

    private fun decimal(value: Double): String = BigDecimal.valueOf(value).toPlainString()

    private fun <T> array(items: List<T>, map: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> items.forEach { arr.put(map(it)) } }

    private fun writeMedication(bm: BackupMedication): JSONObject {
        val m = bm.medication
        return JSONObject()
            .put("id", m.id.toString())
            .put("name", m.name.value)
            .put("form", nullable(m.form?.name))
            .put("strengthAmount", nullable(m.dosage.strength?.amount?.toPlainString()))
            .put("strengthUnit", nullable(m.dosage.strength?.unit?.name))
            .put("doseAmount", m.dosage.amountPerIntake.toPlainString())
            .put("doseUnit", m.dosage.doseUnit.name)
            .put("color", nullable(m.appearance?.color?.name))
            .put("shape", nullable(m.appearance?.shape?.name))
            .put("comment", nullable(m.comment?.text))
            .put("archivedFrom", nullable(m.archivedFrom?.toString()))
            .put("schedules", array(m.schedules) { writeSchedule(it) })
            .put("intakes", array(bm.intakes.sortedWith(compareBy({ it.planned }, { it.takenAt }))) { i ->
                JSONObject().put("id", i.id.toString())
                    .put("planned", nullable(i.planned?.toString()))
                    .put("status", i.status.name)
                    .put("takenAt", nullable(i.takenAt?.toString()))
                    .put("actualAmount", nullable(i.actualAmount?.toPlainString()))
                    .put("comment", nullable(i.comment?.text))
            })
    }

    private fun writeSchedule(v: ScheduleVersion): JSONObject {
        val o = JSONObject().put("effectiveFrom", v.effectiveFrom.toString())
        when (val s = v.schedule) {
            Schedule.AsNeeded -> o.put("asNeeded", true).put("daysOfWeek", JSONObject.NULL)
                .put("intervalDays", JSONObject.NULL).put("startDate", JSONObject.NULL)
                .put("endDate", JSONObject.NULL).put("times", JSONArray())
            is Schedule.Recurring -> {
                o.put("asNeeded", false)
                when (val d = s.days) {
                    is DayPattern.Weekdays -> o.put("daysOfWeek", JSONArray().also { arr ->
                        d.days.sorted().forEach { arr.put(it.name.take(3)) }
                    }).put("intervalDays", JSONObject.NULL)
                    is DayPattern.EveryNDays -> o.put("daysOfWeek", JSONObject.NULL).put("intervalDays", d.interval)
                }
                o.put("startDate", s.start.toString()).put("endDate", nullable(s.end?.toString()))
                    .put("times", JSONArray().also { arr -> s.times.forEach { arr.put(it.toString()) } })
            }
        }
        return o
    }

    // ---- read ----

    /** @throws BackupException.Unsupported for another schema version, [BackupException.Malformed] for bad content. */
    fun read(bytes: ByteArray): BackupDocument {
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: org.json.JSONException) {
            throw BackupException.Malformed("not JSON")
        }
        if (root.optString("format") != FORMAT) throw BackupException.Malformed("format")
        val version = root.optInt("schemaVersion", -1)
        if (version != SCHEMA_VERSION) throw BackupException.Unsupported("schema version $version, a newer app is needed")
        val unknown = UnknownItems()
        return BackupDocument(
            createdAt = guard("createdAt") { Instant.parse(root.getString("createdAt")) },
            appVersion = root.optJSONObject("app")?.let { if (it.isNull("version")) null else it.optString("version") },
            preferences = root.optJSONObject("preferences")?.let(::readPreferences),
            profiles = guard("profiles") { readList(root.getJSONArray("profiles")) { readProfile(it, unknown) } },
            unreadableItems = unknown.count
        )
    }

    /** Counts what a file from a newer app holds that this version leaves out, so the restore can say so. */
    private class UnknownItems {
        var count = 0
    }

    /** Wraps any parse or validation failure in [BackupException.Malformed]; the cause (which may hold values) is dropped. */
    private inline fun <T> guard(where: String, block: () -> T): T = try {
        block()
    } catch (e: BackupException) {
        throw e
    } catch (e: Exception) {
        throw BackupException.Malformed("invalid $where")
    }

    private fun <T> readList(arr: JSONArray?, map: (JSONObject) -> T): List<T> =
        if (arr == null) emptyList() else List(arr.length()) { map(arr.getJSONObject(it)) }

    private fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.req(key: String): String = text(key) ?: throw IllegalArgumentException(key)
    private fun JSONObject.dec(key: String): BigDecimal = BigDecimal(req(key))
    private fun JSONObject.comment(): EntryComment? = EntryComment.ofOrNull(text("comment"))

    private fun readPreferences(o: JSONObject): BackupPreferences {
        fun tag(key: String, allowed: Set<String>): String =
            o.optString(key, "system").takeIf { it in allowed } ?: "system"
        val snooze = o.optInt("reminderSnoozeMinutes", 30).takeIf { it in SNOOZE_MINUTES } ?: 30
        return BackupPreferences(
            language = tag("language", setOf("system", "en", "nl")),
            region = tag("region", setOf("system", "NL", "US")),
            unitSystem = tag("unitSystem", setOf("system", "metric", "imperial")),
            glucoseUnit = tag("glucoseUnit", setOf("system", "mmol", "mgdl")),
            theme = tag("theme", setOf("system", "light", "dark")),
            reminderSnoozeMinutes = snooze
        )
    }

    private fun readProfile(o: JSONObject, unknown: UnknownItems): BackupProfile {
        val profile = guard("profile") {
            Profile.reconstruct(
                id = ProfileId.fromString(o.req("id")),
                name = o.req("name"),
                dateOfBirth = LocalDate.parse(o.req("dateOfBirth")),
                height = if (o.isNull("heightCm")) null else HeightCm(o.getInt("heightCm")),
                sex = o.text("sex")?.let { Sex.valueOf(it) }
            )
        }
        val pid = profile.id
        return BackupProfile(
            profile = profile,
            isActive = o.optBoolean("isActive", false),
            weights = guard("weights") {
                readList(o.optJSONArray("weights")) { e ->
                    val weight = WeightKg(e.dec("weightKg"))
                    WeightEntry(
                        MeasurementId.fromString(e.req("id")), pid, Instant.parse(e.req("timestamp")), weight,
                        profile.calculateBmi(weight)?.bmi, e.comment()
                    )
                }
            },
            bloodPressures = guard("bloodPressures") {
                readList(o.optJSONArray("bloodPressures")) { e ->
                    val reading = BloodPressureReading(
                        e.getInt("systolic"), e.getInt("diastolic"), if (e.isNull("pulse")) null else e.getInt("pulse")
                    )
                    BloodPressureEntry(
                        MeasurementId.fromString(e.req("id")), pid, Instant.parse(e.req("timestamp")), reading,
                        NhgBloodPressureCategory.classify(reading), e.comment()
                    )
                }
            },
            glucoses = guard("glucoses") {
                readList(o.optJSONArray("glucoses")) { e ->
                    val level = GlucoseLevel(e.dec("glucoseMmolL"))
                    val context = GlucoseContext.valueOf(e.req("context"))
                    GlucoseEntry(
                        MeasurementId.fromString(e.req("id")), pid, Instant.parse(e.req("timestamp")), level, context,
                        NhgGlucoseCategory.classify(level, context), e.comment()
                    )
                }
            },
            waistCircumferences = guard("waistCircumferences") {
                readList(o.optJSONArray("waistCircumferences")) { e ->
                    val waist = WaistCircumferenceCm(e.dec("waistCm").toDouble())
                    WaistCircumferenceEntry(
                        MeasurementId.fromString(e.req("id")), pid, Instant.parse(e.req("timestamp")), waist,
                        profile.classifyWaistCircumference(waist), e.comment()
                    )
                }
            },
            activities = guard("activities") {
                readList(o.optJSONArray("activities")) { e ->
                    ActivitySession(
                        MeasurementId.fromString(e.req("id")), pid,
                        Instant.parse(e.req("startTime")), Instant.parse(e.req("endTime")),
                        e.dec("distanceMeters").toDouble(), e.comment()
                    )
                }
            },
            medications = guard("medications") { readList(o.optJSONArray("medications")) { readMedication(it, pid) } },
            migraines = guard("migraines") { readList(o.optJSONArray("migraines")) { readMigraine(it, pid, unknown) } },
            migraineTags = guard("migraineTags") {
                // A tag of a kind this version does not know (from a newer file) is left out and counted.
                readList(o.optJSONArray("migraineTags")) { t ->
                    enumOrNull<MigraineTagKind>(t.req("kind"), unknown)?.let { kind ->
                        MigraineTag(MigraineTagId.fromString(t.req("id")), pid, kind, t.req("label"), t.optBoolean("archived", false))
                    }
                }.filterNotNull()
            }
        )
    }

    private fun readMigraine(o: JSONObject, profileId: ProfileId, unknown: UnknownItems) = MigraineEntry(
        id = MeasurementId.fromString(o.req("id")),
        profileId = profileId,
        start = Instant.parse(o.req("start")),
        wokeUpWithIt = o.optBoolean("wokeUpWithIt", false),
        end = o.text("end")?.let { Instant.parse(it) },
        intensity = if (o.isNull("intensity")) null else o.getInt("intensity"),
        areas = enums<HeadArea>(o, "areas", unknown),
        painCharacters = enums<PainCharacter>(o, "painCharacters", unknown),
        type = enumOrNull<MigraineType>(o.text("type"), unknown),
        symptoms = enums<MigraineSymptom>(o, "symptoms", unknown),
        triggers = enums<MigraineTrigger>(o, "triggers", unknown),
        customTriggerIds = tagIds(o, "customTriggers"),
        tookSomething = o.optBoolean("tookSomething", false),
        medicationText = o.text("medicationText"),
        secondDose = o.optBoolean("secondDose", false),
        intensityAfter2Hours = if (o.isNull("intensityAfter2Hours")) null else o.getInt("intensityAfter2Hours"),
        medicationHelpedMe = o.optBoolean("medicationHelpedMe", false),
        reliefMethods = enums<ReliefMethod>(o, "reliefMethods", unknown),
        customReliefIds = tagIds(o, "customReliefMethods"),
        reliefHelpedMe = o.optBoolean("reliefHelpedMe", false),
        activities = enumOrNull<DailyActivities>(o.text("activities"), unknown),
        comment = o.comment()
    )

    private fun strings(o: JSONObject, key: String): List<String> =
        o.optJSONArray(key)?.let { arr -> List(arr.length()) { arr.getString(it) } }.orEmpty()

    /** Codes this version does not know (from a newer file) are left out and counted. */
    private inline fun <reified E : Enum<E>> enums(o: JSONObject, key: String, unknown: UnknownItems): Set<E> =
        strings(o, key).mapNotNull { name -> enumOrNull<E>(name, unknown) }.toSet()

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?, unknown: UnknownItems): E? =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } ?: null.also { unknown.count++ } }

    private fun tagIds(o: JSONObject, key: String): Set<MigraineTagId> =
        strings(o, key).mapNotNull { runCatching { MigraineTagId.fromString(it) }.getOrNull() }.toSet()

    private fun readMedication(o: JSONObject, profileId: ProfileId): BackupMedication {
        val id = MedicationId.fromString(o.req("id"))
        val strength = if (o.isNull("strengthAmount")) null else
            Strength(o.dec("strengthAmount"), StrengthUnit.valueOf(o.req("strengthUnit")))
        val appearance = if (o.isNull("color") || o.isNull("shape")) null else
            PillAppearance(PillColor.valueOf(o.req("color")), PillShape.valueOf(o.req("shape")))
        val medication = Medication(
            id = id,
            profileId = profileId,
            name = MedicationName.of(o.req("name")),
            form = o.text("form")?.let { MedicationForm.valueOf(it) },
            dosage = Dosage(strength, o.dec("doseAmount"), DoseUnit.valueOf(o.req("doseUnit"))),
            appearance = appearance,
            schedules = readList(o.getJSONArray("schedules")) { readSchedule(it) }.sortedBy { it.effectiveFrom },
            comment = o.comment(),
            archivedFrom = o.text("archivedFrom")?.let { LocalDate.parse(it) }
        )
        val intakes = readList(o.optJSONArray("intakes")) { e ->
            Intake(
                id = IntakeId.fromString(e.req("id")),
                medicationId = id,
                planned = e.text("planned")?.let { LocalDateTime.parse(it) },
                status = IntakeStatus.valueOf(e.req("status")),
                takenAt = e.text("takenAt")?.let { Instant.parse(it) },
                actualAmount = if (e.isNull("actualAmount")) null else e.dec("actualAmount"),
                comment = e.comment()
            )
        }
        return BackupMedication(medication, intakes)
    }

    private fun readSchedule(o: JSONObject): ScheduleVersion {
        val from = LocalDate.parse(o.req("effectiveFrom"))
        if (o.optBoolean("asNeeded", false)) return ScheduleVersion(from, Schedule.AsNeeded)
        val times = o.getJSONArray("times").let { arr -> List(arr.length()) { LocalTime.parse(arr.getString(it)) } }
        val days = if (!o.isNull("intervalDays")) {
            DayPattern.EveryNDays(o.getInt("intervalDays"))
        } else {
            val arr = o.getJSONArray("daysOfWeek")
            DayPattern.Weekdays(List(arr.length()) { dayOfWeek(arr.getString(it)) }.toSet())
        }
        val recurring = Schedule.Recurring.of(
            times, days, LocalDate.parse(o.req("startDate")), o.text("endDate")?.let { LocalDate.parse(it) }
        )
        return ScheduleVersion(from, recurring)
    }

    private fun dayOfWeek(tag: String): DayOfWeek =
        DayOfWeek.values().firstOrNull { it.name.take(3) == tag } ?: throw IllegalArgumentException("day")
}
