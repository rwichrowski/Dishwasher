package pl.radoslaw.zmywarka

import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import pl.radoslaw.zmywarka.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val dx = e2.x - (e1?.x ?: return false)
                val dy = e2.y - (e1.y)
                if (abs(dx) > abs(dy) * 1.5f && dx < -100f && abs(velocityX) > 300f) {
                    startActivity(Intent(this@MainActivity, WeightActivity::class.java))
                    @Suppress("DEPRECATION")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    return true
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private val people = listOf("Antek", "Weronika")
    private val prefs by lazy { getSharedPreferences("prefs", MODE_PRIVATE) }
    // Poniedziałek, od którego zaczął Antek (indeks 0)
    private val referenceMonday = LocalDate.of(2026, 5, 11)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.tvVersion.text = "v${BuildConfig.VERSION_NAME}"
        updateUI()
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), RC_NOTIF)
                return
            }
        }
        checkExactAlarmAndSchedule()
    }

    private fun checkExactAlarmAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms() && !prefs.getBoolean("alarm_perm_asked", false)) {
                prefs.edit().putBoolean("alarm_perm_asked", true).apply()
                AlertDialog.Builder(this)
                    .setTitle("Uprawnienie do alarmów")
                    .setMessage("Aby powiadomienia (waga / kcal) były punktualne, zezwól aplikacji na planowanie dokładnych alarmów.")
                    .setPositiveButton("Ustawienia") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                    }
                    .setNegativeButton("Pomiń", null)
                    .show()
                return
            }
        }
        NotificationScheduler.scheduleAll(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_NOTIF && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            checkExactAlarmAndSchedule()
        }
    }

    companion object {
        private const val RC_NOTIF = 42
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val pl = Locale("pl")
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val weekIndex = ChronoUnit.WEEKS.between(referenceMonday, monday)
        val personIndex = ((weekIndex % people.size) + people.size).toInt() % people.size

        binding.tvCurrentPerson.text = people[personIndex]

        // Masthead edition line — "✦  TYDZIEŃ 24 · 2026  ✦"
        val weekOfYear = monday.get(WeekFields.ISO.weekOfWeekBasedYear())
        binding.tvWeekKicker.text = "✦  TYDZIEŃ $weekOfYear · ${monday.year}  ✦"

        // Hero week range — "11–17 MAJA · TYDZIEŃ DYŻURU"
        val fmtFull = DateTimeFormatter.ofPattern("d MMMM", pl)
        val fmtShort = DateTimeFormatter.ofPattern("d MMM", pl)
        val range = if (monday.month == sunday.month)
            "${monday.dayOfMonth}–${sunday.format(fmtFull)}"
        else
            "${monday.format(fmtShort)} – ${sunday.format(fmtShort)}"
        binding.tvHeroDates.text = "${range.uppercase(pl)} · TYDZIEŃ DYŻURU"

        val nextMonday = monday.plusWeeks(1)
        val nextPersonIndex = (personIndex + 1) % people.size
        binding.tvQueue.text = "od ${nextMonday.format(fmtFull)} — ${people[nextPersonIndex]}"

        buildCalendar()
    }

    private fun buildCalendar() {
        val container = binding.calendarLayout
        container.removeAllViews()

        val today = LocalDate.now()
        val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale("pl"))
        val fmtFull = DateTimeFormatter.ofPattern("d MMMM", Locale("pl"))

        val ink = ContextCompat.getColor(this, R.color.ink)
        val inkSoft = ContextCompat.getColor(this, R.color.ink_soft)
        val terracotta = ContextCompat.getColor(this, R.color.terracotta)
        val fraunces = ResourcesCompat.getFont(this, R.font.fraunces_semibold)
        val frauncesLight = ResourcesCompat.getFont(this, R.font.fraunces)
        val mono = ResourcesCompat.getFont(this, R.font.dm_mono)
        val monoMed = ResourcesCompat.getFont(this, R.font.dm_mono_medium)

        for (offset in -2..2) {
            val monday = currentMonday.plusWeeks(offset.toLong())
            val sunday = monday.plusDays(6)
            val weekIndex = ChronoUnit.WEEKS.between(referenceMonday, monday)
            val personIndex = ((weekIndex % people.size) + people.size).toInt() % people.size
            val isCurrent = offset == 0

            val dateStr = if (monday.month == sunday.month) {
                "${monday.dayOfMonth}–${sunday.format(fmtFull)}"
            } else {
                "${monday.format(fmtShort)} – ${sunday.format(fmtShort)}"
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                if (isCurrent) {
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.row_current)
                    setPadding(dp(16), dp(15), dp(8), dp(15))
                } else {
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.row_rule)
                    setPadding(dp(4), dp(14), dp(8), dp(14))
                }
                alpha = if (offset < 0) 0.4f else 1f
            }

            TextView(this).apply {
                text = if (isCurrent) "▸  $dateStr" else dateStr
                typeface = if (isCurrent) monoMed else mono
                setTextColor(if (isCurrent) ink else inkSoft)
                textSize = 13f
                letterSpacing = 0.02f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(this)
            }

            TextView(this).apply {
                text = people[personIndex]
                if (isCurrent) {
                    typeface = fraunces
                    setTextColor(terracotta)
                    textSize = 20f
                } else {
                    typeface = frauncesLight
                    setTextColor(ink)
                    textSize = 17f
                }
                row.addView(this)
            }

            container.addView(row)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
