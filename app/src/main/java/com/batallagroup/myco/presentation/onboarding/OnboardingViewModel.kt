package com.batallagroup.myco.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    fun completeOnboarding() {
        preferenceManager.putBoolean(Constants.PREF_ONBOARDING_DONE, true)
    }
}
