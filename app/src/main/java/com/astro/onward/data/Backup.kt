package com.astro.onward.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Full-state JSON backup. Export writes to Downloads via MediaStore; import
 * replaces table contents wholesale (a restore, not a merge).
 */
object Backup {

    private const val FORMAT_VERSION = 1

    suspend fun export(context: Context, db: OnwardDatabase): String = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("format", FORMAT_VERSION)
            put("appVersion", com.astro.onward.BuildConfig.VERSION_NAME)
            put("exportedAt", System.currentTimeMillis())

            put("dayEntries", JSONArray().apply {
                db.dayEntryDao().observeAll().first().forEach { e ->
                    put(JSONObject().apply {
                        put("epochDay", e.epochDay)
                        put("status", e.status.name)
                        put("note", e.note ?: JSONObject.NULL)
                    })
                }
            })
            put("calorieEntries", JSONArray().apply {
                db.calorieDao().observeFrom(0).first().forEach { e ->
                    put(JSONObject().apply {
                        put("epochDay", e.epochDay)
                        put("label", e.label)
                        put("kcal", e.kcal)
                        put("timestamp", e.timestamp)
                    })
                }
            })
            put("planDays", JSONArray().apply {
                db.planDao().observeDays().first().forEach { d ->
                    put(JSONObject().apply {
                        put("dayIndex", d.dayIndex)
                        put("breakfast", d.breakfast)
                        put("lunchTemplate", d.lunchTemplate)
                        put("lunchExample", d.lunchExample)
                        put("dinnerTemplate", d.dinnerTemplate)
                        put("dinnerExample", d.dinnerExample)
                    })
                }
            })
            put("mealOptions", JSONArray().apply {
                db.planDao().observeOptions().first().forEach { o ->
                    put(JSONObject().apply {
                        put("slot", o.slot.name)
                        put("example", o.example)
                        put("custom", o.custom)
                    })
                }
            })
            put("shoppingItems", JSONArray().apply {
                db.shoppingDao().observeAll().first().forEach { s ->
                    put(JSONObject().apply {
                        put("name", s.name)
                        put("checked", s.checked)
                        put("custom", s.custom)
                        put("position", s.position)
                    })
                }
            })
            put("reminders", JSONArray().apply {
                db.reminderDao().getAll().forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("label", r.label)
                        put("message", r.message)
                        put("enabled", r.enabled)
                        put("hour", r.hour)
                        put("minute", r.minute)
                        put("daysMask", r.daysMask)
                        put("destination", r.destination ?: JSONObject.NULL)
                    })
                }
            })
            (db.settingsDao().get() ?: AppSettings()).let { s ->
                put("settings", JSONObject().apply {
                    put("started", s.started)
                    put("calorieTrackerEnabled", s.calorieTrackerEnabled)
                })
            }
        }

        val name = "onward-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Couldn't create the backup file")
        resolver.openOutputStream(uri)!!.use { it.write(json.toString(2).toByteArray()) }
        name
    }

    /** Returns a short human summary of what was restored. */
    suspend fun import(context: Context, db: OnwardDatabase, uri: Uri): String =
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)!!
                .bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            require(json.optInt("format", -1) == FORMAT_VERSION) { "Not an Onward backup" }

            val days = json.getJSONArray("dayEntries").let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DayEntry(
                        epochDay = o.getLong("epochDay"),
                        status = DayStatus.valueOf(o.getString("status")),
                        note = if (o.isNull("note")) null else o.getString("note"),
                    )
                }
            }
            db.dayEntryDao().upsertAll(days)

            json.optJSONArray("calorieEntries")?.let { arr ->
                db.calorieDao().clearAll()
                db.calorieDao().insertAll(
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        CalorieEntry(
                            epochDay = o.getLong("epochDay"),
                            label = o.getString("label"),
                            kcal = o.getInt("kcal"),
                            timestamp = o.getLong("timestamp"),
                        )
                    },
                )
            }

            json.optJSONArray("planDays")?.let { arr ->
                db.planDao().replaceDays(
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        PlanDay(
                            dayIndex = o.getInt("dayIndex"),
                            breakfast = o.getString("breakfast"),
                            lunchTemplate = o.getString("lunchTemplate"),
                            lunchExample = o.getString("lunchExample"),
                            dinnerTemplate = o.getString("dinnerTemplate"),
                            dinnerExample = o.getString("dinnerExample"),
                        )
                    },
                )
            }

            json.optJSONArray("shoppingItems")?.let { arr ->
                db.shoppingDao().clearAll()
                db.shoppingDao().insertAll(
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        ShoppingItem(
                            name = o.getString("name"),
                            checked = o.getBoolean("checked"),
                            custom = o.getBoolean("custom"),
                            position = o.getInt("position"),
                        )
                    },
                )
            }

            json.optJSONArray("reminders")?.let { arr ->
                db.reminderDao().replaceAll(
                    (0 until arr.length()).map { i ->
                        val o = arr.getJSONObject(i)
                        ReminderSetting(
                            id = o.getInt("id"),
                            label = o.getString("label"),
                            message = o.getString("message"),
                            enabled = o.getBoolean("enabled"),
                            hour = o.getInt("hour"),
                            minute = o.getInt("minute"),
                            daysMask = o.getInt("daysMask"),
                            destination = if (o.isNull("destination")) null else o.getString("destination"),
                        )
                    },
                )
            }

            json.optJSONObject("settings")?.let { s ->
                val current = db.settingsDao().get() ?: AppSettings()
                db.settingsDao().upsert(
                    current.copy(
                        started = s.optBoolean("started", current.started),
                        calorieTrackerEnabled = s.optBoolean("calorieTrackerEnabled", current.calorieTrackerEnabled),
                    ),
                )
            }

            "Restored ${days.size} days of history"
        }
}
