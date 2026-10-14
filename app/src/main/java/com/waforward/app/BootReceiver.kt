package com.waforward.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-start the forward service after device reboot.
 * The NotificationListenerService itself is re-bound by the system,
 * but this keeps the foreground notification alive.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startService(
                Intent(context, ForwardService::class.java).apply {
                    action = ForwardService.ACTION_START_FOREGROUND
                }
            )
        }
    }
}
