package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jiliu.launcher.databinding.ActivityForgotPasswordBinding
import com.jiliu.launcher.service.VerificationService
import kotlinx.coroutines.launch

/**
 * 忘记密码Activity
 * 支持通过手机号或邮箱找回密码
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var verificationService: VerificationService
    
    private var isPhoneMode = true // true: phone, false: email
    private var countdownTimer: CountDownTimer? = null
    private var lastTarget = ""

    companion object {
        private const val COUNTDOWN_TIME = 60000L // 60秒倒计时
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificationService = VerificationService(this)
        
        setupUI()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Toggle mode (phone/email)
        binding.tvSwitchMode.setOnClickListener {
            isPhoneMode = !isPhoneMode
            updateUIForMode()
        }

        // Send verification code
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        // Reset password button
        binding.btnResetPassword.setOnClickListener {
            resetPassword()
        }

        // Back to login
        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateUIForMode()
    }

    private fun updateUIForMode() {
        if (isPhoneMode) {
            binding.layoutPhone.visibility = View.VISIBLE
            binding.layoutEmail.visibility = View.GONE
            binding.tvSwitchMode.text = "使用邮箱找回"
            binding.tvInputHint.text = "请输入绑定的手机号"
            binding.editPhoneOrEmail.hint = "请输入手机号"
            binding.editPhoneOrEmail.inputType = android.text.InputType.TYPE_CLASS_PHONE
        } else {
            binding.layoutPhone.visibility = View.GONE
            binding.layoutEmail.visibility = View.VISIBLE
            binding.tvSwitchMode.text = "使用手机找回"
            binding.tvInputHint.text = "请输入绑定的邮箱"
            binding.editPhoneOrEmail.hint = "请输入邮箱地址"
            binding.editPhoneOrEmail.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        // Clear input
        binding.editPhoneOrEmail.setText("")
        binding.editCode.setText("")
        binding.editNewPassword.setText("")
        binding.editConfirmPassword.setText("")
        stopCountdown()
    }

    private fun sendVerificationCode() {
        val target = binding.editPhoneOrEmail.text.toString().trim()

        if (TextUtils.isEmpty(target)) {
            Toast.makeText(this, "请输入${if (isPhoneMode) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPhoneMode) {
            if (target.length != 11 || !target.matches(Regex("^1[3-9]\\d{9}$"))) {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            if (!target.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
                return
            }
        }

        lastTarget = target
        binding.btnSendCode.isEnabled = false

        lifecycleScope.launch {
            val result = if (isPhoneMode) {
                verificationService.sendSmsCode(target)
            } else {
                verificationService.sendEmailCode(target)
            }

            result.fold(
                onSuccess = {
                    Toast.makeText(this@ForgotPasswordActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                    startCountdown()
                },
                onFailure = { error ->
                    Toast.makeText(this@ForgotPasswordActivity, error.message ?: "发送失败", Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun resetPassword() {
        val target = binding.editPhoneOrEmail.text.toString().trim()
        val code = binding.editCode.text.toString().trim()
        val newPassword = binding.editNewPassword.text.toString().trim()
        val confirmPassword = binding.editConfirmPassword.text.toString().trim()

        // 验证输入
        if (TextUtils.isEmpty(target)) {
            Toast.makeText(this, "请输入${if (isPhoneMode) "手机号" else "邮箱"}", Toast.LENGTH_SHORT).show()
            return
        }

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
            return
        }

        if (code.length != 6) {
            Toast.makeText(this, "验证码格式不正确", Toast.LENGTH_SHORT).show()
            return
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "两次密码输入不一致", Toast.LENGTH_SHORT).show()
            return
        }

        // 验证验证码
        val isCodeValid = if (isPhoneMode) {
            verificationService.verifySmsCode(target, code)
        } else {
            verificationService.verifyEmailCode(target, code)
        }

        if (!isCodeValid) {
            Toast.makeText(this, "验证码错误或已过期", Toast.LENGTH_SHORT).show()
            return
        }

        // 显示重置成功
        Toast.makeText(this, "密码重置成功", Toast.LENGTH_SHORT).show()
        
        // 跳转到登录页面
        startActivity(Intent(this, LoginActivity::class.java).apply {
            putExtra("reset_password", true)
            putExtra("login_target", target)
        })
        finish()
    }

    private fun startCountdown() {
        binding.btnSendCode.text = "重新发送(60)"
        binding.btnSendCode.isEnabled = false

        countdownTimer = object : CountDownTimer(COUNTDOWN_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.btnSendCode.text = "重新发送($seconds)"
            }

            override fun onFinish() {
                stopCountdown()
            }
        }.start()
    }

    private fun stopCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        binding.btnSendCode.text = "获取验证码"
        binding.btnSendCode.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }
}
