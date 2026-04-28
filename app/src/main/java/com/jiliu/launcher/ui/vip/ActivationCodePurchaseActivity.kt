package com.jiliu.launcher.ui.vip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityActivationCodePurchaseBinding
import com.jiliu.launcher.viewmodel.VipPurchaseViewModel

/**
 * 激活码购买页面
 */
class ActivationCodePurchaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationCodePurchaseBinding
    private val viewModel: VipPurchaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivationCodePurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 兑换激活码按钮
        binding.btnRedeem.setOnClickListener {
            val code = binding.etActivationCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "请输入激活码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.redeemActivationCode(code)
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

        // 查看激活码规则
        binding.tvCodeRules.setOnClickListener {
            showCodeRulesDialog()
        }

        // 测试激活码按钮（演示用）
        binding.btnTestCode.setOnClickListener {
            showTestCodesDialog()
        }
    }

    private fun observeData() {
        viewModel.activationState.observe(this) { state ->
            when (state) {
                is VipPurchaseViewModel.ActivationState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRedeem.isEnabled = false
                }
                is VipPurchaseViewModel.ActivationState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRedeem.isEnabled = true
                    showSuccessDialog(state.message, state.remainingDays)
                }
                is VipPurchaseViewModel.ActivationState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRedeem.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRedeem.isEnabled = true
                }
            }
        }
    }

    private fun showCodeRulesDialog() {
        AlertDialog.Builder(this)
            .setTitle("激活码规则")
            .setMessage("""
                激活码格式说明：
                
                • 月卡：JLMxxxxxxxxxxxx（16位）
                • 季卡：JLSxxxxxxxxxxxx（16位）
                • 年卡：JLYxxxxxxxxxxxx（16位）
                
                使用说明：
                1. 请确保输入正确的激活码
                2. 激活码区分大小写
                3. 每个激活码只能使用一次
                4. 激活码有效期为购买后30天内
                5. 如有问题请联系客服
                
                测试激活码（演示用）：
                • 月卡测试码：JLM123456789ABCD
                • 季卡测试码：JLS123456789ABCD
                • 年卡测试码：JLY123456789ABCD
            """.trimIndent())
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun showTestCodesDialog() {
        AlertDialog.Builder(this)
            .setTitle("测试激活码")
            .setMessage("""
                以下为演示用测试激活码：
                
                • 月卡：JLM123456789ABCD
                  → 开通30天VIP
                  
                • 季卡：JLS123456789ABCD
                  → 开通90天VIP
                  
                • 年卡：JLY123456789ABCD
                  → 开通365天VIP
                  
                点击下方按钮复制测试码：
            """.trimIndent())
            .setPositiveButton("复制月卡码") { _, _ ->
                copyToClipboard("JLM123456789ABCD")
            }
            .setNegativeButton("复制季卡码") { _, _ ->
                copyToClipboard("JLS123456789ABCD")
            }
            .setNeutralButton("复制年卡码") { _, _ ->
                copyToClipboard("JLY123456789ABCD")
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Activation Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    private fun showSuccessDialog(message: String, remainingDays: Int) {
        AlertDialog.Builder(this)
            .setTitle("兑换成功")
            .setMessage("$message\n\n剩余 $remainingDays 天\n\n您已成为VIP会员，享有多项特权！")
            .setPositiveButton("确定") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
