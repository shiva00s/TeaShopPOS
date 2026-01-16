package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.R
import com.teashop.pos.data.entity.AdvancePayment
import com.teashop.pos.databinding.DialogAttendanceLogsBinding
import com.teashop.pos.databinding.ItemFinanceRowBinding
import com.teashop.pos.databinding.ItemMonthCardBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AdvanceLogsDialogFragment : DialogFragment() {

    private lateinit var viewModel: StaffViewModel
    private var _binding: DialogAttendanceLogsBinding? = null
    private val binding get() = _binding!!

    private var currentView = "MONTHS"
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private val today = Calendar.getInstance()
    private var isFirstOpen = true

    companion object {
        const val TAG = "AdvanceLogsDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"
        private const val ARG_EMPLOYEE_NAME = "employeeName"

        fun newInstance(employeeId: String, employeeName: String): AdvanceLogsDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
                putString(ARG_EMPLOYEE_NAME, employeeName)
            }
            return AdvanceLogsDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[StaffViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAttendanceLogsBinding.inflate(LayoutInflater.from(context))
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val employeeName = requireArguments().getString(ARG_EMPLOYEE_NAME)!!

        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.btnYearToggle.text = selectedYear.toString()

        binding.btnYearToggle.setOnClickListener {
            showYearPickerDialog()
        }

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle("$employeeName's Advance Logs")
            .setView(binding.root)
            .setPositiveButton("Close", null)
            .setNegativeButton("Back", null)
            .create()

        dialog.setOnShowListener {
            val backBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            backBtn.setOnClickListener {
                if (currentView == "DAYS") {
                    showMonthsView(employeeId)
                }
            }
            
            showMonthsView(employeeId)
            
            if (isFirstOpen && selectedYear == today.get(Calendar.YEAR)) {
                isFirstOpen = false
                showDaysView(employeeId, today)
            }
        }

        return dialog
    }

    private fun showYearPickerDialog() {
        val employeeId = requireArguments().getString(ARG_EMPLOYEE_ID)!!
        val years = arrayOf("2024", "2025", "2026")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Year")
            .setItems(years) { _, which ->
                selectedYear = years[which].toInt()
                binding.btnYearToggle.text = selectedYear.toString()
                showMonthsView(employeeId)
            }
            .show()
    }

    private fun showMonthsView(employeeId: String) {
        currentView = "MONTHS"
        binding.tvCurrentViewLabel.text = "Select Month"
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        
        val monthsList = mutableListOf<Calendar>()
        val maxMonth = if (selectedYear == today.get(Calendar.YEAR)) today.get(Calendar.MONTH) else 11
        
        for (i in 0..maxMonth) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, selectedYear)
            cal.set(Calendar.MONTH, i)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            monthsList.add(cal)
        }
        monthsList.reverse()

        binding.rvLogs.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                ItemMonthCardBinding.inflate(LayoutInflater.from(p.context), p, false).root
            ) {}
            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                val cal = monthsList[pos]
                val itemBinding = ItemMonthCardBinding.bind(h.itemView)
                val df = SimpleDateFormat("MMMM", Locale.getDefault())
                itemBinding.tvMonthName.text = df.format(cal.time)
                itemBinding.root.setOnClickListener { showDaysView(employeeId, cal) }
            }
            override fun getItemCount() = monthsList.size
        }
    }

    private fun showDaysView(employeeId: String, monthCal: Calendar) {
        currentView = "DAYS"
        binding.tvCurrentViewLabel.text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
        (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.VISIBLE
        
        val start = monthCal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0)
        
        val end = monthCal.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)

        lifecycleScope.launch {
            viewModel.getAdvanceRecords(employeeId, start.timeInMillis, end.timeInMillis).collect { records ->
                if (currentView != "DAYS") return@collect
                binding.rvLogs.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                        ItemFinanceRowBinding.inflate(LayoutInflater.from(p.context), p, false).root
                    ) {}
                    override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                        val r = records[pos]
                        val itemBinding = ItemFinanceRowBinding.bind(h.itemView)
                        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        
                        itemBinding.tvDate.text = df.format(Date(r.date))
                        itemBinding.tvParticulars.text = if (r.isRecovered) "Recovered" else "Advance"
                        if (r.isRecovered) {
                            itemBinding.tvCredit.text = String.format("₹%.2f", r.amount)
                            itemBinding.tvCredit.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.green))
                            itemBinding.tvDebit.text = ""
                        } else {
                            itemBinding.tvDebit.text = String.format("₹%.2f", r.amount)
                            itemBinding.tvDebit.setTextColor(ContextCompat.getColor(h.itemView.context, R.color.red))
                            itemBinding.tvCredit.text = ""
                        }
                        
                        itemBinding.root.setOnClickListener {
                            showEditAdvanceDialog(r)
                        }

                        itemBinding.root.setOnLongClickListener {
                            AlertDialog.Builder(requireContext()).setTitle("Delete Entry?").setMessage("Delete this advance record?")
                                .setPositiveButton("Delete") { _, _ -> viewModel.deleteAdvance(r) }
                                .setNegativeButton("Cancel", null).show()
                            true
                        }
                    }
                    override fun getItemCount() = records.size
                }
            }
        }
    }

    private fun showEditAdvanceDialog(advance: AdvancePayment) {
        val input = EditText(requireContext()).apply {
            setText(advance.amount.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Advance Amount")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newAmount = input.text.toString().toDoubleOrNull() ?: return@setPositiveButton
                viewModel.updateAdvance(advance.copy(amount = newAmount))
                Toast.makeText(context, "Advance Updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
