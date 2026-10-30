package com.dreamsigner.wizard.storage

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private var storageContext: Context? = null

fun initializeAndroidStorage(context: Context) { storageContext = context.applicationContext }

// https://developer.android.com/training/data-storage/shared-preferences
actual fun createProgressStore(): ProgressStore = object : ProgressStore {
    private fun preferences() = checkNotNull(storageContext) { "Storage context not initialized" }
        .getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
    override fun read(): String? = preferences().getString("progress", null)
    override fun write(value: String) {
        check(preferences().edit().putString("progress", value).commit()) { "Progress could not be saved" }
    }
}

actual fun todayLocalDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
