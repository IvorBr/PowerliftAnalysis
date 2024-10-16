package com.example.powerlift_analysis

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.powerlift_analysis/pose"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val messenger = flutterEngine?.dartExecutor?.binaryMessenger
        if (messenger != null) {
            MethodChannel(messenger, CHANNEL).setMethodCallHandler { call, result ->
                if (call.method == "startPoseDetection") {
                    startPoseDetection()
                    result.success("Pose detection started")
                } else {
                    result.notImplemented()
                }
            }
        } else {
            println("Flutter engine is not available.")
        }
    }

    private fun startPoseDetection() {
    }
}
