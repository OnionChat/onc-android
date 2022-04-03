package com.onionchat.dr0id.broadcast

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.legacy.content.WakefulBroadcastReceiver
import com.onionchat.common.DateTimeHelper
import com.onionchat.common.Logging
import com.onionchat.dr0id.service.ServicesManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AlarmReceiver : WakefulBroadcastReceiver() {


    companion object {

        fun completeWork(intent:Intent) {
            completeWakefulIntent(intent)
        }


            const val TAG = "AlarmReceiver"


        fun scheduleAlarmReceiver(context: Context) {
            Logging.d(TAG, "scheduleBackgroundService [+] schedule AlarmReceiver broadcast")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scheduleAlarm(context)
            } else {
                // todo fix android N
            }
//            val calendar: Calendar = Calendar.getInstance()
//            calendar.set(Calendar.HOUR_OF_DAY, 0)
//            calendar.set(Calendar.MINUTE, 10)
//            calendar.set(Calendar.SECOND, 0)
//            val pi = PendingIntent.getService(
//                context, 0,
//                Intent(context, OnionChatRefreshService::class.java), PendingIntent.FLAG_IMMUTABLE
//            )
//            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            am.setRepeating(
//                AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
//                AlarmManager.INTERVAL_HOUR, pi
//            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun scheduleAlarm(context: Context) {
            val start = LocalDateTime.now()
            val end: LocalDateTime = start.plusHours(1).truncatedTo(ChronoUnit.HOURS)
            val duration: Duration = Duration.between(start, end)
//            powerSafeQuestion(context)
//            val intent = Intent(context, OnionChatRefreshService::class.java)
//            val pIntent = PendingIntent.getBroadcast(context.applicationContext,
//                IntentIntegrator.REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//            val firstMillis = System.currentTimeMillis()+1000
//            val alarm = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager
////            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, AlarmManager.INTERVAL_HOUR, pIntent)
//            alarm.setRepeating(
//                AlarmManager.ELAPSED_REALTIME, firstMillis+1000, 10*1000, pIntent);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                Logging.d(TAG, "scheduleBackgroundService [+] alarm.canScheduleExactAlarms ${alarm.canScheduleExactAlarms()}")
//            }

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
            val startTime = System.currentTimeMillis() + duration.toMillis() //
            Logging.e(TAG, "scheduleAlarm [+] startTime=(${DateTimeHelper.timestampToString(startTime)})")
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startTime, pi)
            // + duration.toMillis()
            //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() , (1000 * 60 * context.resources.getInteger(R.integer.poll_intervall_minutes)).toLong(), pi)
        }

        fun powerSafeQuestion(context: Context) {
            val intent = Intent()
            val packageName: String = context.getPackageName()
            val pm = context.getSystemService(Service.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
            }

//            else {
//            intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
//            context.startActivity(intent)
//            }
        }

        fun alarmQuestion(context: Context) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            }

            context.startActivity(intent)
        }
    }

    override fun onReceive(p0: Context?, p1: Intent?) {
        Logging.d(TAG, "onReceive [+] received alarm")
        if (p0 == null) {
            Logging.e(TAG, "onRecieve [-] context is null. Abort.")
            return
        }
        ServicesManager.launchServices(p0)
    }
}