package com.jiliu.launcher.ui.wallpaper

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityWallpaperBinding
import com.jiliu.launcher.model.WallpaperInfo
import com.jiliu.launcher.util.WallpaperUtils
import com.jiliu.launcher.viewmodel.WallpaperViewModel

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperBinding
    private val viewModel: WallpaperViewModel by viewModels()

    private lateinit var categoryAdapter: WallpaperCategoryAdapter
    private lateinit var wallpaperAdapter: WallpaperAdapter

    private var selectedWallpaper: WallpaperInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Back button
        binding.btnBack.setOnClickListener {
            if (binding.categoryContainer.visibility == View.GONE) {
                binding.categoryContainer.visibility = View.VISIBLE
                binding.wallpaperContainer.visibility = View.GONE
            } else {
                finish()
            }
        }

        // Setup category list
        categoryAdapter = WallpaperCategoryAdapter { category ->
            viewModel.loadWallpapersByCategory(category.id)
            binding.categoryContainer.visibility = View.GONE
            binding.wallpaperContainer.visibility = View.VISIBLE
        }

        binding.categoryRecycler.apply {
            layoutManager = LinearLayoutManager(this@WallpaperActivity)
            adapter = categoryAdapter
        }

        // Setup wallpaper grid
        wallpaperAdapter = WallpaperAdapter { wallpaper ->
            selectedWallpaper = wallpaper
            showWallpaperOptions(wallpaper)
        }

        binding.wallpaperRecycler.apply {
            layoutManager = GridLayoutManager(this@WallpaperActivity, 2)
            adapter = wallpaperAdapter
        }

        // Featured wallpapers
        binding.featuredViewpager.adapter = FeaturedWallpaperAdapter(viewModel.featuredWallpapers.value ?: emptyList())

        // Apply button
        binding.btnApply.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl)
            }
        }

        // Set as home screen
        binding.btnSetHome.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.HOME)
            }
        }

        // Set as lock screen
        binding.btnSetLock.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.LOCK)
            }
        }

        // Local wallpapers button
        binding.btnLocalWallpaper.setOnClickListener {
            // Open local wallpaper picker
        }

        // Initial state
        binding.categoryContainer.visibility = View.VISIBLE
        binding.wallpaperContainer.visibility = View.GONE
    }

    private fun observeData() {
        viewModel.categories.observe(this) { categories ->
            categoryAdapter.submitList(categories)
        }

        viewModel.wallpapers.observe(this) { wallpapers ->
            wallpaperAdapter.submitList(wallpapers)
        }

        viewModel.featuredWallpapers.observe(this) { featured ->
            (binding.featuredViewpager.adapter as? FeaturedWallpaperAdapter)?.submitList(featured)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.setWallpaperResult.observe(this) { result ->
            when (result) {
                is WallpaperViewModel.SetWallpaperResult.Success -> {
                    Toast.makeText(this, "壁纸设置成功", Toast.LENGTH_SHORT).show()
                    viewModel.clearSetWallpaperResult()
                }
                is WallpaperViewModel.SetWallpaperResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSetWallpaperResult()
                }
                null -> { }
            }
        }
    }

    private fun showWallpaperOptions(wallpaper: WallpaperInfo) {
        binding.wallpaperOptions.visibility = View.VISIBLE
        binding.selectedWallpaperTitle.text = wallpaper.title
    }
}
