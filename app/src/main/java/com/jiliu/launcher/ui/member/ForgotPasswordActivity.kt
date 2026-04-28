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
import com.jiliu.launcher.service.MemberApiService
import kotlinx.coroutines.launch

/**
 * 忘记密码页面
 * 通过邮箱验证码重置密码
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val apiService by lazy { MemberApiService(this) }
    
    private var countdownTimer: CountDownTimer? = null
    private var verificationStep = 1  // 1=输入邮箱, 2=输入验证码和新密码
    private var currentEmail: String = ""

    companion object {
        private const val COUNTDOWN_TIME = 60000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            if (verificationStep == 2) {
                // 返回第一步
                verificationStep = 1
                updateUIForStep()
            } else {
                finish()
            }
        }

        // 发送验证码
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        // 重置密码按钮
        binding.btnResetPassword.setOnClickListener {
            if (validateInput()) {
                performResetPassword()
            }
        }

        // 返回登录
        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateUIForStep()
    }

    private fun updateUIForStep() {
        when (verificationStep) {
            1 -> {
                // 步骤1：输入邮箱
                binding.layoutStep1.visibility = View.VISIBLE
                binding.layoutStep2.visibility = View.GONE
                binding.tvTitle.text = "找回密码"
                binding.tvStepIndicator.text = "步骤 1/2"
            }
            2 -> {
                // 步骤2：输入验证码和新密码
                binding.layoutStep1.visibility = View.GONE
                binding.layoutStep2.visibility = View.VISIBLE
                binding.tvTitle.text = "设置新密码"
                binding.tvStepIndicator.text = "步骤 2/2"
                binding.tvEmailHint.text = "验证码已发送到\n$currentEmail"
            }
        }
    }

    private fun sendVerificationCode() {
        val email = binding.editEmailStep1.text.toString().trim()
        
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidEmail(email)) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentEmail = email
        binding.btnSendCode.isEnabled = false
        
        lifecycleScope.launch {
            val result = apiService.sendCode(email, "reset")
            
            result.fold(
                onSuccess = {
                    Toast.makeText(this@ForgotPasswordActivity, "验证码已发送到您的邮箱", Toast.LENGTH_SHORT).show()
                    verificationStep = 2
                    updateUIForStep()
                    startCountdown()
                },
                onFailure = { message, code ->
                    Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                },
                onError = { message ->
                    Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun validateInput(): Boolean {
        val code = binding.editCode.text.toString()
        val newPassword = binding.editNewPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()
        
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (code.length != 6) {
            Toast.makeText(this, "验证码为6位数字", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (newPassword.length < 6) {
            Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (newPassword != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun performResetPassword() {
        val code = binding.editCode.text.toString()
        val newPassword = binding.editNewPassword.text.toString()
        
        binding.btnResetPassword.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = apiService.resetPassword(
                email = currentEmail,
                newPassword = newPassword,
                verificationCode = code
            )
            
            binding.progressBar.visibility = View.GONE
            binding.btnResetPassword.isEnabled = true
            
            result.fold(
                onSuccess = {
                    Toast.makeText(this@ForgotPasswordActivity, "密码重置成功！", Toast.LENGTH_SHORT).show()
                    
                    // 跳转到登录页
                    startActivity(Intent(this@ForgotPasswordActivity, LoginActivity::class.java).apply {
                        putExtra("reset_success", true)
                        putExtra("email", currentEmail)
                    })
                    finish()
                },
                onFailure = { message, _ ->
                    Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun startCountdown() {
        binding.btnSendCodeStep2.text = "重新发送(60)"
        binding.btnSendCodeStep2.isEnabled = false

        countdownTimer = object : CountDownTimer(COUNTDOWN_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                binding.btnSendCodeStep2.text = "重新发送($seconds)"
            }

            override fun onFinish() {
                stopCountdown()
            }
        }.start()
    }

    private fun stopCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        binding.btnSendCodeStep2.text = "重新发送"
        binding.btnSendCodeStep2.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.matches(emailRegex)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }

    // 扩展函数：处理不同类型的结果
    private inline fun <T> MemberApiService.ApiResult<T>.fold(
        onSuccess: (T) -> Unit,
        onFailure: (String, String?) -> Unit,
        onError: (String) -> Unit
    ) {
        when (this) {
            is MemberApiService.ApiResult.Success -> onSuccess(data)
            is MemberApiService.ApiResult.Failure -> onFailure(message, code)
            is MemberApiService.ApiResult.Error -> onError(message)
        }
    }
}
