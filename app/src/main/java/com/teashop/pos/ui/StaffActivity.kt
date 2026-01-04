package com.teashop.pos.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.ActivityStaffBinding
import com.teashop.pos.databinding.DialogManualAttendanceBinding
import com.teashop.pos.ui.adapter.StaffAdapter
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch
import java.util.*

class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding
    private lateinit var viewModel: StaffViewModel
    private lateinit var adapter: StaffAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StaffViewModel(app.repository) as T
            }
        })[StaffViewModel::class.java]

        val shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        viewModel.setShop(shopId)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = StaffAdapter(
            onAttendanceClick = { employee -> showManualAttendanceDialog(employee) },
            onAdvanceClick = { employee -> showAdvanceDialog(employee) },
            onSalaryClick = { employee -> showPaySalaryDialog(employee) }
        )
        binding.rvStaff.layoutManager = LinearLayoutManager(this)
        binding.rvStaff.adapter = adapter

        binding.fabAddStaff.setOnClickListener { showAddStaffDialog() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.employees.collect { adapter.submitList(it) }
            }
        }
    }

    private fun showManualAttendanceDialog(employee: Employee) {
        val dialogBinding = DialogManualAttendanceBinding.inflate(LayoutInflater.from(this))
        val inCal = Calendar.getInstance()
        val outCal = Calendar.getInstance()

        dialogBinding.btnInTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                inCal.set(Calendar.HOUR_OF_DAY, h); inCal.set(Calendar.MINUTE, m)
                dialogBinding.btnInTime.text = "IN: $h:$m"
            }, 10, 0, false).show()
        }

        dialogBinding.btnOutTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                outCal.set(Calendar.HOUR_OF_DAY, h); outCal.set(Calendar.MINUTE, m)
                dialogBinding.btnOutTime.text = "OUT: $h:$m"
            }, 22, 0, false).show()
        }

        AlertDialog.Builder(this)
            .setTitle("Manual Time Entry: ${employee.name}")
            .setView(dialogBinding.root)
            .setPositiveButton("Save Entry") { _, _ ->
                viewModel.addManualAttendance(
                    employee,
                    inCal.timeInMillis,
                    outCal.timeInMillis,
                    dialogBinding.rbGap.isChecked
                )
                Toast.makeText(this, "Attendance Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddStaffDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New Staff (Strict Shift)")
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        
        val nameInput = EditText(this).apply { hint = "Full Name" }
        val rateInput = EditText(this).apply { hint = "Rate (per hour/month)" }
        val startInput = EditText(this).apply { hint = "Shift Start (e.g. 10:00)"; setText("10:00") }
        val endInput = EditText(this).apply { hint = "Shift End (e.g. 22:00)"; setText("22:00") }
        val otRateInput = EditText(this).apply { hint = "OT Rate (e.g. 1.5)"; setText("1.0") }

        layout.addView(nameInput)
        layout.addView(rateInput)
        layout.addView(startInput)
        layout.addView(endInput)
        layout.addView(otRateInput)
        
        builder.setView(layout)
        builder.setPositiveButton("Save") { _, _ ->
            val rate = rateInput.text.toString().toDoubleOrNull() ?: 0.0
            val ot = otRateInput.text.toString().toDoubleOrNull() ?: 1.0
            viewModel.addEmployee(nameInput.text.toString(), rate, "PER_HOUR", startInput.text.toString(), endInput.text.toString(), ot)
        }
        builder.show()
    }

    private fun showAdvanceDialog(employee: Employee) {
        val amountInput = EditText(this).apply { hint = "Advance Amount (₹)" }
        AlertDialog.Builder(this)
            .setTitle("Give Advance")
            .setView(amountInput)
            .setPositiveButton("Pay") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) viewModel.giveAdvance(employee.employeeId, amount)
            }
            .show()
    }

    private fun showPaySalaryDialog(employee: Employee) {
        AlertDialog.Builder(this)
            .setTitle("Settle Monthly Salary")
            .setMessage("Calculate work hours, OT, and Late Deductions for ${employee.name}?")
            .setPositiveButton("Process Pay") { _, _ ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                viewModel.calculateAndPaySalary(employee, cal.timeInMillis, System.currentTimeMillis())
                Toast.makeText(this, "Salary Disbursed", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
