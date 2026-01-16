package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.teashop.pos.ui.viewmodel.StaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PaySalaryDialogFragment : DialogFragment() {

    private val viewModel: StaffViewModel by activityViewModels()

    companion object {
        const val TAG = "PaySalaryDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"
        private const val ARG_EMPLOYEE_NAME = "employeeName"

        fun newInstance(employeeId: String, employeeName: String): PaySalaryDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
                putString(ARG_EMPLOYEE_NAME, employeeName)
            }
            return PaySalaryDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val employeeName = requireArguments().getString(ARG_EMPLOYEE_NAME)!!

        return AlertDialog.Builder(requireActivity())
            .setTitle("Settle Monthly Salary")
            .setMessage("Calculate work hours and settle pay for $employeeName?")
            .setPositiveButton("Process Pay") { _, _ ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                viewModel.employees.value.find { it.employeeId == employeeId }?.let {
                    viewModel.calculateAndPaySalary(
                        it,
                        cal.timeInMillis,
                        System.currentTimeMillis()
                    )
                    Toast.makeText(context, "Salary Disbursed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
