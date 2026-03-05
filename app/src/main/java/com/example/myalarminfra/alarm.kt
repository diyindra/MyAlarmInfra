package com.example.myalarminfra

class Alarm {
    data class Alarm(
        val site_id: String,
        val alarm_name: String,
        val alarm_value: String,
        val alarm_string: String
    )
}