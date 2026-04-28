package com.jiliu.launcher.config

/**
 * 会员系统配置
 * 包含API端点和服务器地址
 */
object MemberConfig {
    
    // ==================== 服务器配置 ====================
    
    /** 会员服务API地址（使用激活码服务的端口） */
    const val BASE_URL = "http://47.92.220.102:3200"
    
    /** API超时时间（毫秒） */
    const val API_TIMEOUT = 30000L
    
    // ==================== API端点 ====================
    
    object Endpoints {
        // 会员相关
        const val SEND_CODE = "/api/member/send-code"
        const val VERIFY_CODE = "/api/member/verify-code"
        const val REGISTER = "/api/member/register"
        const val LOGIN = "/api/member/login"
        const val RESET_PASSWORD = "/api/member/reset-password"
        const val MEMBER_INFO = "/api/member/info"
        const val UPDATE_VIP = "/api/member/update-vip"
        
        // 管理员相关（用于配置邮箱）
        const val EMAIL_STATUS = "/api/admin/email/status"
        const val EMAIL_CONFIGURE = "/api/admin/email/configure"
        const val EMAIL_PRESET_QQ = "/api/admin/email/preset-qq"
        const val EMAIL_PRESET_163 = "/api/admin/email/preset-163"
        const val EMAIL_TEST = "/api/admin/email/test"
        
        // 激活码相关
        const val VERIFY_CODE_API = "/api/verify"
        const val CHECK_VIP = "/api/check-vip"
        
        // 管理员激活码接口
        const val ADMIN_GENERATE = "/api/admin/generate"
        const val ADMIN_CODES = "/api/admin/codes"
        const val ADMIN_STATS = "/api/admin/stats"
    }
    
    // ==================== 验证配置 ====================
    
    /** 验证码有效期（秒） */
    const val CODE_VALIDITY_SECONDS = 300
    
    /** 验证码发送冷却时间（秒） */
    const val CODE_COOLDOWN_SECONDS = 60
    
    /** 验证码长度 */
    const val CODE_LENGTH = 6
    
    // ==================== 管理员密码 ====================
    /** 管理员密码（需要用户修改） */
    const val ADMIN_PASSWORD = "jiliu2024"
    
    /**
     * 获取API完整URL
     */
    fun getApiUrl(endpoint: String): String {
        return BASE_URL + endpoint
    }
}
