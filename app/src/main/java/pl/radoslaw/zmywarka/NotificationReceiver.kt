package pl.radoslaw.zmywarka

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return

        showNotification(context, notifId, message)
        NotificationScheduler.scheduleNext(context, notifId)
    }

    private fun showNotification(context: Context, id: Int, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(CHANNEL_ID, "Przypomnienia", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Codzienne przypomnienia o wadze i kaloriach"
        }
        nm.createNotificationChannel(channel)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_scale)
            .setContentTitle("Zmywarka")
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_MESSAGE = "message"
    }
}
