package pl.radoslaw.zmywarka

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import pl.radoslaw.zmywarka.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }
}
