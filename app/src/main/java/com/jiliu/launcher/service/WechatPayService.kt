package com.jiliu.launcher.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jiliu.launcher.config.PayConfig
import com.jiliu.launcher.model.PayMethod
import com.jiliu.launcher.model.PayOrder
import com.jiliu.launcher.model.PayResult
import com.jiliu.launcher.model.PayStatus
import com.jiliu.launcher.model.VipPackage
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 微信支付服务
 * 支持真实支付和沙箱模式
 */
class WechatPayService(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    
    // 支付回调
    var paymentCallback: ((PayResult) -> Unit)? = null

    companion object {
        private const val TAG = "WechatPayService"
        
        // 微信配置
        private const val WECHAT_APP_ID = PayConfig.WECHAT_APP_ID
        private const val WECHAT_MCH_ID = PayConfig.WECHAT_MCH_ID
        private const val USE_SANDBOX = PayConfig.WECHAT_USE_SANDBOX
        
        // OkHttp客户端
        private val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .apply {
                    if (PayConfig.DEBUG_MODE) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                    }
                }
                .build()
        }
    }

    /**
     * 创建微信支付订单
     * 调用后端API创建微信预支付订单
     */
    suspend fun createPayOrder(vipPackage: VipPackage, userId: String? = null): Result<PayOrder> {
        return withContext(Dispatchers.IO) {
            try {
                // 如果启用模拟支付，返回模拟订单
                if (PayConfig.ENABLE_SIMULATE_PAY && USE_SANDBOX) {
                    return@withContext createSimulateOrder(vipPackage)
                }
                
                // 检查微信AppID配置
                if (WECHAT_APP_ID.isEmpty() || WECHAT_MCH_ID.isEmpty()) {
                    Log.w(TAG, "微信支付未配置，使用模拟模式")
                    return@withContext createSimulateOrder(vipPackage)
                }
                
                // 构建请求
                val requestBody = mapOf(
                    "packageId" to vipPackage.id,
                    "packageName" to vipPackage.name,
                    "amount" to vipPackage.currentPrice,
                    "userId" to (userId ?: "")
                )
                
                val jsonBody = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url(PayConfig.getApiUrl(PayConfig.Endpoints.WECHAT_CREATE))
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                // 发送请求
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    return@withContext Result.failure(Exception("服务器无响应"))
                }
                
                Log.d(TAG, "创建微信订单响应: $responseBody")
                
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                
                if (jsonResponse.get("success")?.asBoolean == true) {
                    val orderId = jsonResponse.get("orderId")?.asString 
                        ?: "WX${System.currentTimeMillis()}"
                    
                    val order = PayOrder(
                        orderId = orderId,
                        packageId = vipPackage.id,
                        packageName = vipPackage.name,
                        amount = vipPackage.currentPrice,
                        payMethod = PayMethod.WECHAT,
                        status = PayStatus.PENDING
                    )
                    
                    Result.success(order)
                } else {
                    val message = jsonResponse.get("message")?.asString ?: "订单创建失败"
                    Result.failure(Exception(message))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "创建微信订单失败", e)
                
                // 失败时使用模拟订单
                if (PayConfig.ENABLE_SIMULATE_PAY) {
                    Log.d(TAG, "使用模拟微信订单")
                    createSimulateOrder(vipPackage)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * 创建模拟订单（用于测试）
     */
    private fun createSimulateOrder(vipPackage: VipPackage): Result<PayOrder> {
        val order = PayOrder(
            orderId = "WX${System.currentTimeMillis()}${UUID.randomUUID().toString().take(8).uppercase()}",
            packageId = vipPackage.id,
            packageName = vipPackage.name,
            amount = vipPackage.currentPrice,
            payMethod = PayMethod.WECHAT,
            status = PayStatus.PENDING
        )
        return Result.success(order)
    }

    /**
     * 发起微信支付
     * 沙箱模式：模拟支付流程
     * 正式模式：调起微信支付SDK
     */
    fun pay(order: PayOrder, callback: (PayResult) -> Unit) {
        paymentCallback = callback
        
        if (PayConfig.ENABLE_SIMULATE_PAY && (USE_SANDBOX || WECHAT_APP_ID.isEmpty())) {
            // 模拟支付流程
            simulatePayment(order, callback)
        } else {
            // TODO: 调起真实微信支付SDK
            // 真实集成需要：
            // 1. 从后端获取预支付订单信息
            // 2. 使用微信SDK调起支付
            Log.d(TAG, "准备调起微信支付SDK，订单: ${order.orderId}")
            
            // 微信SDK调用示例：
            // val req = PayReq()
            // req.appId = WECHAT_APP_ID
            // req.partnerId = WECHAT_MCH_ID
            // req.prepayId = prepayId
            // req.packageValue = "Sign=WXPay"
            // req.nonceStr = nonceStr
            // req.timeStamp = timeStamp
            // req.sign = sign
            // api.sendReq(req)
        }
    }

    /**
     * 模拟支付（沙箱模式）
     */
    private fun simulatePayment(order: PayOrder, callback: (PayResult) -> Unit) {
        Thread {
            try {
                // 模拟支付延迟
                Thread.sleep(1500)
                
                mainHandler.post {
                    callback(PayResult(
                        success = true,
                        message = "微信支付成功（沙箱模式）",
                        orderId = order.orderId,
                        packageInfo = null
                    ))
                }
            } catch (e: InterruptedException) {
                mainHandler.post {
                    callback(PayResult(
                        success = false,
                        message = "支付已取消",
                        orderId = order.orderId
                    ))
                }
            }
        }.start()
    }

    /**
     * 处理微信支付回调结果
     * @param errorCode 错误码 (0=成功, -1=失败, -2=取消)
     */
    fun handlePayCallback(errorCode: Int, result: Map<String, Any>?): PayResult {
        return when (errorCode) {
            0 -> {
                Log.d(TAG, "微信支付成功")
                val orderId = result?.get("out_trade_no") as? String
                PayResult(
                    success = true,
                    message = "支付成功",
                    orderId = orderId,
                    packageInfo = null
                )
            }
            -1 -> {
                Log.e(TAG, "微信支付失败")
                PayResult(
                    success = false,
                    message = "支付失败",
                    orderId = result?.get("out_trade_no") as? String
                )
            }
            -2 -> {
                Log.d(TAG, "微信支付取消")
                PayResult(
                    success = false,
                    message = "支付已取消",
                    orderId = result?.get("out_trade_no") as? String
                )
            }
            else -> {
                Log.e(TAG, "微信支付未知状态: $errorCode")
                PayResult(
                    success = false,
                    message = "支付异常",
                    orderId = result?.get("out_trade_no") as? String
                )
            }
        }
    }

    /**
     * 查询订单支付状态
     */
    suspend fun queryOrderStatus(orderId: String): Result<PayStatus> {
        return withContext(Dispatchers.IO) {
            try {
                if (PayConfig.ENABLE_SIMULATE_PAY) {
                    return@withContext Result.success(PayStatus.SUCCESS)
                }
                
                val request = Request.Builder()
                    .url(PayConfig.getApiUrl("${PayConfig.Endpoints.PAY_STATUS}/$orderId"))
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    return@withContext Result.failure(Exception("服务器无响应"))
                }
                
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                
                if (jsonResponse.get("success")?.asBoolean == true) {
                    val statusStr = jsonResponse.get("status")?.asString ?: "pending"
                    val status = PayStatus.valueOf(statusStr.uppercase())
                    Result.success(status)
                } else {
                    Result.failure(Exception("查询失败"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "查询微信订单状态失败", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 注册微信支付回调
     * 需要在Application或Activity中调用
     */
    fun registerCallback() {
        // IWXAPIEventHandler接口由调用方实现
        Log.d(TAG, "微信支付回调已注册")
    }
}
