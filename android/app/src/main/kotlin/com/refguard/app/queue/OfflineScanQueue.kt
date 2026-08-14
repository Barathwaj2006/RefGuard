package com.refguard.app.queue

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.refguard.platform.models.ScanRequest
import com.refguard.platform.models.ContentType

/**
 * Offline queue backed by SharedPreferences.
 * Stores ScanRequests that could not be submitted due to no network.
 * No sensitive credential data is ever stored here.
 */
class OfflineScanQueue(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "refguard_offline_queue", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    data class QueueEntry(
        val contentType: String,
        val contentValue: String,
        val sourceContext: String,
        val timestamp: String,
        val queuedAt: Long = System.currentTimeMillis()
    )

    fun enqueue(request: ScanRequest) {
        val entry = QueueEntry(
            contentType = request.contentType.name,
            contentValue = request.contentValue,
            sourceContext = request.sourceContext,
            timestamp = request.timestamp
        )
        val key = "entry_${entry.queuedAt}_${(Math.random() * 1000).toInt()}"
        prefs.edit().putString(key, gson.toJson(entry)).apply()
    }

    fun dequeueAll(): List<ScanRequest> {
        val all = mutableListOf<ScanRequest>()
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            try {
                val entry = gson.fromJson(value as String, QueueEntry::class.java)
                val contentType = ContentType.valueOf(entry.contentType)
                all.add(
                    ScanRequest(
                        contentType = contentType,
                        contentValue = entry.contentValue,
                        sourceContext = entry.sourceContext,
                        timestamp = entry.timestamp
                    )
                )
                prefs.edit().remove(key).apply()
            } catch (e: Exception) {
                prefs.edit().remove(key).apply()
            }
        }
        return all
    }

    fun size(): Int = prefs.all.size

    fun clear() {
        prefs.edit().clear().apply()
    }
}
