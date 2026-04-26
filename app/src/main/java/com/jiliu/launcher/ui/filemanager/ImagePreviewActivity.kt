package com.jiliu.launcher.ui.filemanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.jiliu.launcher.databinding.ActivityImagePreviewBinding

class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePath = intent.getStringExtra("path")

        setupUI()
        loadImage()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun loadImage() {
        imagePath?.let { path ->
            binding.imageView.load(path) {
                crossfade(true)
            }
        }
    }
}
