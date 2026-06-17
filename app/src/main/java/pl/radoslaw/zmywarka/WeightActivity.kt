package pl.radoslaw.zmywarka

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import pl.radoslaw.zmywarka.databinding.ActivityWeightBinding
import java.time.LocalDate
import java.util.Calendar

class WeightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeightBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val userId = "radek"
    private val appId = "weight-tracker-cloud"

    private var cachedEntries: List<Triple<String, Double, Long?>> = emptyList()
    private var initialPrefillDone = false

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val dx = e2.x - (e1?.x ?: return false)
                val dy = e2.y - (e1.y)
                if (abs(dx) > abs(dy) * 1.5f && dx > 100f && abs(velocityX) > 300f) {
                    navigateBack()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { navigateBack() }
        })

        binding.etDate.setText(LocalDate.now().toString())
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnSave.setOnClickListener { saveEntry() }

        signInAndListen()
    }

    private fun showDatePicker() {
        val current = binding.etDate.text.toString().let {
            runCatching { LocalDate.parse(it) }.getOrDefault(LocalDate.now())
        }
        val cal = Calendar.getInstance().apply {
            set(current.year, current.monthValue - 1, current.dayOfMonth)
        }
        DatePickerDialog(this, { _, y, m, d ->
            val picked = LocalDate.of(y, m + 1, d).toString()
            binding.etDate.setText(picked)
            prefillFormForDate(picked)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun prefillFormForDate(dateStr: String) {
        val entry = cachedEntries.find { it.first == dateStr }
        if (entry != null) {
            binding.etWeight.setText("%.1f".format(entry.second))
            binding.etCalories.setText(entry.third?.toString() ?: "")
            binding.btnSave.text = getString(R.string.weight_update)
        } else {
            binding.etWeight.text?.clear()
            binding.etCalories.text?.clear()
            binding.btnSave.text = getString(R.string.weight_save)
        }
    }

    private fun signInAndListen() {
        if (auth.currentUser != null) {
            listenToEntries()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { listenToEntries() }
            .addOnFailureListener { showToast("Błąd połączenia z Firebase") }
    }

    private fun saveEntry() {
        val date = binding.etDate.text.toString().trim()
        val weight = binding.etWeight.text.toString().replace(',', '.').toDoubleOrNull()
        val calories = binding.etCalories.text.toString().toLongOrNull()

        if (date.isEmpty() || weight == null) {
            showToast("Wprowadź datę i wagę")
            return
        }

        val data = mutableMapOf<String, Any>(
            "date" to date,
            "weight" to weight,
            "timestamp" to System.currentTimeMillis()
        )
        calories?.let { data["calories"] = it }

        weightsRef().document(date).set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { showToast("Zapisano!") }
            .addOnFailureListener { showToast("Błąd zapisu") }
    }

    private fun listenToEntries() {
        val cutoff = LocalDate.now().minusDays(30).toString()
        weightsRef()
            .whereGreaterThanOrEqualTo("date", cutoff)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                cachedEntries = snapshot?.documents?.mapNotNull { doc ->
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val weight = doc.getDouble("weight") ?: return@mapNotNull null
                    Triple(date, weight, doc.getLong("calories"))
                } ?: emptyList()

                if (!initialPrefillDone && snapshot != null) {
                    initialPrefillDone = true
                    prefillFormForDate(binding.etDate.text.toString())
                }
                renderList(cachedEntries)
            }
    }

    private fun renderList(entries: List<Triple<String, Double, Long?>>) {
        val container = binding.listContainer
        container.removeAllViews()

        val ink = ContextCompat.getColor(this, R.color.ink)
        val inkSoft = ContextCompat.getColor(this, R.color.ink_soft)
        val ochreDeep = ContextCompat.getColor(this, R.color.ochre_deep)
        val fraunces = ResourcesCompat.getFont(this, R.font.fraunces_semibold)
        val italic = ResourcesCompat.getFont(this, R.font.fraunces_italic)
        val mono = ResourcesCompat.getFont(this, R.font.dm_mono)
        val monoMed = ResourcesCompat.getFont(this, R.font.dm_mono_medium)

        if (entries.isEmpty()) {
            TextView(this).apply {
                text = getString(R.string.weight_empty)
                typeface = italic
                setTextColor(inkSoft)
                textSize = 16f
                setPadding(0, dp(16), 0, dp(8))
                container.addView(this)
            }
            return
        }

        entries.forEach { (date, weight, calories) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(this@WeightActivity, R.drawable.row_rule)
                setPadding(0, dp(15), 0, dp(15))
            }

            TextView(this).apply {
                text = date
                typeface = mono
                setTextColor(inkSoft)
                textSize = 13f
                letterSpacing = 0.02f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(this)
            }

            TextView(this).apply {
                text = "%.1f".format(weight)
                typeface = fraunces
                setTextColor(ink)
                textSize = 23f
                row.addView(this)
            }

            TextView(this).apply {
                text = " kg"
                typeface = mono
                setTextColor(inkSoft)
                textSize = 12f
                row.addView(this)
            }

            if (calories != null) {
                TextView(this).apply {
                    text = "$calories kcal"
                    typeface = monoMed
                    setTextColor(ochreDeep)
                    textSize = 11f
                    background = ContextCompat.getDrawable(this@WeightActivity, R.drawable.pill_ochre)
                    setPadding(dp(10), dp(4), dp(10), dp(5))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dp(12) }
                    row.addView(this)
                }
            }

            container.addView(row)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun weightsRef() = db
        .collection("artifacts").document(appId)
        .collection("users").document(userId)
        .collection("weights")

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            navigateBack()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateBack() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
