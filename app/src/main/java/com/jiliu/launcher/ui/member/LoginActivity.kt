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
import com.jiliu.launcher.service.MemberApiService
import com.jiliu.launcher.viewmodel.MemberViewModel
import kotlinx.coroutines.launch

/**
 * 会员登录页面
 * 支持邮箱+密码登录和邮箱+验证码登录
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val apiService by lazy { MemberApiService(this) }
    private val viewModel: MemberViewModel by viewModels()
    
    private var isPasswordMode = true  // true=密码登录, false=验证码登录
    private var countdownTimer: CountDownTimer? = null
    private var currentEmail: String = ""

    companion object {
        private const val COUNTDOWN_TIME = 60000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            finish()
        }

        // 切换登录模式
        binding.tvSwitchMode.setOnClickListener {
            isPasswordMode = !isPasswordMode
            updateUIForMode()
        }

        // 忘记密码
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // 发送验证码
        binding.btnSendCode.setOnClickListener {
            sendVerificationCode()
        }

        // 登录按钮
        binding.btnLogin.setOnClickListener {
            if (validateInput()) {
                performLogin()
            }
        }

        // 注册链接
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        updateUIForMode()
    }

    private fun updateUIForMode() {
        if (isPasswordMode) {
            // 密码登录模式
            binding.layoutEmail.visibility = View.VISIBLE
            binding.layoutCode.visibility = View.GONE
            binding.layoutPassword.visibility = View.VISIBLE
            binding.tvForgotPassword.visibility = View.VISIBLE
            binding.tvSwitchMode.text = "使用邮箱验证码登录"
            binding.tvRegister.text = "没有账号？立即注册"
            binding.btnLogin.text = "登录"
        } else {
            // 验证码登录模式
            binding.layoutEmail.visibility = View.VISIBLE
            binding.layoutCode.visibility = View.VISIBLE
            binding.layoutPassword.visibility = View.GONE
            binding.tvForgotPassword.visibility = View.GONE
            binding.tvSwitchMode.text = "使用密码登录"
            binding.tvRegister.text = "没有账号？立即注册"
            binding.btnLogin.text = "登录"
        }
        // 清空输入
        binding.editEmail.setText("")
        binding.editCode.setText("")
        binding.editPassword.setText("")
        stopCountdown()
    }

    private fun validateInput(): Boolean {
        val email = binding.editEmail.text.toString().trim()
        
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "请输入邮箱", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (!isValidEmail(email)) {
            Toast.makeText(this, "邮箱格式不正确", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (isPasswordMode) {
            val password = binding.editPassword.text.toString()
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                return false
            }
            if (password.length < 6) {
                Toast.makeText(this, "密码长度至少6位", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            val code = binding.editCode.text.toString()
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(this, "请输入验证码", Toast.LENGTH_SHORT).show()
                return false
            }
            if (code.length != 6) {
                Toast.makeText(this, "验证码为6位数字", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        
        return true
    }

    private fun sendVerificationCode() {
        val email = binding.editEmail.text.toString().trim()
        
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
            val result = apiService.sendCode(email, "login")
            
            result.fold(
                onSuccess = {
                    Toast.makeText(this@LoginActivity, "验证码已发送到您的邮箱", Toast.LENGTH_SHORT).show()
                    startCountdown()
                },
                onFailure = { message, code ->
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                },
                onError = { message ->
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    binding.btnSendCode.isEnabled = true
                }
            )
        }
    }

    private fun performLogin() {
        val email = binding.editEmail.text.toString().trim()
        currentEmail = email
        
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val result = if (isPasswordMode) {
                val password = binding.editPassword.text.toString()
                apiService.login(email, password, getDeviceIdentifier())
            } else {
                val code = binding.editCode.text.toString()
                apiService.loginWithCode(email, code, getDeviceIdentifier())
            }
            
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            
            result.fold(
                onSuccess = { response ->
                    // 保存登录信息
                    saveLoginInfo(response)
                    
                    Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                },
                onFailure = { message, _ ->
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun saveLoginInfo(response: MemberApiService.MemberResponse) {
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

    private fun getDeviceIdentifier(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
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
        binding.btnSendCode.text = "发送验证码"
        binding.btnSendCode.isEnabled = true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return email.matches(emailRegex)
    }

    private fun observeData() {
        // 可以观察ViewModel的数据变化
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
