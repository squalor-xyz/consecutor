package com.squalor.consecutor

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/** A backup file that cannot be imported; [message] is shown to the user. */
class BackupFormatException(message: String) : IllegalArgumentException(message)

object BackupCodec {
    private const val VERSION = 1

    fun encode(bundles: List<TrackerBundle>, exportedAtEpochMs: Long? = null): String {
        val root = JSONObject()
        root.put("version", VERSION)
        exportedAtEpochMs?.let { root.put("exportedAtEpochMs", it) }
        root.put("trackers", JSONArray().apply {
            bundles.forEach { bundle ->
                put(JSONObject().apply {
                    put("tracker", JSONObject().apply {
                        put("id", bundle.tracker.id)
                        put("name", bundle.tracker.name)
                        put("emoji", bundle.tracker.emoji)
                        put("description", bundle.tracker.description)
                        put("type", bundle.tracker.type.name)
                        put("unit", bundle.tracker.unit)
                        put("colorHex", bundle.tracker.colorHex)
                        put("isArchived", bundle.tracker.isArchived)
                        put("createdAtEpochMs", bundle.tracker.createdAtEpochMs)
                        put("updatedAtEpochMs", bundle.tracker.updatedAtEpochMs)
                    })
                    put("target", bundle.target.firstOrNull()?.let { target ->
                        JSONObject().apply {
                            put("period", target.period.name)
                            put("targetValue", target.targetValue)
                        }
                    })
                    put("reminder", bundle.reminder.firstOrNull()?.let { reminder ->
                        JSONObject().apply {
                            put("enabled", reminder.enabled)
                            put("hourOfDay", reminder.hourOfDay)
                            put("minuteOfHour", reminder.minuteOfHour)
                            put("daysOfWeekCsv", reminder.daysOfWeekCsv)
                        }
                    })
                    put("entries", JSONArray().apply {
                        bundle.entries.forEach { entry ->
                            put(JSONObject().apply {
                                put("effectiveDate", entry.effectiveDate)
                                put("occurredAtEpochMs", entry.occurredAtEpochMs)
                                put("value", entry.value)
                                put("note", entry.note)
                                put("createdAtEpochMs", entry.createdAtEpochMs)
                                put("updatedAtEpochMs", entry.updatedAtEpochMs)
                                put("isDeleted", entry.isDeleted)
                            })
                        }
                    })
                })
            }
        })
        return root.toString(2)
    }

