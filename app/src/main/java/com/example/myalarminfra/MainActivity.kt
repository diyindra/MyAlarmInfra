package com.example.myalarminfra

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myalarminfra.MqttService
import com.example.myalarminfra.R
import java.util.Locale


class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {
    private val CHANNEL_ID = "my infra_alert_channel"
    private lateinit var tts: TextToSpeech

    private fun showNotification(area:String) {
        // 1. Buat Notification Channel (Android 8+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel untuk alarm berbunyi"
                setSound(soundUri, null)
                enableVibration(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // 2. Buat Notifikasi
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Myinfra Alarm")
            .setContentText("STARTMonitoring ALARM area->"+area)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(500, 500, 500))

        val manager = NotificationManagerCompat.from(this)
        manager.notify(1001, builder.build())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        tts = TextToSpeech(this, this)
        val areatextview = findViewById<TextView>(R.id.areaterpilih)
        val area=areatextview.text.toString()
        val intent = Intent(this, konektifitas::class.java)
        intent.putExtra("area", area)
        startForegroundService(intent)

        val jabotombol= findViewById<CardView>(R.id.areajabotombol)
        val jjbbtombol= findViewById<CardView>(R.id.areajbbtombol)
        val sbbtombol = findViewById<CardView>(R.id.areasbbtombol)
        val alltombol = findViewById<CardView>(R.id.areaalltombol)
        val tblShowActiveAlarm =findViewById<Button>(R.id.active_alarm_open)


        tblShowActiveAlarm.setOnClickListener {
            val intent = Intent(this, active_alarm_list::class.java)
            startActivity(intent)
        }



          jabotombol.setOnClickListener{
              Toast.makeText(this, "JABO dipilih", Toast.LENGTH_SHORT).show()
              val areadipilih = findViewById<TextView>(R.id.areaterpilih)
              areadipilih.text= "jabo"
              speak("kamu memilih area jabo untuk mendapatkan informasi alarm")
              gantiarea()
          }
          jjbbtombol.setOnClickListener{
              Toast.makeText(this, "JBB dipilih", Toast.LENGTH_SHORT).show()
              val areadipilih = findViewById<TextView>(R.id.areaterpilih)
              gantiarea()
              areadipilih.text= "jbb"
              speak("kamu memilih area jbb untuk mendapatkan informasi alarm")


          }
        sbbtombol.setOnClickListener{
            Toast.makeText(this, "SBB dipilih", Toast.LENGTH_SHORT).show()
            val areadipilih = findViewById<TextView>(R.id.areaterpilih)
            gantiarea()
            areadipilih.text= "Sbb"
            speak("kamu memilih area SBB untuk mendapatkan informasi alarm")


        }
        alltombol.setOnClickListener{
            Toast.makeText(this, "ALL dipilih", Toast.LENGTH_SHORT).show()
            val areadipilih = findViewById<TextView>(R.id.areaterpilih)
            gantiarea()
            areadipilih.text= "all"
            speak("kamu memilih semuaarea untuk mendapatkan informasi alarm")


        }

    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("id", "ID"))

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.e("TTS", "Bahasa tidak didukung")
            }
        } else {
            Log.e("TTS", "Init gagal")
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
    private fun speak(text: String) {
        if (::tts.isInitialized) {
            tts.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "TTS_ID"
            )
        }
    }
    private fun gantiarea(){
        val areatextview = findViewById<TextView>(R.id.areaterpilih)
        val area=areatextview.text.toString()
        val intent = Intent(this, konektifitas::class.java)
        intent.putExtra("area", area)
        startService(intent)
    }

}

