package com.example.myalarminfra

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Timer
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttService : Service() {
    private val channelId = "qlat_mqtt_channel"
    private val SERVICE_NOTIFICATION_ID = 1
    private val MESSAGE_NOTIFICATION_ID = 2

    private var isTryingToReconnect = false
    private var reconnectTimer: Timer? = null
    lateinit var mqttClient: MqttAndroidClient


    override fun onCreate() {
        super.onCreate()
        Log.d("MyService", "Service created")
        createNotificationChannel()
        startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification("MyInfraAlarm Service is running...onCreate"))
        updateServiceNotification("MyInfraAlarm Service is running updater")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "QLAT Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications MyInfraAlarm"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun createServiceNotification(content: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("MyInfa alarm Notification")
            .setContentText(content)
            .setSmallIcon(R.drawable.stat_notify_sync)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }
    private fun updateServiceNotification(content: String) {
        val notification = createServiceNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(SERVICE_NOTIFICATION_ID, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("MyService", "Service started")


        // Contoh proses background
        Thread {
            while (true) {
                Log.d("MyService", "Running background task...")
                Thread.sleep(3000)
            }
        }.start()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MyService", "Service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
