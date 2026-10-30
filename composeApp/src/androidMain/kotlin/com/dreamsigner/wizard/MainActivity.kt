package com.dreamsigner.wizard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dreamsigner.wizard.storage.initializeAndroidStorage
import com.dreamsigner.wizard.media.initializeAndroidAudio
import com.dreamsigner.wizard.media.initializeAndroidRecording
import com.dreamsigner.wizard.media.handleAndroidRecordingPermissionResult

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        handleAndroidRecordingPermissionResult(requestCode, grantResults)
    }
}
