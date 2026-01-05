package com.teashop.pos.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.ActivityStaffBinding
import com.teashop.pos.ui.adapter.StaffAdapter
import com.teashop.pos.ui.viewmodel.StaffViewModel
import kotlinx.coroutines.launch

class StaffActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffBinding
    private lateinit var viewModel: StaffViewModel
    private lateinit var adapter: StaffAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[StaffViewModel::class.java]

        val shopId = intent.getStringExtra("SHOP_ID") ?: return finish()
        viewModel.setShop(shopId)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = StaffAdapter(
            onAttendanceClick = { employee -> 
                ManualAttendanceDialogFragment.newInstance(employee)
                    .show(supportFragmentManager, ManualAttendanceDialogFragment.TAG)
            },
            onAdvanceClick = { employee -> 
                GiveAdvanceDialogFragment.newInstance(employee.employeeId)
                    .show(supportFragmentManager, GiveAdvanceDialogFragment.TAG) 
            },
            onSalaryClick = { employee -> 
                PaySalaryDialogFragment.newInstance(employee.employeeId, employee.name)
                    .show(supportFragmentManager, PaySalaryDialogFragment.TAG) 
            },
            onViewLogsClick = { employee -> 
                AttendanceLogsDialogFragment.newInstance(employee.employeeId, employee.name)
                    .show(supportFragmentManager, AttendanceLogsDialogFragment.TAG)
            }
        )
        binding.rvStaff.layoutManager = LinearLayoutManager(this)
        binding.rvStaff.adapter = adapter

        binding.fabAddStaff.setOnClickListener { 
            AddStaffDialogFragment.newInstance().show(supportFragmentManager, AddStaffDialogFragment.TAG)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.employees.collect { adapter.submitList(it) }
                }
                launch {
                    viewModel.employeeStats.collect { stats ->
                        adapter.updateStats(stats)
                    }
                }
            }
        }
    }
}
