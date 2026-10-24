package nl.healthjournal.domain.model.migraine

import java.time.LocalTime

/** Where on the head the pain sat. Plain list the person picks from; stored by code name. */
enum class HeadArea {
    WHOLE_HEAD, LEFT_SIDE, RIGHT_SIDE, FOREHEAD, BEHIND_EYES, TEMPLES, BACK_OF_HEAD_AND_NECK, OTHER
}

/** The person's own label for the headache. The system never chooses or changes it. */
enum class MigraineType { WITH_AURA, WITHOUT_AURA, RECOGNIZED_HEADACHE, OTHER_HEADACHE, NOT_SURE }

enum class MigraineSymptom {
    NAUSEA, AURA, SENSITIVE_TO_LIGHT, SENSITIVE_TO_SOUND, SENSITIVE_TO_SMELL, DIZZINESS, VERTIGO,
    MOVEMENT_SYMPTOMS, WORSE_WITH_ACTIVITY, BRAIN_FOG, EXTREME_FATIGUE, OTHER
}

/** Triggers the person noted themselves; never presented as a cause. */
enum class MigraineTrigger { SLEEP, STRESS, FOOD, WEATHER, MENSTRUATION, OTHER }

enum class PainCharacter { THROBBING, PRESSING, PIERCING }

/** What the person did besides taking something. No medicine or supplement names. */
enum class ReliefMethod { SLEEP_OR_REST, DARK_QUIET_ROOM, COLD_PACK, HEAT, MASSAGE, COFFEE_OR_TEA, OTHER }

/** The person's own note on whether they could carry on. Never counted or interpreted. */
enum class DailyActivities { COULD_CARRY_ON, HAD_TO_STOP }

enum class MigraineTagKind { TRIGGER, RELIEF }

/** Heat map buckets: night 00:00-05:59, morning 06:00-11:59, afternoon 12:00-17:59, evening 18:00-23:59. */
enum class TimeOfDay {
    NIGHT, MORNING, AFTERNOON, EVENING;

    companion object {
        fun of(time: LocalTime): TimeOfDay = when (time.hour) {
            in 0..5 -> NIGHT
            in 6..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}
