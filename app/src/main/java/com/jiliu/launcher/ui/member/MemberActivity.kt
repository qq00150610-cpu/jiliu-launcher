package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.App
import com.jiliu.launcher.R
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

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Activate button
        binding.btnActivate.setOnClickListener {
            val code = binding.editActivationCode.text.toString().trim()
            if (code.isNotEmpty()) {
                viewModel.activate(code)
            } else {
                Toast.makeText(this, "请输入激活码", Toast.LENGTH_SHORT).show()
            }
        }

        // Trial button
        binding.btnTrial.setOnClickListener {
            viewModel.startTrial()
        }

        // Phone click - dial
        binding.layoutPhone.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = android.net.Uri.parse("tel:13325136914")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法拨打电话", Toast.LENGTH_SHORT).show()
            }
        }

        // QQ click - copy to clipboard
        binding.layoutQq.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("QQ", "251662887")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "QQ号已复制: 251662887", Toast.LENGTH_SHORT).show()
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, com.jiliu.launcher.ui.settings.SettingsActivity::class.java))
        }

        updateUI()
    }

    private fun observeData() {
        viewModel.memberInfo.observe(this) { memberInfo ->
            updateMemberUI(memberInfo)
        }

        viewModel.activationResult.observe(this) { state ->
            when (state) {
                is MemberViewModel.ActivationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnActivate.isEnabled = false
                }
                is MemberViewModel.ActivationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActivate.isEnabled = true
                    Toast.makeText(this, "激活成功", Toast.LENGTH_SHORT).show()
                    updateMemberUI(state.memberInfo)
                }
                is MemberViewModel.ActivationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActivate.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateUI() {
        val isVip = viewModel.isVip()
        
        if (isVip) {
            binding.vipStatusGroup.visibility = View.VISIBLE
            binding.activationGroup.visibility = View.GONE
            binding.btnVipFeatures.visibility = View.GONE
        } else {
            binding.vipStatusGroup.visibility = View.GONE
            binding.activationGroup.visibility = View.VISIBLE
            binding.btnVipFeatures.visibility = View.VISIBLE
        }
    }

    private fun updateMemberUI(memberInfo: com.jiliu.launcher.model.MemberInfo) {
        if (memberInfo.isVip) {
            binding.tvVipStatus.text = "VIP会员"
            binding.tvVipStatus.setTextColor(getColor(R.color.vip_gold))
            
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
            binding.tvVipStatus.setTextColor(getColor(R.color.text_secondary))
            binding.tvExpireDate.visibility = View.GONE
            binding.tvRemainingDays.visibility = View.GONE
        }
        
        updateUI()
    }
}
