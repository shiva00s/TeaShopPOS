package com.teashop.pos.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.teashop.pos.databinding.ActivityShopDashboardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShopDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopDashboardBinding
    private var shopId: String? = null
    private var shopName: String? = null
    private var tableCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID")
        shopName = intent.getStringExtra("SHOP_NAME")
        tableCount = intent.getIntExtra("TABLE_COUNT", 0)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = shopName

        setupCards()
    }

    private fun setupCards() {
        binding.cardPOS.setOnClickListener {
            startActivity(Intent(this, POSActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
                putExtra("TABLE_COUNT", tableCount)
            })
        }

        binding.cardStaff.setOnClickListener {
            startActivity(Intent(this, StaffActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            })
        }

        binding.cardFinance.setOnClickListener {
            startActivity(Intent(this, FinanceEntryActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            })
        }

        binding.cardDetailedReport.setOnClickListener {
            startActivity(Intent(this, DetailedReportActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            })
        }

        binding.cardPosReport.setOnClickListener {
            startActivity(Intent(this, PosReportActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            })
        }

        binding.cardClosedDays.setOnClickListener {
            startActivity(Intent(this, ShopClosedDaysActivity::class.java).apply {
                putExtra("SHOP_ID", shopId)
                putExtra("SHOP_NAME", shopName)
            })
        }

        binding.cardFixedExpenses.setOnClickListener {
            shopId?.let {
                val dialog = FixedExpensesDialogFragment.newInstance(it)
                dialog.show(supportFragmentManager, "FixedExpensesDialog")
            }
        }
    }
}
