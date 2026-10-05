package com.squalor.consecutor.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.squalor.consecutor.FormError
import com.squalor.consecutor.R

@Composable
internal fun formErrorText(error: FormError, limit: Int? = null): String = when (error) {
    FormError.REQUIRED -> stringResource(R.string.error_required)
    FormError.TOO_LONG ->
        if (limit != null) pluralStringResource(R.plurals.error_too_long_limit, limit, limit)
        else stringResource(R.string.error_too_long)
    FormError.NOT_A_NUMBER -> stringResource(R.string.error_not_a_number)
    FormError.MUST_BE_POSITIVE -> stringResource(R.string.error_must_be_positive)
    FormError.OUT_OF_RANGE -> stringResource(R.string.error_out_of_range)
    FormError.FUTURE_DATE -> stringResource(R.string.error_future_date)
    FormError.INVALID_DATE -> stringResource(R.string.error_invalid_date)
    FormError.ONE_CHARACTER_ONLY -> stringResource(R.string.error_one_character_only)
    FormError.ALREADY_LOGGED -> stringResource(R.string.error_already_logged)
}

internal fun errorSupportingText(error: FormError?, limit: Int? = null): (@Composable () -> Unit)? =
    error?.let { { Text(formErrorText(it, limit)) } }
