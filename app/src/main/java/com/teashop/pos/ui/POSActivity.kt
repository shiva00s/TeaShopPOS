package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.teashop.pos.R
import com.teashop.pos.data.entity.ShopMenuItem
import com.teashop.pos.databinding.ActivityPosBinding
import com.teashop.pos.ui.adapter.CartAdapter
import com.teashop.pos.ui.adapter.CategoryAdapter
import com.teashop.pos.ui.adapter.MenuAdapter
import com.teashop.pos.ui.viewmodel.POSViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class POSActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPosBinding
    private val viewModel: POSViewModel by viewModels()

    private lateinit var menuAdapter: MenuAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var cartAdapter: CartAdapter
    private var shopId: String? = null
    private var tableCount: Int = 0
    private var allMenuItems = emptyList<ShopMenuItem>()
    private val selectedCategories = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        tableCount = intent.getIntExtra("TABLE_COUNT", 0)
        
        shopId?.let { viewModel.setShop(it) }
        binding.toolbar.title = intent.getStringExtra("SHOP_NAME") ?: "POS"
        setSupportActionBar(binding.toolbar)

        setupUI()
        setupTableSelection()
        observeViewModel()
    }

    private fun setupUI() {
        categoryAdapter = CategoryAdapter { category ->
            if (category == null) {
                selectedCategories.clear()
            } else {
                if (selectedCategories.contains(category)) {
                    selectedCategories.remove(category)
                } else {
                    selectedCategories.add(category)
                }
            }
            updateCategoryList()
            filterMenu()
        }
        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        binding.rvCategories.adapter = categoryAdapter

        menuAdapter = MenuAdapter { viewModel.addToCart(it) }
        binding.rvMenu.layoutManager = GridLayoutManager(this, 4) 
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
        
        binding.btnStanding.setOnClickListener {
            viewModel.setServiceType("STANDING", null)
        }

        binding.btnViewHeld.setOnClickListener {
            HeldOrdersDialogFragment.newInstance().show(supportFragmentManager, HeldOrdersDialogFragment.TAG)
        }
    }

    private fun setupTableSelection() {
        val container = binding.glTableContainer
        val standingBtn = binding.btnStanding
        container.removeAllViews()
        
        // Dynamically set column count to fit all buttons in one row
        container.columnCount = tableCount + 1
        
        // Use a function to set common properties
        fun applyTableButtonStyle(btn: MaterialButton) {
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 40.toPx()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(2.toPx(), 4.toPx(), 2.toPx(), 4.toPx())
            }
            btn.layoutParams = params
            btn.textSize = 11f
            btn.insetTop = 0
            btn.insetBottom = 0
            btn.setPadding(0, 0, 0, 0)
        }

        applyTableButtonStyle(standingBtn)
        standingBtn.cornerRadius = 8.toPx()
        container.addView(standingBtn)
        standingBtn.text = "S"

        for (i in 1..tableCount) {
            val tableBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                applyTableButtonStyle(this)
                text = i.toString()
                cornerRadius = 20.toPx() 
                setOnClickListener {
                    viewModel.setServiceType("TABLE", i.toString())
                }
            }
            container.addView(tableBtn)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { 
                    viewModel.menuItems.collect { menu ->
                        allMenuItems = menu
                        updateCategoryList()
                        filterMenu() 
                    }
                }
                launch {
                    viewModel.cart.collect {
                        cartAdapter.submitList(it)
                        val total = it.sumOf { item -> (item.price * item.quantity) + item.parcelCharge }
                        binding.tvTotalAmount.text = String.format(Locale.getDefault(), "₹ %.2f", total)
                    }
                }
                
                launch {
                    combine(viewModel.serviceType, viewModel.tableId) { type, table ->
                        updateTableSelectionUI(type, table)
                        if (type == "TABLE") "Service: Table ${table ?: "Unknown"}" else "Service: $type"
                    }.collect { subtitle ->
                        binding.toolbar.subtitle = subtitle
                    }
                }

                launch {
                    viewModel.transactionComplete.collect { isComplete ->
                        if (isComplete) {
                            Toast.makeText(this@POSActivity, "Action Successful", Toast.LENGTH_SHORT).show()
                            viewModel.onTransactionComplete()
                        }
                    }
                }
            }
        }

        viewModel.heldOrders.observe(this) { orders ->
            binding.btnViewHeld.text = String.format(Locale.getDefault(), "H (%d)", orders.size)
        }
    }

    private fun updateCategoryList() {
        val categorySales = allMenuItems.groupBy { it.item.category }
            .mapValues { entry -> entry.value.sumOf { it.salesCount } }

        val sortedCategories = allMenuItems.map { it.item.category }
            .distinct()
            .sortedWith(compareByDescending<String> { categorySales[it] ?: 0 }.thenBy { it })

        val categories = mutableListOf<String?>()
        categories.add(null) 
        categories.addAll(sortedCategories)
        
        categoryAdapter.submitList(categories, selectedCategories)
    }

    private fun filterMenu() {
        val filtered = if (selectedCategories.isEmpty()) {
            allMenuItems
        } else {
            allMenuItems.filter { selectedCategories.contains(it.item.category) }
        }
        menuAdapter.submitList(filtered.sortedByDescending { it.salesCount })
    }

    private fun updateTableSelectionUI(selectedType: String, selectedTable: String?) {
        val container = binding.glTableContainer
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i) as? MaterialButton ?: continue
            if (view.id == R.id.btnStanding) {
                if (selectedType == "STANDING") {
                    view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    view.setTextColor(ContextCompat.getColor(this, R.color.white))
                    view.strokeWidth = 0
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    view.strokeWidth = 1.toPx()
                }
            } else {
                val tableText = view.text.toString()
                if (selectedType == "TABLE" && selectedTable == tableText) {
                    view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    view.setTextColor(ContextCompat.getColor(this, R.color.white))
                    view.strokeWidth = 0
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                    view.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    view.strokeWidth = 1.toPx()
                }
            }
        }
    }

    private fun Int.toPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.pos_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_items -> startActivity(Intent(this, ItemMasterActivity::class.java).putExtra("SHOP_ID", shopId))
        }
        return super.onOptionsItemSelected(item)
    }
}
