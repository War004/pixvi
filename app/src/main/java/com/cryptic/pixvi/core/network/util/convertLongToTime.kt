package com.cryptic.pixvi.core.network.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun convertLongToTime(time: Long): String {
    val date = Instant.ofEpochMilli(time)
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(date)
}