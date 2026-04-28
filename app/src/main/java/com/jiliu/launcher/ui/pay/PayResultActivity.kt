package com.jiliu.launcher.ui.pay

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jiliu.launcher.R
import com.jiliu.launcher.databinding.ActivityPayResultBinding
import com.jiliu.launcher.model.PayMethod
import com.jiliu.launcher.model.PayResult
import com.jiliu.launcher.model.VipPackage
import com.jiliu.launcher.service.AlipayService
import com.jiliu.launcher.util.PreferencesManager
import com.jiliu.launcher.service.WechatPayService
import kotlinx.coroutines.launch

/**
 * 支付结果页面
 * 显示支付结果并自动激活VIP
 */
class PayResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPayResultBinding
    
    private var payMethod: PayMethod = PayMethod.ALIPAY
    private var orderId: String = ""
    private var vipPackage: VipPackage? = null
    
    private val alipayService by lazy { AlipayService(this) }
    private val wechatPayService by lazy { WechatPayService(this) }

    companion object {
        private const val EXTRA_PAY_METHOD = "pay_method"
        private const val EXTRA_ORDER_ID = "order_id"
        private const val EXTRA_PACKAGE_ID = "package_id"
        private const val EXTRA_PACKAGE_NAME = "package_name"
        private const val EXTRA_PACKAGE_DURATION = "package_duration"
        private const val EXTRA_AMOUNT = "amount"
        
        /**
         * 启动支付结果页面
         */
        fun start(
            context: android.content.Context,
            payMethod: PayMethod,
            orderId: String,
            vipPackage: VipPackage
        ) {
            val intent = Intent(context, PayResultActivity::class.java).apply {
                putExtra(EXTRA_PAY_METHOD, payMethod.name)
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_PACKAGE_ID, vipPackage.id)
                putExtra(EXTRA_PACKAGE_NAME, vipPackage.name)
                putExtra(EXTRA_PACKAGE_DURATION, vipPackage.duration)
                putExtra(EXTRA_AMOUNT, vipPackage.currentPrice)
            }
            context.startActivity(intent)
        }
        
        /**
         * 启动支付结果页面（简化版）
         */
        fun start(
            context: android.content.Context,
            payMethod: PayMethod,
            orderId: String,
            packageName: String,
            duration: Int,
            amount: Double
        ) {
            val intent = Intent(context, PayResultActivity::class.java).apply {
                putExtra(EXTRA_PAY_METHOD, payMethod.name)
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_PACKAGE_DURATION, duration)
                putExtra(EXTRA_AMOUNT, amount)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取传递的参数
        payMethod = PayMethod.valueOf(
            intent.getStringExtra(EXTRA_PAY_METHOD) ?: PayMethod.ALIPAY.name
        )
        orderId = intent.getStringExtra(EXTRA_ORDER_ID) ?: ""
        
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val duration = intent.getIntExtra(EXTRA_PACKAGE_DURATION, 30)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        
        vipPackage = VipPackage(
            id = intent.getStringExtra(EXTRA_PACKAGE_ID) ?: "",
            name = packageName,
            description = "",
            duration = duration,
            originalPrice = amount,
            currentPrice = amount,
            discount = null,
            features = emptyList()
        )
        
        setupUI()
        setupClickListeners()
        
        // 模拟支付成功回调
        simulatePaymentResult()
    }

    private fun setupUI() {
        // 设置支付方式图标
        val iconRes = when (payMethod) {
            PayMethod.ALIPAY -> R.drawable.ic_alipay
            PayMethod.WECHAT -> R.drawable.ic_wechat
            else -> R.drawable.ic_payment
        }
        binding.ivPayMethodIcon.setImageResource(iconRes)
        
        // 设置支付方式名称
        binding.tvPayMethod.text = when (payMethod) {
            PayMethod.ALIPAY -> "支付宝"
            PayMethod.WECHAT -> "微信支付"
            else -> "支付"
        }
        
        // 设置订单信息
        binding.tvOrderId.text = "订单号: $orderId"
        binding.tvPackageName.text = vipPackage?.name ?: ""
        binding.tvAmount.text = String.format("¥%.2f", vipPackage?.currentPrice ?: 0.0)
        
        // 默认显示处理中
        showLoading()
    }

    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // 查看订单按钮
        binding.btnViewOrder.setOnClickListener {
            // TODO: 跳转到订单详情页面
            Toast.makeText(this, "订单详情开发中", Toast.LENGTH_SHORT).show()
        }
        
        // 重新支付按钮
        binding.btnRetry.setOnClickListener {
            finish()
        }
    }

    /**
     * 模拟支付结果
     * 实际项目中，这里应该接收真实的支付回调
     */
    private fun simulatePaymentResult() {
        lifecycleScope.launch {
            try {
                // 模拟延迟
                kotlinx.coroutines.delay(2000)
                
                // 根据支付方式处理
                val result = when (payMethod) {
                    PayMethod.ALIPAY -> alipayService.handlePayCallback("9000", "{}")
                    PayMethod.WECHAT -> wechatPayService.handlePayCallback(0, mapOf("out_trade_no" to orderId))
                    else -> PayResult(false, "未知支付方式", orderId)
                }
                
                handlePaymentResult(result)
                
            } catch (e: Exception) {
                showFailed("支付结果查询失败")
            }
        }
    }

    /**
     * 处理支付结果
     */
    private fun handlePaymentResult(result: PayResult) {
        if (result.success) {
            showSuccess()
            // 自动激活VIP
            activateVip()
        } else {
            showFailed(result.message)
        }
    }

    /**
     * 显示成功状态
     */
    private fun showSuccess() {
        binding.layoutResult.visibility = View.VISIBLE
        binding.layoutLoading.visibility = View.GONE
        binding.layoutFailed.visibility = View.GONE
        
        // 播放成功动画
        binding.ivResultIcon.setImageResource(R.drawable.ic_success)
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale_in)
        binding.ivResultIcon.startAnimation(animation)
        
        binding.tvResultTitle.text = "支付成功"
        binding.tvResultMessage.text = "感谢您的支持，VIP特权已生效"
        binding.tvResultMessage.visibility = View.VISIBLE
        
        binding.btnBack.text = "返回首页"
        binding.btnRetry.visibility = View.GONE
        binding.btnViewOrder.visibility = View.VISIBLE
    }

    /**
     * 显示失败状态
     */
    private fun showFailed(message: String) {
        binding.layoutResult.visibility = View.VISIBLE
        binding.layoutLoading.visibility = View.GONE
        binding.layoutFailed.visibility = View.VISIBLE
        
        // 播放失败动画
        binding.ivResultIcon.setImageResource(R.drawable.ic_failed)
        val animation = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.ivResultIcon.startAnimation(animation)
        
        binding.tvResultTitle.text = "支付失败"
        binding.tvResultMessage.text = message
        binding.tvResultMessage.visibility = View.VISIBLE
        
        binding.btnBack.text = "返回"
        binding.btnRetry.visibility = View.VISIBLE
        binding.btnViewOrder.visibility = View.GONE
    }

    /**
     * 显示加载状态
     */
    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutResult.visibility = View.GONE
        binding.layoutFailed.visibility = View.GONE
    }

    /**
     * 激活VIP
     */
    private fun activateVip() {
        vipPackage?.let { pkg ->
            lifecycleScope.launch {
                try {
                    // 更新VIP状态
                    val preferencesManager = PreferencesManager(this@PayResultActivity)
                    val currentExpireTime = preferencesManager.vipExpireTime
                    val now = System.currentTimeMillis()
                    val newExpireTime = if (currentExpireTime > now) {
                        currentExpireTime + (pkg.duration * 24 * 60 * 60 * 1000L)
                    } else {
                        now + (pkg.duration * 24 * 60 * 60 * 1000L)
                    }
                    preferencesManager.isVip = true
                    preferencesManager.vipExpireTime = newExpireTime
                    
                    Toast.makeText(
                        this@PayResultActivity,
                        "恭喜！您已成为VIP会员，有效期${pkg.duration}天",
                        Toast.LENGTH_LONG
                    ).show()
                    
                } catch (e: Exception) {
                    // 即使激活失败，也显示支付成功
                    Toast.makeText(
                        this@PayResultActivity,
                        "支付成功，请联系客服激活VIP",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
    }
}
