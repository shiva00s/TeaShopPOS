package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.DialogAddStaffBinding
import com.teashop.pos.ui.viewmodel.StaffViewModel

class AddStaffDialogFragment : DialogFragment() {

    private var _binding: DialogAddStaffBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StaffViewModel

    companion object {
        const val TAG = "AddStaffDialog"
        fun newInstance(): AddStaffDialogFragment {
            return AddStaffDialogFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(requireActivity(), factory)[StaffViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddStaffBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireActivity())
            .setTitle("New Staff (Strict Shift)")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etStaffName.text.toString()
                val phone = binding.etStaffPhone.text.toString()
                val rate = binding.etSalaryRate.text.toString().toDoubleOrNull() ?: 0.0
                val start = binding.etShiftStart.text.toString()
                val end = binding.etShiftEnd.text.toString()
                val ot = binding.etOtRate.text.toString().toDoubleOrNull() ?: 1.0
                
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    viewModel.addEmployee(name, phone, rate, "PER_HOUR", start, end, ot)
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
