package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.teashop.pos.R
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.DialogSplitPaymentBinding
import com.teashop.pos.ui.viewmodel.POSViewModel

class SplitPaymentDialogFragment : DialogFragment() {

    private var _binding: DialogSplitPaymentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: POSViewModel

    companion object {
        const val TAG = "SplitPaymentDialog"
        private const val ARG_SERVICE_TYPE = "serviceType"
        private const val ARG_TABLE_ID = "tableId"

        fun newInstance(serviceType: String, tableId: String?): SplitPaymentDialogFragment {
            val args = Bundle().apply {
                putString(ARG_SERVICE_TYPE, serviceType)
                putString(ARG_TABLE_ID, tableId)
            }
            return SplitPaymentDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = requireActivity().application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(requireActivity(), factory)[POSViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogSplitPaymentBinding.inflate(LayoutInflater.from(context))
        val serviceType = requireArguments().getString(ARG_SERVICE_TYPE, "STANDING")
        val tableId = requireArguments().getString(ARG_TABLE_ID)

        val totalDue = viewModel.cart.value.sumOf { (it.price * it.quantity) + it.parcelCharge }
        binding.tvTotalDue.text = "Total Due: ₹ %.2f".format(totalDue)

        val cashEditText = binding.etCashAmount
        val qrEditText = binding.etQrAmount
        val remainingTextView = binding.tvRemaining

        val neutralColor = remainingTextView.currentTextColor
        val errorColor = ContextCompat.getColor(requireContext(), R.color.red)
        val successColor = ContextCompat.getColor(requireContext(), R.color.green)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val cash = cashEditText.text.toString().toDoubleOrNull() ?: 0.0
                val qr = qrEditText.text.toString().toDoubleOrNull() ?: 0.0
                val remaining = totalDue - (cash + qr)
                remainingTextView.text = "Remaining: ₹ %.2f".format(remaining)

                if (remaining < -0.01) {
                    remainingTextView.setTextColor(errorColor)
                } else if (remaining > 0.01) {
                    remainingTextView.setTextColor(neutralColor)
                } else {
                    remainingTextView.setTextColor(successColor)
                }
            }
        }

        cashEditText.addTextChangedListener(textWatcher)
        qrEditText.addTextChangedListener(textWatcher)

        cashEditText.setText("")
        qrEditText.setText("")

        val dialog = AlertDialog.Builder(requireActivity())
            .setTitle("Payment Checkout")
            .setView(binding.root)
            .setPositiveButton("Complete Order", null) 
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener { 
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener { 
                val cash = cashEditText.text.toString().toDoubleOrNull() ?: 0.0
                val qr = qrEditText.text.toString().toDoubleOrNull() ?: 0.0
                val totalPaid = cash + qr
                val remaining = totalDue - totalPaid

                if (remaining > -0.01 && remaining < 0.01) {
                    viewModel.checkoutSplit(mapOf("CASH" to cash, "QR" to qr), serviceType, tableId)
                    dismiss()
                } else {
                    Toast.makeText(context, "Error: Paid amount must match Total Due.", Toast.LENGTH_LONG).show()
                }
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
