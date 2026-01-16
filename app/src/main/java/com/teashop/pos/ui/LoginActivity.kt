package com.teashop.pos.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.teashop.pos.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        biometricAuthManager = BiometricAuthManager(
            context = this,
            activity = this,
            onAuthSuccess = {
                proceedToMain()
            },
            onAuthError = { errString ->
                // Fallback to OTP if biometric fails
                binding.llOtpLogin.visibility = View.VISIBLE
                Toast.makeText(this, errString, Toast.LENGTH_SHORT).show()
            }
        )

        // If user is already logged in with Firebase, try biometric
        if (auth.currentUser != null) {
            if (biometricAuthManager.canAuthenticate()) {
                biometricAuthManager.authenticate()
            } else {
                // If biometric is not available, proceed to main
                proceedToMain()
            }
        } else {
            // If no user, show OTP login
            binding.llOtpLogin.visibility = View.VISIBLE
        }

        binding.btnSendOtp.setOnClickListener { 
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            if (phoneNumber.length == 10) {
                startPhoneNumberVerification("+91$phoneNumber")
            } else {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogin.setOnClickListener { 
            val code = binding.etOtp.text.toString().trim()
            if (code.isNotEmpty() && verificationId != null) {
                verifyOtp(code)
            } else {
                Toast.makeText(this, "Please enter the OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhoneNumberVerification(phone: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@LoginActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    binding.tilOtp.visibility = View.VISIBLE
                    binding.btnLogin.visibility = View.VISIBLE
                    binding.btnSendOtp.text = "Resend OTP"
                    Toast.makeText(this@LoginActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    proceedToMain()
                } else {
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun proceedToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
