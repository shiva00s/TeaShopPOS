package com.teashop.pos.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
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
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FinanceEntryViewModel(app.repository) as T
            }
        })[FinanceEntryViewModel::class.java]

        val shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        binding.toolbar.title = "Manual Entry: Cash & Stock"
        setSupportActionBar(binding.toolbar)

        setupUI(shopId)
        observeViewModel()
    }

    private fun setupUI(shopId: String) {
        // Updated Category Spinner to handle Daily Sales specifically
        val categories = arrayOf("DAILY SALES (CASH)", "DAILY SALES (QR)", "OPENING CASH", "PURCHASE", "SALARY", "ADVANCE", "EB/GAS", "RENT", "MAINTENANCE", "OTHER")
        binding.spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        // Date Picker for past date entries (like 01-01-2026)
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                binding.btnPickDate.text = "Transaction Date: ${format.format(selectedDate.time)}"
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCashIn.setOnClickListener { 
            val category = binding.spinnerCategory.selectedItem.toString()
            recordFinance(shopId, "IN", category) 
        }
        
        binding.btnCashOut.setOnClickListener { 
            val category = binding.spinnerCategory.selectedItem.toString()
            recordFinance(shopId, "OUT", category) 
        }

        binding.btnUpdateStock.setOnClickListener {
            val qty = binding.etQty.text.toString().toDoubleOrNull() ?: 0.0
            val selectedItemIndex = binding.spinnerItems.selectedItemPosition
            if (qty != 0.0 && selectedItemIndex >= 0) {
                val item = itemList[selectedItemIndex]
                viewModel.updateStock(shopId, item.itemId, qty, "ADJUSTMENT", selectedDate.timeInMillis)
                Toast.makeText(this, "Stock Recorded for ${item.name}", Toast.LENGTH_SHORT).show()
                binding.etQty.text.clear()
            }
        }
    }

    private fun recordFinance(shopId: String, type: String, category: String) {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = binding.etDescription.text.toString()
        if (amount > 0) {
            viewModel.addFinancialEntry(shopId, amount, type, category, desc, selectedDate.timeInMillis)
            Toast.makeText(this, "$category Recorded: ₹$amount", Toast.LENGTH_SHORT).show()
            binding.etAmount.text.clear()
            binding.etDescription.text.clear()
        } else {
            Toast.makeText(this, "Please enter valid amount", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    itemList = items
                    val adapter = ArrayAdapter(this@FinanceEntryActivity, android.R.layout.simple_spinner_item, items.map { it.name })
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerItems.adapter = adapter
                }
            }
        }
    }
}
