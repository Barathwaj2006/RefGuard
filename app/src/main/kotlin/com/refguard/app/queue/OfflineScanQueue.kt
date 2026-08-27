package com.refguard.app.queue

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.refguard.platform.models.ScanRequest
import com.refguard.platform.models.ContentType

/**
 * Offline queue backed by SharedPreferences (or in-memory when Context is null for tests).
 * Stores ScanRequests that could not be submitted due to no network.
 * No sensitive credential data is ever stored here.
 */
open class OfflineScanQueue(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(
        "refguard_offline_queue", Context.MODE_PRIVATE
    )
    private val memoryEntries = mutableListOf<QueueEntry>()
    private val gson = Gson()

    data class QueueEntry(
        val contentType: String,
        val contentValue: String,
        val sourceContext: String,
        val timestamp: String,
        val queuedAt: Long = System.currentTimeMillis()
    )

    open fun enqueue(request: ScanRequest) {
        val entry = QueueEntry(
            contentType = request.contentType.name,
            contentValue = request.contentValue,
            sourceContext = request.sourceContext,
            timestamp = request.timestamp
        )
        if (prefs != null) {
            val key = "entry_${entry.queuedAt}_${(Math.random() * 1000).toInt()}"
            prefs.edit().putString(key, gson.toJson(entry)).apply()
        } else {
            memoryEntries.add(entry)
        }
    }

    open fun dequeueAll(): List<ScanRequest> {
        val all = mutableListOf<ScanRequest>()
        if (prefs != null) {
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
        } else {
            for (entry in memoryEntries) {
                try {
                    val contentType = ContentType.valueOf(entry.contentType)
                    all.add(
                        ScanRequest(
                            contentType = contentType,
                            contentValue = entry.contentValue,
                            sourceContext = entry.sourceContext,
                            timestamp = entry.timestamp
                        )
                    )
                } catch (_: Exception) {}
            }
            memoryEntries.clear()
        }
        return all
    }

    open fun size(): Int = prefs?.all?.size ?: memoryEntries.size

    open fun clear() {
        if (prefs != null) {
            prefs.edit().clear().apply()
        } else {
            memoryEntries.clear()
        }
    }
}
