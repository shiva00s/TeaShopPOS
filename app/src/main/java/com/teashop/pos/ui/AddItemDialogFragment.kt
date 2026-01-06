package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.teashop.pos.databinding.DialogAddItemBinding
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemMasterViewModel by activityViewModels()
    private var itemId: String? = null

    companion object {
        const val TAG = "AddItemDialog"
        private const val ARG_ITEM_ID = "itemId"

        fun newInstance(itemId: String? = null): AddItemDialogFragment {
            val args = Bundle().apply {
                putString(ARG_ITEM_ID, itemId)
            }
            return AddItemDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getString(ARG_ITEM_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddItemBinding.inflate(LayoutInflater.from(context))

        if (itemId != null) {
            viewModel.loadItem(itemId!!)
            lifecycleScope.launch {
                viewModel.selectedItem.collectLatest {
                    if (it != null) {
                        binding.etItemName.setText(it.name)
                        binding.etCategory.setText(it.category)
                        binding.cbHasParcelCharge.isChecked = it.hasParcelCharge
                        binding.etParcelAmount.setText(it.defaultParcelCharge.toString())
                    }
                }
            }
        }

        binding.cbHasParcelCharge.setOnCheckedChangeListener { _, isChecked ->
            binding.tilParcelAmount.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle(if (itemId == null) "Add Global Item" else "Edit Item")
            .setView(binding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = binding.etItemName.text.toString()
                val cat = binding.etCategory.text.toString()
                val hasParcel = binding.cbHasParcelCharge.isChecked
                val pAmt = binding.etParcelAmount.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    if (itemId == null) {
                        viewModel.addItemWithParcel(name, if (cat.isEmpty()) "General" else cat, hasParcel, pAmt)
                    } else {
                        viewModel.selectedItem.value?.let {
                            viewModel.updateItem(it, name, if (cat.isEmpty()) "General" else cat, hasParcel, pAmt)
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
