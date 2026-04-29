package com.jiliu.launcher.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.App
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityWallpaperBinding
import com.jiliu.launcher.model.WallpaperInfo
import com.jiliu.launcher.util.WallpaperUtils
import com.jiliu.launcher.viewmodel.WallpaperViewModel
import java.io.File

class WallpaperActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperBinding
    private val viewModel: WallpaperViewModel by viewModels()

    private lateinit var categoryAdapter: WallpaperCategoryAdapter
    private lateinit var wallpaperAdapter: WallpaperAdapter

    private var selectedWallpaper: WallpaperInfo? = null

    // 本地壁纸选择器
    private val localWallpaperPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            loadLocalWallpaper(selectedUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置透明状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
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

        // Apply button - 设置为系统壁纸
        binding.btnApply.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.BOTH)
            }
        }

        // Set as home screen - 设置为桌面背景
        binding.btnSetHome.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.HOME)
                // 同时保存到应用作为自定义壁纸
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.HOME)
            }
        }

        // Set as lock screen
        binding.btnSetLock.setOnClickListener {
            selectedWallpaper?.let { wallpaper ->
                viewModel.setWallpaperFromUrl(wallpaper.imageUrl, WallpaperUtils.WallpaperTarget.LOCK)
            }
        }

        // Local wallpapers button - 从相册选择
        binding.btnLocalWallpaper.setOnClickListener {
            openLocalWallpaperPicker()
        }

        // Initial state
        binding.categoryContainer.visibility = View.VISIBLE
        binding.wallpaperContainer.visibility = View.GONE
    }

    /**
     * 打开本地壁纸选择器
     */
    private fun openLocalWallpaperPicker() {
        localWallpaperPicker.launch("image/*")
    }

    /**
     * 加载本地壁纸
     */
    private fun loadLocalWallpaper(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                // 保存到应用私有目录
                val wallpaperDir = File(filesDir, "wallpaper")
                if (!wallpaperDir.exists()) {
                    wallpaperDir.mkdirs()
                }
                
                val wallpaperFile = File(wallpaperDir, "custom_wallpaper.jpg")
                wallpaperFile.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                // 保存路径
                App.instance.preferencesManager.lastWallpaperPath = wallpaperFile.absolutePath
                
                Toast.makeText(this, "壁纸已设置", Toast.LENGTH_SHORT).show()
                
                // 返回主界面
                finish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "壁纸设置失败", Toast.LENGTH_SHORT).show()
        }
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
