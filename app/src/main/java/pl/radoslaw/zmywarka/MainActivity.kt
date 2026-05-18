package pl.radoslaw.zmywarka

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pl.radoslaw.zmywarka.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
    // Poniedziałek, od którego zaczął Antek (indeks 0)
    private val referenceMonday = LocalDate.of(2026, 5, 11)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekIndex = ChronoUnit.WEEKS.between(referenceMonday, monday)
        val personIndex = ((weekIndex % people.size) + people.size).toInt() % people.size

        binding.tvCurrentPerson.text = people[personIndex]

        val nextMonday = monday.plusWeeks(1)
        val nextPersonIndex = (personIndex + 1) % people.size
        val fmt = DateTimeFormatter.ofPattern("d MMMM", Locale("pl"))
        binding.tvQueue.text = "od ${nextMonday.format(fmt)}: ${people[nextPersonIndex]}"

        buildCalendar()
    }

    private fun buildCalendar() {
        val container = binding.calendarLayout
        container.removeAllViews()

        val today = LocalDate.now()
        val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale("pl"))
        val fmtFull = DateTimeFormatter.ofPattern("d MMMM", Locale("pl"))
        val d = resources.displayMetrics.density
        val padH = (10 * d).toInt()
        val padV = (8 * d).toInt()
        val rowMargin = (4 * d).toInt()

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
                ).apply { bottomMargin = rowMargin }
                setPadding(padH, padV, padH, padV)
                if (isCurrent) setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.week_current_bg))
                alpha = if (offset < 0) 0.5f else 1f
            }

            TextView(this).apply {
                text = dateStr
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                if (isCurrent) setTypeface(null, Typeface.BOLD)
                row.addView(this)
            }

            TextView(this).apply {
                text = people[personIndex]
                if (isCurrent) {
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary_blue))
                }
                row.addView(this)
            }

            container.addView(row)
        }
    }
}
