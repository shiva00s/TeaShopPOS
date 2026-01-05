package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel

class SetPriceDialogFragment : DialogFragment() {

    private val viewModel: ItemMasterViewModel by activityViewModels {
        (requireActivity().application as TeaShopApplication).viewModelFactory
    }

    companion object {
        const val TAG = "SetPriceDialog"
        private const val ARG_ITEM_ID = "itemId"
        private const val ARG_ITEM_NAME = "itemName"
        private const val ARG_SHOP_ID = "shopId"

        fun newInstance(itemId: String, itemName: String, shopId: String): SetPriceDialogFragment {
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
                putString(ARG_ITEM_NAME, itemName)
                putString(ARG_SHOP_ID, shopId)
            }
            return SetPriceDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemId = requireArguments().getString(ARG_ITEM_ID)!!
        val itemName = requireArguments().getString(ARG_ITEM_NAME)!!
        val shopId = requireArguments().getString(ARG_SHOP_ID)!!

        val priceInput = EditText(requireContext()).apply { 
            hint = "Selling Price"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Set Price for $itemName")
            .setView(priceInput)
            .setPositiveButton("Save") { _, _ ->
                val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.setPriceForShop(itemId, shopId, price)
                Toast.makeText(context, "Price Updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
