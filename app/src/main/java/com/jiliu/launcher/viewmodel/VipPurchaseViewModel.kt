package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.model.PayOrder
import com.jiliu.launcher.model.PayResult
import com.jiliu.launcher.model.VipPackage
import com.jiliu.launcher.repository.MemberRepository
import com.jiliu.launcher.service.ActivationCodeService
import com.jiliu.launcher.service.AlipayService
import com.jiliu.launcher.service.WechatPayService
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * VIP购买页面的ViewModel
 */
class VipPurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val memberRepository = MemberRepository(application)
    private val alipayService = AlipayService(application)
    private val wechatPayService = WechatPayService(application)
    private val activationCodeService = ActivationCodeService(application)

    // VIP套餐列表
    private val _vipPackages = MutableLiveData<List<VipPackage>>()
    val vipPackages: LiveData<List<VipPackage>> = _vipPackages

    // 当前选中的套餐
    private val _selectedPackage = MutableLiveData<VipPackage?>()
    val selectedPackage: LiveData<VipPackage?> = _selectedPackage

    // 支付方式
    private val _selectedPayMethod = MutableLiveData<com.jiliu.launcher.model.PayMethod>()
    val selectedPayMethod: LiveData<com.jiliu.launcher.model.PayMethod> = _selectedPayMethod

    // 支付状态
    private val _payState = MutableLiveData<PayState>()
    val payState: LiveData<PayState> = _payState

    // 激活码兑换状态
    private val _activationState = MutableLiveData<ActivationState>()
    val activationState: LiveData<ActivationState> = _activationState

    // VIP套餐数据
    init {
        loadVipPackages()
        _selectedPayMethod.value = com.jiliu.launcher.model.PayMethod.ALIPAY
    }

    /**
     * 加载VIP套餐列表
     */
    private fun loadVipPackages() {
        _vipPackages.value = listOf(
            VipPackage(
                id = "monthly",
                name = "月卡",
                description = "体验VIP特权一个月",
                duration = 30,
                originalPrice = 29.9,
                currentPrice = 19.9,
                discount = "限时7折",
                features = listOf("全部VIP功能", "30天会员", "云端同步")
            ),
            VipPackage(
                id = "seasonal",
                name = "季卡",
                description = "超值的90天VIP特权",
                duration = 90,
                originalPrice = 79.9,
                currentPrice = 49.9,
                discount = "限时6折",
                features = listOf("全部VIP功能", "90天会员", "云端同步", "专属客服")
            ),
            VipPackage(
                id = "yearly",
                name = "年卡",
                description = "最划算的年度VIP特权",
                duration = 365,
                originalPrice = 299.0,
                currentPrice = 159.0,
                discount = "限时5折",
                features = listOf("全部VIP功能", "365天会员", "云端同步", "专属客服", "优先体验新功能")
            )
        )
        
        // 默认选中季卡
        _selectedPackage.value = _vipPackages.value?.find { it.id == "seasonal" }
    }

    /**
     * 选择VIP套餐
     */
    fun selectPackage(vipPackage: VipPackage) {
        _selectedPackage.value = vipPackage
    }

    /**
     * 选择支付方式
     */
    fun selectPayMethod(payMethod: com.jiliu.launcher.model.PayMethod) {
        _selectedPayMethod.value = payMethod
    }

    /**
     * 发起支付
     */
    fun pay() {
        val packageInfo = _selectedPackage.value ?: return
        val payMethod = _selectedPayMethod.value ?: return
        
        viewModelScope.launch {
            _payState.value = PayState.Loading
            
            when (payMethod) {
                com.jiliu.launcher.model.PayMethod.ALIPAY -> {
                    val orderResult = alipayService.createPayOrder(packageInfo)
                    orderResult.fold(
                        onSuccess = { order ->
                            alipayService.pay(order) { result ->
                                handlePayResult(result, order)
                            }
                        },
                        onFailure = { error ->
                            _payState.value = PayState.Error(error.message ?: "创建订单失败")
                        }
                    )
                }
                com.jiliu.launcher.model.PayMethod.WECHAT -> {
                    val orderResult = wechatPayService.createPayOrder(packageInfo)
                    orderResult.fold(
                        onSuccess = { order ->
                            wechatPayService.pay(order) { result ->
                                handlePayResult(result, order)
                            }
                        },
                        onFailure = { error ->
                            _payState.value = PayState.Error(error.message ?: "创建订单失败")
                        }
                    )
                }
                com.jiliu.launcher.model.PayMethod.ACTIVATION_CODE -> {
                    // 激活码方式跳转到激活页面
                    _payState.value = PayState.NeedActivationCode
                }
            }
        }
    }

    /**
     * 处理支付结果
     */
    private fun handlePayResult(result: PayResult, order: PayOrder) {
        if (result.success) {
            viewModelScope.launch {
                // 更新VIP状态
                activateVip(_selectedPackage.value!!)
            }
        } else {
            _payState.value = PayState.Error(result.message)
        }
    }

    /**
     * 激活VIP（模拟支付成功后）
     */
    private suspend fun activateVip(vipPackage: VipPackage) {
        try {
            // 计算新的VIP到期时间
            val currentExpireTime = preferencesManager.vipExpireTime
            val now = System.currentTimeMillis()
            
            val newExpireTime: Long
            if (currentExpireTime > now) {
                // 已有VIP，在原有基础上续期
                newExpireTime = currentExpireTime + (vipPackage.duration * 24 * 60 * 60 * 1000L)
            } else {
                // 无VIP或已过期，从现在开始计算
                newExpireTime = now + (vipPackage.duration * 24 * 60 * 60 * 1000L)
            }
            
            // 更新VIP状态
            preferencesManager.isVip = true
            preferencesManager.vipExpireTime = newExpireTime
            
            _payState.value = PayState.Success(
                message = "${vipPackage.name}开通成功",
                expireTime = newExpireTime,
                remainingDays = vipPackage.duration
            )
        } catch (e: Exception) {
            _payState.value = PayState.Error(e.message ?: "激活失败")
        }
    }

    /**
     * 兑换激活码
     */
    fun redeemActivationCode(code: String) {
        val userId = memberRepository.getCurrentUser()?.userId ?: "guest"
        
        viewModelScope.launch {
            _activationState.value = ActivationState.Loading
            
            val result = activationCodeService.redeemCode(code, userId)
            result.fold(
                onSuccess = { activationResult ->
                    _activationState.value = ActivationState.Success(
                        message = activationResult.message,
                        remainingDays = activationResult.remainingDays
                    )
                },
                onFailure = { error ->
                    _activationState.value = ActivationState.Error(error.message ?: "兑换失败")
                }
            )
        }
    }

    /**
     * 模拟支付（用于演示）
     */
    fun simulatePay() {
        viewModelScope.launch {
            _payState.value = PayState.Loading
            
            // 模拟支付延迟
            kotlinx.coroutines.delay(2000)
            
            val packageInfo = _selectedPackage.value ?: return@launch
            
            // 计算新的VIP到期时间
            val currentExpireTime = preferencesManager.vipExpireTime
            val now = System.currentTimeMillis()
            
            val newExpireTime: Long
            if (currentExpireTime > now) {
                newExpireTime = currentExpireTime + (packageInfo.duration * 24 * 60 * 60 * 1000L)
            } else {
                newExpireTime = now + (packageInfo.duration * 24 * 60 * 60 * 1000L)
            }
            
            // 更新VIP状态
            preferencesManager.isVip = true
            preferencesManager.vipExpireTime = newExpireTime
            
            _payState.value = PayState.Success(
                message = "${packageInfo.name}开通成功",
                expireTime = newExpireTime,
                remainingDays = packageInfo.duration
            )
        }
    }

    /**
     * 支付状态
     */
    sealed class PayState {
        object Idle : PayState()
        object Loading : PayState()
        object NeedActivationCode : PayState()
        data class Success(val message: String, val expireTime: Long, val remainingDays: Int) : PayState()
        data class Error(val message: String) : PayState()
    }

    /**
     * 激活状态
     */
    sealed class ActivationState {
        object Idle : ActivationState()
        object Loading : ActivationState()
        data class Success(val message: String, val remainingDays: Int) : ActivationState()
        data class Error(val message: String) : ActivationState()
    }
}
