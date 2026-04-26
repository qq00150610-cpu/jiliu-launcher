package com.jiliu.launcher.ui.filemanager

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityFileManagerBinding
import com.jiliu.launcher.util.FileUtils
import com.jiliu.launcher.util.IntentUtils
import com.jiliu.launcher.viewmodel.FileManagerViewModel
import java.io.File

class FileManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileManagerBinding
    private val viewModel: FileManagerViewModel by viewModels()

    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Back navigation
        binding.btnBack.setOnClickListener {
            if (!viewModel.navigateBack()) {
                finish()
            }
        }

        // Up navigation
        binding.btnUp.setOnClickListener {
            viewModel.navigateUp()
        }

        // File list
        fileAdapter = FileAdapter(
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    viewModel.navigateToPath(fileItem.path)
                } else {
                    openFile(fileItem)
                }
            },
            onItemLongClick = { fileItem ->
                showFileOptions(fileItem)
            },
            isSelectedMode = { viewModel.selectedFiles.value?.isNotEmpty() == true },
            isSelected = { path ->
                viewModel.selectedFiles.value?.contains(path) == true
            },
            onSelectionChange = { path ->
                viewModel.toggleSelection(path)
            }
        )

        binding.fileRecycler.apply {
            layoutManager = LinearLayoutManager(this@FileManagerActivity)
            adapter = fileAdapter
        }

        // Refresh button
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshCurrentDirectory()
        }

        // Storage tabs
        viewModel.storages.observe(this) { storages ->
            setupStorageTabs(storages)
        }

        // Selection mode toolbar
        binding.btnSelectAll.setOnClickListener {
            viewModel.selectAllFiles()
        }

        binding.btnClearSelection.setOnClickListener {
            viewModel.clearSelection()
        }

        binding.btnDeleteSelected.setOnClickListener {
            showDeleteConfirmation()
        }

        // Create folder button
        binding.btnCreateFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun observeData() {
        viewModel.currentPath.observe(this) { path ->
            binding.tvCurrentPath.text = path
            updateNavigationButtons(path)
        }

        viewModel.files.observe(this) { files ->
            fileAdapter.submitList(files)
            binding.tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.selectedFiles.observe(this) { selected ->
            updateSelectionMode(selected.isNotEmpty())
            fileAdapter.notifyDataSetChanged()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.operationResult.observe(this) { result ->
            when (result) {
                is FileManagerViewModel.OperationResult.Delete -> {
                    Toast.makeText(this, "删除完成: ${result.successCount}成功, ${result.failCount}失败", Toast.LENGTH_SHORT).show()
                    viewModel.clearOperationResult()
                }
                is FileManagerViewModel.OperationResult.Copy -> {
                    Toast.makeText(this, "复制完成: ${result.successCount}成功, ${result.failCount}失败", Toast.LENGTH_SHORT).show()
                    viewModel.clearOperationResult()
                }
                is FileManagerViewModel.OperationResult.Move -> {
                    Toast.makeText(this, "移动完成: ${result.successCount}成功, ${result.failCount}失败", Toast.LENGTH_SHORT).show()
                    viewModel.clearOperationResult()
                }
                null -> { }
            }
        }
    }

    private fun setupStorageTabs(storages: List<FileUtils.StorageInfo>) {
        // Setup storage tabs based on available storages
        binding.storageTabs.removeAllViews()
        
        for (storage in storages) {
            val tab = layoutInflater.inflate(R.layout.tab_storage, binding.storageTabs, false)
            val tabView = tab.findViewById<android.widget.TextView>(R.id.tab_text)
            tabView.text = storage.name
            tab.setOnClickListener {
                viewModel.navigateToPath(storage.path)
            }
            binding.storageTabs.addView(tab)
        }
    }

    private fun updateNavigationButtons(path: String) {
        val parentExists = File(path).parent != null
        binding.btnUp.isEnabled = parentExists
        binding.btnBack.isEnabled = true
    }

    private fun updateSelectionMode(isSelectionMode: Boolean) {
        binding.selectionToolbar.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        binding.mainToolbar.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
        
        val selectedCount = viewModel.selectedFiles.value?.size ?: 0
        binding.tvSelectedCount.text = "已选择 $selectedCount 个项目"
    }

    private fun openFile(fileItem: FileUtils.FileItem) {
        IntentUtils.openFile(this, fileItem.path)
    }

    private fun showFileOptions(fileItem: FileUtils.FileItem) {
        val options = if (fileItem.isDirectory) {
            arrayOf("打开", "重命名", "删除", "复制路径")
        } else {
            arrayOf("打开", "重命名", "删除", "复制", "分享", "复制路径")
        }

        AlertDialog.Builder(this)
            .setTitle(fileItem.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "打开" -> openFile(fileItem)
                    "重命名" -> showRenameDialog(fileItem)
                    "删除" -> deleteFile(fileItem)
                    "复制" -> copyFile(fileItem)
                    "分享" -> shareFile(fileItem)
                    "复制路径" -> copyPath(fileItem)
                }
            }
            .show()
    }

    private fun showDeleteConfirmation() {
        val selectedCount = viewModel.selectedFiles.value?.size ?: 0
        
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 $selectedCount 个项目吗?")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteSelectedFiles()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteFile(fileItem: FileUtils.FileItem) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除 ${fileItem.name} 吗?")
            .setPositiveButton("删除") { _, _ ->
                FileUtils.deleteFile(fileItem.path)
                viewModel.refreshCurrentDirectory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRenameDialog(fileItem: FileUtils.FileItem) {
        val editText = EditText(this)
        editText.setText(fileItem.name)
        
        AlertDialog.Builder(this)
            .setTitle("重命名")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank() && newName != fileItem.name) {
                    FileUtils.renameFile(fileItem.path, newName)
                    viewModel.refreshCurrentDirectory()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(this)
        editText.hint = "文件夹名称"
        
        AlertDialog.Builder(this)
            .setTitle("新建文件夹")
            .setView(editText)
            .setPositiveButton("创建") { _, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    viewModel.createFolder(name)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun copyFile(fileItem: FileUtils.FileItem) {
        val intent = Intent(this, FileManagerActivity::class.java)
        intent.putExtra("action", "copy")
        intent.putExtra("source", fileItem.path)
        startActivity(intent)
    }

    private fun shareFile(fileItem: FileUtils.FileItem) {
        IntentUtils.shareFile(this, fileItem.path)
    }

    private fun copyPath(fileItem: FileUtils.FileItem) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("文件路径", fileItem.path)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "路径已复制", Toast.LENGTH_SHORT).show()
    }
}
