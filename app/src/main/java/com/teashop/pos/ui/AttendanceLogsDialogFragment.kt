package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.R
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.ItemFinanceRowBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AttendanceLogsDialogFragment : DialogFragment() {

    private lateinit var viewModel: StaffViewModel

    companion object {
        const val TAG = "AttendanceLogsDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"
        private const val ARG_EMPLOYEE_NAME = "employeeName"

        fun newInstance(employeeId: String, employeeName: String): AttendanceLogsDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
                putString(ARG_EMPLOYEE_NAME, employeeName)
            }
            return AttendanceLogsDialogFragment().apply {
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
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val employeeName = requireArguments().getString(ARG_EMPLOYEE_NAME)!!

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()

        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            setPadding(16, 16, 16, 16)
        }

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle("$employeeName's Attendance Logs")
            .setView(recyclerView)
            .setPositiveButton("Close", null)
            .create()

        lifecycleScope.launch {
            viewModel.getAttendanceRecords(employeeId, start, end).collect { records ->
                recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                        ItemFinanceRowBinding.inflate(LayoutInflater.from(p.context), p, false).root
                    ) {}
                    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                        val r = records[pos]
                        val binding = ItemFinanceRowBinding.bind(h.itemView)
                        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        val outTime = if (r.checkOutTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(r.checkOutTime) else "IN"
                        binding.tvLabel.text = getString(R.string.attendance_log_label, df.format(r.checkInTime), outTime)
                        binding.tvValue.text = getString(R.string.attendance_log_value, String.format(Locale.US, "%.1f", r.hoursWorked), r.type)
                        binding.tvValue.setTextColor(if(r.type == "GAP") 0xFFC62828.toInt() else 0xFF2E7D32.toInt())
                    }
                    override fun getItemCount() = records.size
                }
            }
        }

        return dialog
    }
}
