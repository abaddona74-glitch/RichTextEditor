package com.example.richtexteditor.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.example.richtexteditor.ui.editor.getSpanStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.example.richtexteditor.data.Note
import com.example.richtexteditor.data.NoteRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCreateNote: () -> Unit
) {
    val notes by NoteRepository.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rich Text Editor") },
                navigationIcon = {
                    IconButton(onClick = { /* Do nothing */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Do nothing */ }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
                    }
                    IconButton(onClick = { /* Do nothing */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color(0xFF365CA3),
                    navigationIconContentColor = Color(0xFF365CA3),
                    actionIconContentColor = Color(0xFF365CA3)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateNote,
                containerColor = Color(0xFF2F64AA),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Note", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(notes) { note ->
                NoteItem(note)
                DashedDivider()
            }
            // If no notes, maybe show empty lines like in the mockup?
            // The mockup shows many empty lines.
            if (notes.isEmpty()) {
                items(15) {
                    DashedDivider(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: Note) {
    val annotatedString = remember(note) {
        buildAnnotatedString {
            append(note.content)
            note.spans.forEach { span ->
                addStyle(getSpanStyle(span.type), span.start, span.end)
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { /* Open note to edit? Not in requirements but good UX */ }
        .padding(vertical = 8.dp)) {
        Text(text = note.title, style = MaterialTheme.typography.titleMedium)
        Text(text = annotatedString, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(1.dp)) {
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}
