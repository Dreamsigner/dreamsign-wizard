package com.duolingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.duolingo.app.storage.initializeAndroidStorage
import com.duolingo.app.media.initializeAndroidAudio
import com.duolingo.app.media.initializeAndroidRecording
import com.duolingo.app.media.handleAndroidRecordingPermissionResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeAndroidStorage(applicationContext)
        initializeAndroidAudio(applicationContext)
        initializeAndroidRecording(this)
        setContent {
            App()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        handleAndroidRecordingPermissionResult(requestCode, grantResults)
    }
}
