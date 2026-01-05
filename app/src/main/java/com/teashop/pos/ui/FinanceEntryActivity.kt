package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Item
import com.teashop.pos.databinding.ActivityFinanceEntryBinding
import com.teashop.pos.ui.viewmodel.FinanceEntryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinanceEntryBinding
    private lateinit var viewModel: FinanceEntryViewModel
    private var itemList: List<Item> = emptyList()
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[FinanceEntryViewModel::class.java]

        val shopId = intent.getStringExtra("SHOP_ID")
        if (shopId.isNullOrEmpty()) {
            Toast.makeText(this, "Shop ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        observeViewModel()
        setupUI(shopId)
    }

    private fun setupUI(shopId: String) {
        val categories = arrayOf("DAILY SALES (CASH)", "DAILY SALES (QR)", "OPENING CASH", "PURCHASE", "SALARY", "ADVANCE", "EB/GAS", "RENT", "MAINTENANCE", "OTHER")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actCategory.setAdapter(categoryAdapter)

        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.btnPickDate.text = "Transaction Date: ${format.format(selectedDate.time)}"
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCashIn.setOnClickListener { 
            val category = binding.actCategory.text.toString()
            recordFinance(shopId, "IN", category) 
        }
        
        binding.btnCashOut.setOnClickListener { 
            val category = binding.actCategory.text.toString()
            recordFinance(shopId, "OUT", category) 
        }

        binding.btnUpdateStock.setOnClickListener {
            val qty = binding.etQty.text.toString().toDoubleOrNull() ?: 0.0
            val selectedItemName = binding.actItems.text.toString()
            val selectedItem = itemList.find { it.name == selectedItemName }

            if (qty != 0.0 && selectedItem != null) {
                viewModel.updateStock(shopId, selectedItem.itemId, qty, "ADJUSTMENT", selectedDate.timeInMillis)
                Toast.makeText(this, "Stock Recorded for ${selectedItem.name}", Toast.LENGTH_SHORT).show()
                binding.etQty.text?.clear()
            } else {
                Toast.makeText(this, "Please select a valid item", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun recordFinance(shopId: String, type: String, category: String) {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = binding.etDescription.text.toString()

        if (category == "OTHER" && desc.isBlank()) {
            binding.tilDescription.error = "Description is required for OTHER"
            return
        } else {
            binding.tilDescription.error = null
        }

        if (amount > 0 && category.isNotEmpty()) {
            viewModel.addFinancialEntry(shopId, amount, type, category, desc, selectedDate.timeInMillis)
            Toast.makeText(this, "$category Recorded: ₹$amount", Toast.LENGTH_SHORT).show()
            binding.etAmount.text?.clear()
            binding.etDescription.text?.clear()
            binding.actCategory.text.clear()
        } else {
            Toast.makeText(this, "Please enter a valid amount and category", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    itemList = items
                    val itemNames = items.map { it.name }
                    val itemAdapter = ArrayAdapter(this@FinanceEntryActivity, android.R.layout.simple_dropdown_item_1line, itemNames)
                    binding.actItems.setAdapter(itemAdapter)
                }
            }
        }
    }
}
