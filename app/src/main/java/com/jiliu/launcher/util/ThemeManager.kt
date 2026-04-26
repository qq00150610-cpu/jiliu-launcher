package com.jiliu.launcher.util

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils

class ThemeManager(private val context: Context) {

    // Primary colors - 国潮风格
    val primaryColor = Color.parseColor("#D4AF37")  // 金色
    val primaryDarkColor = Color.parseColor("#B8962E")
    val primaryLightColor = Color.parseColor("#F5D77A")
    
    // Accent colors - 中国红
    val accentColor = Color.parseColor("#C41E3A")  // 中国红
    val accentDarkColor = Color.parseColor("#8B0000")
    val accentLightColor = Color.parseColor("#FF6B6B")
    
    // Background colors
    val backgroundColor = Color.parseColor("#1A1A1A")  // 深色背景
    val backgroundDarkColor = Color.parseColor("#0D0D0D")
    val surfaceColor = Color.parseColor("#2D2D2D")
    val cardColor = Color.parseColor("#3D3D3D")
    
    // Text colors
    val textPrimaryColor = Color.parseColor("#FFFFFF")
    val textSecondaryColor = Color.parseColor("#B3B3B3")
    val textHintColor = Color.parseColor("#808080")
    
    // Status colors
    val successColor = Color.parseColor("#4CAF50")
    val warningColor = Color.parseColor("#FFC107")
    val errorColor = Color.parseColor("#F44336")
    
    // Gradient colors
    val gradientStartColor = Color.parseColor("#2D2D2D")
    val gradientEndColor = Color.parseColor("#1A1A1A")
    
    // VIP colors
    val vipGoldColor = Color.parseColor("#FFD700")
    val vipGradientStart = Color.parseColor("#FFD700")
    val vipGradientEnd = Color.parseColor("#FFA500")

    fun getBackgroundColor(isDark: Boolean): Int {
        return if (isDark) backgroundDarkColor else backgroundColor
    }

    fun getTextColor(isDark: Boolean): Int {
        return if (isDark) textSecondaryColor else textPrimaryColor
    }

    fun createAlphaColor(color: Int, alpha: Float): Int {
        return ColorUtils.setAlphaComponent(color, (alpha * 255).toInt())
    }

    fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        return ColorUtils.blendARGB(color1, color2, ratio)
    }

    companion object {
        // Theme mode constants
        const val THEME_DARK = 0
        const val THEME_LIGHT = 1
        const val THEME_AUTO = 2

        // Card corner radius
        const val CARD_CORNER_RADIUS = 16f
        const val CARD_ELEVATION = 8f
        
        // Icon sizes
        const val ICON_SIZE_LARGE = 72
        const val ICON_SIZE_MEDIUM = 56
        const val ICON_SIZE_SMALL = 40
        
        // Animation durations
        const val ANIMATION_DURATION_SHORT = 150
        const val ANIMATION_DURATION_MEDIUM = 300
        const val ANIMATION_DURATION_LONG = 500
    }
}
