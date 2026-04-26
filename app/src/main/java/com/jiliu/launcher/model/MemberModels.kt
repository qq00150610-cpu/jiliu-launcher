package com.jiliu.launcher.model

/**
 * Member/VIP model
 */
data class MemberInfo(
    val isVip: Boolean = false,
    val expireTime: Long = 0,
    val activationCode: String? = null,
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
 * Activation result
 */
data class ActivationResult(
    val success: Boolean,
    val message: String,
    val memberInfo: MemberInfo? = null
)
