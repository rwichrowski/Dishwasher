package pl.radoslaw.zmywarka

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        weightsRef().document(date).set(data)
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
        val d = resources.displayMetrics.density
        entries.forEach { (date, weight, calories) ->
            val cal = if (calories != null) "  ·  $calories kcal" else ""
            TextView(this).apply {
                text = "$date   ${"%.1f".format(weight)} kg$cal"
                textSize = 14f
                setPadding(0, (12 * d).toInt(), 0, (12 * d).toInt())
                container.addView(this)
            }
        }
    }

    private fun weightsRef() = db
        .collection("artifacts").document(appId)
        .collection("users").document(userId)
        .collection("weights")

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
