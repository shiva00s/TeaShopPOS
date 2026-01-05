package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.teashop.pos.databinding.DialogAddItemBinding
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel

class AddItemDialogFragment : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemMasterViewModel by activityViewModels()

    companion object {
        const val TAG = "AddItemDialog"
        fun newInstance(): AddItemDialogFragment {
            return AddItemDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddItemBinding.inflate(LayoutInflater.from(context))

        binding.cbHasParcelCharge.setOnCheckedChangeListener { _, isChecked ->
            binding.tilParcelAmount.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        return AlertDialog.Builder(requireActivity())
            .setTitle("Add Global Item")
            .setView(binding.root)
            .setPositiveButton("Add") { _, _ ->
                val name = binding.etItemName.text.toString()
                val cat = binding.etCategory.text.toString()
                val hasParcel = binding.cbHasParcelCharge.isChecked
                val pAmt = binding.etParcelAmount.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty()) {
                    viewModel.addItemWithParcel(name, if (cat.isEmpty()) "General" else cat, hasParcel, pAmt)
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
