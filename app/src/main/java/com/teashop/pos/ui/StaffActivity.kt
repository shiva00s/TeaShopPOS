package com.teashop.pos.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ActivityStaffBinding
import com.teashop.pos.ui.adapter.StaffAdapter
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding
    private val viewModel: StaffViewModel by viewModels()
    private lateinit var adapter: StaffAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private var employees: List<Employee> = emptyList()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        val shopName = intent.getStringExtra("SHOP_NAME")
        viewModel.setShop(shopId)
        binding.toolbar.title = shopName

        setupUI()
        observeViewModel()
        setupSpeechRecognizer()
    }

    private fun setupUI() {
        adapter = StaffAdapter(
            onAttendanceClick = { employee ->
                ManualAttendanceDialogFragment.newInstance(employee)
                    .show(supportFragmentManager, ManualAttendanceDialogFragment.TAG)
            },
            onAdvanceClick = { employee ->
                GiveAdvanceDialogFragment.newInstance(employee.employeeId)
                    .show(supportFragmentManager, GiveAdvanceDialogFragment.TAG)
            },
            onAdvanceLogsClick = { employee ->
                AdvanceLogsDialogFragment.newInstance(employee.employeeId, employee.name)
                    .show(supportFragmentManager, AdvanceLogsDialogFragment.TAG)
            },
            onSalaryClick = { employee ->
                PaySalaryDialogFragment.newInstance(employee.employeeId, employee.name)
                    .show(supportFragmentManager, PaySalaryDialogFragment.TAG)
            },
            onViewLogsClick = { employee ->
                AttendanceLogsDialogFragment.newInstance(employee.employeeId, employee.name)
                    .show(supportFragmentManager, AttendanceLogsDialogFragment.TAG)
            },
            onEditClick = { employee ->
                AddStaffDialogFragment.newInstance(employee.employeeId).show(supportFragmentManager, AddStaffDialogFragment.TAG)
            },
            onDeleteClick = { employee ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Staff?")
                    .setMessage("Are you sure you want to delete ${employee.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteEmployee(employee)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvStaff.layoutManager = LinearLayoutManager(this)
        binding.rvStaff.adapter = adapter

        binding.fabAddStaff.setOnClickListener {
            AddStaffDialogFragment.newInstance().show(supportFragmentManager, AddStaffDialogFragment.TAG)
        }

        binding.fabVoiceInput.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    processVoiceCommand(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        speechRecognizer.startListening(intent)
    }

    private fun processVoiceCommand(command: String) {
        val lowerCaseCommand = command.lowercase(Locale.getDefault())
        val employee = employees.find { lowerCaseCommand.contains(it.name.lowercase(Locale.getDefault())) }
        if (employee != null) {
            viewModel.markAttendance(employee, Date())
            Toast.makeText(this, "Attendance marked for ${employee.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Could not find an employee with that name", Toast.LENGTH_SHORT).show()
        }
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.employees.collect {
                        employees = it
                        adapter.submitList(it)
                    }
                }
                launch {
                    viewModel.employeeStats.collect { stats ->
                        adapter.updateStats(stats)
                        val totalSalary = stats.values.sumOf { it.monthlySalary }
                        binding.totalSalary.text = String.format("Total Payable Salary: ₹%.2f", totalSalary)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
