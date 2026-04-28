package com.jiliu.launcher.model

/**
 * VIP套餐模型
 */
data class VipPackage(
    val id: String,
    val name: String,           // 月卡/季卡/年卡
    val description: String,     // 套餐描述
    val duration: Int,           // 天数
    val originalPrice: Double,   // 原价
    val currentPrice: Double,    // 现价
    val discount: String?,       // 折扣标签，如 "限时8折"
    val features: List<String>   // 包含的功能列表
) {
    val hasDiscount: Boolean
        get() = discount != null

    val priceDifference: Double
        get() = originalPrice - currentPrice
}

/**
 * 支付方式枚举
 */
enum class PayMethod(val displayName: String, val icon: String) {
    ALIPAY("支付宝", "alipay"),
    WECHAT("微信支付", "wechat"),
    ACTIVATION_CODE("激活码", "code")
}

/**
 * 支付订单信息
 */
data class PayOrder(
    val orderId: String,
    val packageId: String,
    val packageName: String,
    val amount: Double,
    val payMethod: PayMethod,
    val status: PayStatus = PayStatus.PENDING,
    val createTime: Long = System.currentTimeMillis(),
    val payTime: Long? = null
)

/**
 * 支付状态
 */
enum class PayStatus {
    PENDING,     // 待支付
    PROCESSING,  // 支付中
    SUCCESS,     // 支付成功
    FAILED,      // 支付失败
    CANCELLED    // 已取消
}

/**
 * 支付结果
 */
data class PayResult(
    val success: Boolean,
    val message: String,
    val orderId: String? = null,
    val packageInfo: VipPackage? = null
)

/**
 * 激活码信息
 */
data class ActivationCode(
    val code: String,
    val packageType: String,    // 月卡/季卡/年卡
    val duration: Int,          // 有效天数
    val used: Boolean = false,
    val usedTime: Long? = null,
    val userId: String? = null
)


