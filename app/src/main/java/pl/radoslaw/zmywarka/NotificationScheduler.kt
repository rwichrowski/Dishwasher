package pl.radoslaw.zmywarka

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {

    const val ID_WEIGHT = 1
    const val ID_CALORIES = 2

    fun scheduleAll(context: Context) {
        schedule(context, ID_WEIGHT, 7, 0, "Wprowadź dzisiejszą wagę")
        schedule(context, ID_CALORIES, 20, 5, "Wprowadź zjedzone kcal")
    }

    fun scheduleNext(context: Context, notifId: Int) {
        when (notifId) {
            ID_WEIGHT -> schedule(context, ID_WEIGHT, 7, 0, "Wprowadź dzisiejszą wagę", tomorrowOnly = true)
            ID_CALORIES -> schedule(context, ID_CALORIES, 20, 5, "Wprowadź zjedzone kcal", tomorrowOnly = true)
        }
    }

    private fun schedule(context: Context, id: Int, hour: Int, minute: Int, message: String, tomorrowOnly: Boolean = false) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (tomorrowOnly || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_NOTIF_ID, id)
            putExtra(NotificationReceiver.EXTRA_MESSAGE, message)
        }
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
}