    fun decode(raw: String): ImportPayload {
        val root = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw BackupFormatException("Not a Consecutor backup file.")
        }
        return try {
            decodeRoot(root)
        } catch (e: JSONException) {
            throw BackupFormatException("Backup is missing required data: ${e.message}")
        }
    }

    private fun decodeRoot(root: JSONObject): ImportPayload {
        if (root.optInt("version", -1) != VERSION) {
            throw BackupFormatException("Unsupported backup version.")
        }

        val trackers = mutableListOf<ImportedTracker>()
        val items = root.getJSONArray("trackers")
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            val trackerJson = item.getJSONObject("tracker")
            val name = trackerJson.getString("name").trim()
            ensure(name.isNotEmpty()) { "Tracker ${index + 1} has a blank name." }
            val context = "Tracker '$name'"
            val trackerType = enumValue<TrackerType>(trackerJson.getString("type"), "$context: unknown tracker type")
            val targetJson = item.optJSONObject("target")
            val reminderJson = item.optJSONObject("reminder")
            val entriesJson = item.getJSONArray("entries")

            val entries = mutableListOf<ImportedEntry>()
            for (entryIndex in 0 until entriesJson.length()) {
                val entryJson = entriesJson.getJSONObject(entryIndex)
                val effectiveDate = entryJson.getString("effectiveDate")
                ensure(runCatching { LocalDate.parse(effectiveDate) }.isSuccess) {
                    "$context, entry ${entryIndex + 1}: invalid date '$effectiveDate'."
                }
                val entryValue = if (entryJson.isNull("value")) null else entryJson.getDouble("value")
                ensure(entryValue == null || NumberRules.isValidValue(entryValue)) {
                    "$context, entry ${entryIndex + 1}: value out of range."
                }
                entries += ImportedEntry(
                    effectiveDate = effectiveDate,
                    occurredAtEpochMs = entryJson.getLong("occurredAtEpochMs"),
                    value = entryValue,
                    note = entryJson.nullableString("note"),
                    createdAtEpochMs = entryJson.getLong("createdAtEpochMs"),
                    updatedAtEpochMs = entryJson.getLong("updatedAtEpochMs"),
                    isDeleted = entryJson.optBoolean("isDeleted", false)
                )
            }

            trackers += ImportedTracker(
                tracker = TrackerEntity(
                    name = name,
                    emoji = trackerJson.nullableString("emoji"),
                    description = trackerJson.nullableString("description"),
                    type = trackerType,
                    unit = trackerJson.nullableString("unit"),
                    colorHex = trackerJson.nullableString("colorHex") ?: "#1F6FEB",
                    isArchived = trackerJson.optBoolean("isArchived", false),
                    createdAtEpochMs = trackerJson.getLong("createdAtEpochMs"),
                    updatedAtEpochMs = trackerJson.getLong("updatedAtEpochMs")
                ),
                target = targetJson?.let {
                    val targetValue = it.getDouble("targetValue")
                    ensure(targetValue > 0.0) { "$context: target must be greater than zero." }
                    ensure(NumberRules.isValidValue(targetValue)) { "$context: target value out of range." }
                    val period = enumValue<TargetPeriod>(it.getString("period"), "$context: unknown target period")
                    ensure(trackerType != TrackerType.YES_NO || isValidYesNoTarget(period, targetValue)) {
                        "$context: a yes/no target must be 1 per day or 1 to 7 per week."
                    }
                    ImportedTarget(period = period, targetValue = targetValue)
                },
                reminder = reminderJson?.let {
                    val hourOfDay = it.getInt("hourOfDay")
                    val minuteOfHour = it.getInt("minuteOfHour")
                    ensure(hourOfDay in 0..23) { "$context: reminder hour $hourOfDay is out of range." }
                    ensure(minuteOfHour in 0..59) { "$context: reminder minute $minuteOfHour is out of range." }
                    val daysOfWeekCsv = it.nullableString("daysOfWeekCsv")?.ifBlank { null }
                    daysOfWeekCsv?.split(",")?.forEach { token ->
                        ensure(token.trim().toIntOrNull() in 1..7) { "$context: invalid reminder weekday '$token'." }
                    }
                    ImportedReminder(
                        enabled = it.getBoolean("enabled"),
                        hourOfDay = hourOfDay,
                        minuteOfHour = minuteOfHour,
                        daysOfWeekCsv = daysOfWeekCsv
                    )
                },
                entries = entries
            )
        }
        return ImportPayload(
            trackers,
            if (root.isNull("exportedAtEpochMs")) null else root.getLong("exportedAtEpochMs")
        )
    }

    // Reads absent or JSON-null keys as null. Avoids optString, whose null handling differs
    // between Android's org.json and other implementations.
    private fun JSONObject.nullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    private inline fun <reified T : Enum<T>> enumValue(name: String, message: String): T =
        enumValues<T>().firstOrNull { it.name == name } ?: throw BackupFormatException("$message '$name'.")

    private inline fun ensure(condition: Boolean, message: () -> String) {
        if (!condition) throw BackupFormatException(message())
    }

    data class ImportPayload(val trackers: List<ImportedTracker>, val exportedAtEpochMs: Long? = null)
    data class ImportedTracker(
        val tracker: TrackerEntity,
        val target: ImportedTarget?,
        val reminder: ImportedReminder?,
        val entries: List<ImportedEntry>
    )
    data class ImportedTarget(val period: TargetPeriod, val targetValue: Double)
    data class ImportedReminder(
        val enabled: Boolean,
        val hourOfDay: Int,
        val minuteOfHour: Int,
        val daysOfWeekCsv: String?
    )
    data class ImportedEntry(
        val effectiveDate: String,
        val occurredAtEpochMs: Long,
        val value: Double?,
        val note: String?,
        val createdAtEpochMs: Long,
        val updatedAtEpochMs: Long,
        val isDeleted: Boolean
    )
}
