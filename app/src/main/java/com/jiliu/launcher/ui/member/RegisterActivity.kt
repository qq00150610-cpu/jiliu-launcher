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
import com.jiliu.launcher.databinding.ActivityRegisterBinding
import com.jiliu.launcher.service.VerificationService
import com.jiliu.launcher.viewmodel.MemberViewModel
import kotlinx.coroutines.launch

/**
 * 注册Activity
 * 支持三种注册方式：
 * 1. 手机号 + 验证码
 * 2. 手机号 + 密码
 * 3. 邮箱 + 验证码
 * 4. 邮箱 + 密码
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var verificationService: VerificationService
    private val viewModel: MemberViewModel by viewModels()
    
    // 注册模式: 0=手机验证码, 1=手机密码, 2=邮箱验证码, 3=邮箱密码
    private var registerMode = 0
    private var countdownTimer: CountDownTimer? = null

    companion object {
        private const val MODE_PHONE_CODE = 0
        private const val MODE_PHONE_PASSWORD = 1
        private const val MODE_EMAIL_CODE = 2
        private const val MODE_EMAIL_PASSWORD = 3
        private const val COUNTDOWN_TIME = 60000L // 60秒倒计时
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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
            cycleRegisterMode()
        }

        // Send verification code
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        // Login link
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        updateUIForMode()
    }

    private fun cycleRegisterMode() {
        registerMode = (registerMode + 1) % 4
        updateUIForMode()
    }

    private fun updateUIForMode() {
        // 重置输入框
        binding.editPhone.setText("")
        binding.editCode.setText("")
        binding.editUsername.setText("")
        binding.editPassword.setText("")
        binding.editEmail.setText("")
        stopCountdown()

        when (registerMode) {
            MODE_PHONE_CODE -> {
                // 手机号 + 验证码
                binding.layoutPhone.visibility = View.VISIBLE
                binding.editPhone.inputType = android.text.InputType.TYPE_CLASS_PHONE
                binding.editPhone.hint = "请输入手机号"
                binding.layoutCode.visibility = View.VISIBLE
                binding.layoutUsername.visibility = View.GONE
                binding.layoutPassword.visibility = View.GONE
                binding.layoutEmail.visibility = View.GONE
                binding.tvSwitchMode.text = "手机号+密码注册"
                binding.tvLogin.text = "已有账号？立即登录"
                binding.btnRegister.text = "注册"
            }
            MODE_PHONE_PASSWORD -> {
                // 手机号 + 密码
                binding.layoutPhone.visibility = View.VISIBLE
                binding.editPhone.inputType = android.text.InputType.TYPE_CLASS_PHONE
                binding.editPhone.hint = "请输入手机号"
                binding.layoutCode.visibility = View.GONE
                binding.layoutUsername.visibility = View.GONE
                binding.layoutPassword.visibility = View.VISIBLE
                binding.layoutEmail.visibility = View.GONE
                binding.tvSwitchMode.text = "邮箱注册"
                binding.tvLogin.text = "已有账号？立即登录"
                binding.btnRegister.text = "注册"
            }
            MODE_EMAIL_CODE -> {
                // 邮箱 + 验证码
                binding.layoutPhone.visibility = View.GONE
                binding.layoutCode.visibility = View.VISIBLE
                binding.layoutUsername.visibility = View.GONE
                binding.layoutPassword.visibility = View.GONE
                binding.layoutEmail.visibility = View.VISIBLE
                binding.tvSwitchMode.text = "邮箱+密码注册"
                binding.tvLogin.text = "已有账号？立即登录"
                binding.btnRegister.text = "注册"
            }
            MODE_EMAIL_PASSWORD -> {
                // 邮箱 + 密码
                binding.layoutPhone.visibility = View.GONE
                binding.layoutCode.visibility = View.GONE
                binding.layoutUsername.visibility = View.GONE
                binding.layoutPassword.visibility = View.VISIBLE
                binding.layoutEmail.visibility = View.VISIBLE
                binding.tvSwitchMode.text = "手机号注册"
                binding.tvLogin.text = "已有账号？立即登录"
                binding.btnRegister.text = "注册"
            }
        }
    }

    private fun sendVerificationCode() {
        val target = when (registerMode) {
            MODE_PHONE_CODE, MODE_PHONE_PASSWORD -> binding.editPhone.text.toString().trim()
            MODE_EMAIL_CODE, MODE_EMAIL_PASSWORD -> binding.editEmail.text.toString().trim()
            else -> ""
        }

        if (TextUtils.isEmpty(target)) {
            val hint = if (registerMode < 2) "手机号" else "邮箱"
            Toast.makeText(this, "请输入$hint", Toast.LENGTH_SHORT).show()
            return
        }

        when (registerMode) {
            MODE_PHONE_CODE, MODE_PHONE_PASSWORD -> {
                if (target.length != 11 || !target.matches(Regex("^1[3-9]\\d{9}$"))) {
                    Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            MODE_EMAIL_CODE, MODE_EMAIL_PASSWORD -> {
                if (!target.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                    Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        binding.btnSendCode.isEnabled = false

        lifecycleScope.launch {
            val result = if (registerMode < 2) {
                verificationService.sendSmsCode(target)
            } else {
                verificationService.sendEmailCode(target)
            }

            result.fold(
                onSuccess = {
                    Toast.makeText(this@RegisterActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                    startCountdown()
                },
                onFailure = { error ->
                    Toast.makeText(this@RegisterActivity, error.message ?: "发送失败", Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun performRegister() {
        when (registerMode) {
            MODE_PHONE_CODE -> registerWithPhoneCode()
            MODE_PHONE_PASSWORD -> registerWithPhonePassword()
            MODE_EMAIL_CODE -> registerWithEmailCode()
            MODE_EMAIL_PASSWORD -> registerWithEmailPassword()
        }
    }

    private fun registerWithPhoneCode() {
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

        viewModel.registerWithPhone(phone, code)
    }

    private fun registerWithPhonePassword() {
        val phone = binding.editPhone.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.length != 11) {
            Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.registerWithPhonePassword(phone, password)
    }

    private fun registerWithEmailCode() {
        val email = binding.editEmail.text.toString().trim()
        val code = binding.editCode.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return
        }
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
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
        if (!verificationService.verifyEmailCode(email, code)) {
            Toast.makeText(this, "验证码错误或已过期", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.registerWithEmail(email, code)
    }

    private fun registerWithEmailPassword() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return
        }
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.registerWithEmailPassword(email, password)
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
            binding.btnRegister.isEnabled = !isLoading
            binding.btnSendCode.isEnabled = !isLoading && (registerMode == MODE_PHONE_CODE || registerMode == MODE_EMAIL_CODE)
        }

        viewModel.registerResult.observe(this) { state ->
            when (state) {
                is MemberViewModel.RegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is MemberViewModel.RegisterState.Success -> {
                    Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is MemberViewModel.RegisterState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.loginResult.observe(this) { state ->
            when (state) {
                is MemberViewModel.LoginState.CodeSent -> {
                    Toast.makeText(this, "验证码已发送", Toast.LENGTH_SHORT).show()
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
