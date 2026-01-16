package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teashop.pos.databinding.ActivityShopMenuBinding

class ShopMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopMenuBinding
    private var shopId: String? = null
    private var shopName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        shopName = intent.getStringExtra("SHOP_NAME")

        if (shopId == null) {
            finish()
            return
        }

        binding.toolbar.title = shopName ?: "Shop Menu"
        setSupportActionBar(binding.toolbar)
        
        setupGridMenu()
    }

    private fun setupGridMenu() {
        binding.cardPos.setOnClickListener {
            val intent = Intent(this, POSActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            }
            startActivity(intent)
        }

        binding.cardStaff.setOnClickListener {
            startActivity(Intent(this, StaffActivity::class.java).putExtra("SHOP_ID", shopId))
        }

        binding.cardItems.setOnClickListener {
            startActivity(Intent(this, ItemMasterActivity::class.java).putExtra("SHOP_ID", shopId))
        }

        binding.cardFinance.setOnClickListener {
            startActivity(Intent(this, FinanceEntryActivity::class.java).putExtra("SHOP_ID", shopId).putExtra("SHOP_NAME", shopName))
        }

        binding.cardReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java).putExtra("SHOP_ID", shopId))
        }
        
        // Removed change table as it is specific to POS context or can be added if needed
    }
}
