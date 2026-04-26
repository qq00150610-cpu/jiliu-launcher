package com.jiliu.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jiliu.launcher.App
import com.jiliu.launcher.service.EdgeGestureService
import com.jiliu.launcher.service.FloatingDockService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
            
            // Launch main activity
            val launchIntent = Intent(context, com.jiliu.launcher.ui.main.MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            
            // Start services based on preferences
            startServices(context)
        }
    }

    private fun startServices(context: Context) {
        val prefs = App.instance.preferencesManager

        // Start floating dock if enabled
        if (prefs.dockEnabled) {
            try {
                context.startService(Intent(context, FloatingDockService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Start edge gesture if enabled
        if (prefs.edgeGestureEnabled) {
            try {
                context.startService(Intent(context, EdgeGestureService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
