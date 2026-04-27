package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.User
import com.jiliu.launcher.repository.MemberRepository
import kotlinx.coroutines.launch

class MemberViewModel(application: Application) : AndroidViewModel(application) {

    private val memberRepository = MemberRepository(application)

    private val _memberInfo = MutableLiveData<MemberInfo>()
    val memberInfo: LiveData<MemberInfo> = _memberInfo

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<LoginState>()
    val loginResult: LiveData<LoginState> = _loginResult

    private val _registerResult = MutableLiveData<RegisterState>()
    val registerResult: LiveData<RegisterState> = _registerResult

    private val _activationResult = MutableLiveData<ActivationState>()
    val activationResult: LiveData<ActivationState> = _activationResult

    init {
        loadMemberInfo()
        checkLoginStatus()
    }

    /**
     * Load member info
     */
    fun loadMemberInfo() {
        _memberInfo.value = memberRepository.getMemberInfo()
    }

    /**
     * Check login status
     */
    fun checkLoginStatus() {
        val user = memberRepository.getCurrentUser()
        _currentUser.value = user
        _isLoggedIn.value = memberRepository.isLoggedIn()
    }

    /**
     * Check if VIP
     */
    fun isVip(): Boolean = memberRepository.isVip()

    /**
     * Check if logged in
     */
    fun isLoggedIn(): Boolean = memberRepository.isLoggedIn()

    /**
     * Check VIP permission for features
     */
    fun hasVipPermission(): Boolean = memberRepository.hasVipPermission()

    /**
     * Send verification code
     */
    fun sendVerificationCode(phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = memberRepository.sendVerificationCode(phone)
                result.fold(
                    onSuccess = {
                        _loginResult.value = LoginState.CodeSent
                    },
                    onFailure = { error ->
                        _loginResult.value = LoginState.Error(error.message ?: "发送失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Register with phone + code
     */
    fun registerWithPhone(phone: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registerResult.value = RegisterState.Loading
            
            try {
                val result = memberRepository.registerWithPhone(phone, code)
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        loadMemberInfo()
                        _registerResult.value = RegisterState.Success(user)
                    },
                    onFailure = { error ->
                        _registerResult.value = RegisterState.Error(error.message ?: "注册失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Register with username + password
     */
    fun registerWithPassword(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registerResult.value = RegisterState.Loading
            
            try {
                val result = memberRepository.registerWithPassword(username, password)
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        loadMemberInfo()
                        _registerResult.value = RegisterState.Success(user)
                    },
                    onFailure = { error ->
                        _registerResult.value = RegisterState.Error(error.message ?: "注册失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Login with phone + code
     */
    fun loginWithPhone(phone: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = LoginState.Loading
            
            try {
                val result = memberRepository.loginWithPhone(phone, code)
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        loadMemberInfo()
                        _loginResult.value = LoginState.Success(user)
                    },
                    onFailure = { error ->
                        _loginResult.value = LoginState.Error(error.message ?: "登录失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Login with username + password
     */
    fun loginWithPassword(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = LoginState.Loading
            
            try {
                val result = memberRepository.loginWithPassword(username, password)
                
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        loadMemberInfo()
                        _loginResult.value = LoginState.Success(user)
                    },
                    onFailure = { error ->
                        _loginResult.value = LoginState.Error(error.message ?: "登录失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logout
     */
    fun logout() {
        viewModelScope.launch {
            memberRepository.logout()
            _currentUser.value = null
            _isLoggedIn.value = false
            loadMemberInfo()
        }
    }

    /**
     * Activate with code
     */
    fun activate(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _activationResult.value = ActivationState.Loading
            
            try {
                val result = memberRepository.activate(code)
                
                result.fold(
                    onSuccess = { memberInfo ->
                        _memberInfo.value = memberInfo
                        checkLoginStatus()
                        _activationResult.value = ActivationState.Success(memberInfo)
                    },
                    onFailure = { error ->
                        _activationResult.value = ActivationState.Error(error.message ?: "激活失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel VIP
     */
    fun cancelVip() {
        viewModelScope.launch {
            memberRepository.cancelVip()
            loadMemberInfo()
        }
    }

    sealed class LoginState {
        object Loading : LoginState()
        object CodeSent : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class RegisterState {
        object Loading : RegisterState()
        data class Success(val user: User) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    sealed class ActivationState {
        object Loading : ActivationState()
        data class Success(val memberInfo: MemberInfo) : ActivationState()
        data class Error(val message: String) : ActivationState()
    }
}
