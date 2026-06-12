package com.batallagroup.myco.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.batallagroup.myco.presentation.navigation.NavGraph

@Composable
fun MycoApp() {
    val navController = rememberNavController()
    NavGraph(navController = navController)
}
