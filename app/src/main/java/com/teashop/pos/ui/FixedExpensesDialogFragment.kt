package com.teashop.pos.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.data.entity.FixedExpense
import com.teashop.pos.databinding.DialogFixedExpensesBinding
import com.teashop.pos.databinding.DialogAddFixedExpenseBinding
import com.teashop.pos.ui.adapter.FixedExpenseAdapter
import com.teashop.pos.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class FixedExpensesDialogFragment : DialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var binding: DialogFixedExpensesBinding
    private lateinit var adapter: FixedExpenseAdapter
    private var shopId: String? = null

    companion object {
        private const val ARG_SHOP_ID = "shop_id"

        fun newInstance(shopId: String): FixedExpensesDialogFragment {
            val args = Bundle().apply {
                putString(ARG_SHOP_ID, shopId)
            }
            return FixedExpensesDialogFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shopId = arguments?.getString(ARG_SHOP_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogFixedExpensesBinding.inflate(LayoutInflater.from(context))

        setupRecyclerView()
        observeViewModel()

        binding.btnAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Fixed Monthly Expenses 💵")
            .setView(binding.root)
            .setPositiveButton("Close", null)
            .create()
    }

    private fun setupRecyclerView() {
        adapter = FixedExpenseAdapter { expense ->
            showAddExpenseDialog(expense)
        }
        binding.rvFixedExpenses.adapter = adapter
        binding.rvFixedExpenses.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.allFixedExpenses.map { expenses ->
                expenses.filter { it.shopId == shopId || it.shopId.isEmpty() }
            }.collect {
                adapter.submitList(it)
            }
        }
    }

    private fun showAddExpenseDialog(existingExpense: FixedExpense? = null) {
        val dialogBinding = DialogAddFixedExpenseBinding.inflate(LayoutInflater.from(context))

        existingExpense?.let {
            dialogBinding.etExpenseName.setText(it.name)
            dialogBinding.etMonthlyAmount.setText(it.monthlyAmount.toString())
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existingExpense == null) "Add Fixed Expense 📝" else "Edit Fixed Expense 📝")
            .setView(dialogBinding.root)
            .setPositiveButton(if (existingExpense == null) "Add" else "Update") { _, _ ->
                val name = dialogBinding.etExpenseName.text.toString()
                val amount = dialogBinding.etMonthlyAmount.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty()) {
                    if (existingExpense != null) {
                        viewModel.updateFixedExpense(existingExpense, amount)
                    } else {
                        val expense = FixedExpense(
                            id = UUID.randomUUID().toString(),
                            shopId = shopId ?: "",
                            name = name,
                            monthlyAmount = amount
                        )
                        viewModel.addFixedExpense(expense)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
