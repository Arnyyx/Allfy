package com.arny.allfy.utils

import com.google.firebase.Timestamp

fun Timestamp.toTimeAgo(): String {
    val currentTime = System.currentTimeMillis()
    val commentTime = this.toDate().time
    val difference = currentTime - commentTime

    return when {
        difference < 1000L * 60 -> "Just now"
        difference < 1000L * 60 * 60 -> "${difference / (1000 * 60)} minutes ago"
        difference < 1000L * 60 * 60 * 24 -> "${difference / (1000 * 60 * 60)} hours ago"
        difference < 1000L * 60 * 60 * 24 * 7 -> "${difference / (1000 * 60 * 60 * 24)} days ago"
        difference < 1000L * 60 * 60 * 24 * 30 -> "${difference / (1000 * 60 * 60 * 24 * 7)} weeks ago"
        difference < 1000L * 60 * 60 * 24 * 365 -> "${difference / (1000 * 60 * 60 * 24 * 30)} months ago"
        else -> "${difference / (1000L * 60 * 60 * 24 * 365)} years ago"
    }
}