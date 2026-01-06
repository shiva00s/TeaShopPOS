package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.DialogAddStaffBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddStaffDialogFragment : DialogFragment() {

    private var _binding: DialogAddStaffBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StaffViewModel
    private var employeeId: String? = null

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
        val app = requireActivity().application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(requireActivity(), factory)[StaffViewModel::class.java]
        employeeId = arguments?.getString(ARG_EMPLOYEE_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddStaffBinding.inflate(LayoutInflater.from(context))

        if (employeeId != null) {
            viewModel.loadEmployee(employeeId!!)
            lifecycleScope.launch {
                viewModel.selectedEmployee.collectLatest {
                    if (it != null) {
                        binding.etStaffName.setText(it.name)
                        binding.etStaffPhone.setText(it.phone)
                        binding.etSalaryRate.setText(it.salaryRate.toString())
                        binding.etShiftStart.setText(it.shiftStart)
                        binding.etShiftEnd.setText(it.shiftEnd)
                        binding.etOtRate.setText(it.otRateMultiplier.toString())
                    }
                }
            }
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle(if (employeeId == null) "New Staff (Strict Shift)" else "Edit Staff")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etStaffName.text.toString()
                val phone = binding.etStaffPhone.text.toString()
                val rate = binding.etSalaryRate.text.toString().toDoubleOrNull() ?: 0.0
                val start = binding.etShiftStart.text.toString()
                val end = binding.etShiftEnd.text.toString()
                val ot = binding.etOtRate.text.toString().toDoubleOrNull() ?: 1.0
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    if (employeeId == null) {
                        viewModel.addEmployee(name, phone, rate, "PER_HOUR", start, end, ot)
                    } else {
                        viewModel.selectedEmployee.value?.let {
                            viewModel.updateEmployee(it, name, phone, rate, "PER_HOUR", start, end, ot)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
