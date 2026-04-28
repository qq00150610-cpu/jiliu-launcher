package com.jiliu.launcher.repository

import android.content.Context
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class MemberRepository(context: Context) {

    private val preferencesManager = PreferencesManager(context)

    /**
     * Get current member info
     */
    fun getMemberInfo(): MemberInfo {
        return MemberInfo(
            isVip = preferencesManager.isVip,
            expireTime = preferencesManager.vipExpireTime,
            activationCode = preferencesManager.activationCode,
            memberLevel = getMemberLevel()
        )
    }

    /**
     * Check if user is VIP
     */
    fun isVip(): Boolean {
        val isVip = preferencesManager.isVip
        val expireTime = preferencesManager.vipExpireTime
        
        return isVip && (expireTime == 0L || System.currentTimeMillis() < expireTime)
    }

    /**
     * Get member level
     */
    private fun getMemberLevel(): MemberLevel {
        return if (preferencesManager.isVip) {
            MemberLevel.VIP
        } else {
            MemberLevel.NONE
        }
    }

    /**
     * Activate with code
     */
    suspend fun activate(code: String): Result<MemberInfo> = withContext(Dispatchers.IO) {
        try {
            // Simulate activation - in production, this would call a server API
            val result = validateActivationCode(code)
            
            if (result.isSuccess) {
                val memberInfo = result.getOrNull()!!
                preferencesManager.isVip = true
                preferencesManager.vipExpireTime = memberInfo.expireTime
                preferencesManager.activationCode = code
                
                Result.success(memberInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Activation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate activation code (mock implementation)
     */
    private fun validateActivationCode(code: String): Result<MemberInfo> {
        // In production, this would validate against a server
        // For demo, accept codes in format: JILIU-XXXX-XXXX-XXXX
        
        if (code.length < 10) {
            return Result.failure(Exception("Invalid activation code"))
        }
        
        // Mock: generate expiration time 1 year from now
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, 1)
        
        return Result.success(
            MemberInfo(
                isVip = true,
                expireTime = calendar.timeInMillis,
                activationCode = code,
                memberLevel = MemberLevel.VIP,
                features = getVipFeatures()
            )
        )
    }

    /**
     * Get VIP features
     */
    private fun getVipFeatures(): List<String> {
        return listOf(
            "无广告体验",
            "所有主题解锁",
            "在线壁纸库",
            "自定义布局",
            "悬浮音乐胶囊",
            "边缘手势",
            "自动壁纸更换",
            "文件管理器",
            "视频播放器",
            "应用管理",
            "一键清理",
            "专属客服支持"
        )
    }

    /**
     * Extend VIP (mock implementation)
     */
    suspend fun extendVip(code: String): Result<MemberInfo> {
        return activate(code)
    }

    /**
     * Cancel VIP subscription
     */
    suspend fun cancelVip() {
        withContext(Dispatchers.IO) {
            preferencesManager.isVip = false
            preferencesManager.vipExpireTime = 0
            preferencesManager.activationCode = null
        }
    }
}
