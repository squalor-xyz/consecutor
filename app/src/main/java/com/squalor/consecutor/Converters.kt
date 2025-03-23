package com.squalor.consecutor

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * Converts LocalDate to and from String for Room database storage.
 */
object Converters {
    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    @JvmStatic
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }
}