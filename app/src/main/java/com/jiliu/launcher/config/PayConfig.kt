package com.jiliu.launcher.config

/**
 * 支付配置
 * 所有支付相关的配置集中管理
 */
object PayConfig {
    
    // ==================== 服务器配置 ====================
    
    /** 后端服务器地址 */
    const val BASE_URL = "http://47.92.220.102:3000"
    
    /** API超时时间（毫秒） */
    const val API_TIMEOUT = 30000L
    
    // ==================== 支付宝配置 ====================
    
    /** 支付宝AppID（从支付宝开放平台获取） */
    const val ALIPAY_APP_ID = ""
    
    /** 支付宝AppID常量（如果使用沙箱） */
    const val ALIPAY_SANDBOX_APP_ID = "2021001234567890"
    
    /** 是否使用沙箱环境 */
    const val ALIPAY_USE_SANDBOX = true
    
    // ==================== 微信支付配置 ====================
    
    /** 微信AppID（从微信支付商户平台获取） */
    const val WECHAT_APP_ID = ""
    
    /** 微信商户ID */
    const val WECHAT_MCH_ID = ""
    
    /** 是否使用沙箱环境 */
    const val WECHAT_USE_SANDBOX = false
    
    // ==================== VIP套餐配置 ====================
    
    /** VIP套餐ID常量 */
    object PackageIds {
        const val MONTHLY = "vip_monthly"
        const val SEASONAL = "vip_seasonal"
        const val YEARLY = "vip_yearly"
    }
    
    // ==================== API端点 ====================
    
    object Endpoints {
        // 支付API
        const val ALIPAY_CREATE = "/api/pay/alipay/create"
        const val ALIPAY_CALLBACK = "/api/pay/alipay/callback"
        const val WECHAT_CREATE = "/api/pay/wechat/create"
        const val WECHAT_CALLBACK = "/api/pay/wechat/callback"
        const val PAY_STATUS = "/api/pay/status"
        
        // 激活码API
        const val CODE_VALIDATE = "/api/code/validate"
        const val CODE_USE = "/api/code/use"
        const val CODE_GENERATE = "/api/code/generate"
    }
    
    // ==================== 调试配置 ====================
    
    /** 是否开启调试模式 */
    const val DEBUG_MODE = true
    
    /** 是否启用模拟支付（用于测试） */
    const val ENABLE_SIMULATE_PAY = true
    
    /**
     * 获取当前有效的支付宝AppID
     */
    fun getAlipayAppId(): String {
        return if (ALIPAY_USE_SANDBOX) ALIPAY_SANDBOX_APP_ID else ALIPAY_APP_ID
    }
    
    /**
     * 获取API完整URL
     */
    fun getApiUrl(endpoint: String): String {
        return BASE_URL + endpoint
    }
}
