package com.example.richtexteditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.richtexteditor.ui.RichTextEditorApp
import com.example.richtexteditor.ui.theme.RichTextEditorTheme

class RichTextEditorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RichTextEditorTheme {
                RichTextEditorApp()
            }
        }
    }
}
