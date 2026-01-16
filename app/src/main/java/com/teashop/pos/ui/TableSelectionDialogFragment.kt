package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.teashop.pos.databinding.DialogTableSelectorBinding
import com.teashop.pos.ui.adapter.TableGridAdapter
import com.teashop.pos.ui.viewmodel.POSViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TableSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogTableSelectorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: POSViewModel by activityViewModels()

    companion object {
        const val TAG = "TableSelectionDialog"

        fun newInstance(): TableSelectionDialogFragment {
            return TableSelectionDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTableSelectorBinding.inflate(LayoutInflater.from(context))

        binding.btnStanding.setOnClickListener {
            viewModel.setServiceType("STANDING", null)
            dismiss()
        }
        binding.btnParcel.setOnClickListener {
            viewModel.setServiceType("PARCEL", null)
            dismiss()
        }

        val tables = (1..10).map { "T$it" }
        val adapter = TableGridAdapter(tables) { tableId ->
            viewModel.setServiceType("TABLE", tableId.replace("T", ""))
            dismiss()
        }

        binding.rvTables.layoutManager = GridLayoutManager(context, 3) 
        binding.rvTables.adapter = adapter

        return AlertDialog.Builder(requireActivity())
            .setTitle("Select Service")
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
