package com.bartaxyz.lensdroid.DateUtils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object DateUtils {
    fun getRelativeTime(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val elapsedTimeMillis = abs(currentTime - timestamp)
        val elapsedTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)

        return when {
            elapsedTimeMinutes < 60 -> "$elapsedTimeMinutes min"
            elapsedTimeMinutes < 1440 -> "${elapsedTimeMinutes / 60}h"
            elapsedTimeMinutes < 43200 -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun parseTimestamp(timestamp: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        val date = format.parse(timestamp)
        return date?.time ?: 0L
    }

    fun getRelativeTime(timestamp: String): String {
        return getRelativeTime(parseTimestamp(timestamp))
    }
}
