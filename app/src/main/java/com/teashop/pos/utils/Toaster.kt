package com.teashop.pos.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.teashop.pos.R
import com.teashop.pos.databinding.LayoutPremiumToastBinding

object Toaster {
    fun show(context: Context, message: String, isError: Boolean = false) {
        val inflater = LayoutInflater.from(context)
        val binding = LayoutPremiumToastBinding.inflate(inflater)

        binding.tvMessage.text = message
        if (isError) {
            binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.red))
            binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert)
        } else {
            binding.root.setCardBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
            binding.ivIcon.setImageResource(android.R.drawable.ic_dialog_info)
        }

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = binding.root
        toast.show()
    }
}
