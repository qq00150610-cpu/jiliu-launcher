package com.jiliu.launcher.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiliu.launcher.App
import com.jiliu.launcher.service.MusicPlaybackService

class BluetoothAutoPlayReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                // Auto play music when Bluetooth device connects
                if (App.instance.preferencesManager.bluetoothAutoPlayEnabled) {
                    startMusicService(context)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // Optionally pause when Bluetooth disconnects
            }
        }
    }

    private fun startMusicService(context: Context) {
        try {
            val serviceIntent = Intent(context, MusicPlaybackService::class.java)
            context.startService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
