package com.waforward.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * WA Forward — NotificationListenerService.
 *
 * Listens for notifications posted by WhatsApp (com.whatsapp) and the
 * default SMS app on the user's OWN phone. For each notification it:
 *   1. Extracts sender + text from the notification the user already receives.
 *   2. POSTs {sender, text, to} as JSON to the user's configured gateway URL
 *      (with ?key=API_KEY if an API key is set).
 *   3. Optionally launches the WhatsApp app.
 *
 * This only forwards notifications the user already receives on their own
 * phone — it does not read WhatsApp messages directly, does not pair,
 * and does not extract any WhatsApp session or credential.
 */
class ForwardService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        startForegroundNotification()
        startOrderPolling()
    }

    /** Cek apakah package adalah aplikasi WhatsApp (untuk mode "semua") */
    private fun isWhatsAppPackage(pkg: String): Boolean {
        if (pkg == PACKAGE_WHATSAPP || pkg == PACKAGE_WHATSAPP_BUSINESS) return true
        // Clone apps biasanya mengandung "whatsapp" di nama package
        if (pkg.contains("whatsapp", ignoreCase = true)) return true
        // Package clone custom yang diisi user
        val clonePkg = prefs.waClonePackage.trim()
        if (clonePkg.isNotEmpty() && pkg == clonePkg) return true
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Paket WA yang dipilih user (biasa / bisnis / clone / semua)
        val clonePkg = prefs.waClonePackage.trim()
        val selectedPkg = when (prefs.waChoice) {
            "bisnis" -> PACKAGE_WHATSAPP_BUSINESS
            "clone" -> clonePkg.ifEmpty { null }
            "semua" -> null // null = semua WA
            else -> PACKAGE_WHATSAPP
        }
        val isWa = when (prefs.waChoice) {
            "semua" -> isWhatsAppPackage(pkg)
            "clone" -> pkg == clonePkg && clonePkg.isNotEmpty()
            else -> pkg == selectedPkg
        }
        val isSms = SMS_PACKAGES.contains(pkg)
        if (isWa && !prefs.forwardWa) return
        if (isSms && !prefs.forwardSms) return
        if (!isWa && !isSms) return

        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val body = if (bigText.isNotEmpty()) bigText else text
        if (body.isBlank()) return

        // Skip our own foreground notification
        if (sbn.id == FOREGROUND_ID) return

        Log.d(TAG, "Forward [$pkg] sender=$title text=${body.take(80)}")
        forwardToGateway(title, body)

        if (prefs.openWa && isWa) {
            openWhatsApp()
        }
    }

    private fun forwardToGateway(sender: String, text: String) {
        val baseUrl = prefs.gatewayUrl.trim()
        if (baseUrl.isEmpty()) return
        scope.launch {
            try {
                val urlStr = if (prefs.apiKey.isNotEmpty()) {
                    val sep = if (baseUrl.contains("?")) "&" else "?"
                    "$baseUrl${sep}key=${prefs.apiKey}"
                } else baseUrl
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    doOutput = true
                }
                val payload = JSONObject().apply {
                    put("sender", sender)
                    put("text", text)
                    put("to", prefs.toNumber)
                }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                val code = conn.responseCode
                Log.d(TAG, "Gateway response: $code")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Forward gagal: ${e.message}")
            }
        }
    }

    private fun openWhatsApp() {
        try {
            val pm = packageManager
            val targetPkg = when (prefs.waChoice) {
                "bisnis" -> PACKAGE_WHATSAPP_BUSINESS
                "clone" -> prefs.waClonePackage.trim().ifEmpty { null }
                "semua" -> PACKAGE_WHATSAPP // default ke WA biasa untuk buka app
                else -> PACKAGE_WHATSAPP
            } ?: return
            val intent = pm.getLaunchIntentForPackage(targetPkg) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Buka WhatsApp gagal: ${e.message}")
        }
    }

    /** Buka chat WA dengan nomor tertentu via wa.me */
    private fun openWhatsAppChat(number: String) {
        try {
            val clean = number.filter { it.isDigit() }
            if (clean.isEmpty()) return
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://wa.me/$clean")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            // Coba buka dengan package WA yang dipilih
            val targetPkg = when (prefs.waChoice) {
                "bisnis" -> PACKAGE_WHATSAPP_BUSINESS
                "clone" -> prefs.waClonePackage.trim().ifEmpty { null }
                else -> PACKAGE_WHATSAPP
            }
            if (targetPkg != null) intent.setPackage(targetPkg)
            startActivity(intent)
            Log.d(TAG, "Buka chat WA: $clean")
        } catch (e: Exception) {
            Log.e(TAG, "Buka chat WA gagal: ${e.message}")
        }
    }

    /** Polling gateway untuk perintah buka WA dari order bot Telegram */
    private fun startOrderPolling() {
        scope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(prefs.pollIntervalSec * 1000L)
                    if (!prefs.autoOpenOrder) continue
                    val to = prefs.toNumber.trim()
                    if (to.isEmpty()) continue
                    // Derive base URL dari gatewayUrl (hapus path /otp/ingest)
                    var base = prefs.gatewayUrl.trim()
                    if (base.isEmpty()) continue
                    // Ambil origin saja, mis. http://72.60.40.224:3000
                    val urlObj = URL(base)
                    val origin = "${urlObj.protocol}://${urlObj.host}" +
                        (if (urlObj.port != -1) ":${urlObj.port}" else "")
                    var pollUrl = "$origin/wa-open?to=$to"
                    if (prefs.apiKey.isNotEmpty()) pollUrl += "&key=${prefs.apiKey}"
                    val conn = (URL(pollUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 10_000
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        conn.disconnect()
                        val json = JSONObject(body)
                        val numbers = json.optJSONArray("open")
                        if (numbers != null) {
                            for (i in 0 until numbers.length()) {
                                val num = numbers.optString(i, "")
                                if (num.isNotBlank()) openWhatsAppChat(num)
                            }
                        }
                    } else {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Polling wa-open gagal: ${e.message}")
                }
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WA Forward",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "WA Forward berjalan" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WA Forward aktif")
            .setContentText("Meneruskan notifikasi WhatsApp / SMS ke gateway")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        startForeground(FOREGROUND_ID, notif)
    }

    companion object {
        const val TAG = "WAForward"
        const val ACTION_START_FOREGROUND = "com.waforward.app.START_FOREGROUND"
        const val CHANNEL_ID = "wa_forward_channel"
        const val FOREGROUND_ID = 1001

        const val PACKAGE_WHATSAPP = "com.whatsapp"
        const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

        /** Common default SMS app packages. Add others as needed. */
        val SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.vivo.mms",
            "com.oppo.mms",
            "com.xiaomi.mms",
            "com.miui.mms"
        )

        /** Check whether this listener is enabled in system settings. */
        fun isListenerEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val component = ComponentName(context, ForwardService::class.java)
            return enabled.split(":").any { TextUtils.equals(it, component.flattenToString()) }
        }
    }
}
