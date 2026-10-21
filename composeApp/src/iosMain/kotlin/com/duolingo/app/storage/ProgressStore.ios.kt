package com.duolingo.app.storage

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults

// https://developer.apple.com/documentation/foundation/userdefaults
actual fun createProgressStore(): ProgressStore = object : ProgressStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun read(): String? = defaults.stringForKey("learning_progress_v1")
    override fun write(value: String) { defaults.setObject(value, forKey = "learning_progress_v1") }
}

actual fun todayLocalDate(): String = NSDateFormatter().apply {
    locale = NSLocale(localeIdentifier = "en_US_POSIX")
    dateFormat = "yyyy-MM-dd"
}.stringFromDate(NSDate())
