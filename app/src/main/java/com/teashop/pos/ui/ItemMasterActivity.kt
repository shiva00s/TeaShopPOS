package com.teashop.pos.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Item
import com.teashop.pos.databinding.ActivityItemMasterBinding
import com.teashop.pos.ui.adapter.ItemMasterAdapter
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel
import kotlinx.coroutines.launch

class ItemMasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemMasterBinding
    private lateinit var viewModel: ItemMasterViewModel
    private lateinit var adapter: ItemMasterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ItemMasterViewModel(app.repository) as T
            }
        })[ItemMasterViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = ItemMasterAdapter { item ->
            showSetPriceDialog(item)
        }
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter

        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allItems.collect { items ->
                    adapter.submitList(items)
                }
            }
        }
    }

    private fun showAddItemDialog() {
        val nameInput = EditText(this).apply { hint = "Item Name" }
        AlertDialog.Builder(this)
            .setTitle("Add Global Item")
            .setView(nameInput)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                if (name.isNotEmpty()) {
                    viewModel.addItem(name, "General")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSetPriceDialog(item: Item) {
        val shopId = intent.getStringExtra("SHOP_ID") ?: return
        val priceInput = EditText(this).apply { hint = "Selling Price" }
        AlertDialog.Builder(this)
            .setTitle("Set Price for ${item.name}")
            .setView(priceInput)
            .setPositiveButton("Save") { _, _ ->
                val price = priceInput.text.toString().toDoubleOrNull() ?: 0.0
                viewModel.setPriceForShop(item.itemId, shopId, price)
                Toast.makeText(this, "Price Updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
