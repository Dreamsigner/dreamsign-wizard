package com.dreamsigner.wizard.media

import android.app.Activity
import android.app.Application
import android.os.Bundle

actual fun bindAudioLifecycle(player: PlatformAudioPlayer, onBackground: () -> Unit): () -> Unit {
    val application = androidAudioApplication()
    var bound = true
    val listener = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity) {
            // Lifecycle callbacks run on main. Never retain the Activity argument.
            if (bound) {
                player.stop()
                onBackground()
            }
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
    application.registerActivityLifecycleCallbacks(listener)
    return {
        if (bound) {
            bound = false
            application.unregisterActivityLifecycleCallbacks(listener)
        }
    }
}
