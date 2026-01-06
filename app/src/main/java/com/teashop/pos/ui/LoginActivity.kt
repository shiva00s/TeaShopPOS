package com.teashop.pos.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.teashop.pos.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendOtp.setOnClickListener { 
            val phoneNumber = binding.etPhoneNumber.text.toString()
            if (phoneNumber.isNotEmpty()) {
                // TODO: Implement OTP sending logic here
                binding.tilOtp.visibility = View.VISIBLE
                binding.btnLogin.visibility = View.VISIBLE
                Toast.makeText(this, "OTP sent to $phoneNumber", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogin.setOnClickListener { 
            val otp = binding.etOtp.text.toString()
            if (otp.isNotEmpty()) {
                // TODO: Implement OTP verification logic here
                val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("is_logged_in", true).apply()
                
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
