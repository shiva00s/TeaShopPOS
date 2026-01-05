package com.teashop.pos.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.teashop.pos.R
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Employee
import com.teashop.pos.databinding.DialogManualAttendanceBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import java.util.*

class ManualAttendanceDialogFragment : DialogFragment() {

    private var _binding: DialogManualAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StaffViewModel

    companion object {
        const val TAG = "ManualAttendanceDialog"
        private const val ARG_EMPLOYEE = "employee"

        fun newInstance(employee: Employee): ManualAttendanceDialogFragment {
            val args = Bundle().apply {
                putSerializable(ARG_EMPLOYEE, employee)
            }
            return ManualAttendanceDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(requireActivity(), factory)[StaffViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogManualAttendanceBinding.inflate(LayoutInflater.from(context))
        @Suppress("DEPRECATION")
        val employee = requireArguments().getSerializable(ARG_EMPLOYEE) as Employee

        val inCal = Calendar.getInstance()
        val outCal = Calendar.getInstance()

        fun updateDurationText() {
            val diff = outCal.timeInMillis - inCal.timeInMillis
            if (diff > 0) {
                val hours = diff / (1000 * 60 * 60)
                val mins = (diff / (1000 * 60)) % 60
                binding.tvDuration.text = getString(R.string.total_duration, hours, mins)
            } else {
                binding.tvDuration.text = getString(R.string.invalid_time_range)
            }
        }

        binding.btnInTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                inCal.set(Calendar.HOUR_OF_DAY, h); inCal.set(Calendar.MINUTE, m); inCal.set(Calendar.SECOND, 0)
                binding.btnInTime.text = getString(R.string.in_time, h, m)
                updateDurationText()
            }, 10, 0, false).show()
        }

        binding.btnOutTime.setOnClickListener {
            TimePickerDialog(requireContext(), { _, h, m ->
                outCal.set(Calendar.HOUR_OF_DAY, h); outCal.set(Calendar.MINUTE, m); outCal.set(Calendar.SECOND, 0)
                binding.btnOutTime.text = getString(R.string.out_time, h, m)
                updateDurationText()
            }, 22, 0, false).show()
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Manual Time Entry: ${employee.name}")
            .setView(binding.root)
            .setPositiveButton("Save Entry") { _, _ ->
                if (outCal.timeInMillis > inCal.timeInMillis) {
                    viewModel.addManualAttendance(
                        employee,
                        inCal.timeInMillis,
                        outCal.timeInMillis,
                        binding.rbGap.isChecked
                    )
                } else {
                    Toast.makeText(context, "Error: Out time must be after In time", Toast.LENGTH_SHORT).show()
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
