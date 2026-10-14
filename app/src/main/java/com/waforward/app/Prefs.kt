package com.waforward.app

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences wrapper for user-entered settings.
 * No credentials are hardcoded — the user fills them in the app.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("wa_forward_prefs", Context.MODE_PRIVATE)

    var gatewayUrl: String
        get() = sp.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL
        set(v) = sp.edit().putString(KEY_URL, v).apply()

    var apiKey: String
        get() = sp.getString(KEY_API_KEY, "") ?: ""
        set(v) = sp.edit().putString(KEY_API_KEY, v).apply()

    var toNumber: String
        get() = sp.getString(KEY_TO, DEFAULT_TO) ?: DEFAULT_TO
        set(v) = sp.edit().putString(KEY_TO, v).apply()

    var openWaNumber: String
        get() = sp.getString(KEY_OPEN_WA_NUM, DEFAULT_OPEN_WA_NUMBER) ?: DEFAULT_OPEN_WA_NUMBER
        set(v) = sp.edit().putString(KEY_OPEN_WA_NUM, v).apply()

    /** Pilihan WA: "biasa" | "bisnis" | "clone" */
    var waChoice: String
        get() = sp.getString(KEY_WA_CHOICE, "biasa") ?: "biasa"
        set(v) = sp.edit().putString(KEY_WA_CHOICE, v).apply()

    /** Package name custom untuk WA Clone (diisi user kalau pilih "clone") */
    var waClonePackage: String
        get() = sp.getString(KEY_WA_CLONE_PKG, "") ?: ""
        set(v) = sp.edit().putString(KEY_WA_CLONE_PKG, v).apply()

    var forwardWa: Boolean
        get() = sp.getBoolean(KEY_FWD_WA, true)
        set(v) = sp.edit().putBoolean(KEY_FWD_WA, v).apply()

    var forwardSms: Boolean
        get() = sp.getBoolean(KEY_FWD_SMS, true)
        set(v) = sp.edit().putBoolean(KEY_FWD_SMS, v).apply()

    var openWa: Boolean
        get() = sp.getBoolean(KEY_OPEN_WA, false)
        set(v) = sp.edit().putBoolean(KEY_OPEN_WA, v).apply()

    companion object {
        private const val KEY_URL = "gateway_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_TO = "to_number"
        private const val KEY_OPEN_WA_NUM = "open_wa_number"
        private const val KEY_WA_CHOICE = "wa_choice"
        private const val KEY_WA_CLONE_PKG = "wa_clone_pkg"
        private const val KEY_FWD_WA = "forward_wa"
        private const val KEY_FWD_SMS = "forward_sms"
        private const val KEY_OPEN_WA = "open_wa"

        /** Default gateway URL (no key). User replaces with their own. */
        const val DEFAULT_URL = "http://72.60.40.224:3000/otp/ingest"
        /** Default numbers (user can change in Settings). Format 62. */
        const val DEFAULT_TO = "628136262899"
        const val DEFAULT_OPEN_WA_NUMBER = "62823738393030"
    }
}
