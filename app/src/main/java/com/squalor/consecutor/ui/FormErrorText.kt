package com.squalor.consecutor.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.squalor.consecutor.FormError

/** English copy for form errors. S18 moves these to string resources. */
internal fun formErrorText(error: FormError, limit: Int? = null): String = when (error) {
    FormError.REQUIRED -> "Required."
    FormError.TOO_LONG -> if (limit != null) "At most $limit characters." else "Too long."
    FormError.NOT_A_NUMBER -> "Enter a number."
    FormError.MUST_BE_POSITIVE -> "Must be greater than zero."
    FormError.OUT_OF_RANGE -> "Enter a whole number from 1 to 7."
    FormError.FUTURE_DATE -> "Date can't be in the future."
    FormError.INVALID_DATE -> "Use the format YYYY-MM-DD"
    FormError.ONE_CHARACTER_ONLY -> "Use a single emoji or character."
    FormError.ALREADY_LOGGED -> "Already logged on this date."
}

internal fun errorSupportingText(error: FormError?, limit: Int? = null): (@Composable () -> Unit)? =
    error?.let { { Text(formErrorText(it, limit)) } }
