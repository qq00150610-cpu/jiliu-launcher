package com.jiliu.launcher.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.jiliu.launcher.model.ActivationCode
import com.jiliu.launcher.model.ActivationResult
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * 激活码服务
 */
class ActivationCodeService(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()

    companion object {
        // 激活码前缀标识
        private const val PREFIX_MONTH = "JLM"   // 月卡
        private const val PREFIX_SEASON = "JLS"  // 季卡
        private const val PREFIX_YEAR = "JLY"    // 年卡
        
        // 激活码长度
        private const val CODE_LENGTH = 16
        
        // 存储键
        private const val KEY_ACTIVATION_CODES = "activation_codes"
        private const val KEY_PURCHASED_CODES = "purchased_activation_codes"
    }

    /**
     * 兑换激活码
     */
    suspend fun redeemCode(code: String, userId: String): Result<ActivationResult> {
        return try {
            // 模拟网络请求延迟
            delay(800)
            
            // 验证激活码格式
            if (!isValidCodeFormat(code)) {
                return Result.failure(Exception("激活码格式不正确"))
            }
            
            // 验证激活码是否存在（模拟从服务器获取）
            val activationCode = validateActivationCode(code)
            if (activationCode == null) {
                return Result.failure(Exception("激活码无效或已过期"))
            }
            
            if (activationCode.used) {
                return Result.failure(Exception("激活码已被使用"))
            }
            
            // 标记激活码为已使用
            markCodeAsUsed(code, userId)
            
            // 计算新的VIP到期时间
            val currentExpireTime = preferencesManager.vipExpireTime
            val now = System.currentTimeMillis()
            
            val newExpireTime: Long
            if (currentExpireTime > now) {
                // 已有VIP，在原有基础上续期
                newExpireTime = currentExpireTime + (activationCode.duration * 24 * 60 * 60 * 1000L)
            } else {
                // 无VIP或已过期，从现在开始计算
                newExpireTime = now + (activationCode.duration * 24 * 60 * 60 * 1000L)
            }
            
            // 更新VIP状态
            preferencesManager.isVip = true
            preferencesManager.vipExpireTime = newExpireTime
            
            val memberInfo = MemberInfo(
                isVip = true,
                expireTime = newExpireTime,
                memberLevel = MemberLevel.VIP,
                features = getVipFeatures()
            )
            
            val remainingDays = ((newExpireTime - now) / (24 * 60 * 60 * 1000L)).toInt()
            
            Result.success(ActivationResult(
                success = true,
                message = "${activationCode.packageType}开通成功",
                memberInfo = memberInfo,
                remainingDays = remainingDays
            ))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 验证激活码格式
     */
    fun isValidCodeFormat(code: String): Boolean {
        if (code.length != CODE_LENGTH) return false
        
        val prefix = code.substring(0, 3).uppercase()
        return prefix in listOf(PREFIX_MONTH, PREFIX_SEASON, PREFIX_YEAR)
    }

    /**
     * 获取激活码对应的套餐类型
     */
    fun getPackageType(code: String): String {
        val prefix = code.substring(0, 3).uppercase()
        return when (prefix) {
            PREFIX_MONTH -> "月卡"
            PREFIX_SEASON -> "季卡"
            PREFIX_YEAR -> "年卡"
            else -> "未知"
        }
    }

    /**
     * 获取激活码对应的天数
     */
    fun getDuration(code: String): Int {
        val prefix = code.substring(0, 3).uppercase()
        return when (prefix) {
            PREFIX_MONTH -> 30
            PREFIX_SEASON -> 90
            PREFIX_YEAR -> 365
            else -> 0
        }
    }

    /**
     * 生成激活码（用于管理员后台，实际项目中应服务端生成）
     */
    fun generateActivationCode(packageType: String): String {
        val prefix = when (packageType) {
            "月卡" -> PREFIX_MONTH
            "季卡" -> PREFIX_SEASON
            "年卡" -> PREFIX_YEAR
            else -> PREFIX_MONTH
        }
        
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = java.util.Random()
        val suffix = (1..CODE_LENGTH.minus(3))
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
        
        return prefix + suffix
    }

    /**
     * 模拟验证激活码（实际应调用服务端API）
     */
    private fun validateActivationCode(code: String): ActivationCode? {
        // 模拟一些可用的激活码
        val mockCodes = mapOf(
            "JLM123456789ABCD" to ActivationCode(
                code = "JLM123456789ABCD",
                packageType = "月卡",
                duration = 30,
                used = false
            ),
            "JLS123456789ABCD" to ActivationCode(
                code = "JLS123456789ABCD",
                packageType = "季卡",
                duration = 90,
                used = false
            ),
            "JLY123456789ABCD" to ActivationCode(
                code = "JLY123456789ABCD",
                packageType = "年卡",
                duration = 365,
                used = false
            )
        )
        
        return mockCodes[code.uppercase()] ?: 
               ActivationCode(
                   code = code,
                   packageType = getPackageType(code),
                   duration = getDuration(code),
                   used = false
               )
    }

    /**
     * 标记激活码为已使用
     */
    private fun markCodeAsUsed(code: String, userId: String) {
        // 实际项目中应调用服务端API标记
        // 这里仅做本地记录
        val usedCodes = getUsedCodes().toMutableMap()
        usedCodes[code.uppercase()] = mapOf(
            "usedTime" to System.currentTimeMillis(),
            "userId" to userId
        )
        saveUsedCodes(usedCodes)
    }

    /**
     * 获取已使用的激活码记录
     */
    private fun getUsedCodes(): Map<String, Map<String, Any>> {
        val json = preferencesManager.getString(KEY_ACTIVATION_CODES, "{}")
        return try {
            gson.fromJson(json, Map::class.java) as? Map<String, Map<String, Any>> ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 保存已使用的激活码记录
     */
    private fun saveUsedCodes(codes: Map<String, Map<String, Any>>) {
        preferencesManager.setString(KEY_ACTIVATION_CODES, gson.toJson(codes))
    }

    /**
     * 获取VIP功能列表
     */
    private fun getVipFeatures(): List<String> {
        return listOf(
            "解锁全部主题",
            "去广告",
            "优先客服支持",
            "云端壁纸同步",
            "高级手势操作",
            "自定义dock栏",
            "消息聚合",
            "数据备份与恢复"
        )
    }
}
