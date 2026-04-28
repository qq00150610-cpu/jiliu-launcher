package com.jiliu.launcher.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.jiliu.launcher.config.MemberConfig
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 会员API服务
 * 负责与后端会员系统通信
 */
class MemberApiService(private val context: Context) {
    
    companion object {
        private const val TAG = "MemberApiService"
    }
    
    private val gson = Gson()
    private val preferencesManager = PreferencesManager(context)
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(MemberConfig.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(MemberConfig.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(MemberConfig.API_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    // ==================== API 请求方法 ====================
    
    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @param type 类型: register, reset, login
     */
    suspend fun sendCode(email: String, type: String = "register"): ApiResult<Unit> {
        return postRequest(
            MemberConfig.Endpoints.SEND_CODE,
            mapOf("email" to email, "type" to type)
        )
    }
    
    /**
     * 验证验证码
     */
    suspend fun verifyCode(email: String, code: String, type: String = "register"): ApiResult<Unit> {
        return postRequest(
            MemberConfig.Endpoints.VERIFY_CODE,
            mapOf("email" to email, "code" to code, "type" to type)
        )
    }
    
    /**
     * 会员注册
     */
    suspend fun register(
        email: String,
        password: String,
        verificationCode: String,
        deviceId: String? = null
    ): ApiResult<MemberResponse> {
        val body = mutableMapOf(
            "email" to email,
            "password" to password,
            "verificationCode" to verificationCode
        )
        deviceId?.let { body["deviceId"] = it }
        
        return postRequest(MemberConfig.Endpoints.REGISTER, body)
    }
    
    /**
     * 会员登录（密码登录）
     */
    suspend fun login(
        email: String,
        password: String,
        deviceId: String? = null
    ): ApiResult<MemberResponse> {
        val body = mutableMapOf(
            "email" to email,
            "password" to password
        )
        deviceId?.let { body["deviceId"] = it }
        
        return postRequest(MemberConfig.Endpoints.LOGIN, body)
    }
    
    /**
     * 会员登录（验证码登录）
     */
    suspend fun loginWithCode(
        email: String,
        verificationCode: String,
        deviceId: String? = null
    ): ApiResult<MemberResponse> {
        val body = mutableMapOf(
            "email" to email,
            "verificationCode" to verificationCode
        )
        deviceId?.let { body["deviceId"] = it }
        
        return postRequest(MemberConfig.Endpoints.LOGIN, body)
    }
    
    /**
     * 重置密码
     */
    suspend fun resetPassword(
        email: String,
        newPassword: String,
        verificationCode: String
    ): ApiResult<Unit> {
        return postRequest(
            MemberConfig.Endpoints.RESET_PASSWORD,
            mapOf(
                "email" to email,
                "newPassword" to newPassword,
                "verificationCode" to verificationCode
            )
        )
    }
    
    /**
     * 获取会员信息
     */
    suspend fun getMemberInfo(email: String): ApiResult<MemberInfoResponse> {
        return getRequest("${MemberConfig.Endpoints.MEMBER_INFO}?email=$email")
    }
    
    /**
     * 检查VIP状态
     */
    suspend fun checkVip(deviceId: String): ApiResult<VipResponse> {
        return postRequest(
            MemberConfig.Endpoints.CHECK_VIP,
            mapOf("device_id" to deviceId)
        )
    }
    
    // ==================== 网络请求方法 ====================
    
    private suspend inline fun <reified T> postRequest(endpoint: String, body: Map<String, Any?>): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(body.filterValues { it != null })
                val requestBody = jsonBody.toRequestBody(jsonMediaType)
                
                val request = Request.Builder()
                    .url(MemberConfig.getApiUrl(endpoint))
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                parseResponseInline(response)
            } catch (e: Exception) {
                Log.e(TAG, "请求失败: ${e.message}")
                ApiResult.Error("网络请求失败: ${e.message}")
            }
        }
    }
    
    private suspend inline fun <reified T> getRequest(url: String): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(MemberConfig.getApiUrl(url))
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                parseResponseInline(response)
            } catch (e: Exception) {
                Log.e(TAG, "请求失败: ${e.message}")
                ApiResult.Error("网络请求失败: ${e.message}")
            }
        }
    }
    
    private inline fun <reified T> parseResponseInline(response: Response): ApiResult<T> {
        return try {
            val body = response.body?.string()
            if (body == null) {
                return ApiResult.Error("服务器无响应")
            }
            
            Log.d(TAG, "响应: $body")
            
            val jsonObject = com.google.gson.JsonParser.parseString(body).asJsonObject
            val success = jsonObject.get("success")?.asBoolean ?: false
            
            if (success) {
                // 尝试解析数据
                val data = jsonObject.get("data")
                if (data != null && !data.isJsonNull) {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(gson.fromJson(data, T::class.java))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    ApiResult.Success(Unit as T)
                }
            } else {
                val message = jsonObject.get("message")?.asString ?: "操作失败"
                val code = jsonObject.get("code")?.asString
                ApiResult.Failure(message, code)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析响应失败: ${e.message}")
            ApiResult.Error("解析响应失败: ${e.message}")
        }
    }
    
    // ==================== 结果类 ====================
    
    sealed class ApiResult<T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Failure<T>(val message: String, val code: String? = null) : ApiResult<T>()
        data class Error<T>(val message: String) : ApiResult<T>()
    }
    
    // ==================== 响应数据类 ====================
    
    data class MemberResponse(
        val id: String,
        val email: String,
        val createdAt: String,
        val boundDeviceId: String?,
        val isVip: Boolean,
        val vipExpireTime: String?,
        val vipRemainingDays: Int
    )
    
    data class MemberInfoResponse(
        val id: String,
        val email: String,
        val createdAt: String,
        val boundDeviceId: String?,
        val isVip: Boolean,
        val vipExpireTime: String?,
        val vipRemainingDays: Int
    )
    
    data class VipResponse(
        val is_vip: Boolean,
        val remaining_days: Int,
        val vip_expire_at: Long?,
        val activation_code: String?,
        val message: String?
    )
}
