package com.teashop.pos.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.teashop.pos.databinding.ActivityItemMasterBinding
import com.teashop.pos.ui.adapter.ItemMasterAdapter
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ItemMasterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemMasterBinding
    private val viewModel: ItemMasterViewModel by viewModels()
    private lateinit var adapter: ItemMasterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemMasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        // Updated to 2 columns as requested
        binding.rvItems.layoutManager = GridLayoutManager(this, 2)
        binding.rvItems.adapter = adapter

        binding.fabAddItem.setOnClickListener { 
            AddItemDialogFragment.newInstance().show(supportFragmentManager, AddItemDialogFragment.TAG)
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
