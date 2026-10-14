package com.waforward.app

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences wrapper for user-entered settings.
 * Mendukung multi-sesi: tiap sesi (Sesi 1, 2, 3...) punya nomor & pengaturan sendiri.
 * Gateway URL & API Key bersifat global (sama untuk semua sesi).
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("wa_forward_prefs", Context.MODE_PRIVATE)

    /** Sesi aktif (1-10) */
    var activeSession: Int
        get() = sp.getInt(KEY_ACTIVE_SESSION, 1).coerceIn(1, MAX_SESSIONS)
        set(v) = sp.edit().putInt(KEY_ACTIVE_SESSION, v.coerceIn(1, MAX_SESSIONS)).apply()

    // ===== Global (sama untuk semua sesi) =====

    var gatewayUrl: String
        get() = sp.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(v) = sp.edit().putString(KEY_URL, v).apply()

    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "") ?: ""
        set(v) = sp.edit().putString(KEY_API_KEY, v).apply()

    /** Interval polling gateway untuk perintah buka WA (detik) */
    var pollIntervalSec: Int
        get() = sp.getInt(KEY_POLL_INTERVAL, 30)
        set(v) = sp.edit().putInt(KEY_POLL_INTERVAL, v).apply()

    // ===== Per-sesi (s = nomor sesi aktif) =====

    private fun sk(key: String) = "${key}_s$activeSession"

    var toNumber: String
        get() = sp.getString(sk(KEY_TO), if (activeSession == 1) DEFAULT_TO else "") ?: ""
        set(v) = sp.edit().putString(sk(KEY_TO), v).apply()

    /** Daftar nomor HP ini (bisa banyak, untuk terima OTP) */
    var toNumbers: MutableSet<String>
        get() {
            val def = if (activeSession == 1) mutableSetOf(DEFAULT_TO) else mutableSetOf()
            return sp.getStringSet(sk(KEY_TO_NUMS), def)?.toMutableSet() ?: def
        }
        set(v) = sp.edit().putStringSet(sk(KEY_TO_NUMS), v).apply()

    var openWaNumber: String
        get() = sp.getString(sk(KEY_OPEN_WA_NUM), if (activeSession == 1) DEFAULT_OPEN_WA_NUMBER else "") ?: ""
        set(v) = sp.edit().putString(sk(KEY_OPEN_WA_NUM), v).apply()

    /** Daftar nomor untuk Buka WA (bisa banyak) */
    var openWaNumbers: MutableSet<String>
        get() {
            val def = if (activeSession == 1) mutableSetOf(DEFAULT_OPEN_WA_NUMBER) else mutableSetOf()
            return sp.getStringSet(sk(KEY_OPEN_WA_NUMS), def)?.toMutableSet() ?: def
        }
        set(v) = sp.edit().putStringSet(sk(KEY_OPEN_WA_NUMS), v).apply()

    /** Pilihan WA: "biasa" | "bisnis" | "clone" | "semua" */
    var waChoice: String
        get() = sp.getString(sk(KEY_WA_CHOICE), "biasa") ?: "biasa"
        set(v) = sp.edit().putString(sk(KEY_WA_CHOICE), v).apply()

    /** Package name custom untuk WA Clone (diisi user kalau pilih "clone") */
    var waClonePackage: String
        get() = sp.getString(sk(KEY_WA_CLONE_PKG), "") ?: ""
        set(v) = sp.edit().putString(sk(KEY_WA_CLONE_PKG), v).apply()

    var forwardWa: Boolean
        get() = sp.getBoolean(sk(KEY_FWD_WA), true)
        set(v) = sp.edit().putBoolean(sk(KEY_FWD_WA), v).apply()

    var forwardSms: Boolean
        get() = sp.getBoolean(sk(KEY_FWD_SMS), true)
        set(v) = sp.edit().putBoolean(sk(KEY_FWD_SMS), v).apply()

    var openWa: Boolean
        get() = sp.getBoolean(sk(KEY_OPEN_WA), false)
        set(v) = sp.edit().putBoolean(sk(KEY_OPEN_WA), v).apply()

    /** Auto-buka chat dari order bot Telegram (polling gateway) */
    var autoOpenOrder: Boolean
        get() = sp.getBoolean(sk(KEY_AUTO_OPEN_ORDER), true)
        set(v) = sp.edit().putBoolean(sk(KEY_AUTO_OPEN_ORDER), v).apply()

    companion object {
        const val MAX_SESSIONS = 1000

        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_URL = "gateway_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_POLL_INTERVAL = "poll_interval_sec"
        private const val KEY_TO = "to_number"
        private const val KEY_TO_NUMS = "to_numbers"
        private const val KEY_OPEN_WA_NUM = "open_wa_number"
        private const val KEY_OPEN_WA_NUMS = "open_wa_numbers"
        private const val KEY_WA_CHOICE = "wa_choice"
        private const val KEY_WA_CLONE_PKG = "wa_clone_pkg"
        private const val KEY_FWD_WA = "forward_wa"
        private const val KEY_FWD_SMS = "forward_sms"
        private const val KEY_OPEN_WA = "open_wa"
        private const val KEY_AUTO_OPEN_ORDER = "auto_open_order"

        /** Default gateway URL (no key). User replaces with their own. */
        const val DEFAULT_URL = "http://72.60.40.224:3000/otp/ingest"
        /** Default numbers (user can change in Settings). Format 62. */
        const val DEFAULT_TO = "628136262899"
        const val DEFAULT_OPEN_WA_NUMBER = "62823738393030"
    }
}
