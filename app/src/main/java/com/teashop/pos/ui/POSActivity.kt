package com.teashop.pos.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.R
import com.teashop.pos.data.dao.ShopMenuItem
import com.teashop.pos.databinding.ActivityPosBinding
import com.teashop.pos.ui.adapter.CartAdapter
import com.teashop.pos.ui.adapter.MenuAdapter
import com.teashop.pos.ui.viewmodel.POSViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class POSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosBinding
    private lateinit var viewModel: POSViewModel

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var cartAdapter: CartAdapter
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[POSViewModel::class.java]

        shopId = intent.getStringExtra("SHOP_ID")
        shopId?.let { viewModel.setShop(it) }
        binding.toolbar.title = intent.getStringExtra("SHOP_NAME") ?: "POS"
        setSupportActionBar(binding.toolbar)

        setupUI()
        observeViewModel()

        if (savedInstanceState == null) {
            TableSelectionDialogFragment.newInstance().show(supportFragmentManager, TableSelectionDialogFragment.TAG)
        }
    }

    private fun setupUI() {
        menuAdapter = MenuAdapter { viewModel.addToCart(it) }
        // Changed to grid with 3 columns to make the menu shorter and more compact
        binding.rvMenu.layoutManager = GridLayoutManager(this, 3) 
        binding.rvMenu.adapter = menuAdapter

        cartAdapter = CartAdapter(
            onIncrease = { cartItem -> viewModel.addToCart(ShopMenuItem(cartItem.item, cartItem.price)) },
            onDecrease = { viewModel.removeFromCart(it) },
            onParcelClick = { cartItem ->
                ManualParcelDialogFragment.newInstance(cartItem)
                    .show(supportFragmentManager, ManualParcelDialogFragment.TAG)
            }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = cartAdapter

        binding.btnCheckout.setOnClickListener {
            if (viewModel.cart.value.isEmpty()) return@setOnClickListener
            SplitPaymentDialogFragment.newInstance(viewModel.serviceType.value, viewModel.tableId.value)
                .show(supportFragmentManager, SplitPaymentDialogFragment.TAG)
        }

        binding.btnHold.setOnClickListener { 
            viewModel.holdOrder() 
        }

        binding.btnClearCart.setOnClickListener {
            viewModel.clearCart()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.menuItems.collect { menuAdapter.submitList(it) } }
                launch {
                    viewModel.cart.collect {
                        cartAdapter.submitList(it)
                        val total = it.sumOf { item -> (item.price * item.quantity) + item.parcelCharge }
                        binding.tvTotalAmount.text = String.format("₹ %.2f", total)
                    }
                }
                
                // FIXED: Combine serviceType and tableId to update header correctly
                launch {
                    combine(viewModel.serviceType, viewModel.tableId) { type, table ->
                        if (type == "TABLE") "Service: Table ${table ?: "Unknown"}" else "Service: $type"
                    }.collect { subtitle ->
                        binding.toolbar.subtitle = subtitle
                    }
                }

                launch {
                    viewModel.transactionComplete.collect { isComplete ->
                        if (isComplete) {
                            Toast.makeText(this@POSActivity, "Order Held Successfully", Toast.LENGTH_SHORT).show()
                            if (viewModel.serviceType.value != "STANDING" && viewModel.serviceType.value != "PARCEL") {
                                TableSelectionDialogFragment.newInstance().show(supportFragmentManager, TableSelectionDialogFragment.TAG)
                            }
                            viewModel.onTransactionComplete()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pos_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_held_orders -> HeldOrdersDialogFragment.newInstance().show(supportFragmentManager, HeldOrdersDialogFragment.TAG)
            R.id.action_theme -> toggleTheme()
            R.id.action_staff -> startActivity(Intent(this, StaffActivity::class.java).putExtra("SHOP_ID", shopId))
            R.id.action_items -> startActivity(Intent(this, ItemMasterActivity::class.java).putExtra("SHOP_ID", shopId))
            R.id.action_finance -> if (shopId != null) {
                startActivity(Intent(this, FinanceEntryActivity::class.java).putExtra("SHOP_ID", shopId).putExtra("SHOP_NAME", binding.toolbar.title))
            } else {
                Toast.makeText(this, "Shop not selected", Toast.LENGTH_SHORT).show()
            }
            R.id.action_reports -> startActivity(Intent(this, ReportsActivity::class.java).putExtra("SHOP_ID", shopId))
            R.id.action_change_service -> TableSelectionDialogFragment.newInstance().show(supportFragmentManager, TableSelectionDialogFragment.TAG)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleTheme() {
        val sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            sharedPreferences.edit().putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO).apply()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            sharedPreferences.edit().putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES).apply()
        }
    }
}
