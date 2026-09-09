package nl.healthjournal.data.local.mapper

import nl.healthjournal.data.local.entity.ActivityEntity
import nl.healthjournal.data.local.entity.BloodPressureEntity
import nl.healthjournal.data.local.entity.GlucoseEntity
import nl.healthjournal.data.local.entity.WeightEntity
import nl.healthjournal.domain.model.common.MeasurementId
import nl.healthjournal.domain.model.common.ProfileId
import nl.healthjournal.domain.model.metrics.*
import nl.healthjournal.domain.model.nhg.NhgBloodPressureCategory
import nl.healthjournal.domain.model.nhg.NhgGlucoseCategory
import java.math.BigDecimal
import java.time.Instant

object HealthLogMapper {

    // Weight
    fun toEntity(domain: WeightEntry): WeightEntity {
        return WeightEntity(
            id = domain.id.value.toString(),
            profileId = domain.profileId.value.toString(),
            timestamp = domain.timestamp.toEpochMilli(),
            weightKg = domain.weight.value.toDouble(),
            bmi = domain.bmi?.toDouble()
        )
    }

    fun toDomain(entity: WeightEntity): WeightEntry {
        return WeightEntry(
            id = MeasurementId.fromString(entity.id),
            profileId = ProfileId.fromString(entity.profileId),
            timestamp = Instant.ofEpochMilli(entity.timestamp),
            weight = WeightKg(BigDecimal.valueOf(entity.weightKg)),
            bmi = entity.bmi?.let { BigDecimal.valueOf(it) }
        )
    }

    // Blood Pressure
    fun toEntity(domain: BloodPressureEntry): BloodPressureEntity {
        return BloodPressureEntity(
            id = domain.id.value.toString(),
            profileId = domain.profileId.value.toString(),
            timestamp = domain.timestamp.toEpochMilli(),
            systolic = domain.reading.systolic,
            diastolic = domain.reading.diastolic,
            category = domain.category.name
        )
    }

    fun toDomain(entity: BloodPressureEntity): BloodPressureEntry {
        return BloodPressureEntry(
            id = MeasurementId.fromString(entity.id),
            profileId = ProfileId.fromString(entity.profileId),
            timestamp = Instant.ofEpochMilli(entity.timestamp),
            reading = BloodPressureReading(entity.systolic, entity.diastolic),
            category = NhgBloodPressureCategory.valueOf(entity.category)
        )
    }

    // Glucose
    fun toEntity(domain: GlucoseEntry): GlucoseEntity {
        return GlucoseEntity(
            id = domain.id.value.toString(),
            profileId = domain.profileId.value.toString(),
            timestamp = domain.timestamp.toEpochMilli(),
            glucoseMmolL = domain.glucose.valueInMmolL.toDouble(),
            context = domain.context.name,
            category = domain.category.name
        )
    }

    fun toDomain(entity: GlucoseEntity): GlucoseEntry {
        return GlucoseEntry(
            id = MeasurementId.fromString(entity.id),
            profileId = ProfileId.fromString(entity.profileId),
            timestamp = Instant.ofEpochMilli(entity.timestamp),
            glucose = GlucoseLevel(BigDecimal.valueOf(entity.glucoseMmolL)),
            context = GlucoseContext.valueOf(entity.context),
            category = NhgGlucoseCategory.valueOf(entity.category)
        )
    }

    // Activity
    fun toEntity(domain: ActivitySession): ActivityEntity {
        return ActivityEntity(
            id = domain.id.value.toString(),
            profileId = domain.profileId.value.toString(),
            startTime = domain.startTime.toEpochMilli(),
            endTime = domain.endTime.toEpochMilli(),
            distanceMeters = domain.distanceInMeters
        )
    }

    fun toDomain(entity: ActivityEntity): ActivitySession {
        return ActivitySession(
            id = MeasurementId.fromString(entity.id),
            profileId = ProfileId.fromString(entity.profileId),
            startTime = Instant.ofEpochMilli(entity.startTime),
            endTime = Instant.ofEpochMilli(entity.endTime),
            distanceInMeters = entity.distanceMeters
        )
    }
}
