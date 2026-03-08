package com.example.myalarminfra

import AlarmAdapter
import android.app.DownloadManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.privacysandbox.tools.core.model.Method
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class active_alarm_list : AppCompatActivity() {
    private val alarmList = ArrayList<AlarmModel>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlarmAdapter
    private lateinit var loadingBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_active_alarm_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnBack = findViewById<Button>(R.id.button_back)
        btnBack.setOnClickListener {
            finish()  // Kembali ke Activity sebelumnya
        }

        recyclerView = findViewById(R.id.alarmList)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AlarmAdapter(alarmList)

        recyclerView.adapter = adapter
        loadingBar = findViewById(R.id.loadingBar)
        getAlarms()
    }
    private fun getAlarms() {
        Log.d("API_RESPONSE", "get alarm called")
        loadingBar.visibility = View.VISIBLE


        val url = "http://103.123.248.212:8080/new/api/get_active_alarms"

        val queue = Volley.newRequestQueue(this)

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                loadingBar.visibility = View.GONE
//                Log.d("API_RESPONSSE", response.toString())

                for (i in 0 until response.length()) {

                    val obj = response.getJSONObject(i)

                    val siteId = obj.getString("site_id")
                    val al_s = JSONObject(obj.getString("alarm_string"))
                    val site = obj.getString("site_id")+" | "+al_s.getString("site_name")
                    val alarm = obj.getString("alarm")+"(Val:"+al_s.getString("value")+")"
                    val severity = obj.optString("saverity", "unknown")+" | th-Hi:"+al_s.getString("thr_hi")+" | th_Lo:"+al_s.getString("thr_lo")
                    val icon = obj.optString("iconapps", "")

                    Log.d("API_RESPONSE", "$al_s")
                    Log.d("API_RESPONSE", "$severity - $icon")

                    val alarmModel = AlarmModel(site, alarm, severity, icon)
                    alarmList.add(alarmModel)
                }
                adapter.notifyDataSetChanged()
            },
            { error ->

                Log.e("API_ERROR", error.toString())

                error.networkResponse?.let {
                    Log.e("API_ERROR_CODE", it.statusCode.toString())
                }
            }
        )

        queue.add(request)
    }
}