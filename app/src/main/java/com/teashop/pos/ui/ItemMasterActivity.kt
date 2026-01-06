package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
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
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[ItemMasterViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = ItemMasterAdapter(
            onSetPriceClick = { item ->
                val shopId = intent.getStringExtra("SHOP_ID") ?: return@ItemMasterAdapter
                SetPriceDialogFragment.newInstance(item.itemId, item.name, shopId)
                    .show(supportFragmentManager, SetPriceDialogFragment.TAG)
            },
            onEditClick = { item ->
                AddItemDialogFragment.newInstance(item.itemId).show(supportFragmentManager, AddItemDialogFragment.TAG)
            },
            onDeleteClick = { item ->
                viewModel.deleteItem(item)
            }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(this)
        binding.rvItems.adapter = adapter

        binding.fabAddItem.setOnClickListener { 
            AddItemDialogFragment.newInstance().show(supportFragmentManager, AddItemDialogFragment.TAG)
        }

        binding.fabScanMenu.setOnClickListener {
            val shopId = intent.getStringExtra("SHOP_ID")
            startActivity(Intent(this, ScanMenuActivity::class.java).putExtra("SHOP_ID", shopId))
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
}
