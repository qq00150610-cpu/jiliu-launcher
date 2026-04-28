package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.databinding.ActivityMemberBinding
import com.jiliu.launcher.viewmodel.MemberViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberBinding
    private val viewModel: MemberViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkLoginStatus()
        viewModel.loadMemberInfo()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Login/Register button
        binding.btnLogin.setOnClickListener {
            showLoginDialog()
        }

        // Open VIP button
        binding.btnOpenVip.setOnClickListener {
            // 跳转到VIP购买页面
            startActivity(Intent(this, com.jiliu.launcher.ui.vip.VipPurchaseActivity::class.java))
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, com.jiliu.launcher.ui.settings.SettingsActivity::class.java))
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
            updateUI()
        }

        updateUI()
    }

    private fun observeData() {
        viewModel.memberInfo.observe(this) { memberInfo ->
            updateMemberUI(memberInfo)
        }

        viewModel.currentUser.observe(this) { user ->
            updateUserUI(user)
        }

        viewModel.isLoggedIn.observe(this) { isLoggedIn ->
            updateLoginStatusUI(isLoggedIn)
        }

        viewModel.activationResult.observe(this) { state ->
            when (state) {
                is MemberViewModel.ActivationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is MemberViewModel.ActivationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "VIP开通成功", Toast.LENGTH_SHORT).show()
                    updateMemberUI(state.memberInfo)
                }
                is MemberViewModel.ActivationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        val isLoggedIn = viewModel.isLoggedIn()
        val isVip = viewModel.isVip()

        // User info visibility
        binding.layoutUserInfo.visibility = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.btnLogin.visibility = if (isLoggedIn) View.GONE else View.VISIBLE
        binding.btnLogout.visibility = if (isLoggedIn) View.VISIBLE else View.GONE

        // VIP status
        if (isVip) {
            binding.vipStatusGroup.visibility = View.VISIBLE
            binding.btnOpenVip.visibility = View.GONE
            binding.tvVipHint.visibility = View.GONE
        } else {
            binding.vipStatusGroup.visibility = View.GONE
            binding.btnOpenVip.visibility = View.VISIBLE
            binding.tvVipHint.visibility = View.VISIBLE
        }
    }

    private fun updateUserUI(user: com.jiliu.launcher.model.User?) {
        if (user != null) {
            binding.tvUsername.text = user.displayName
            binding.tvPhone.text = if (user.phone.isNotEmpty()) user.phone else "未绑定手机"
        } else {
            binding.tvUsername.text = "未登录"
            binding.tvPhone.text = ""
        }
        updateUI()
    }

    private fun updateMemberUI(memberInfo: com.jiliu.launcher.model.MemberInfo) {
        if (memberInfo.isVip) {
            binding.tvVipStatus.text = "VIP会员"
            binding.tvVipStatus.setTextColor(getColor(com.jiliu.launcher.R.color.vip_gold))
            
            if (memberInfo.expireTime > 0) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expireDate = dateFormat.format(Date(memberInfo.expireTime))
                binding.tvExpireDate.text = "到期时间: $expireDate"
                binding.tvExpireDate.visibility = View.VISIBLE
                
                val remainingDays = memberInfo.remainingDays
                binding.tvRemainingDays.text = "剩余 $remainingDays 天"
                binding.tvRemainingDays.visibility = View.VISIBLE
            } else {
                binding.tvExpireDate.visibility = View.GONE
                binding.tvRemainingDays.visibility = View.GONE
            }
        } else {
            binding.tvVipStatus.text = "普通用户"
            binding.tvVipStatus.setTextColor(getColor(com.jiliu.launcher.R.color.text_secondary))
            binding.tvExpireDate.visibility = View.GONE
            binding.tvRemainingDays.visibility = View.GONE
        }
        
        updateUI()
    }

    private fun updateLoginStatusUI(isLoggedIn: Boolean) {
        updateUI()
    }

    private fun showLoginDialog() {
        val options = arrayOf("手机号 + 验证码登录", "用户名 + 密码登录", "注册新账号")
        AlertDialog.Builder(this)
            .setTitle("选择登录方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, LoginActivity::class.java).apply {
                        putExtra("mode", "phone")
                    })
                    1 -> startActivity(Intent(this, LoginActivity::class.java).apply {
                        putExtra("mode", "password")
                    })
                    2 -> startActivity(Intent(this, RegisterActivity::class.java))
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showLoginPrompt() {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage("开通VIP会员需要先登录账号，是否立即登录？")
            .setPositiveButton("立即登录") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showActivationDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "请输入激活码"
            setPadding(48, 32, 48, 32)
        }
        
        AlertDialog.Builder(this)
            .setTitle("开通VIP会员")
            .setMessage("请输入VIP激活码\n\n演示验证码: 123456")
            .setView(input)
            .setPositiveButton("开通") { _, _ ->
                val code = input.text.toString().trim()
                if (code.isNotEmpty()) {
                    viewModel.activate(code)
                } else {
                    Toast.makeText(this, "请输入激活码", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
