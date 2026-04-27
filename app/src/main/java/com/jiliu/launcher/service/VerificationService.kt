package com.jiliu.launcher.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.random.Random

/**
 * 验证码服务类
 * 支持手机短信验证码和邮箱验证码
 * 验证码有效期：5分钟
 * 发送频率限制：60秒一次
 */
class VerificationService(private val context: Context) {

    companion object {
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
        
        // SMTP配置（需从SECRET.md获取）
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"
        private const val SMTP_USERNAME = "your_email@gmail.com"
        private const val SMTP_PASSWORD = "your_app_password"
        private const val SMTP_FROM_NAME = "极流桌面"
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
            
            // 发送邮件
            val sendResult = sendEmail(email, code)
            
            if (sendResult) {
                // 保存验证码
                saveCode("email", email, code)
                // 设置冷却时间
                setCooldown("email", email)
                Result.success(Unit)
            } else {
                Result.failure(Exception("发送失败，请稍后重试"))
            }
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

    /**
     * 发送邮箱验证码
     */
    private fun sendEmail(email: String, code: String): Boolean {
        return try {
            // 判断是否配置了真实的SMTP服务器
            if (SMTP_USERNAME == "your_email@gmail.com") {
                // 使用模拟方式
                preferencesManager.lastVerificationCode = code
                return true
            }

            // 邮件内容
            val subject = "【极流桌面】验证码"
            val content = """
                <html>
                <body style="font-family: 'Microsoft YaHei', Arial, sans-serif;">
                    <div style="max-width: 500px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #FF6B35; margin-bottom: 20px;">极流桌面</h2>
                        <div style="background: #fff3e0; border-radius: 10px; padding: 30px; text-align: center;">
                            <p style="color: #333; font-size: 16px; margin-bottom: 20px;">您的验证码是：</p>
                            <p style="color: #FF6B35; font-size: 36px; font-weight: bold; letter-spacing: 8px; margin: 0;">$code</p>
                            <p style="color: #666; font-size: 12px; margin-top: 20px;">验证码有效期5分钟，请勿泄露给他人</p>
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            // 配置邮件属性
            val props = Properties().apply {
                put("mail.smtp.host", SMTP_HOST)
                put("mail.smtp.port", SMTP_PORT)
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
            }

            // 创建认证器
            val authenticator = object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD)
                }
            }

            // 创建邮件会话
            val session = Session.getInstance(props, authenticator)

            // 创建邮件消息
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SMTP_USERNAME, SMTP_FROM_NAME))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                setSubject(subject, "UTF-8")
                setContent(content, "text/html; charset=UTF-8")
            }

            // 发送邮件
            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
