package com.jiliu.launcher.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.jiliu.launcher.model.PayMethod
import com.jiliu.launcher.model.PayOrder
import com.jiliu.launcher.model.PayResult
import com.jiliu.launcher.model.PayStatus
import com.jiliu.launcher.model.VipPackage
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * 微信支付服务（模拟实现）
 * 实际集成需要配置微信AppId
 */
class WechatPayService(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    // 模拟支付回调
    var paymentCallback: ((PayResult) -> Unit)? = null

    companion object {
        // TODO: 替换为实际的微信AppId
        const val WECHAT_APP_ID = "wx1234567890abcd"
        
        // 微信商户Id
        const val WECHAT_MCH_ID = "1234567890"
        
        // 微信API密钥
        const val WECHAT_API_KEY = "YOUR_API_KEY"
    }

    /**
     * 创建微信支付订单
     * 实际项目中应调用后端API生成预支付订单
     */
    suspend fun createPayOrder(vipPackage: VipPackage): Result<PayOrder> {
        return try {
            // 模拟调用后端API创建预支付订单
            delay(500)
            
            val order = PayOrder(
                orderId = "WX${System.currentTimeMillis()}${UUID.randomUUID().toString().take(8).uppercase()}",
                packageId = vipPackage.id,
                packageName = vipPackage.name,
                amount = vipPackage.currentPrice,
                payMethod = PayMethod.WECHAT,
                status = PayStatus.PENDING
            )
            
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 发起微信支付
     * 模拟实现，实际需要：
     * 1. 调用后端获取预支付订单信息
     * 2. 使用微信SDK调起支付
     */
    fun pay(order: PayOrder, callback: (PayResult) -> Unit) {
        paymentCallback = callback
        
        // 模拟支付流程
        simulatePayment(order, callback)
    }

    /**
     * 模拟支付（演示用）
     * 实际项目中替换为真实的微信支付SDK调用
     * 
     * 真实集成代码示例：
     * ```kotlin
     * val req = PayReq()
     * req.appId = WECHAT_APP_ID
     * req.partnerId = WECHAT_MCH_ID
     * req.prepayId = prepayId  // 从服务端获取
     * req.packageValue = "Sign=WXPay"
     * req.nonceStr = nonceStr   // 从服务端获取
     * req.timeStamp = timeStamp // 从服务端获取
     * req.sign = sign           // 从服务端获取签名
     * api.sendReq(req)
     * ```
     */
    private fun simulatePayment(order: PayOrder, callback: (PayResult) -> Unit) {
        Thread {
            // 模拟支付延迟
            Thread.sleep(1500)
            
            mainHandler.post {
                // 模拟支付成功
                callback(PayResult(
                    success = true,
                    message = "微信支付成功",
                    orderId = order.orderId,
                    packageInfo = null // 实际应返回套餐信息
                ))
            }
        }.start()
    }

    /**
     * 处理微信支付结果回调
     * 实际项目中在WXPayEntryActivity中处理
     */
    fun handlePayCallback(errCode: Int, errStr: String?): PayResult {
        return when (errCode) {
            0 -> PayResult(
                success = true,
                message = "支付成功"
            )
            -1 -> PayResult(
                success = false,
                message = errStr ?: "支付失败"
            )
            -2 -> PayResult(
                success = false,
                message = "用户取消支付"
            )
            else -> PayResult(
                success = false,
                message = "支付异常: $errCode"
            )
        }
    }

    /**
     * 验证支付结果（实际项目中应调用后端验证）
     */
    suspend fun verifyPayResult(orderId: String): Boolean {
        // 模拟验证
        delay(300)
        return true
    }

    /**
     * 检查是否安装微信
     */
    fun isWechatInstalled(): Boolean {
        return try {
            val uri = android.net.Uri.parse("weixin://")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            context.packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 注册微信支付回调（实际项目中调用）
     */
    fun registerCallback() {
        // 实际项目中需要在应用入口注册微信支付回调
        // IWXAPI api = WXAPIFactory.createWXAPI(context, WECHAT_APP_ID);
        // api.registerApp(WECHAT_APP_ID);
    }
}
