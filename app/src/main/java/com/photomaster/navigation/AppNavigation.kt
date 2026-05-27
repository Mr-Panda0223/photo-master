package com.photomaster.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photomaster.ui.screens.editor.EditorScreen
import com.photomaster.ui.screens.export.ExportScreen
import com.photomaster.ui.screens.home.HomeScreen
import com.photomaster.ui.screens.profile.ProfileContent

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "首页", Icons.Default.PhotoLibrary)
    data object Profile : Screen("profile", "我的", Icons.Default.Person)
    data object Editor : Screen("editor/{imageUri}?draftId={draftId}", "编辑") {
        fun createRoute(imageUri: String, draftId: String? = null): String {
            return if (draftId != null) {
                "editor/${Uri.encode(imageUri)}?draftId=$draftId"
            } else {
                "editor/${Uri.encode(imageUri)}"
            }
        }
    }
    data object Export : Screen("export/{imageUri}", "导出") {
        fun createRoute(imageUri: String) = "export/${Uri.encode(imageUri)}"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // 判断是否在主页面（显示底部导航栏）
    val isMainScreen = currentRoute == Screen.Home.route || currentRoute == Screen.Profile.route
    
    Scaffold(
        bottomBar = {
            if (isMainScreen) {
                MainBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToEditor = { imageUri, draftId ->
                        navController.navigate(Screen.Editor.createRoute(imageUri, draftId))
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileContent(
                    onNavigateToEditor = { imageUri, draftId ->
                        navController.navigate(Screen.Editor.createRoute(imageUri, draftId))
                    }
                )
            }
            
            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("imageUri") { type = NavType.StringType },
                    navArgument("draftId") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val draftId = backStackEntry.arguments?.getString("draftId")
                EditorScreen(
                    imageUri = imageUri,
                    draftId = draftId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToExport = { editedImageUri ->
                        navController.navigate(Screen.Export.createRoute(editedImageUri))
                    }
                )
            }
            
            composable(
                route = Screen.Export.route,
                arguments = listOf(
                    navArgument("imageUri") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                ExportScreen(
                    imageUri = imageUri,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(Screen.Home, Screen.Profile)
    
    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) }
            )
        }
    }
}
