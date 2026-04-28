package com.jiliu.launcher.ui.member

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.databinding.ActivityActivationBinding

class ActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnActivate.setOnClickListener {
            val code = binding.editActivationCode.text.toString().trim()
            if (code.isNotEmpty()) {
                // Call activation API
                performActivation(code)
            }
        }
    }

    private fun performActivation(code: String) {
        // Implement activation logic
    }
}
