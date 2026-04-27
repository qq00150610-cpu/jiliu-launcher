package com.jiliu.launcher.ui.member

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.jiliu.launcher.databinding.ActivityRegisterBinding
import com.jiliu.launcher.viewmodel.MemberViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: MemberViewModel by viewModels()
    
    private var isPhoneMode = true // true: phone+code, false: username+password
    private var lastPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Send verification code
        binding.btnSendCode.setOnClickListener {
            val phone = binding.editPhone.text.toString().trim()
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (phone.length != 11) {
                Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lastPhone = phone
            viewModel.sendVerificationCode(phone)
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            if (isPhoneMode) {
                registerWithPhone()
            } else {
                registerWithPassword()
            }
        }

        // Login link
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
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
            binding.tvSwitchMode.text = "使用用户名密码注册"
            binding.tvLogin.text = "已有账号？立即登录"
            binding.btnRegister.text = "注册"
        } else {
            binding.layoutPhone.visibility = View.GONE
            binding.layoutCode.visibility = View.GONE
            binding.layoutUsername.visibility = View.VISIBLE
            binding.layoutPassword.visibility = View.VISIBLE
            binding.tvSwitchMode.text = "使用手机号注册"
            binding.tvLogin.text = "已有账号？立即登录"
            binding.btnRegister.text = "注册"
        }
    }

    private fun registerWithPhone() {
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

        viewModel.registerWithPhone(phone, code)
    }

    private fun registerWithPassword() {
        val username = binding.editUsername.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
            return
        }
        if (username.length < 3) {
            Toast.makeText(this, "用户名至少3个字符", Toast.LENGTH_SHORT).show()
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

        viewModel.registerWithPassword(username, password)
    }

    private fun observeData() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.btnSendCode.isEnabled = !isLoading
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
}
