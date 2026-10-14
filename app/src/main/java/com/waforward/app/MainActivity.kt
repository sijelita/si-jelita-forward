package com.waforward.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * WA Forward — Settings screen.
 *
 * The user enters their own gateway URL + API key here.
 * Nothing is hardcoded. Values are stored in SharedPreferences.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        val etUrl = findViewById<EditText>(R.id.etUrl)
        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val etTo = findViewById<EditText>(R.id.etTo)
        val swWa = findViewById<Switch>(R.id.swForwardWa)
        val swSms = findViewById<Switch>(R.id.swForwardSms)
        val swOpenWa = findViewById<Switch>(R.id.swOpenWa)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // Load saved values
        etUrl.setText(prefs.gatewayUrl)
        etApiKey.setText(prefs.apiKey)
        etTo.setText(prefs.toNumber)
        etTo.hint = "Nomor untuk Forward (WA) — default 628136262899"
        val etOpenWaNum = findViewById<EditText>(R.id.etOpenWaNum)
        etOpenWaNum.setText(prefs.openWaNumber)
        etOpenWaNum.hint = "Nomor untuk Buka WA — default 62823738393030"
        // Pilihan WA: Biasa / Bisnis / Clone
        val spWaChoice = findViewById<Spinner>(R.id.spWaChoice)
        val waOptions = arrayOf("WA Biasa", "WA Bisnis", "WA Clone")
        spWaChoice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, waOptions)
        spWaChoice.setSelection(when (prefs.waChoice) { "bisnis" -> 1; "clone" -> 2; else -> 0 })
        val etClonePkg = findViewById<EditText>(R.id.etClonePkg)
        etClonePkg.setText(prefs.waClonePackage)
        etClonePkg.hint = "Package name WA Clone (mis. com.whatsapp.clone)"
        // Tampilkan field clone hanya kalau pilih "WA Clone"
        etClonePkg.visibility = if (prefs.waChoice == "clone") android.view.View.VISIBLE else android.view.View.GONE
        spWaChoice.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                etClonePkg.visibility = if (pos == 2) android.view.View.VISIBLE else android.view.View.GONE
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        swWa.isChecked = prefs.forwardWa
        swSms.isChecked = prefs.forwardSms
        swOpenWa.isChecked = prefs.openWa

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Gateway URL wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.gatewayUrl = url
            prefs.apiKey = etApiKey.text.toString().trim()
            prefs.toNumber = etTo.text.toString().trim()
            prefs.openWaNumber = findViewById<EditText>(R.id.etOpenWaNum).text.toString().trim()
            prefs.waChoice = when (findViewById<Spinner>(R.id.spWaChoice).selectedItemPosition) {
                1 -> "bisnis"; 2 -> "clone"; else -> "biasa"
            }
            prefs.waClonePackage = findViewById<EditText>(R.id.etClonePkg).text.toString().trim()
            prefs.forwardWa = swWa.isChecked
            prefs.forwardSms = swSms.isChecked
            prefs.openWa = swOpenWa.isChecked
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
            updateStatus(tvStatus)
        }

        btnPermission.setOnClickListener {
            // Open system Notification Access settings
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnStart.setOnClickListener {
            // Trigger the listener service to bind (system binds it once
            // notification access is granted). Starting a foreground
            // notification keeps the process alive.
            startService(Intent(this, ForwardService::class.java).apply {
                action = ForwardService.ACTION_START_FOREGROUND
            })
            Toast.makeText(this, "Service dimulai", Toast.LENGTH_SHORT).show()
            updateStatus(tvStatus)
        }

        updateStatus(tvStatus)
    }

    private fun updateStatus(tv: TextView) {
        val granted = ForwardService.isListenerEnabled(this)
        tv.text = if (granted) {
            "Status: Izin notifikasi AKTIF"
        } else {
            "Status: Izin notifikasi BELUM aktif — tap 'Buka Izin Notifikasi'"
        }
    }
}
