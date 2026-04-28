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
 * 支付宝支付服务
 * 支持真实支付和沙箱模式
 */
class AlipayService(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val gson = Gson()
    
    // 支付回调
    var paymentCallback: ((PayResult) -> Unit)? = null

    companion object {
        private const val TAG = "AlipayService"
        
        // 支付宝配置
        private val APP_ID = PayConfig.getAlipayAppId()
        private const val USE_SANDBOX = PayConfig.ALIPAY_USE_SANDBOX
        
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
     * 创建支付订单
     * 调用后端API创建支付宝订单
     */
    suspend fun createPayOrder(vipPackage: VipPackage, userId: String? = null): Result<PayOrder> {
        return withContext(Dispatchers.IO) {
            try {
                // 如果启用模拟支付，返回模拟订单
                if (PayConfig.ENABLE_SIMULATE_PAY && USE_SANDBOX) {
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
                    .url(PayConfig.getApiUrl(PayConfig.Endpoints.ALIPAY_CREATE))
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                // 发送请求
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (responseBody == null) {
                    return@withContext Result.failure(Exception("服务器无响应"))
                }
                
                Log.d(TAG, "创建订单响应: $responseBody")
                
                val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
                
                if (jsonResponse.get("success")?.asBoolean == true) {
                    val orderId = jsonResponse.get("orderId")?.asString 
                        ?: "ALI${System.currentTimeMillis()}"
                    
                    val order = PayOrder(
                        orderId = orderId,
                        packageId = vipPackage.id,
                        packageName = vipPackage.name,
                        amount = vipPackage.currentPrice,
                        payMethod = PayMethod.ALIPAY,
                        status = PayStatus.PENDING
                    )
                    
                    // 如果是沙箱模式，返回模拟支付信息
                    if (jsonResponse.get("sandbox")?.asBoolean == true) {
                        Result.success(order)
                    } else {
                        Result.success(order)
                    }
                } else {
                    val message = jsonResponse.get("message")?.asString ?: "订单创建失败"
                    Result.failure(Exception(message))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "创建订单失败", e)
                
                // 失败时使用模拟订单
                if (PayConfig.ENABLE_SIMULATE_PAY) {
                    Log.d(TAG, "使用模拟订单")
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
            orderId = "ALI${System.currentTimeMillis()}${UUID.randomUUID().toString().take(8).uppercase()}",
            packageId = vipPackage.id,
            packageName = vipPackage.name,
            amount = vipPackage.currentPrice,
            payMethod = PayMethod.ALIPAY,
            status = PayStatus.PENDING
        )
        return Result.success(order)
    }

    /**
     * 发起支付宝支付
     * 沙箱模式：模拟支付流程
     * 正式模式：调起支付宝SDK
     */
    fun pay(order: PayOrder, callback: (PayResult) -> Unit) {
        paymentCallback = callback
        
        if (PayConfig.ENABLE_SIMULATE_PAY && USE_SANDBOX) {
            // 模拟支付流程
            simulatePayment(order, callback)
        } else {
            // TODO: 调起真实支付宝SDK
            // 真实集成需要：
            // 1. 从后端获取签名后的订单信息
            // 2. 使用支付宝SDK调起支付
            Log.d(TAG, "准备调起支付宝SDK，订单: ${order.orderId}")
            
            // 这里可以集成真实的支付宝SDK调用
            // PayTask(context).pay()
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
                        message = "支付宝支付成功（沙箱模式）",
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
     * 处理支付宝回调结果
     * @param resultStatus 支付状态码 (9000=成功, 8000=处理中, 4000=失败, 6001=取消, 6002=网络错误)
     * @param result 支付宝返回的原始结果
     */
    fun handlePayCallback(resultStatus: String, result: String): PayResult {
        return when (resultStatus) {
            "9000" -> {
                Log.d(TAG, "支付宝支付成功")
                PayResult(
                    success = true,
                    message = "支付成功",
                    orderId = extractOrderId(result),
                    packageInfo = null
                )
            }
            "8000" -> {
                Log.d(TAG, "支付宝支付处理中")
                PayResult(
                    success = false,
                    message = "支付处理中，请稍后查询",
                    orderId = extractOrderId(result)
                )
            }
            "4000" -> {
                Log.e(TAG, "支付宝支付失败")
                PayResult(
                    success = false,
                    message = "支付失败",
                    orderId = extractOrderId(result)
                )
            }
            "6001" -> {
                Log.d(TAG, "支付宝支付取消")
                PayResult(
                    success = false,
                    message = "支付已取消",
                    orderId = extractOrderId(result)
                )
            }
            "6002" -> {
                Log.e(TAG, "支付宝网络错误")
                PayResult(
                    success = false,
                    message = "网络连接失败",
                    orderId = extractOrderId(result)
                )
            }
            else -> {
                Log.e(TAG, "支付宝未知状态: $resultStatus")
                PayResult(
                    success = false,
                    message = "支付异常",
                    orderId = extractOrderId(result)
                )
            }
        }
    }

    /**
     * 从支付宝返回结果中提取订单号
     */
    private fun extractOrderId(result: String): String? {
        return try {
            // 支付宝返回格式: resultStatus={status};result={json}
            val resultMatch = Regex("\"out_trade_no\"\\s*:\\s*\"([^\"]+)\"").find(result)
            resultMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "解析订单号失败", e)
            null
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
                Log.e(TAG, "查询订单状态失败", e)
                Result.failure(e)
            }
        }
    }
}
