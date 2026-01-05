package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.ui.viewmodel.StaffViewModel

class GiveAdvanceDialogFragment : DialogFragment() {

    private lateinit var viewModel: StaffViewModel

    companion object {
        const val TAG = "GiveAdvanceDialog"
        private const val ARG_EMPLOYEE_ID = "employeeId"

        fun newInstance(employeeId: String): GiveAdvanceDialogFragment {
            val args = Bundle().apply {
                putString(ARG_EMPLOYEE_ID, employeeId)
            }
            return GiveAdvanceDialogFragment().apply {
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

        val amountInput = EditText(requireContext()).apply { 
            hint = "Advance Amount (₹)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Give Advance")
            .setView(amountInput)
            .setPositiveButton("Pay") { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    viewModel.giveAdvance(employeeId, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
