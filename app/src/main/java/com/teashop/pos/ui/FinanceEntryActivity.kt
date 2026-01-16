package com.teashop.pos.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teashop.pos.R
import com.teashop.pos.data.entity.Cashbook
import com.teashop.pos.databinding.ActivityFinanceEntryBinding
import com.teashop.pos.ui.viewmodel.FinanceEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class FinanceEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinanceEntryBinding
    private val viewModel: FinanceEntryViewModel by viewModels()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedEntryForEdit: Cashbook? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recentEntriesAdapter: RecentEntriesAdapter

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val shopId = intent.getStringExtra("SHOP_ID")
        val shopName = intent.getStringExtra("SHOP_NAME")

        if (shopId.isNullOrEmpty()) {
            Toast.makeText(this, "Shop ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        viewModel.setShopId(shopId)
        binding.toolbar.title = shopName

        observeViewModel()
        setupUI(shopId)
        setupSpeechRecognizer()
    }

    private fun setupUI(shopId: String) {
        val categories = arrayOf("DAILY SALES (CASH)", "DAILY SALES (QR)", "OPENING CASH", "PURCHASE", "SALARY", "ADVANCE", "EB/GAS", "RENT", "MAINTENANCE", "OTHER")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actCategory.setAdapter(categoryAdapter)

        recentEntriesAdapter = RecentEntriesAdapter(::onEntryClick, ::onEntryLongClick)
        binding.rvRecentEntries.layoutManager = LinearLayoutManager(this)
        binding.rvRecentEntries.adapter = recentEntriesAdapter

        updateDateButton()

        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateButton()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPrevDate.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateButton()
        }

        binding.btnNextDate.setOnClickListener {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateButton()
        }

        binding.btnCashIn.setOnClickListener { 
            val category = binding.actCategory.text.toString()
            recordFinance(shopId, "IN", category) 
        }
        
        binding.btnCashOut.setOnClickListener { 
            val category = binding.actCategory.text.toString()
            recordFinance(shopId, "OUT", category) 
        }

        binding.btnUpdate.setOnClickListener { 
            selectedEntryForEdit?.let { entry ->
                val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val desc = binding.etDescription.text.toString()
                val category = binding.actCategory.text.toString()
                val type = if(binding.btnCashIn.visibility == View.VISIBLE) "IN" else "OUT"
                viewModel.updateFinancialEntry(entry, amount, type, category, desc, selectedDate.timeInMillis)
                clearForm()
            }
        }

        binding.btnClear.setOnClickListener { clearForm() }

        binding.fabVoiceInput.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun onEntryClick(entry: Cashbook) {
        selectedEntryForEdit = entry
        binding.etAmount.setText(entry.amount.toString())
        binding.actCategory.setText(entry.category, false)
        binding.etDescription.setText(entry.description)
        binding.llActionButtons.visibility = View.GONE
        binding.llEditButtons.visibility = View.VISIBLE
        binding.tvCardTitle.text = "Edit Entry"
    }

    private fun onEntryLongClick(entry: Cashbook) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry?")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFinancialEntry(entry)
                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearForm() {
        selectedEntryForEdit = null
        binding.etAmount.text?.clear()
        binding.etDescription.text?.clear()
        binding.actCategory.text.clear()
        binding.llActionButtons.visibility = View.VISIBLE
        binding.llEditButtons.visibility = View.GONE
        binding.tvCardTitle.text = "Cash In and Out"
    }

    private fun updateDateButton() {
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        binding.btnPickDate.text = "Transaction Date: ${format.format(selectedDate.time)}"
        viewModel.setDate(selectedDate.time)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {}
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    processVoiceCommand(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        speechRecognizer.startListening(intent)
    }

    private fun processVoiceCommand(command: String) {
        // Basic parsing logic, can be improved with more sophisticated NLP
        val parts = command.split(" ")
        var amount: Double? = null
        var category: String? = null

        // Look for amount
        for (part in parts) {
            val num = part.toDoubleOrNull()
            if (num != null) {
                amount = num
                break
            }
        }

        // Look for category and description
        // This is a very basic implementation and can be improved
        if (command.contains("purchase")) category = "PURCHASE"
        if (command.contains("salary")) category = "SALARY"
        if (command.contains("advance")) category = "ADVANCE"

        binding.etDescription.setText(command)

        amount?.let { binding.etAmount.setText(it.toString()) }
        category?.let { binding.actCategory.setText(it) }
    }

    private fun recordFinance(shopId: String, type: String, category: String) {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = binding.etDescription.text.toString()

        if (category == "OTHER" && desc.isBlank()) {
            binding.tilDescription.error = "Description is required for OTHER"
            return
        } else {
            binding.tilDescription.error = null
        }

        if (amount > 0 && category.isNotEmpty()) {
            if (selectedEntryForEdit == null) {
                viewModel.addFinancialEntry(shopId, amount, type, category, desc, selectedDate.timeInMillis)
                Toast.makeText(this, "$category Recorded: ₹$amount", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.updateFinancialEntry(selectedEntryForEdit!!, amount, type, category, desc, selectedDate.timeInMillis)
                Toast.makeText(this, "$category Updated: ₹$amount", Toast.LENGTH_SHORT).show()
            }
            clearForm()
        } else {
            Toast.makeText(this, "Please enter a valid amount and category", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.recentPurchaseDescriptions.collect { descs ->
                        val descAdapter = ArrayAdapter(this@FinanceEntryActivity, android.R.layout.simple_dropdown_item_1line, descs)
                        binding.etDescription.setAdapter(descAdapter)
                    }
                }
                launch {
                    viewModel.recentEntries.collect { entries ->
                        recentEntriesAdapter.submitList(entries)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedEntry.collect { entry ->
                    if (entry != null && selectedEntryForEdit == null) {
                        onEntryClick(entry)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    inner class RecentEntriesAdapter(
        private val onItemClick: (Cashbook) -> Unit,
        private val onLongClick: (Cashbook) -> Unit
    ) : RecyclerView.Adapter<RecentEntriesAdapter.ViewHolder>() {
        private var items = listOf<Cashbook>()

        fun submitList(newItems: List<Cashbook>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDate: TextView = itemView.findViewById(R.id.tvDate)
            val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
            val tvParticulars: TextView = itemView.findViewById(R.id.tvParticulars)
            val tvDebit: TextView = itemView.findViewById(R.id.tvDebit)
            val tvCredit: TextView = itemView.findViewById(R.id.tvCredit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_finance_row, parent, false)
            return ViewHolder(view)
        }
        
        private fun getEmojiForEntry(category: String, description: String?): String {
            val searchText = "$category ${description.orEmpty()}"
            return when {
                searchText.contains("MILK", ignoreCase = true) -> "🥛"
                searchText.contains("BREAD", ignoreCase = true) -> "🍞"
                searchText.contains("FOOD", ignoreCase = true) -> "🍔"
                category.equals("DAILY SALES (CASH)", ignoreCase = true) -> "💵"
                category.equals("DAILY SALES (QR)", ignoreCase = true) -> "📱"
                category.equals("OPENING CASH", ignoreCase = true) -> "💰"
                category.equals("PURCHASE", ignoreCase = true) -> "🛒"
                category.equals("SALARY", ignoreCase = true) -> "🧑‍💼"
                category.equals("ADVANCE", ignoreCase = true) -> "💸"
                category.equals("RENT", ignoreCase = true) -> "🏠"
                category.equals("EB/GAS", ignoreCase = true) -> "💡"
                category.equals("MAINTENANCE", ignoreCase = true) -> "🔧"
                else -> "💲"
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            holder.tvDate.text = dateFormat.format(Date(item.transactionDate))
            holder.tvEmoji.text = getEmojiForEntry(item.category, item.description)
            
            val description = item.description
            val particulars = if (description.isNullOrEmpty()) {
                item.category
            } else {
                if (description.startsWith(item.category, ignoreCase = true)) {
                    description
                } else {
                    "${item.category} - $description"
                }
            }
            holder.tvParticulars.text = particulars

            if (item.transactionType == "IN") {
                holder.tvCredit.text = String.format("₹%.2f", item.amount)
                holder.tvCredit.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
                holder.tvDebit.text = ""
            } else {
                holder.tvDebit.text = String.format("₹%.2f", item.amount)
                holder.tvDebit.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
                holder.tvCredit.text = ""
            }
            holder.itemView.setOnClickListener { onItemClick(item) }
            holder.itemView.setOnLongClickListener { onLongClick(item); true }
        }

        override fun getItemCount() = items.size
    }
}
