package com.batallagroup.myco.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun Long.toFormattedTime(): String {
    val date = Date(this)
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000 -> "ahora"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
    }
}

fun Long.toFullDateTime(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(this))

fun String.truncate(maxLength: Int = 30): String =
    if (length > maxLength) take(maxLength - 1) + "…" else this
