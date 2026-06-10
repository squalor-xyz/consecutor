package com.squalor.consecutor

import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    private const val VERSION = 1

    fun encode(bundles: List<TrackerBundle>): String {
        val root = JSONObject()
        root.put("version", VERSION)
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
        val root = JSONObject(raw)
        require(root.getInt("version") == VERSION) { "Unsupported backup version." }

        val trackers = mutableListOf<ImportedTracker>()
        val items = root.getJSONArray("trackers")
        for (index in 0 until items.length()) {
            val item = items.getJSONObject(index)
            val trackerJson = item.getJSONObject("tracker")
            val targetJson = item.optJSONObject("target")
            val reminderJson = item.optJSONObject("reminder")
            val entriesJson = item.getJSONArray("entries")

            val entries = mutableListOf<ImportedEntry>()
            for (entryIndex in 0 until entriesJson.length()) {
                val entryJson = entriesJson.getJSONObject(entryIndex)
                entries += ImportedEntry(
                    effectiveDate = entryJson.getString("effectiveDate"),
                    occurredAtEpochMs = entryJson.getLong("occurredAtEpochMs"),
                    value = if (entryJson.isNull("value")) null else entryJson.getDouble("value"),
                    note = if (entryJson.isNull("note")) null else entryJson.getString("note"),
                    createdAtEpochMs = entryJson.getLong("createdAtEpochMs"),
                    updatedAtEpochMs = entryJson.getLong("updatedAtEpochMs"),
                    isDeleted = entryJson.optBoolean("isDeleted", false)
                )
            }

            trackers += ImportedTracker(
                tracker = TrackerEntity(
                    name = trackerJson.getString("name"),
                    emoji = trackerJson.optString("emoji").ifBlank { null },
                    description = trackerJson.optString("description").ifBlank { null },
                    type = TrackerType.valueOf(trackerJson.getString("type")),
                    unit = trackerJson.optString("unit").ifBlank { null },
                    colorHex = trackerJson.optString("colorHex", "#1F6FEB"),
                    isArchived = trackerJson.optBoolean("isArchived", false),
                    createdAtEpochMs = trackerJson.getLong("createdAtEpochMs"),
                    updatedAtEpochMs = trackerJson.getLong("updatedAtEpochMs")
                ),
                target = targetJson?.let {
                    ImportedTarget(
                        period = TargetPeriod.valueOf(it.getString("period")),
                        targetValue = it.getDouble("targetValue")
                    )
                },
                reminder = reminderJson?.let {
                    ImportedReminder(
                        enabled = it.getBoolean("enabled"),
                        hourOfDay = it.getInt("hourOfDay"),
                        minuteOfHour = it.getInt("minuteOfHour"),
                        daysOfWeekCsv = it.optString("daysOfWeekCsv").ifBlank { null }
                    )
                },
                entries = entries
            )
        }
        return ImportPayload(trackers)
    }

    data class ImportPayload(val trackers: List<ImportedTracker>)
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
