package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.teashop.pos.R
import com.teashop.pos.databinding.DialogAddStaffBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddStaffDialogFragment : DialogFragment() {

    private var _binding: DialogAddStaffBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StaffViewModel
    private var employeeId: String? = null
    
    private var hireDate: Long = System.currentTimeMillis()
    private var terminateDate: Long? = null
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    companion object {
        const val TAG = "AddStaffDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"

        fun newInstance(employeeId: String? = null): AddStaffDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
            }
            return AddStaffDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[StaffViewModel::class.java]
        employeeId = arguments?.getString(ARG_EMPLOYEE_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddStaffBinding.inflate(LayoutInflater.from(context))

        setupDatePickers()

        if (employeeId != null) {
            viewModel.loadEmployee(employeeId!!)
            lifecycleScope.launch {
                viewModel.selectedEmployee.collectLatest { employee ->
                    employee?.let {
                        binding.etStaffName.setText(it.name)
                        binding.etStaffPhone.setText(it.phone)
                        binding.etSalaryRate.setText(it.salaryRate.toString())
                        binding.etShiftStart.setText(it.shiftStart)
                        binding.etShiftEnd.setText(it.shiftEnd)
                        binding.etBreakHours.setText(it.breakHours.toString())
                        binding.etOtRate.setText(it.otRateMultiplier.toString())
                        
                        hireDate = it.hireDate
                        terminateDate = it.terminateDate
                        updateDateFields()

                        if (it.salaryType == "MONTHLY_FIXED") {
                            binding.toggleSalaryType.check(R.id.btnMonthly)
                        } else {
                            binding.toggleSalaryType.check(R.id.btnHourly)
                        }
                    }
                }
            }
        } else {
            updateDateFields()
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle(if (employeeId == null) "New Staff" else "Edit Staff")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etStaffName.text.toString()
                val phone = binding.etStaffPhone.text.toString()
                val rate = binding.etSalaryRate.text.toString().toDoubleOrNull() ?: 0.0
                val start = binding.etShiftStart.text.toString().ifEmpty { "10:00" }
                val end = binding.etShiftEnd.text.toString().ifEmpty { "22:00" }
                val breakHrs = binding.etBreakHours.text.toString().toDoubleOrNull() ?: 0.0
                val ot = binding.etOtRate.text.toString().toDoubleOrNull() ?: 1.0
                
                val type = if (binding.toggleSalaryType.checkedButtonId == R.id.btnMonthly) {
                    "MONTHLY_FIXED"
                } else {
                    "PER_HOUR"
                }
                
                if (name.isNotEmpty()) {
                    if (employeeId == null) {
                        viewModel.addEmployee(name, phone, rate, type, start, end, ot, breakHrs, hireDate)
                    } else {
                        viewModel.selectedEmployee.value?.let {
                            viewModel.updateEmployee(it, name, phone, rate, type, start, end, ot, breakHrs, hireDate, terminateDate)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun setupDatePickers() {
        binding.etHireDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = hireDate }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                hireDate = cal.timeInMillis
                updateDateFields()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.etTerminateDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = terminateDate ?: System.currentTimeMillis() }
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                terminateDate = cal.timeInMillis
                updateDateFields()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                    terminateDate = null
                    updateDateFields()
                }
            }.show()
        }
    }

    private fun updateDateFields() {
        binding.etHireDate.setText(dateFormat.format(Date(hireDate)))
        binding.etTerminateDate.setText(terminateDate?.let { dateFormat.format(Date(it)) } ?: "Active")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
