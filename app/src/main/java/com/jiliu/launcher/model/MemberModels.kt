package com.jiliu.launcher.model

/**
 * User model for login/register
 */
data class User(
    val userId: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val avatar: String = "",
    val vipLevel: MemberLevel = MemberLevel.NONE,
    val vipExpireTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isVip: Boolean
        get() = vipLevel != MemberLevel.NONE && (vipExpireTime == 0L || System.currentTimeMillis() < vipExpireTime)

    val remainingDays: Int
        get() {
            if (vipLevel == MemberLevel.NONE || vipExpireTime <= 0) return 0
            val diff = vipExpireTime - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }

    val displayName: String
        get() = if (username.isNotEmpty()) username else if (phone.isNotEmpty()) phone else "用户"
}

/**
 * Member/VIP model
 */
data class MemberInfo(
    val isVip: Boolean = false,
    val expireTime: Long = 0,
    val memberLevel: MemberLevel = MemberLevel.NONE,
    val features: List<String> = emptyList()
) {
    val isExpired: Boolean
        get() = isVip && expireTime > 0 && System.currentTimeMillis() > expireTime

    val remainingDays: Int
        get() {
            if (!isVip || expireTime <= 0) return 0
            val diff = expireTime - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
}

enum class MemberLevel {
    NONE,
    VIP,
    SVIP,
    PREMIUM
}

/**
 * Login result
 */
data class LoginResult(
    val success: Boolean,
    val message: String,
    val user: User? = null
)

/**
 * Register result
 */
data class RegisterResult(
    val success: Boolean,
    val message: String,
    val user: User? = null
)
