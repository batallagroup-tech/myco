package com.batallagroup.myco.presentation.splash

import androidx.lifecycle.ViewModel
import com.batallagroup.myco.core.Constants
import com.batallagroup.myco.core.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    fun isOnboardingDone(): Boolean =
        preferenceManager.getBoolean(Constants.PREF_ONBOARDING_DONE, false)
}
