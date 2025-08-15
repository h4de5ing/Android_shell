package com.android.shell2.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import com.android.shell2.utils.log
import com.android.shell2.utils.runCommand
import com.github.h4de5ing.base.exec

@SuppressLint("UnspecifiedRegisterReceiverFlag")
class ShellService : Service() {
    override fun onCreate() {
        super.onCreate()
        runCommand()
        val filter = IntentFilter("com.android.shell2.exec")
        if (Build.VERSION.SDK_INT >= 33)
            registerReceiver(receiver, filter, RECEIVER_VISIBLE_TO_INSTANT_APPS)
        else registerReceiver(receiver, filter)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                val com = intent?.getStringExtra("com")
                com?.apply { exec(com).log(com) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}