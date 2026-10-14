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
    private lateinit var etUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var etTo: EditText
    private lateinit var etOpenWaNum: EditText
    private lateinit var etClonePkg: EditText
    private lateinit var spWaChoice: Spinner
    private lateinit var spSession: Spinner
    private lateinit var swWa: Switch
    private lateinit var swSms: Switch
    private lateinit var swOpenWa: Switch
    private lateinit var swAutoOpenOrder: Switch
    private lateinit var tvStatus: TextView
    private lateinit var numAdapter: ArrayAdapter<String>
    private val numberList = mutableListOf<String>()
    private lateinit var toAdapter: ArrayAdapter<String>
    private val toList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        etUrl = findViewById<EditText>(R.id.etUrl)
        etApiKey = findViewById<EditText>(R.id.etApiKey)
        etTo = findViewById<EditText>(R.id.etTo)
        swWa = findViewById<Switch>(R.id.swForwardWa)
        swSms = findViewById<Switch>(R.id.swForwardSms)
        swOpenWa = findViewById<Switch>(R.id.swOpenWa)
        swAutoOpenOrder = findViewById<Switch>(R.id.swAutoOpenOrder)
        tvStatus = findViewById<TextView>(R.id.tvStatus)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val btnStart = findViewById<Button>(R.id.btnStart)

        // Sesi: Sesi 1 .. Sesi 10
        spSession = findViewById<Spinner>(R.id.spSession)
        val sessionOptions = (1..Prefs.MAX_SESSIONS).map { "Sesi $it" }.toTypedArray()
        spSession.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sessionOptions)
        spSession.setSelection(prefs.activeSession - 1)
        var sessionChanging = false
        spSession.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                if (sessionChanging) return
                val newSession = pos + 1
                if (newSession != prefs.activeSession) {
                    // Simpan sesi lama dulu
                    saveCurrentSession()
                    prefs.activeSession = newSession
                    sessionChanging = true
                    recreate() // reload UI dengan data sesi baru
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Load saved values
        etUrl.setText(prefs.gatewayUrl)
        etApiKey.setText(prefs.apiKey)
        // Daftar Nomor HP ini (bisa tambah banyak)
        val lvToNumbers = findViewById<android.widget.ListView>(R.id.lvToNumbers)
        val btnAddTo = findViewById<Button>(R.id.btnAddTo)
        toList.clear()
        toList.addAll(prefs.toNumbers.sorted())
        toAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, toList)
        lvToNumbers.adapter = toAdapter
        btnAddTo.setOnClickListener {
            val num = etTo.text.toString().trim().filter { it.isDigit() }
            if (num.isEmpty()) {
                Toast.makeText(this, "Isi nomor dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (toList.contains(num)) {
                Toast.makeText(this, "Nomor sudah ada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            toList.add(num)
            toList.sort()
            toAdapter.notifyDataSetChanged()
            etTo.text.clear()
            Toast.makeText(this, "Nomor ditambah", Toast.LENGTH_SHORT).show()
        }
        lvToNumbers.setOnItemClickListener { _, _, pos, _ ->
            val removed = toList.removeAt(pos)
            toAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Hapus: $removed", Toast.LENGTH_SHORT).show()
        }
        // Daftar nomor Buka WA (bisa tambah banyak)
        etOpenWaNum = findViewById<EditText>(R.id.etOpenWaNum)
        val lvNumbers = findViewById<android.widget.ListView>(R.id.lvNumbers)
        val btnAddNumber = findViewById<Button>(R.id.btnAddNumber)
        numberList.clear()
        numberList.addAll(prefs.openWaNumbers.sorted())
        numAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, numberList)
        lvNumbers.adapter = numAdapter
        btnAddNumber.setOnClickListener {
            val num = etOpenWaNum.text.toString().trim().filter { it.isDigit() }
            if (num.isEmpty()) {
                Toast.makeText(this, "Isi nomor dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (numberList.contains(num)) {
                Toast.makeText(this, "Nomor sudah ada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            numberList.add(num)
            numberList.sort()
            numAdapter.notifyDataSetChanged()
            etOpenWaNum.text.clear()
            Toast.makeText(this, "Nomor ditambah", Toast.LENGTH_SHORT).show()
        }
        lvNumbers.setOnItemClickListener { _, _, pos, _ ->
            val removed = numberList.removeAt(pos)
            numAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Hapus: $removed", Toast.LENGTH_SHORT).show()
        }
        // Pilihan WA: Biasa / Bisnis / Clone / Semua
        spWaChoice = findViewById<Spinner>(R.id.spWaChoice)
        val waOptions = arrayOf("WA Biasa", "WA Bisnis", "WA Clone", "Semua WA")
        spWaChoice.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, waOptions)
        spWaChoice.setSelection(when (prefs.waChoice) { "bisnis" -> 1; "clone" -> 2; "semua" -> 3; else -> 0 })
        etClonePkg = findViewById<EditText>(R.id.etClonePkg)
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
        swAutoOpenOrder.isChecked = prefs.autoOpenOrder

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "Gateway URL wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveCurrentSession()
            Toast.makeText(this, "Sesi ${prefs.activeSession} disimpan", Toast.LENGTH_SHORT).show()
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

    /** Simpan semua field ke sesi aktif */
    private fun saveCurrentSession() {
        prefs.gatewayUrl = etUrl.text.toString().trim()
        prefs.apiKey = etApiKey.text.toString().trim()
        // Simpan daftar Nomor HP ini
        prefs.toNumbers = toList.toMutableSet()
        prefs.toNumber = toList.firstOrNull() ?: ""
        // Simpan daftar nomor Buka WA
        prefs.openWaNumbers = numberList.toMutableSet()
        prefs.openWaNumber = numberList.firstOrNull() ?: ""
        prefs.waChoice = when (spWaChoice.selectedItemPosition) {
            1 -> "bisnis"; 2 -> "clone"; 3 -> "semua"; else -> "biasa"
        }
        prefs.waClonePackage = etClonePkg.text.toString().trim()
        prefs.forwardWa = swWa.isChecked
        prefs.forwardSms = swSms.isChecked
        prefs.openWa = swOpenWa.isChecked
        prefs.autoOpenOrder = swAutoOpenOrder.isChecked
    }
}
