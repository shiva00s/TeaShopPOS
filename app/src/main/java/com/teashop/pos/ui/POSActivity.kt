package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.R
import com.teashop.pos.data.dao.ShopMenuItem
import com.teashop.pos.databinding.ActivityPosBinding
import com.teashop.pos.databinding.DialogTableSelectorBinding
import com.teashop.pos.ui.adapter.CartAdapter
import com.teashop.pos.ui.adapter.MenuAdapter
import com.teashop.pos.ui.viewmodel.POSViewModel
import kotlinx.coroutines.launch

class POSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosBinding
    private lateinit var viewModel: POSViewModel
    
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var cartAdapter: CartAdapter
    private var shopId: String? = null
    private var currentServiceType: String = "STANDING"
    private var currentTableId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return POSViewModel(app.repository) as T
            }
        })[POSViewModel::class.java]

        shopId = intent.getStringExtra("SHOP_ID")
        shopId?.let { viewModel.setShop(it) }
        binding.toolbar.title = intent.getStringExtra("SHOP_NAME") ?: "POS"
        setSupportActionBar(binding.toolbar)

        setupUI()
        observeViewModel()
        
        // Initial Table Selection
        showTableSelector()
    }

    private fun showTableSelector() {
        val dialogBinding = DialogTableSelectorBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Service")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnStanding.setOnClickListener {
            currentServiceType = "STANDING"; currentTableId = null
            binding.toolbar.subtitle = "Service: Standing"
            dialog.dismiss()
        }
        dialogBinding.btnParcel.setOnClickListener {
            currentServiceType = "PARCEL"; currentTableId = null
            binding.toolbar.subtitle = "Service: Parcel"
            dialog.dismiss()
        }

        val tables = (1..10).map { "T$it" }
        dialogBinding.rvTables.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    setPadding(20, 20, 20, 20)
                    textSize = 18sp
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    setBackgroundResource(android.R.drawable.btn_default)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = tables[position]
                holder.itemView.setOnClickListener {
                    currentServiceType = "TABLE"
                    currentTableId = tables[position]
                    binding.toolbar.subtitle = "Service: Table $currentTableId"
                    dialog.dismiss()
                }
            }
            override fun getItemCount() = tables.size
        }

        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Staff Management")
        menu?.add(0, 2, 1, "Menu & Pricing")
        menu?.add(0, 3, 2, "Cash & Stock Entry")
        menu?.add(0, 4, 3, "Reports")
        menu?.add(0, 5, 4, "Change Table")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> startActivity(Intent(this, StaffActivity::class.java).putExtra("SHOP_ID", shopId))
            2 -> startActivity(Intent(this, ItemMasterActivity::class.java).putExtra("SHOP_ID", shopId))
            3 -> startActivity(Intent(this, FinanceEntryActivity::class.java).putExtra("SHOP_ID", shopId).putExtra("SHOP_NAME", binding.toolbar.title))
            4 -> startActivity(Intent(this, ReportsActivity::class.java).putExtra("SHOP_ID", shopId))
            5 -> showTableSelector()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupUI() {
        menuAdapter = MenuAdapter { viewModel.addToCart(it) }
        binding.rvMenu.layoutManager = GridLayoutManager(this, 3)
        binding.rvMenu.adapter = menuAdapter

        cartAdapter = CartAdapter(
            onIncrease = { viewModel.addToCart(ShopMenuItem(it.item, it.price)) },
            onDecrease = { viewModel.removeFromCart(it) }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = cartAdapter

        binding.btnCheckout.setOnClickListener {
            if (viewModel.cart.value.isEmpty()) return@setOnClickListener
            showPaymentDialog()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.menuItems.collect { menuAdapter.submitList(it) } }
                launch { viewModel.cart.collect { cartItems ->
                    cartAdapter.submitList(cartItems)
                    binding.tvTotalAmount.text = String.format("₹ %.2f", cartItems.sumOf { it.price * it.quantity })
                } }
            }
        }
    }

    private fun showPaymentDialog() {
        val options = arrayOf("CASH", "ONLINE (UPI)", "CREDIT (UDHAAR)")
        AlertDialog.Builder(this)
            .setTitle("Payment for $currentServiceType ${currentTableId ?: ""}")
            .setItems(options) { _, which ->
                viewModel.checkout(options[which], currentServiceType, currentTableId)
                Toast.makeText(this, "Order Closed", Toast.LENGTH_SHORT).show()
                showTableSelector() // Ready for next table
            }
            .show()
    }
}
