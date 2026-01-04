package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.data.entity.Shop
import com.teashop.pos.databinding.ActivityMainBinding
import com.teashop.pos.databinding.DialogAddShopBinding
import com.teashop.pos.ui.adapter.ShopAdapter
import com.teashop.pos.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: ShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)

        val app = application as TeaShopApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(app.repository, app.database.reportDao()) as T
            }
        })[MainViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ShopAdapter { shop ->
            val intent = Intent(this, POSActivity::class.java).apply {
                putExtra("SHOP_ID", shop.shopId)
                putExtra("SHOP_NAME", shop.name)
            }
            startActivity(intent)
        }
        binding.rvShops.layoutManager = LinearLayoutManager(this)
        binding.rvShops.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddShop.setOnClickListener {
            showAddShopDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allShops.collect { shops ->
                        adapter.submitList(shops)
                    }
                }
                launch {
                    viewModel.globalTodaySales.collect { total ->
                        binding.tvGlobalTotal.text = String.format("₹ %.2f", total ?: 0.0)
                    }
                }
            }
        }
    }

    private fun showAddShopDialog() {
        val dialogBinding = DialogAddShopBinding.inflate(LayoutInflater.from(this))
        
        AlertDialog.Builder(this)
            .setTitle("Add New Shop")
            .setView(dialogBinding.root)
            .setPositiveButton("Create") { _, _ ->
                val name = dialogBinding.etShopName.text.toString()
                val location = dialogBinding.etLocation.text.toString()
                if (name.isNotEmpty()) {
                    val newShop = Shop(
                        shopId = UUID.randomUUID().toString(),
                        name = name,
                        location = location,
                        openingDate = System.currentTimeMillis(),
                        openingCashBalance = 0.0
                    )
                    viewModel.addShop(newShop)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
