package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jiliu.launcher.databinding.ActivityLoginBinding
import com.jiliu.launcher.service.VerificationService
import com.jiliu.launcher.viewmodel.MemberViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var verificationService: VerificationService
    private val viewModel: MemberViewModel by viewModels()
    
    private var isPhoneMode = true
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val COUNTDOWN_TIME = 60000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        verificationService = VerificationService(this)
        
        setupUI()
        observeData()
    }

    private fun setupUI() {
        // Close button
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Toggle mode
        binding.tvSwitchMode.setOnClickListener {
            isPhoneMode = !isPhoneMode
            updateUIForMode()
        }

        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // Send verification code
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        // Login button
        binding.btnLogin.setOnClickListener {
            if (isPhoneMode) {
                loginWithPhone()
            } else {
                loginWithPassword()
            }
        }

        // Register link
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        updateUIForMode()
    }

    private fun updateUIForMode() {
        if (isPhoneMode) {
            binding.layoutPhone.visibility = View.VISIBLE
            binding.layoutCode.visibility = View.VISIBLE
            binding.layoutUsername.visibility = View.GONE
            binding.layoutPassword.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.GONE
            binding.tvSwitchMode.text = "使用用户名密码登录"
            binding.tvRegister.text = "没有账号？立即注册"
            binding.btnLogin.text = "登录"
        } else {
            binding.layoutPhone.visibility = View.GONE
            binding.layoutCode.visibility = View.GONE
            binding.layoutUsername.visibility = View.VISIBLE
            binding.layoutPassword.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.tvSwitchMode.text = "使用手机验证码登录"
            binding.tvRegister.text = "没有账号？立即注册"
            binding.btnLogin.text = "登录"
        }
        // Clear inputs
        binding.editPhone.setText("")
        binding.editCode.setText("")
        binding.editUsername.setText("")
        binding.editPassword.setText("")
        stopCountdown()
    }

    private fun sendVerificationCode() {
        val phone = binding.editPhone.text.toString().trim()
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.length != 11 || !phone.matches(Regex("^1[3-9]\\d{9}$"))) {
            Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendCode.isEnabled = false

        lifecycleScope.launch {
            val result = verificationService.sendSmsCode(phone)
            
            result.fold(
                onSuccess = {
                    Toast.makeText(this@LoginActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                    startCountdown()
                },
                onFailure = { error ->
                    Toast.makeText(this@LoginActivity, error.message ?: "发送失败", Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun loginWithPhone() {
        val phone = binding.editPhone.text.toString().trim()
        val code = binding.editCode.text.toString().trim()

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.length != 11) {
            Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
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

        // 验证验证码
        if (!verificationService.verifySmsCode(phone, code)) {
            Toast.makeText(this, "验证码错误或已过期", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loginWithPhone(phone, code)
    }

    private fun loginWithPassword() {
        val username = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loginWithPassword(username, password)
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

    private fun observeData() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.btnSendCode.isEnabled = !isLoading && isPhoneMode
        }

        viewModel.loginResult.observe(this) { state ->
            when (state) {
                is MemberViewModel.LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is MemberViewModel.LoginState.Success -> {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is MemberViewModel.LoginState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdown()
    }
}
