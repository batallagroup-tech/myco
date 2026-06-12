package com.batallagroup.myco.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.batallagroup.myco.R
import com.batallagroup.myco.presentation.splash.SplashViewModel
import com.batallagroup.myco.presentation.theme.MycoBg
import com.batallagroup.myco.presentation.theme.MycoGreen
import com.batallagroup.myco.presentation.theme.MycoSubtle
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToOnboarding: () -> Unit,
    onNavigateToChats: () -> Unit
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
        delay(2500)
        if (viewModel.isOnboardingDone()) {
            onNavigateToChats()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MycoBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value)
        ) {
            MycoLogoCanvas(size = 140.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MYCO",
                color = MycoGreen,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "red mesh sin internet",
                color = MycoSubtle,
                fontSize = 13.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "desarrollado por Batalla Group",
                color = Color(0xFF444444),
                fontSize = 11.sp
            )
        }
    }
}
