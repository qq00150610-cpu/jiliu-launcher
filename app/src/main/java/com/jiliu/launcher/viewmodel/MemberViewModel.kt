package com.jiliu.launcher.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jiliu.launcher.App
import com.jiliu.launcher.model.MemberInfo
import com.jiliu.launcher.model.MemberLevel
import com.jiliu.launcher.repository.MemberRepository
import kotlinx.coroutines.launch

class MemberViewModel(application: Application) : AndroidViewModel(application) {

    private val memberRepository = MemberRepository(application)

    private val _memberInfo = MutableLiveData<MemberInfo>()
    val memberInfo: LiveData<MemberInfo> = _memberInfo

    private val _activationResult = MutableLiveData<ActivationState>()
    val activationResult: LiveData<ActivationState> = _activationResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData< Boolean> = _isLoading

    init {
        loadMemberInfo()
    }

    /**
     * Load member info
     */
    fun loadMemberInfo() {
        _memberInfo.value = memberRepository.getMemberInfo()
    }

    /**
     * Check if VIP
     */
    fun isVip(): Boolean = memberRepository.isVip()

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

    /**
     * Start 15-day free trial
     */
    fun startTrial() {
        viewModelScope.launch {
            _isLoading.value = true
            _activationResult.value = ActivationState.Loading
            
            try {
                val result = memberRepository.startTrial()
                
                result.fold(
                    onSuccess = { memberInfo ->
                        _memberInfo.value = memberInfo
                        _activationResult.value = ActivationState.Success(memberInfo)
                    },
                    onFailure = { error ->
                        _activationResult.value = ActivationState.Error(error.message ?: "试用激活失败")
                    }
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class ActivationState {
        object Loading : ActivationState()
        data class Success(val memberInfo: MemberInfo) : ActivationState()
        data class Error(val message: String) : ActivationState()
    }
}
