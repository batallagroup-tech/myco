package com.batallagroup.myco.presentation.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batallagroup.myco.domain.usecase.GenerateIdentityUseCase
import com.batallagroup.myco.domain.usecase.UserIdentity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val generateIdentity: GenerateIdentityUseCase
) : ViewModel() {

    private val _identity = MutableStateFlow<UserIdentity?>(null)
    val identity: StateFlow<UserIdentity?> = _identity

    private val _isGenerating = MutableStateFlow(true)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    init {
        viewModelScope.launch {
            _isGenerating.value = true
            _identity.value = generateIdentity()
            _isGenerating.value = false
        }
    }
}
