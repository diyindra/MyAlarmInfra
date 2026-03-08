import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.myalarminfra.AlarmModel
import com.example.myalarminfra.R

class AlarmAdapter(private val list: List<AlarmModel>) :
    RecyclerView.Adapter<AlarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val txtSite: TextView = view.findViewById(R.id.sitename)
        val txtAlarm: TextView = view.findViewById(R.id.alarmname)
        val txtSeverity: TextView = view.findViewById(R.id.saverity)
        val iconAlarm: ImageView = view.findViewById(R.id.iconAlarm)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val alarm = list[position]

        holder.txtSite.text = alarm.site
        holder.txtAlarm.text = alarm.alarm
        holder.txtSeverity.text = alarm.severity

        // icon mapping sederhana
        when {

            alarm.icon.contains("bolt") ->
                holder.iconAlarm.setImageResource(android.R.drawable.ic_dialog_alert)

            alarm.icon.contains("thermometer") ->
                holder.iconAlarm.setImageResource(android.R.drawable.ic_menu_compass)

            alarm.icon.contains("door") ->
                holder.iconAlarm.setImageResource(android.R.drawable.ic_lock_idle_alarm)

        }

    }
}