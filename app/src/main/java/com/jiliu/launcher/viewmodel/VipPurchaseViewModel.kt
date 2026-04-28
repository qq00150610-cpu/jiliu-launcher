package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.model.VipPackage
import com.jiliu.launcher.model.ActivationResult
import com.jiliu.launcher.service.ActivationCodeService
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.launch

/**
 * VIP购买页面的ViewModel
 * 功能：
 * 1. 新用户免费试用15天
 * 2. 激活码激活（设备绑定）
 * 
 * 联系电话：13325136914
 * QQ：251662887
 */
class VipPurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val activationCodeService = ActivationCodeService(application)

    // VIP套餐列表
    private val _vipPackages = MutableLiveData<List<VipPackage>>()
    val vipPackages: LiveData<List<VipPackage>> = _vipPackages

    // 当前选中的套餐
    private val _selectedPackage = MutableLiveData<VipPackage?>()
    val selectedPackage: LiveData<VipPackage?> = _selectedPackage

    // 激活码兑换状态
    private val _activationState = MutableLiveData<ActivationState>()
    val activationState: LiveData<ActivationState> = _activationState

    // 会员信息
    private val _memberInfo = MutableLiveData<MemberInfo?>()
    val memberInfo: LiveData<MemberInfo?> = _memberInfo

    init {
        loadVipPackages()
        loadMemberInfo()
    }

    private fun loadVipPackages() {
        _vipPackages.value = listOf(
            VipPackage(
                id = "monthly",
                name = "月卡",
                description = "30天VIP会员",
                duration = 30,
                originalPrice = 49.90,
                currentPrice = 29.90,
                discount = "限时优惠",
                features = listOf("VIP壁纸", "高级主题", "语音服务")
            ),
            VipPackage(
                id = "quarterly",
                name = "季卡",
                description = "90天VIP会员",
                duration = 90,
                originalPrice = 99.90,
                currentPrice = 69.90,
                discount = "限时6折",
                features = listOf("VIP壁纸", "高级主题", "语音服务")
            ),
            VipPackage(
                id = "yearly",
                name = "年卡",
                description = "365天VIP会员",
                duration = 365,
                originalPrice = 299.90,
                currentPrice = 199.90,
                discount = "超值推荐",
                features = listOf("VIP壁纸", "高级主题", "语音服务", "优先客服")
            )
        )
    }

    private fun loadMemberInfo() {
        val isVip = preferencesManager.isVip
        val expireTime = preferencesManager.vipExpireTime
        
        if (isVip && expireTime > System.currentTimeMillis()) {
            _memberInfo.value = MemberInfo(
                isVip = true,
                expireTime = expireTime,
                memberLevel = MemberLevel.VIP,
                features = listOf("VIP壁纸", "高级主题", "语音服务")
            )
        } else {
            _memberInfo.value = null
        }
    }

    fun selectPackage(vipPackage: VipPackage) {
        _selectedPackage.value = vipPackage
    }

    /**
     * 检查是否可以使用免费试用
     */
    fun canUseTrial(): Boolean {
        return activationCodeService.canUseTrial()
    }

    /**
     * 获取试用剩余天数
     */
    fun getTrialRemainingDays(): Int {
        return activationCodeService.getTrialRemainingDays()
    }

    /**
     * 激活免费试用
     */
    fun activateTrial(): ActivationResult {
        return activationCodeService.activateTrial()
    }

    /**
     * 兑换激活码
     */
    fun redeemActivationCode(code: String) {
        _activationState.value = ActivationState.Loading
        
        viewModelScope.launch {
            // 获取用户ID
            val userId = preferencesManager.getString("member_id", null) 
                ?: "device_${System.currentTimeMillis()}"
            
            val result = activationCodeService.redeemCode(code, userId)
            
            result.fold(
                onSuccess = { activationResult ->
                    if (activationResult.success) {
                        _memberInfo.value = activationResult.memberInfo
                        _activationState.value = ActivationState.Success(
                            message = activationResult.message,
                            remainingDays = activationResult.remainingDays
                        )
                    } else {
                        _activationState.value = ActivationState.Error(
                            message = activationResult.message
                        )
                    }
                },
                onFailure = { error ->
                    _activationState.value = ActivationState.Error(
                        message = error.message ?: "激活码无效"
                    )
                }
            )
        }
    }

    // 激活码状态
    sealed class ActivationState {
        object Loading : ActivationState()
        data class Success(val message: String, val remainingDays: Int) : ActivationState()
        data class Error(val message: String) : ActivationState()
    }
}
