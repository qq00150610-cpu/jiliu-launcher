package com.jiliu.launcher.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 验证码服务类
 * 支持手机短信验证码和邮箱验证码
 * 验证码有效期：5分钟
 * 发送频率限制：60秒一次
 */
class VerificationService(private val context: Context) {

    companion object {
        private const val TAG = "VerificationService"
        private const val PREFS_NAME = "verification_codes"
        private const val KEY_CODE_PREFIX = "verify_"
        private const val KEY_TIMESTAMP_PREFIX = "timestamp_"
        private const val KEY_COOLDOWN_PREFIX = "cooldown_"
        
        // 验证码有效期：5分钟
        private const val CODE_VALIDITY_MS = 5 * 60 * 1000L
        
        // 发送冷却时间：60秒
        private const val COOLDOWN_MS = 60 * 1000L
        
        // 验证码长度
        private const val CODE_LENGTH = 6
        
        // 阿里云短信配置（需从SECRET.md获取）
        private const val ALIYUN_ACCESS_KEY_ID = "YOUR_ACCESS_KEY_ID"
        private const val ALIYUN_ACCESS_KEY_SECRET = "YOUR_ACCESS_KEY_SECRET"
        private const val ALIYUN_SIGN_NAME = "极流桌面"
        private const val ALIYUN_TEMPLATE_CODE = "SMS_XXXXXXXXX"
        private const val ALIYUN_SMS_ENDPOINT = "dysmsapi.aliyuncs.com"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val preferencesManager = PreferencesManager(context)

    /**
     * 发送手机验证码
     * @param phone 手机号（仅支持+86中国号码）
     * @return Result<Unit>
     */
    suspend fun sendSmsCode(phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 验证手机号格式
            if (!isValidPhone(phone)) {
                return@withContext Result.failure(Exception("手机号格式不正确"))
            }

            // 检查发送频率限制
            if (isInCooldown("sms", phone)) {
                val remainingTime = getCooldownRemaining("sms", phone)
                return@withContext Result.failure(Exception("请${remainingTime}秒后再试"))
            }

            // 生成验证码
            val code = generateCode()
            
            // 调用阿里云短信API发送
            val sendResult = sendAliyunSms(phone, code)
            
            if (sendResult) {
                // 保存验证码
                saveCode("sms", phone, code)
                // 设置冷却时间
                setCooldown("sms", phone)
                Result.success(Unit)
            } else {
                Result.failure(Exception("发送失败，请稍后重试"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发送邮箱验证码
     * 注意：实际邮件发送需要配置后端服务器，此处使用模拟方式
     * @param email 邮箱地址
     * @return Result<Unit>
     */
    suspend fun sendEmailCode(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 验证邮箱格式
            if (!isValidEmail(email)) {
                return@withContext Result.failure(Exception("邮箱格式不正确"))
            }

            // 检查发送频率限制
            if (isInCooldown("email", email)) {
                val remainingTime = getCooldownRemaining("email", email)
                return@withContext Result.failure(Exception("请${remainingTime}秒后再试"))
            }

            // 生成验证码
            val code = generateCode()
            
            // 模拟发送邮件（实际需要后端服务）
            preferencesManager.lastVerificationCode = code
            Log.d(TAG, "邮箱验证码已保存: $code (实际发送需后端支持)")
            
            // 保存验证码
            saveCode("email", email, code)
            // 设置冷却时间
            setCooldown("email", email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 验证手机验证码
     * @param phone 手机号
     * @param code 用户输入的验证码
     * @return Boolean 验证是否通过
     */
    fun verifySmsCode(phone: String, code: String): Boolean {
        return verifyCode("sms", phone, code)
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 用户输入的验证码
     * @return Boolean 验证是否通过
     */
    fun verifyEmailCode(email: String, code: String): Boolean {
        return verifyCode("email", email, code)
    }

    /**
     * 清除验证码
     */
    fun clearCode(type: String, target: String) {
        val codeKey = "${KEY_CODE_PREFIX}${type}_$target"
        val timestampKey = "${KEY_TIMESTAMP_PREFIX}${type}_$target"
        prefs.edit()
            .remove(codeKey)
            .remove(timestampKey)
            .apply()
    }

    /**
     * 生成随机验证码
     */
    private fun generateCode(): String {
        val random = Random(System.currentTimeMillis())
        return (100000 + random.nextInt(900000)).toString()
    }

    /**
     * 验证手机号格式
     */
    private fun isValidPhone(phone: String): Boolean {
        if (phone.length != 11) return false
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    /**
     * 验证邮箱格式
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.matches(emailRegex)
    }

    /**
     * 保存验证码和发送时间
     */
    private fun saveCode(type: String, target: String, code: String) {
        val codeKey = "${KEY_CODE_PREFIX}${type}_$target"
        val timestampKey = "${KEY_TIMESTAMP_PREFIX}${type}_$target"
        val timestamp = System.currentTimeMillis()
        
        prefs.edit()
            .putString(codeKey, code)
            .putLong(timestampKey, timestamp)
            .apply()
    }

    /**
     * 验证验证码
     */
    private fun verifyCode(type: String, target: String, code: String): Boolean {
        val codeKey = "${KEY_CODE_PREFIX}${type}_$target"
        val timestampKey = "${KEY_TIMESTAMP_PREFIX}${type}_$target"
        
        val savedCode = prefs.getString(codeKey, null) ?: return false
        val timestamp = prefs.getLong(timestampKey, 0L)
        
        // 检查验证码是否正确
        if (savedCode != code) return false
        
        // 检查是否过期
        val now = System.currentTimeMillis()
        if (now - timestamp > CODE_VALIDITY_MS) {
            // 已过期，清除验证码
            clearCode(type, target)
            return false
        }
        
        // 验证成功后清除验证码（一次性）
        clearCode(type, target)
        return true
    }

    /**
     * 设置冷却时间
     */
    private fun setCooldown(type: String, target: String) {
        val cooldownKey = "${KEY_COOLDOWN_PREFIX}${type}_$target"
        val timestamp = System.currentTimeMillis()
        prefs.edit().putLong(cooldownKey, timestamp).apply()
    }

    /**
     * 检查是否在冷却中
     */
    private fun isInCooldown(type: String, target: String): Boolean {
        val cooldownKey = "${KEY_COOLDOWN_PREFIX}${type}_$target"
        val timestamp = prefs.getLong(cooldownKey, 0L)
        val now = System.currentTimeMillis()
        return (now - timestamp) < COOLDOWN_MS
    }

    /**
     * 获取剩余冷却时间（秒）
     */
    private fun getCooldownRemaining(type: String, target: String): Int {
        val cooldownKey = "${KEY_COOLDOWN_PREFIX}${type}_$target"
        val timestamp = prefs.getLong(cooldownKey, 0L)
        val now = System.currentTimeMillis()
        val remaining = COOLDOWN_MS - (now - timestamp)
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }

    /**
     * 发送阿里云短信
     */
    private fun sendAliyunSms(phone: String, code: String): Boolean {
        return try {
            // 判断是否配置了真实的阿里云API密钥
            if (ALIYUN_ACCESS_KEY_ID == "YOUR_ACCESS_KEY_ID") {
                // 使用模拟方式
                preferencesManager.lastVerificationCode = code
                return true
            }

            // 使用阿里云SDK发送真实短信
            // 注意：需要添加阿里云SDK依赖
            // implementation("com.aliyun:alibabacloud-dysmsapi20170525:2.0.24")
            
            // 以下是阿里云SDK调用示例（实际使用时取消注释）
            /*
            val config = Config().apply {
                accessKeyId = ALIYUN_ACCESS_KEY_ID
                accessKeySecret = ALIYUN_ACCESS_KEY_SECRET
                endpoint = ALIYUN_SMS_ENDPOINT
            }
            
            val client = Client(config)
            val sendRequest = SendSmsRequest().apply {
                signName = ALIYUN_SIGN_NAME
                templateCode = ALIYUN_TEMPLATE_CODE
                phoneNumbers = phone
                templateParam = """{"code":"$code"}"""
            }
            
            val response = client.sendSms(sendRequest)
            response.body?.code == "OK"
            */
            
            // 暂时使用模拟方式
            preferencesManager.lastVerificationCode = code
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
