package com.teashop.pos.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.teashop.pos.TeaShopApplication
import com.teashop.pos.databinding.ActivityScanMenuBinding
import com.teashop.pos.databinding.ItemDetectedMenuBinding
import com.teashop.pos.ui.viewmodel.ItemMasterViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class ScanMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanMenuBinding
    private lateinit var viewModel: ItemMasterViewModel
    private var imageUri: Uri? = null
    private var shopId: String? = null
    private val detectedItemsAdapter = DetectedItemsAdapter()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            binding.ivPreview.setImageURI(imageUri)
            processImage(imageUri!!)
        }
    }

    private val pickGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.ivPreview.setImageURI(uri)
            processImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as TeaShopApplication
        val factory = app.viewModelFactory
        viewModel = ViewModelProvider(this, factory)[ItemMasterViewModel::class.java]
        
        shopId = intent.getStringExtra("SHOP_ID")

        setupUI()
    }

    private fun setupUI() {
        binding.btnCamera.setOnClickListener {
            val photoFile = File(cacheDir, "scan_menu_temp.jpg")
            imageUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
        }

        binding.btnGallery.setOnClickListener {
            pickGallery.launch("image/*")
        }

        binding.rvDetectedItems.layoutManager = LinearLayoutManager(this)
        binding.rvDetectedItems.adapter = detectedItemsAdapter

        binding.btnAddSelected.setOnClickListener {
            val itemsToAdd = detectedItemsAdapter.getSelectedItems()
            if (itemsToAdd.isNotEmpty()) {
                addItems(itemsToAdd)
            } else {
                Toast.makeText(this, "No valid items selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAddSelected.isEnabled = false
        
        try {
            val image = InputImage.fromFilePath(this, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    parseDetectedText(visionText.text)
                    binding.progressBar.visibility = View.GONE
                    binding.btnAddSelected.isEnabled = true
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }
        } catch (e: IOException) {
            e.printStackTrace()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun parseDetectedText(text: String) {
        val candidates = mutableListOf<DetectedItem>()
        // Improved Regex: Matches name followed by price (currency symbol optional)
        // Matches line ending with digits, possibly with decimals. 
        // Example: "Ginger Tea 20.00", "Masala Tea ₹ 15"
        val lines = text.split("\n")
        val regex = Regex("^(.*?)[^0-9\\.]*([0-9]+(?:\\.[0-9]{2})?)$")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            
            val match = regex.find(trimmed)
            if (match != null && match.groupValues.size >= 3) {
                var name = match.groupValues[1].trim()
                // Cleanup name symbols
                name = name.replace(Regex("[^a-zA-Z0-9 &()-]"), " ").trim()
                
                val priceStr = match.groupValues[2]
                val price = priceStr.toDoubleOrNull() ?: 0.0
                
                if (name.isNotEmpty() && price > 0) {
                    candidates.add(DetectedItem(name, price))
                }
            }
        }
        
        if (candidates.isEmpty()) {
            Toast.makeText(this, "No items detected with prices", Toast.LENGTH_SHORT).show()
        }
        detectedItemsAdapter.submitList(candidates)
    }

    private fun addItems(items: List<DetectedItem>) {
        lifecycleScope.launch {
            items.forEach { 
                viewModel.addItemFromScan(it.name, it.price, shopId)
            }
            Toast.makeText(this@ScanMenuActivity, "${items.size} items added successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

data class DetectedItem(
    var name: String,
    var price: Double,
    var isSelected: Boolean = true
)

class DetectedItemsAdapter : RecyclerView.Adapter<DetectedItemsAdapter.ViewHolder>() {
    private var items = mutableListOf<DetectedItem>()

    fun submitList(newItems: List<DetectedItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun getSelectedItems(): List<DetectedItem> {
        return items.filter { it.isSelected && it.name.isNotBlank() && it.price > 0 }
    }

    class ViewHolder(val binding: ItemDetectedMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDetectedMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        // Remove listeners before setting values to avoid loops
        holder.binding.cbSelect.setOnCheckedChangeListener(null)
        holder.binding.etName.removeTextChangedListener(holder.binding.etName.tag as? TextWatcher)
        holder.binding.etPrice.removeTextChangedListener(holder.binding.etPrice.tag as? TextWatcher)

        holder.binding.cbSelect.isChecked = item.isSelected
        holder.binding.etName.setText(item.name)
        holder.binding.etPrice.setText(item.price.toString())

        holder.binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }

        val nameWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { item.name = s.toString() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.binding.etName.addTextChangedListener(nameWatcher)
        holder.binding.etName.tag = nameWatcher

        val priceWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { 
                item.price = s.toString().toDoubleOrNull() ?: 0.0 
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        holder.binding.etPrice.addTextChangedListener(priceWatcher)
        holder.binding.etPrice.tag = priceWatcher
    }

    override fun getItemCount() = items.size
}
