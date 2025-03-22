package com.squalor.consecutor

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Converts LocalDate to and from String for Room database storage.
 */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString() // ISO-8601 format (e.g., "2023-10-25")
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }
}