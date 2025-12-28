package com.example.richtexteditor.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.richtexteditor.ui.editor.EditorScreen
import com.example.richtexteditor.ui.home.HomeScreen

@Composable
fun RichTextEditorApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToCreateNote = {
                    navController.navigate("editor")
                }
            )
        }
        composable("editor") {
            EditorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
