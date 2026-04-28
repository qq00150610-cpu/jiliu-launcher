package com.jiliu.launcher.service

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jiliu.launcher.model.ActivationResult
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 激活码服务 - 联网验证版
 * 服务器地址：http://47.92.220.102:3200
 * 
 * 功能：
 * 1. 激活码验证（设备绑定）
 * 2. 新用户免费试用15天
 */
class ActivationCodeService(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val gson = Gson()
    
    // HTTP客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val SERVER_URL = "http://47.92.220.102:3200"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CACHED_VIP = "cached_vip_info"
        private const val KEY_TRIAL_USED = "trial_used"  // 是否已使用过试用
        private const val KEY_TRIAL_START_TIME = "trial_start_time"  // 试用开始时间
        private const val TRIAL_DAYS = 15  // 试用天数
    }

    /**
     * 获取设备唯一ID
     */
    private fun getDeviceId(): String {
        var deviceId = preferencesManager.getString(KEY_DEVICE_ID, "")
        
        if (deviceId.isEmpty()) {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
            
            deviceId = "${Build.BRAND}_${Build.MODEL}_${androidId}"
                .replace(" ", "_")
                .replace("[^a-zA-Z0-9_]".toRegex(), "")
                .take(32)
            
            preferencesManager.setString(KEY_DEVICE_ID, deviceId)
        }
        
        return deviceId
    }

    /**
     * 检查是否可以使用免费试用
     */
    fun canUseTrial(): Boolean {
        // 如果已经是VIP，不能使用试用
        if (preferencesManager.isVip && preferencesManager.vipExpireTime > System.currentTimeMillis()) {
            return false
        }
        // 检查是否已使用过试用
        return !preferencesManager.getBoolean(KEY_TRIAL_USED, false)
    }

    /**
     * 获取试用剩余天数
     */
    fun getTrialRemainingDays(): Int {
        val used = preferencesManager.getBoolean(KEY_TRIAL_USED, false)
        if (!used) return 0
        
        val startTime = preferencesManager.getLong(KEY_TRIAL_START_TIME, 0)
        if (startTime == 0L) return 0
        
        val expireTime = startTime + (TRIAL_DAYS * 24 * 60 * 60 * 1000L)
        val remaining = expireTime - System.currentTimeMillis()
        
        return if (remaining > 0) {
            ((remaining + 24 * 60 * 60 * 1000 - 1) / (24 * 60 * 60 * 1000)).toInt()
        } else 0
    }

    /**
     * 激活免费试用
     */
    fun activateTrial(): ActivationResult {
        if (!canUseTrial()) {
            return ActivationResult(
                success = false,
                message = if (preferencesManager.getBoolean(KEY_TRIAL_USED, false)) {
                    "您已使用过免费试用"
                } else {
                    "您已是VIP会员，无需试用"
                }
            )
        }

        val now = System.currentTimeMillis()
        val expireTime = now + (TRIAL_DAYS * 24 * 60 * 60 * 1000L)

        // 标记已使用试用
        preferencesManager.setBoolean(KEY_TRIAL_USED, true)
        preferencesManager.setLong(KEY_TRIAL_START_TIME, now)
        
        // 设置VIP状态
        preferencesManager.isVip = true
        preferencesManager.vipExpireTime = expireTime

        // 缓存VIP信息
        val vipInfo = mapOf(
            "is_vip" to true,
            "expire_time" to expireTime,
            "remaining_days" to TRIAL_DAYS,
            "package_type" to "免费试用",
            "is_trial" to true,
            "device_id" to getDeviceId()
        )
        preferencesManager.setString(KEY_CACHED_VIP, gson.toJson(vipInfo))

        return ActivationResult(
            success = true,
            message = "恭喜您获得${TRIAL_DAYS}天VIP免费试用！",
            memberInfo = MemberInfo(
                isVip = true,
                expireTime = expireTime,
                memberLevel = MemberLevel.VIP,
                features = getVipFeatures()
            ),
            remainingDays = TRIAL_DAYS
        )
    }

    /**
     * 兑换激活码（联网验证）
     */
    suspend fun redeemCode(code: String, userId: String): Result<ActivationResult> = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            
            val formBody = FormBody.Builder()
                .add("code", code.trim())
                .add("device_id", deviceId)
                .add("user_id", userId)
                .build()
            
            val request = Request.Builder()
                .url("$SERVER_URL/api/verify")
                .post(formBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val success = json.get("success")?.asBoolean ?: false
            val message = json.get("message")?.asString ?: "验证失败"
            
            if (success) {
                val remainingDays = json.get("remaining_days")?.asInt ?: 0
                val packageType = json.get("package_type")?.asString ?: ""
                val vipExpireAt = json.get("vip_expire_at")?.asLong ?: 0L
                
                // 更新本地VIP状态
                preferencesManager.isVip = true
                preferencesManager.vipExpireTime = vipExpireAt
                
                // 缓存VIP信息
                val vipInfo = mapOf(
                    "is_vip" to true,
                    "expire_time" to vipExpireAt,
                    "remaining_days" to remainingDays,
                    "package_type" to packageType,
                    "activation_code" to code,
                    "device_id" to deviceId
                )
                preferencesManager.setString(KEY_CACHED_VIP, gson.toJson(vipInfo))
                
                val memberInfo = MemberInfo(
                    isVip = true,
                    expireTime = vipExpireAt,
                    memberLevel = MemberLevel.VIP,
                    features = getVipFeatures()
                )
                
                Result.success(ActivationResult(
                    success = true,
                    message = message,
                    memberInfo = memberInfo,
                    remainingDays = remainingDays
                ))
            } else {
                Result.failure(Exception(message))
            }
            
        } catch (e: Exception) {
            // 网络错误，尝试离线验证
            val cachedResult = tryOfflineVerify(code)
            if (cachedResult != null) {
                Result.success(cachedResult)
            } else {
                Result.failure(Exception("网络连接失败，请检查网络后重试"))
            }
        }
    }

    /**
     * 离线验证（使用缓存）
     */
    private fun tryOfflineVerify(code: String): ActivationResult? {
        val cachedVipJson = preferencesManager.getString(KEY_CACHED_VIP, "")
        if (cachedVipJson.isEmpty()) return null
        
        try {
            val vipInfo = gson.fromJson(cachedVipJson, Map::class.java)
            val expireTime = (vipInfo["expire_time"] as? Number)?.toLong() ?: 0L
            
            if (expireTime > System.currentTimeMillis()) {
                val remainingDays = ((expireTime - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                
                return ActivationResult(
                    success = true,
                    message = "离线验证成功（已缓存）",
                    memberInfo = MemberInfo(
                        isVip = true,
                        expireTime = expireTime,
                        memberLevel = MemberLevel.VIP,
                        features = getVipFeatures()
                    ),
                    remainingDays = remainingDays
                )
            }
        } catch (e: Exception) {
        }
        
        return null
    }

    /**
     * 检查VIP状态（联网验证）
     */
    suspend fun checkVipStatus(): VipStatus = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            
            val formBody = FormBody.Builder()
                .add("device_id", deviceId)
                .build()
            
            val request = Request.Builder()
                .url("$SERVER_URL/api/check-vip")
                .post(formBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val isVip = json.get("is_vip")?.asBoolean ?: false
            
            if (isVip) {
                val remainingDays = json.get("remaining_days")?.asInt ?: 0
                val vipExpireAt = json.get("vip_expire_at")?.asLong ?: 0L
                
                preferencesManager.isVip = true
                preferencesManager.vipExpireTime = vipExpireAt
                
                VipStatus(
                    isVip = true,
                    remainingDays = remainingDays,
                    expireTime = vipExpireAt
                )
            } else {
                // 服务器无VIP记录，检查本地试用
                checkLocalVipStatus()
            }
            
        } catch (e: Exception) {
            checkLocalVipStatus()
        }
    }

    /**
     * 检查本地VIP状态（包括试用）
     */
    private fun checkLocalVipStatus(): VipStatus {
        // 检查试用
        val trialDays = getTrialRemainingDays()
        if (trialDays > 0) {
            val startTime = preferencesManager.getLong(KEY_TRIAL_START_TIME, 0)
            val expireTime = startTime + (TRIAL_DAYS * 24 * 60 * 60 * 1000L)
            
            preferencesManager.isVip = true
            preferencesManager.vipExpireTime = expireTime
            
            return VipStatus(
                isVip = true,
                remainingDays = trialDays,
                expireTime = expireTime,
                isTrial = true
            )
        }
        
        // 检查本地缓存
        val localExpireTime = preferencesManager.vipExpireTime
        val isLocalVip = preferencesManager.isVip && localExpireTime > System.currentTimeMillis()
        
        if (isLocalVip) {
            val remainingDays = ((localExpireTime - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            return VipStatus(
                isVip = true,
                remainingDays = remainingDays,
                expireTime = localExpireTime,
                isOffline = true
            )
        }
        
        preferencesManager.isVip = false
        return VipStatus(isVip = false)
    }

    private fun getVipFeatures(): List<String> {
        return listOf("解锁全部主题", "去广告", "优先客服支持", "云端壁纸同步", "语音助手服务")
    }

    data class VipStatus(
        val isVip: Boolean = false,
        val remainingDays: Int = 0,
        val expireTime: Long = 0,
        val isOffline: Boolean = false,
        val isTrial: Boolean = false
    )
}
