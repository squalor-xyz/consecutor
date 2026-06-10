package com.squalor.consecutor

import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    fun fromTrackerType(value: TrackerType): String = value.name

    @TypeConverter
    fun toTrackerType(value: String): TrackerType = TrackerType.valueOf(value)

    @TypeConverter
    fun fromTargetPeriod(value: TargetPeriod): String = value.name

    @TypeConverter
    fun toTargetPeriod(value: String): TargetPeriod = TargetPeriod.valueOf(value)
}
