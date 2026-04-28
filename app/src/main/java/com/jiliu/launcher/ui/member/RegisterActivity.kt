package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jiliu.launcher.databinding.ActivityRegisterBinding
import com.jiliu.launcher.service.MemberApiService
import kotlinx.coroutines.launch

/**
 * 会员注册页面
 * 使用邮箱注册
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val apiService by lazy { MemberApiService(this) }
    
    private var countdownTimer: CountDownTimer? = null
    private var verificationStep = 1  // 1=输入邮箱, 2=输入验证码和密码
    private var currentEmail: String = ""

    companion object {
        private const val COUNTDOWN_TIME = 60000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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

        // 注册按钮
        binding.btnRegister.setOnClickListener {
            if (validateInput()) {
                performRegister()
            }
        }

        // 登录链接
        binding.tvLogin.setOnClickListener {
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
                binding.tvTitle.text = "注册账号"
                binding.tvStepIndicator.text = "步骤 1/2"
            }
            2 -> {
                // 步骤2：输入验证码和密码
                binding.layoutStep1.visibility = View.GONE
                binding.layoutStep2.visibility = View.VISIBLE
                binding.tvTitle.text = "验证邮箱"
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
            val result = apiService.sendCode(email, "register")
            
            result.fold(
                onSuccess = {
                    Toast.makeText(this@RegisterActivity, "验证码已发送到您的邮箱", Toast.LENGTH_SHORT).show()
                    verificationStep = 2
                    updateUIForStep()
                    startCountdown()
                },
                onFailure = { message, code ->
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                },
                onError = { message ->
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun validateInput(): Boolean {
        val code = binding.editCode.text.toString()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()
        
        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (code.length != 6) {
            Toast.makeText(this, "验证码为6位数字", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (password != confirmPassword) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun performRegister() {
        val code = binding.editCode.text.toString()
        val password = binding.editPassword.text.toString()
        
        binding.btnRegister.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = apiService.register(
                email = currentEmail,
                password = password,
                verificationCode = code,
                deviceId = getDeviceId()
            )
            
            binding.progressBar.visibility = View.GONE
            binding.btnRegister.isEnabled = true
            
            result.fold(
                onSuccess = { response ->
                    // 保存注册信息
                    saveMemberInfo(response)
                    
                    Toast.makeText(this@RegisterActivity, "注册成功！", Toast.LENGTH_SHORT).show()
                    
                    // 跳转到登录页
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java).apply {
                        putExtra("registered_email", currentEmail)
                    })
                    finish()
                },
                onFailure = { message, _ ->
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun saveMemberInfo(response: MemberApiService.MemberResponse) {
        val prefs = getSharedPreferences("member_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putString("member_id", response.id)
            putString("email", response.email)
            putBoolean("is_vip", response.isVip)
            putInt("vip_remaining_days", response.vipRemainingDays)
            response.vipExpireTime?.let { putString("vip_expire_time", it) }
            apply()
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
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
