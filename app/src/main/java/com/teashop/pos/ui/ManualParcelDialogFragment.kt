package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.ui.viewmodel.CartItem
import com.teashop.pos.ui.viewmodel.POSViewModel

class ManualParcelDialogFragment : DialogFragment() {

    private val viewModel: POSViewModel by activityViewModels {
        (requireActivity().application as TeaShopApplication).viewModelFactory
    }

    companion object {
        const val TAG = "ManualParcelDialog"
        private const val ARG_ITEM_ID = "itemId"

        fun newInstance(cartItem: CartItem): ManualParcelDialogFragment {
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, cartItem.item.itemId)
            }
            return ManualParcelDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemId = requireArguments().getString(ARG_ITEM_ID)
        val cartItem = viewModel.cart.value.find { it.item.itemId == itemId }
            ?: return super.onCreateDialog(savedInstanceState)

        val input = EditText(requireContext()).apply {
            hint = "Manual Parcel Charge (0 to remove)"
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(cartItem.parcelCharge.toString())
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Dynamic Parcel Control")
            .setMessage("Set amount for ${cartItem.item.name}")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val charge = input.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.updateParcelCharge(cartItem, charge)
            }
            .setNeutralButton("Remove") { _, _ ->
                viewModel.updateParcelCharge(cartItem, 0.0)
            }
            .create()
    }
}
