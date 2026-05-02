package com.jiliu.launcher.ui.vip

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityVipPurchaseBinding
import com.jiliu.launcher.viewmodel.VipPurchaseViewModel

/**
 * VIP购买页面 - 激活码购买方式
 * 联系电话：13325136914
 * QQ：251662887
 * 
 * 功能：
 * 1. 新用户免费试用15天
 * 2. 激活码激活（设备绑定）
 */
class VipPurchaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVipPurchaseBinding
    private val viewModel: VipPurchaseViewModel by viewModels()

    companion object {
        const val CONTACT_PHONE = "13325136914"
        const val CONTACT_QQ = "251662887"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVipPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
        checkTrialStatus()
    }

    private fun setupUI() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 免费试用按钮
        binding.btnTrial.setOnClickListener {
            showTrialConfirmDialog()
        }

        // 粘贴按钮
        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text.toString()
                binding.etActivationCode.setText(text)
                Toast.makeText(this, "已粘贴", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "剪贴板为空", Toast.LENGTH_SHORT).show()
            }
        }

        // 激活码规则
        binding.tvCodeRules.setOnClickListener {
            showCodeRulesDialog()
        }

        // 立即激活按钮
        binding.btnActivate.setOnClickListener {
            val code = binding.etActivationCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "请输入激活码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.redeemActivationCode(code)
        }
    }

    private fun checkTrialStatus() {
        val canUseTrial = viewModel.canUseTrial()
        val trialDays = viewModel.getTrialRemainingDays()

        if (trialDays > 0) {
            // 正在使用试用
            binding.btnTrial.visibility = View.GONE
            binding.tvTrialStatus.text = "试用期剩余 $trialDays 天"
            binding.tvTrialStatus.setTextColor(getColor(R.color.vip_gold))
        } else if (canUseTrial) {
            // 可以使用试用
            binding.btnTrial.visibility = View.VISIBLE
            binding.tvTrialStatus.text = "首次使用可免费体验全部VIP功能"
        } else {
            // 已使用过试用
            binding.btnTrial.visibility = View.GONE
            binding.tvTrialStatus.text = "您已使用过免费试用，请购买激活码"
            binding.tvTrialStatus.setTextColor(getColor(R.color.text_hint))
        }
    }

    private fun showTrialConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("领取免费试用")
            .setMessage("确认领取15天VIP免费试用吗？\n\n每个设备只能领取一次，领取后立即生效。")
            .setPositiveButton("立即领取") { _, _ ->
                activateTrial()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun activateTrial() {
        binding.progressBar.visibility = View.VISIBLE
        
        val result = viewModel.activateTrial()
        
        binding.progressBar.visibility = View.GONE
        
        if (result.success) {
            showSuccessDialog(result.message, result.remainingDays)
            checkTrialStatus()
        } else {
            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        viewModel.activationState.observe(this) { state ->
            when (state) {
                is VipPurchaseViewModel.ActivationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnActivate.isEnabled = false
                }
                is VipPurchaseViewModel.ActivationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActivate.isEnabled = true
                    showSuccessDialog(state.message, state.remainingDays)
                }
                is VipPurchaseViewModel.ActivationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActivate.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnActivate.isEnabled = true
                }
            }
        }
    }

    private fun showCodeRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("激活码规则")
            .setMessage("""
                【激活码格式】
                • 月卡：JLM + 13位字符
                • 季卡：JLS + 13位字符  
                • 年卡：JLY + 13位字符
                • 永久卡：JLP + 13位字符
                
                【购买方式】
                电话：13325136914
                QQ：251662887
                
                【套餐价格】
                月卡：¥6.9（30天）
                季卡：¥16.9（90天）
                年卡：¥72.9（365天）
                永久卡：¥188（永久）
                
                【使用规则】
                1. 每个激活码仅可使用一次
                2. 激活码绑定当前设备，不可更换
                3. VIP权益到期后自动失效
                4. 新用户可免费试用15天
                5. 如有问题请联系客服
            """.trimIndent())
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showSuccessDialog(message: String, remainingDays: Int) {
        AlertDialog.Builder(this)
            .setTitle("激活成功")
            .setMessage("$message\n\nVIP有效期：$remainingDays 天")
            .setPositiveButton("确定") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
