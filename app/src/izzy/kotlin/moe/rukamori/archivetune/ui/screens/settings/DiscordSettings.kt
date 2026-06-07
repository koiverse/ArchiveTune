package moe.rukamori.archivetune.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LaunchedEffect(navController) {
        navController.navigateUp()
    }
}

@Composable
fun DiscordExperimental(navController: NavController) {
    LaunchedEffect(navController) {
        navController.navigateUp()
    }
}
