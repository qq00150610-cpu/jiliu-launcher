package com.jiliu.launcher.repository

import android.content.Context
import com.google.gson.Gson
import com.jiliu.launcher.model.*
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class MemberRepository(context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val gson = Gson()

    /**
     * Get current user info
     */
    fun getCurrentUser(): User? {
        val userJson = preferencesManager.currentUserJson ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Check if user is VIP
     */
    fun isVip(): Boolean {
        val user = getCurrentUser()
        return user?.isVip ?: preferencesManager.isVip
    }

    /**
     * Get member info from current user or legacy VIP status
     */
    fun getMemberInfo(): MemberInfo {
        val user = getCurrentUser()
        return if (user != null) {
            MemberInfo(
                isVip = user.isVip,
                expireTime = user.vipExpireTime,
                memberLevel = user.vipLevel,
                features = getVipFeatures()
            )
        } else {
            MemberInfo(
                isVip = preferencesManager.isVip,
                expireTime = preferencesManager.vipExpireTime,
                memberLevel = if (preferencesManager.isVip) MemberLevel.VIP else MemberLevel.NONE,
                features = getVipFeatures()
            )
        }
    }

    /**
     * Register with phone + verification code
     */
    suspend fun registerWithPhone(phone: String, code: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Mock verification code validation
            if (code.length != 6) {
                return@withContext Result.failure(Exception("验证码格式错误"))
            }

            // Mock: Create new user
            val user = User(
                userId = UUID.randomUUID().toString(),
                phone = phone,
                username = "用户${phone.takeLast(4)}",
                vipLevel = MemberLevel.NONE,
                createdAt = System.currentTimeMillis()
            )

            // Save user
            preferencesManager.currentUserJson = gson.toJson(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register with username + password
     */
    suspend fun registerWithPassword(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (username.length < 3) {
                return@withContext Result.failure(Exception("用户名至少3个字符"))
            }
            if (password.length < 6) {
                return@withContext Result.failure(Exception("密码至少6位"))
            }

            // Mock: Create new user
            val user = User(
                userId = UUID.randomUUID().toString(),
                username = username,
                vipLevel = MemberLevel.NONE,
                createdAt = System.currentTimeMillis()
            )

            // Save user and password hash
            preferencesManager.currentUserJson = gson.toJson(user)
            preferencesManager.userPasswordHash = password.hashCode().toString()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with phone + code
     */
    suspend fun loginWithPhone(phone: String, code: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            if (code.length != 6) {
                return@withContext Result.failure(Exception("验证码格式错误"))
            }

            // Mock: Find or create user by phone
            val user = User(
                userId = UUID.randomUUID().toString(),
                phone = phone,
                username = "用户${phone.takeLast(4)}",
                vipLevel = MemberLevel.VIP, // Mock: grant VIP on login
                vipExpireTime = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis,
                createdAt = System.currentTimeMillis()
            )

            preferencesManager.currentUserJson = gson.toJson(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with username + password
     */
    suspend fun loginWithPassword(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // Mock: Accept any username/password for demo
            val user = User(
                userId = UUID.randomUUID().toString(),
                username = username,
                vipLevel = MemberLevel.VIP, // Mock: grant VIP
                vipExpireTime = Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis,
                createdAt = System.currentTimeMillis()
            )

            preferencesManager.currentUserJson = gson.toJson(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout
     */
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            preferencesManager.currentUserJson = null
        }
    }

    /**
     * Send verification code (mock implementation)
     */
    suspend fun sendVerificationCode(phone: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Mock: Simulate sending code
            Thread.sleep(500)
            
            // Store code for verification (mock)
            preferencesManager.lastVerificationCode = "123456"
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activate VIP with code (legacy support)
     */
    suspend fun activate(code: String): Result<MemberInfo> = withContext(Dispatchers.IO) {
        try {
            val result = validateActivationCode(code)
            
            if (result.isSuccess) {
                val memberInfo = result.getOrNull()!!
                preferencesManager.isVip = true
                preferencesManager.vipExpireTime = memberInfo.expireTime
                
                // Update current user if logged in
                getCurrentUser()?.let { user ->
                    val updatedUser = user.copy(
                        vipLevel = memberInfo.memberLevel,
                        vipExpireTime = memberInfo.expireTime
                    )
                    preferencesManager.currentUserJson = gson.toJson(updatedUser)
                }
                
                Result.success(memberInfo)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("激活失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate activation code (mock implementation)
     */
    private fun validateActivationCode(code: String): Result<MemberInfo> {
        if (code.length < 10) {
            return Result.failure(Exception("无效的激活码"))
        }
        
        // Mock: generate expiration time 1 year from now
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, 1)
        
        return Result.success(
            MemberInfo(
                isVip = true,
                expireTime = calendar.timeInMillis,
                memberLevel = MemberLevel.VIP,
                features = getVipFeatures()
            )
        )
    }

    /**
     * Get VIP features
     */
    fun getVipFeatures(): List<String> {
        return listOf(
            "无广告体验",
            "会员专属壁纸",
            "会员专属主题",
            "在线壁纸库",
            "自定义布局",
            "悬浮音乐胶囊",
            "边缘手势增强",
            "自动壁纸更换",
            "文件管理器",
            "视频播放器",
            "应用管理增强",
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
            
            getCurrentUser()?.let { user ->
                val updatedUser = user.copy(
                    vipLevel = MemberLevel.NONE,
                    vipExpireTime = 0
                )
                preferencesManager.currentUserJson = gson.toJson(updatedUser)
            }
        }
    }

    /**
     * Check if user has permission for VIP feature
     */
    fun hasVipPermission(): Boolean {
        return isLoggedIn() && isVip()
    }
}
