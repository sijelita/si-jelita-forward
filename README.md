# WA Forward — Notification Forwarder + WhatsApp Opener

Android app (Kotlin) yang meneruskan notifikasi WhatsApp / SMS di HP **kamu sendiri**
ke gateway, dan opsional membuka app WhatsApp saat notifikasi masuk.
Pengganti MacroDroid untuk jalur forward OTP.

## Yang dilakukan app ini

- `NotificationListenerService` membaca notifikasi WhatsApp / SMS yang sudah
  masuk ke HP kamu (notifikasi yang kamu sendiri terima).
- Setiap notifikasi: kirim JSON `{sender, text, to}` via POST ke Gateway URL
  yang kamu isi di app (default `http://72.60.40.224:3000/otp/ingest`).
- Jika API Key diisi, dikirim sebagai `?key=...` di URL.
- Opsional: buka app WhatsApp saat notifikasi WhatsApp masuk (toggle di app).
- Berjalan sebagai foreground service + auto-start setelah reboot.

## Yang TIDAK dilakukan app ini

- Tidak membaca pesan WhatsApp langsung (tidak pairing, tidak Baileys).
- Tidak ekstrak / simpan sesi WhatsApp, kredensial, atau kunci apa pun.
- Tidak ada kredensial hardcoded — URL gateway + API key diisi user di app,
  disimpan di SharedPreferences HP.

## Cara build APK (di PC)

1. Install **Android Studio** (gratis, developer.android.com/studio).
2. Buka proyek ini: **File → Open** → pilih folder `wa-forward-apk`.
3. Tunggu Gradle sync selesai (butuh internet untuk download dependensi).
4. Build APK: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.
6. Pindahkan APK ke HP, install (izinkan "Install unknown apps" bila diminta).

## Cara pakai (di HP)

1. Buka app WA Forward.
2. Isi:
   - **Gateway URL** — mis. `http://72.60.40.224:3000/otp/ingest`
   - **API Key** — key Worker dari bot Telegram (jangan paste ke chat).
   - **Nomor HP ini** — nomor SIM HP ini (field `to`, mis. `62812xxxxxxx`).
3. Aktifkan toggle sesuai kebutuhan (Forward WA / Forward SMS / Buka WhatsApp).
4. Tap **Simpan Pengaturan**.
5. Tap **Buka Izin Notifikasi** → aktifkan **WA Forward** di daftar.
6. Tap **Mulai Service**.
7. Matikan optimasi baterai untuk app ini:
   Pengaturan HP → Baterai → WA Forward → Tanpa batas / Jangan optimalkan.
   (Kalau tidak, Android bisa bunuh service saat layar mati.)

## Catatan

- `minSdk 26` (Android 8.0+), `targetSdk 34`.
- Notifikasi yang diteruskan hanya yang muncul di status bar HP.
- Untuk OTP resmi via WhatsApp: notifikasi di HP tetap dibaca (app ini baca
  notifikasi, bukan pesan via linked device), jadi OTP masuk seperti MacroDroid.
- Repo ini hanya source code — tidak ada APK biner di dalamnya.
