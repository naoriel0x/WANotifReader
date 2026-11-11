package com.devandra.wanotifread

import android.app.Notification
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class NotificationReaderService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var prefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        prefs = getSharedPreferences("wa_reader_prefs", MODE_PRIVATE)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts.setLanguage(Locale("id", "ID"))
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.ENGLISH)
            }
            tts.setSpeechRate(0.9f)
            tts.setPitch(1.0f)
            ttsReady = true
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val isWhatsApp = pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b"
        if (!isWhatsApp) return

        val isEnabled = prefs.getBoolean("service_enabled", true)
        if (!isEnabled) return

        val extras = sbn.notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE) ?: return
        val rawMessage = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        if (rawMessage.matches(Regex("\\d+ pesan baru")) || rawMessage.contains("new message")) return

        val isGroupMessage = rawMessage.contains(": ")
        val readGroups = prefs.getBoolean("read_groups", false)
        if (isGroupMessage && !readGroups) return

        val priorityContacts = prefs.getStringSet("priority_contacts", emptySet()) ?: emptySet()
        val shouldRead = priorityContacts.isEmpty() || priorityContacts.any {
            sender.contains(it, ignoreCase = true)
        }

        if (!shouldRead || !ttsReady) return

        val finalMessage = if (isGroupMessage) rawMessage.substringAfter(": ") else rawMessage
        val spoken = "$sender bilang, $finalMessage"

        tts.speak(spoken, TextToSpeech.QUEUE_ADD, null, sbn.key)
    }

    override fun onDestroy() {
        if (ttsReady) tts.shutdown()
        super.onDestroy()
    }
}