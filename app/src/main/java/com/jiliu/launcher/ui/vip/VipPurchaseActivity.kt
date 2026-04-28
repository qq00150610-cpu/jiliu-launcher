package com.jiliu.launcher.ui.vip

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityVipPurchaseBinding
import com.jiliu.launcher.model.PayMethod
import com.jiliu.launcher.model.VipPackage
import com.jiliu.launcher.viewmodel.VipPurchaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * VIP购买页面
 */
class VipPurchaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVipPurchaseBinding
    private val viewModel: VipPurchaseViewModel by viewModels()
    private lateinit var packageAdapter: VipPackageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVipPurchaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        observeData()
    }

    private fun setupUI() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 支付宝
        binding.layoutAlipay.setOnClickListener {
            selectPayMethod(PayMethod.ALIPAY)
        }

        // 微信支付
        binding.layoutWechat.setOnClickListener {
            selectPayMethod(PayMethod.WECHAT)
        }

        // 激活码
        binding.layoutActivationCode.setOnClickListener {
            selectPayMethod(PayMethod.ACTIVATION_CODE)
        }

        // 购买按钮
        binding.btnPurchase.setOnClickListener {
            when (viewModel.selectedPayMethod.value) {
                PayMethod.ACTIVATION_CODE -> {
                    // 跳转激活码页面
                    startActivity(Intent(this, ActivationCodePurchaseActivity::class.java))
                }
                else -> {
                    // 显示模拟支付对话框
                    showSimulatePayDialog()
                }
            }
        }

        // 模拟支付按钮（用于演示）
        binding.btnSimulatePay.setOnClickListener {
            showSimulatePayDialog()
        }
    }

    private fun setupRecyclerView() {
        packageAdapter = VipPackageAdapter { vipPackage ->
            viewModel.selectPackage(vipPackage)
        }
        
        binding.recyclerPackages.apply {
            layoutManager = LinearLayoutManager(this@VipPurchaseActivity)
            adapter = packageAdapter
        }
    }

    private fun observeData() {
        // 观察套餐列表
        viewModel.vipPackages.observe(this) { packages ->
            packageAdapter.submitList(packages)
            packageAdapter.setSelectedPackage(viewModel.selectedPackage.value)
        }

        // 观察选中的套餐
        viewModel.selectedPackage.observe(this) { selectedPackage ->
            packageAdapter.setSelectedPackage(selectedPackage)
            updatePackageInfo(selectedPackage)
        }

        // 观察选中的支付方式
        viewModel.selectedPayMethod.observe(this) { payMethod ->
            updatePayMethodUI(payMethod)
        }

        // 观察支付状态
        viewModel.payState.observe(this) { state ->
            when (state) {
                is VipPurchaseViewModel.PayState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnPurchase.isEnabled = false
                }
                is VipPurchaseViewModel.PayState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccessDialog(state.message, state.remainingDays)
                }
                is VipPurchaseViewModel.PayState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPurchase.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is VipPurchaseViewModel.PayState.NeedActivationCode -> {
                    binding.progressBar.visibility = View.GONE
                    startActivity(Intent(this, ActivationCodePurchaseActivity::class.java))
                }
                else -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnPurchase.isEnabled = true
                }
            }
        }
    }

    private fun updatePackageInfo(packageInfo: VipPackage?) {
        if (packageInfo != null) {
            binding.tvPackageName.text = packageInfo.name
            binding.tvPackagePrice.text = "¥${String.format("%.2f", packageInfo.currentPrice)}"
            binding.tvOriginalPrice.text = "原价 ¥${String.format("%.2f", packageInfo.originalPrice)}"
            binding.tvDiscount.text = packageInfo.discount ?: ""
            
            if (packageInfo.hasDiscount) {
                binding.tvDiscount.visibility = View.VISIBLE
                binding.tvOriginalPrice.visibility = View.VISIBLE
            } else {
                binding.tvDiscount.visibility = View.GONE
                binding.tvOriginalPrice.visibility = View.GONE
            }
        }
    }

    private fun selectPayMethod(payMethod: PayMethod) {
        viewModel.selectPayMethod(payMethod)
    }

    private fun updatePayMethodUI(payMethod: PayMethod) {
        // 重置所有选中状态
        binding.ivAlipayCheck.visibility = View.GONE
        binding.ivWechatCheck.visibility = View.GONE
        binding.ivActivationCodeCheck.visibility = View.GONE
        
        binding.tvAlipay.setTextColor(getColor(R.color.text_secondary))
        binding.tvWechat.setTextColor(getColor(R.color.text_secondary))
        binding.tvActivationCode.setTextColor(getColor(R.color.text_secondary))
        
        // 设置选中状态
        when (payMethod) {
            PayMethod.ALIPAY -> {
                binding.ivAlipayCheck.visibility = View.VISIBLE
                binding.tvAlipay.setTextColor(getColor(R.color.alipay_blue))
            }
            PayMethod.WECHAT -> {
                binding.ivWechatCheck.visibility = View.VISIBLE
                binding.tvWechat.setTextColor(getColor(R.color.wechat_green))
            }
            PayMethod.ACTIVATION_CODE -> {
                binding.ivActivationCodeCheck.visibility = View.VISIBLE
                binding.tvActivationCode.setTextColor(getColor(R.color.vip_gold))
            }
        }
        
        // 更新按钮文字
        binding.btnPurchase.text = when (payMethod) {
            PayMethod.ACTIVATION_CODE -> "前往兑换"
            else -> "立即开通"
        }
    }

    private fun showSimulatePayDialog() {
        val packageInfo = viewModel.selectedPackage.value ?: return
        val payMethod = viewModel.selectedPayMethod.value ?: PayMethod.ALIPAY
        
        AlertDialog.Builder(this)
            .setTitle("确认支付")
            .setMessage("确认支付 ¥${String.format("%.2f", packageInfo.currentPrice)} 开通${packageInfo.name}？")
            .setPositiveButton("确认支付") { _, _ ->
                viewModel.simulatePay()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSuccessDialog(message: String, remainingDays: Int) {
        AlertDialog.Builder(this)
            .setTitle("支付成功")
            .setMessage("$message\n\n剩余 $remainingDays 天")
            .setPositiveButton("确定") { _, _ ->
                setResult(RESULT_OK)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
