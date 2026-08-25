package com.refguard.app.history

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.refguard.app.domain.ScanResult

data class HistoryItem(
    val scanId: String,
    val timestamp: String,
    val title: String,
    val subtitle: String,
    val riskLevelName: String,
    val riskScore: Int,
    val scanResultJson: String
)

class InvestigationHistoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("refguard_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "saved_investigations"

    fun saveInvestigation(result: ScanResult) {
        val current = getHistory().toMutableList()
        // Remove existing if duplicate
        current.removeAll { it.scanId == result.scanId }

        val title = if (result.recipientVpa != null) {
            "UPI: "
        } else if (result.mismatchStatus?.name == "DETECTED") {
            "Payment Intent Mismatch"
        } else {
            result.detectedSummary
        }

        val subtitle = result.userInstruction.take(60)

        val item = HistoryItem(
            scanId = result.scanId,
            timestamp = result.timestamp,
            title = title,
            subtitle = subtitle,
            riskLevelName = result.riskLevel.name,
            riskScore = result.riskScore,
            scanResultJson = gson.toJson(result)
        )

        current.add(0, item) // Prepend newest
        val trimmed = current.take(30) // Keep last 30
        prefs.edit().putString(key, gson.toJson(trimmed)).apply()
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getResult(scanId: String): ScanResult? {
        val item = getHistory().find { it.scanId == scanId } ?: return null
        return try {
            gson.fromJson(item.scanResultJson, ScanResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearHistory() {
        prefs.edit().remove(key).apply()
    }
}
