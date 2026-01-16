package com.teashop.pos.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.teashop.pos.R
import com.teashop.pos.data.entity.Item
import com.teashop.pos.data.entity.Shop
import com.teashop.pos.data.entity.UserProfile
import com.teashop.pos.databinding.ActivityMainBinding
import com.teashop.pos.databinding.DialogAddShopBinding
import com.teashop.pos.ui.adapter.ShopAdapter
import com.teashop.pos.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MainActivity : androidx.appcompat.app.AppCompatActivity(), PaymentResultListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ShopAdapter
    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CHECK LOGIN STATE IMMEDIATELY
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // 2. FORCE RE-SYNC ONCE LOGGED IN
        viewModel.startRealtimeSync()

        // Preload Razorpay
        Checkout.preload(applicationContext)

        // Add default items
        val sharedPrefs = getSharedPreferences("defaults", Context.MODE_PRIVATE)
        if (!sharedPrefs.getBoolean("items_added", false)) {
            addDefaultItems()
            sharedPrefs.edit { putBoolean("items_added", true) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Initial profit calculation
        viewModel.refreshData()
    }

    private fun addDefaultItems() {
        val items = listOf(
            Item("1", "☕ Dum Tea", "Tea ☕", globalPrice = 12.0),
            Item("2", "☕ Nattu Sakkarai Tea", "Tea ☕", globalPrice = 15.0),
            Item("3", "🫚 Ginger Tea", "Tea ☕", globalPrice = 15.0),
            Item("4", "🌿 Masala Tea", "Tea ☕", globalPrice = 15.0),
            Item("5", "🍋 Lemon Tea", "Tea ☕", globalPrice = 15.0),
            Item("6", "🍋 Ginger Lemon Tea", "Tea ☕", globalPrice = 20.0),
            Item("7", "☕ Coffee", "Coffee ☕", globalPrice = 20.0),
            Item("8", "☕ Sukku Coffee", "Coffee ☕", globalPrice = 25.0),
            Item("9", "🍯 Kullad Tea", "Tea ☕", globalPrice = 20.0),
            Item("10", "🌸 Kashmiri Chai", "Tea ☕", globalPrice = 20.0),
            Item("11", "🍵 Green Tea", "Tea ☕", globalPrice = 25.0),
            Item("12", "☕ Black Tea", "Tea ☕", globalPrice = 10.0),
            Item("13", "☕ Elaichi Tea", "Tea ☕", globalPrice = 20.0),
            Item("14", "⚡ Boost", "Health Drink 💪", globalPrice = 25.0),
            Item("15", "🥛 Horlicks", "Health Drink 💪", globalPrice = 25.0),
            Item("16", "🥛 Sukku Milk", "Health Drink 💪", globalPrice = 20.0),
            Item("17", "🥛 Hot Badam Milk", "Milk Drink 🥛", globalPrice = 20.0),
            Item("18", "🌹 Chilled Rose Milk", "Flavoured Milk 🌹", globalPrice = 45.0),
            Item("19", "🧊 Chilled Badam Milk", "Flavoured Milk 🌹", globalPrice = 45.0),
            Item("20", "🥛 Milk", "Milk Drink 🥛", globalPrice = 15.0),
            Item("21", "🍳 Veg Maggie", "Maggie 🍜", globalPrice = 50.0),
            Item("22", "🍳 Egg Maggie", "Maggie 🍜", globalPrice = 60.0),
            Item("23", "🍋 Lemon", "Fresh Juice 🍹", globalPrice = 20.0),
            Item("24", "🍉 Watermelon", "Fresh Juice 🍹", globalPrice = 30.0),
            Item("25", "🍈 Muskmelon", "Fresh Juice 🍹", globalPrice = 40.0),
            Item("26", "🍊 Sathukudi", "Fresh Juice 🍹", globalPrice = 50.0),
            Item("27", "🍎 Apple", "Fresh Juice 🍹", globalPrice = 50.0),
            Item("28", "🎈 Pomegranate", "Fresh Juice 🍹", globalPrice = 60.0),
            Item("29", "🍌 Red Banana", "Fresh Juice 🍹", globalPrice = 60.0),
            Item("30", "🫐 Fig", "Fresh Juice 🍹", globalPrice = 60.0),
            Item("31", "🍦 Vanilla Shake", "Milkshake 🥤", globalPrice = 50.0),
            Item("32", "🍓 Strawberry Shake", "Milkshake 🥤", globalPrice = 50.0),
            Item("33", "🍫 Chocolate Shake", "Milkshake 🥤", globalPrice = 50.0),
            Item("34", "🥭 Mango Milkshake", "Milkshake 🥤", globalPrice = 50.0),
            Item("35", "🧈 Butterscotch Shake", "Milkshake 🥤", globalPrice = 55.0),
            Item("36", "🥛 Dairy Milk Shake", "Milkshake 🥤", globalPrice = 55.0),
            Item("37", "⭐ 5 Star Milkshake", "Milkshake 🥤", globalPrice = 55.0),
            Item("38", "🍪 Oreo Milkshake", "Milkshake 🥤", globalPrice = 60.0),
            Item("39", "🥜 Pista Milkshake", "Milkshake 🥤", globalPrice = 60.0),
            Item("40", "🍇 Blackcurrant Shake", "Milkshake 🥤", globalPrice = 60.0),
            Item("41", "🍫 KitKat Milkshake", "Milkshake 🥤", globalPrice = 70.0),
            Item("42", "🍌 Red Banana Shake", "Milkshake 🥤", globalPrice = 70.0),
            Item("43", "🌴 Dates Milkshake", "Milkshake 🥤", globalPrice = 70.0),
            Item("44", "🍋 Nannari Sarbath", "Cooler 🧊", globalPrice = 30.0),
            Item("45", "🥛 Pal Sarbath", "Cooler 🧊", globalPrice = 30.0),
            Item("46", "🍃 Lemon Mint Cooler", "Cooler 🧊", globalPrice = 35.0),
            Item("47", "🥛 Matka Lassi", "Cooler 🧊", globalPrice = 45.0),
            Item("48", "🧊 Cold Coffee", "Cooler 🧊", globalPrice = 50.0),
            Item("49", "🍹 Fizz Mojito", "Cooler 🧊", globalPrice = 55.0),
            Item("50", "🍦 Vannila", "Ice Cream 🍨", globalPrice = 40.0),
            Item("51", "🍫 Chocolate", "Ice Cream 🍨", globalPrice = 40.0),
            Item("52", "🍓 Fruit Falooda", "Ice Cream 🍨", globalPrice = 80.0),
            Item("53", "👑 Royal Falooda", "Ice Cream 🍨", globalPrice = 120.0),
            Item("54", "🍧 Falooda", "Ice Cream 🍨", globalPrice = 80.0),
            Item("55", "🍟 French Fries", "Snack 🍘", globalPrice = 50.0),
            Item("56", "😊 Smiley", "Snack 🍘", globalPrice = 50.0),
            Item("57", "🍗 Veg Nuggets", "Snack 🍘", globalPrice = 60.0),
            Item("58", "🍳 Bread Omelet", "Bread Item 🍞", globalPrice = 50.0),
            Item("59", "🧀 Cheese Bread Omelet", "Bread Item 🍞", globalPrice = 60.0),
            Item("60", "🥪 Veg Sandwich", "Sandwich 🥪", globalPrice = 40.0),
            Item("61", "🧀 Cheese Sandwich", "Sandwich 🥪", globalPrice = 60.0),
            Item("62", "🥪 Paneer Sandwich", "Sandwich 🥪", globalPrice = 70.0),
            Item("63", "🍗 Chicken Lollipop (2 pcs)", "Fried Chicken 🍗", globalPrice = 60.0),
            Item("64", "🍿 Chicken Popcorn (100 g)", "Fried Chicken 🍗", globalPrice = 80.0),
            Item("65", "🍔 Chicken Burger", "Burger 🍔", globalPrice = 100.0),
            Item("66", "🍘 Masala Vadai", "Vadai 🍘", globalPrice = 5.0),
            Item("67", "🍘 Medhu Vadai", "Vadai 🍘", globalPrice = 5.0),
            Item("68", "🍘 Vazhaikkai Bajji", "Bajji 🍘", globalPrice = 5.0),
            Item("69", "🌶️ Chilli Bajji", "Bajji 🍘", globalPrice = 5.0),
            Item("70", "🧅 Onion Bajji", "Bajji 🍘", globalPrice = 5.0),
            Item("71", "🥔 Potato Bajji", "Bajji 🍘", globalPrice = 5.0),
            Item("72", "🍘 Masala Bonda", "Bonda 🍘", globalPrice = 10.0),
            Item("73", "🍘 Kaara Bonda", "Bonda 🍘", globalPrice = 5.0),
            Item("74", "🍘 Sundal", "Snack 🍘", globalPrice = 10.0),
            Item("75", "🥟 Mini Samosa 4Pcs", "Samosa 🍘", globalPrice = 10.0),
            Item("76", "🥟 Samosa", "Samosa 🍘", globalPrice = 10.0),
            Item("77", "🥟 Samosa 1pc", "Samosa 🍘", globalPrice = 8.0),
            Item("78", "🥟 Samosa 2pcs", "Samosa 🍘", globalPrice = 15.0),
            Item("79", "🍘 Vadai 2pcs", "Vadai 🍘", globalPrice = 15.0),
            Item("80", "🍘 Vadai 1pc", "Vadai 🍘", globalPrice = 8.0),
            Item("81", "🍘 Soyasundal", "Bonda 🍘", globalPrice = 10.0),
            Item("82", "🍘 Ginger Muruppu", "Chocolate 🍘", globalPrice = 10.0),
            Item("83", "🍰 Rava Cake", "Cake 🍘", globalPrice = 10.0),
            Item("84", "🥥 Coconut Laddu", "Chocolate 🍘", globalPrice = 5.0),
            Item("85", "🍭 Lollipop", "Chocolate 🍘", globalPrice = 10.0),
            Item("86", "🍰 Brownie Britania", "Brownie 🍘", globalPrice = 0.0),
            Item("87", "🍰 Brownie Nemo", "Brownie 🍘", globalPrice = 45.0),
            Item("88", "🥖 Paalkova Bun", "Bun 🍘", globalPrice = 20.0),
            Item("89", "🍰 Banana Cake", "Cake 🍘", globalPrice = 10.0),
            Item("90", "🍰 Tea Cake", "Cake 🍘", globalPrice = 5.0),
            Item("91", "🍬 Banana Burfi", "Chocolate 🍘", globalPrice = 5.0),
            Item("92", "🥨 Peanut Bar", "Chocolate 🍘", globalPrice = 5.0),
            Item("93", "🍪 Biscuits", "Biscuits 🍘", globalPrice = 5.0),
            Item("94", "🍫 Chocolate 1rs", "Chocolate", globalPrice = 1.0),
            Item("95", "🍫 Chocolate 2rs", "Chocolate", globalPrice = 2.0),
            Item("96", "🍯 Honey Amla", "Chocolate", globalPrice = 12.0),
            Item("97", "🍯 Honey Addon", "Addon", globalPrice = 5.0),
            Item("98", "🍯 Honey 250 Gram", "Honey", globalPrice = 200.0),
            Item("99", "🍬 Nattu Sarkari", "Addon", globalPrice = 5.0),
            Item("100", "🍟 Omlet", "Egg 🍘", globalPrice = 35.0),
            Item("101", "🍟 Cutlet", "Snack 🍘", globalPrice = 10.0)
        )
        viewModel.addDefaultItems(items)
    }

    private fun setupRecyclerView() {
        adapter = ShopAdapter(
            onShopClick = { shop ->
                val intent = Intent(this, ShopDashboardActivity::class.java).apply {
                    putExtra("SHOP_ID", shop.shopId)
                    putExtra("SHOP_NAME", shop.name)
                    putExtra("TABLE_COUNT", shop.tableCount)
                }
                startActivity(intent)
            },
            onLongClick = { shop ->
                showAddShopDialog(shop)
            }
        )

        binding.rvShops.layoutManager = LinearLayoutManager(this)
        binding.rvShops.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnAddShop.setOnClickListener {
            showAddShopDialog()
        }

        binding.btnGlobalFilter.setOnClickListener {
            val periods = arrayOf("Today", "Up To Date", "Weekly", "Monthly", "Quarterly", "Annually")
            MaterialAlertDialogBuilder(this)
                .setTitle("Select Global Period")
                .setItems(periods) { _, which ->
                    val selected = periods[which]
                    selectedDate = Calendar.getInstance() // Reset to current day when changing period
                    viewModel.setFilter(selected, selectedDate.timeInMillis)
                }
                .show()
        }

        binding.cardGlobalInsights.setOnClickListener {
            val intent = Intent(this, DetailedReportActivity::class.java).apply {
                putExtra("SHOP_ID", "") // Empty string for global reports
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSubscribed.collectLatest { subscribed ->
                        if (!subscribed) showSubscriptionLock()
                    }
                }
                launch {
                    viewModel.shopsWithProfit.collect { shopsWithProfit ->
                        adapter.submitList(shopsWithProfit)
                        if (shopsWithProfit.isNotEmpty()) {
                            viewModel.setFilter("Up To Date")
                        }
                    }
                }
                launch {
                    viewModel.globalProfit.collect { total ->
                        binding.tvGlobalTotal.text = String.format("₹ %.2f", total)
                    }
                }
                launch {
                    viewModel.currentPeriod.collect { period ->
                        if (period == "Today") {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            val formattedDate = dateFormat.format(selectedDate.time)
                            binding.btnGlobalFilter.text = formattedDate
                            binding.tvGlobalPeriod.text = "NET GLOBAL PROFIT (${formattedDate.uppercase()})"
                        } else {
                            binding.btnGlobalFilter.text = period
                            binding.tvGlobalPeriod.text = "NET GLOBAL PROFIT (${period.uppercase()})"
                        }
                    }
                }
            }
        }
    }

    private fun showSubscriptionLock() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Trial Expired ⏳")
            .setMessage("Your 7-day free trial has ended. Please subscribe to continue managing your shops.")
            .setCancelable(false)
            .setPositiveButton("Subscribe Now") { _, _ ->
                startPayment()
            }
            .setNegativeButton("Logout") { _, _ ->
                logout()
            }
            .show()
    }

    private fun startPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_RSU1rXBX2q7CSJ")

        try {
            val options = JSONObject()
            options.put("name", "Tea Shop ERP Premium")
            options.put("description", "Lifetime Premium Subscription")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#4F46E5")
            options.put("currency", "INR")
            options.put("amount", "99900") // Amount in paise (999 INR)

            val user = FirebaseAuth.getInstance().currentUser
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            options.put("prefill.contact", user?.phoneNumber ?: "")

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment Successful! Unlocking Premium...", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val profile = UserProfile(
                    uid = user.uid,
                    phone = user.phoneNumber ?: "",
                    joinDate = System.currentTimeMillis(),
                    isPremium = true
                )
                viewModel.upgradeToPremium(profile)
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showAddShopDialog(existingShop: Shop? = null) {
        val dialogBinding = DialogAddShopBinding.inflate(LayoutInflater.from(this))

        if (existingShop != null) {
            dialogBinding.etShopName.setText(existingShop.name)
            dialogBinding.etLocation.setText(existingShop.location)
            dialogBinding.etTableCount.setText(existingShop.tableCount.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (existingShop == null) "Add New Shop" else "Edit Shop")
            .setView(dialogBinding.root)
            .setPositiveButton(if (existingShop == null) "Create" else "Update") { _, _ ->
                val name = dialogBinding.etShopName.text.toString()
                val location = dialogBinding.etLocation.text.toString()
                val tableCountStr = dialogBinding.etTableCount.text.toString()
                val tableCount = tableCountStr.toIntOrNull() ?: 0

                if (name.isNotEmpty()) {
                    if (existingShop == null) {
                        val newShop = Shop(
                            shopId = UUID.randomUUID().toString(),
                            name = name,
                            location = location,
                            openingDate = System.currentTimeMillis(),
                            openingCashBalance = 0.0,
                            tableCount = tableCount
                        )
                        viewModel.addShop(newShop)
                    } else {
                        viewModel.addShop(existingShop.copy(name = name, location = location, tableCount = tableCount))
                    }
                }
            }
            .setNeutralButton(if (existingShop != null) "Delete" else null) { _, _ ->
                if (existingShop != null) {
                    viewModel.deleteShop(existingShop)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_theme -> {
                val sharedPrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                val currentMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.MODE_NIGHT_NO
                } else {
                    AppCompatDelegate.MODE_NIGHT_YES
                }

                sharedPrefs.edit {
                    putInt("theme_mode", newMode)
                }
                AppCompatDelegate.setDefaultNightMode(newMode)
                return true
            }
            R.id.action_logout -> {
                logout()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
