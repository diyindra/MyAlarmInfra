package com.example.myalarminfra

//import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import java.util.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.json.JSONObject
import com.example.myalarminfra.R
import org.json.JSONArray


class konektifitas: Service(), TextToSpeech.OnInitListener  {
    private var area: String = ""
    private lateinit var client: MqttClient
    private val broker = "tcp://103.123.248.212:1883"
    private val topic = "moratel/#"
    private lateinit var mqttClient: MqttClient

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val channelId = "qlat_mqtt_channel"
    private val SERVICE_NOTIFICATION_ID = 100
    private val MESSAGE_NOTIFICATION_ID = 200
    private var tts: TextToSpeech? = null
    private val TAG = "MyService"
    private lateinit var queue: RequestQueue
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            SERVICE_NOTIFICATION_ID,
            createServiceNotification("Myinfra Alarm","MyInfraAlarm Service is running...onCreate")
        )
        updateServiceNotification("Myinfra Alarm","MyInfraAlarm Konektifitas Service is running updater on start command")
        connectMqtt()
        tts = TextToSpeech(this, this)
        queue = Volley.newRequestQueue(this)
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("id", "ID")     // Bahasa Indonesia
        }
    }
    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mqtt_tts_id")
    }
    private fun showToast(msg: String) {
        android.os.Handler(mainLooper).post {
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    private fun reconnectMqtt() {
        Thread {
            try {
                Log.d(TAG, "MQTT: mencoba reconnect...")
                client.connect()
                Log.d(TAG, "MQTT: reconnect OK")
                showToast("MQTT Reconnected!")
                createServiceNotification("state","terkoneksi kembali")


                client.subscribe(topic)
            } catch (e: Exception) {
                Log.e(TAG, "MQTT reconnect gagal: ${e.message}")
                showToast("Reconnect gagal, coba lagi...")

                // coba lagi 3 detik kemudian
                Thread.sleep(3000)
                reconnectMqtt()
            }
        }.start()
    }
    private fun alarmValidation(alarm_name:String,site_name:String, region:String){
        val urlAlarm="http://103.123.248.212:8080/new/api/get_alarm_detail?alarm_name=${alarm_name}"
        Log.d("DEBUG",  "alamat alarm $urlAlarm")  // tampilkan hasil di l
        val antrian = Volley.newRequestQueue(this)
        val meminta = StringRequest(Request.Method.GET, urlAlarm,
            { jawaban ->
                val jsonArray = JSONArray(jawaban.toString())
                val alarm_info=jsonArray.getJSONObject(0)
                if (alarm_info.getString("notif").toString()==1.toString()){
                    Log.d("MA_DEBUG", "jawaban :${jawaban}")
//                    speak(" Alarm dari site :  ${site_name}${alarm_info.getString("name")}")// og
                    updateServiceNotification("ALARM ${site_name}","alarm{$region}:${alarm_info.getString("name")}lihat dashboard EMS untuk memastikan")
                    playAlarmSound()

                }else{
                    Log.d("NOTIFDEBUG", "TIDAK AKTIF")

                }
            },
            { nok ->
                Log.e("MA_DEBUG", "Error: ${nok.message}")
            })
        antrian.add(meminta)

    }
    private fun alarmcheck(siteId:String, pesan:String){
        val urlregion = "http://103.123.248.212:8080/new/api/get_regionsite?site_id=${siteId.replace(" ","")}"
        Log.d("MA_DEBUG",  "$urlregion")  // tampilkan hasil di log

        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(Request.Method.GET, urlregion,
            { response ->
                val jsonArray = JSONArray(response.toString())
                val pesanObj = JSONObject(pesan.toString())
                val alarmString=JSONObject(pesanObj.getString("alarm_string"))
                val regionObj = jsonArray.getJSONObject(0)
                val region = regionObj.getString("region")
                    if (area.uppercase()=="ALL" ||area.uppercase()== region.uppercase()) {
                        Log.d("MA_DEBUG", "MQTT MESSAGE ${siteId}: $pesan")

//                updateServiceNotification("alarm Site ${siteId} ${alarmString.getString("site_name")}","alarm -> $pesan")
//                        speak("ada Alarm dari site :  ${alarmString.getString("site_name")}")
                Log.d("MA_DEBUG", "Response: ${response.toString()}")  // tampilkan hasil di log
                Log.d("MA_DEBUG","REGION IS : ${region.uppercase()}")
                Log.d("MA_DEBUG","area terpilih : ${area.uppercase()}")
                Log.d("MA_DEBUG","alarm name :${pesanObj.getString("alarm_name")}")
                    alarmValidation(
                        pesanObj.getString("alarm_name"),
                        alarmString.getString("site_name"), region.toString()
                    )
                }
                else{
                        Log.d("MA_DEBUG","area alarm tidak cocok ${area.uppercase()}  ${region.uppercase()}")

                    }
            },
            { error ->
                Log.e("MA_DEBUG", "Error: ${error.message}")
            })

        queue.add(request)

    }
    private fun connectMqtt() {
        try {
            val clientId = MqttClient.generateClientId()
            client = MqttClient(broker, clientId, null)

            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
            }

            client.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    Log.e("debug", "MQTT LOST: ${cause?.message}")
                    createServiceNotification("MA_DEBUG","koneksi server terputus")
                    createServiceNotification("MA_DEBUG","mencoba koneksi kambali")
                    reconnectMqtt()
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    val pesan = message.toString()
                    val pesanObj = JSONObject(pesan)

                    val topik = topic.toString().split("/")
                    val bagian1 = topik.getOrNull(1)
                    if (bagian1 == "alarm") {
                        alarmcheck(" ${pesanObj.getString("site_id")}", pesan)
//                        showToast("ALARM: $pesan")
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    Log.d(TAG, "Delivery Complete")
                }
            })

            Thread {
                try {
                    client.connect(options)

                    Log.d("MA_DEBUG", "MQTT CONNECTED")
                    showToast("MQTT Terhubung ke broker!")

                    client.subscribe(topic)
                    Log.d("MA_DEBUG", "SUBSCRIBED topic: $topic")

                } catch (ex: Exception) {
                    Log.e(TAG, "MQTT ERROR CONNECT: ${ex.message}")
                    showToast("Gagal konek MQTT: ${ex.message}")
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "MQTT ERROR: ${e.message}")
            showToast("MQTT Error: ${e.message}")
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playAlarmSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.notif)
            mediaPlayer?.isLooping = false   // kalau ingin loop
        }

        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    private fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        area = intent?.getStringExtra("area") ?: ""
        createNotificationChannel()
        val action = intent?.getStringExtra("action")
        if (action == "stop") {
            // hentikan semua proses internal / thread
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.disconnect()
                mqttClient.close()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error disconnecting MQTT: ${t.message}")
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Myalarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications MyInfraAlarm"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    private fun createServiceNotification(title:String,content: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.redbell)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }
    private fun updateServiceNotification(title: String,content: String) {
        val notification = createServiceNotification(title,content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        manager.notify(notificationId, notification)
    }
    

}