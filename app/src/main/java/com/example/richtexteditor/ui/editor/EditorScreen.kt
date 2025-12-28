package com.example.richtexteditor.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.richtexteditor.data.Note
import com.example.richtexteditor.data.NoteRepository
import com.example.richtexteditor.data.NoteSpan
import com.example.richtexteditor.data.SpanType

import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var activeStyles by remember { mutableStateOf(setOf<SpanType>()) }
    
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    fun updateText(newValue: TextFieldValue) {
        if (newValue.text != textFieldValue.text || newValue.annotatedString != textFieldValue.annotatedString) {
            undoStack.add(textFieldValue)
            redoStack.clear()
        }
        textFieldValue = newValue
    }

    fun updateActiveStyles(value: TextFieldValue) {
        val cursor = value.selection.start
        val styles = value.annotatedString.spanStyles
        val newActiveStyles = mutableSetOf<SpanType>()
        
        // Inherit from the character BEFORE the cursor (standard editor behavior)
        if (cursor > 0) {
            val lookBackIndex = cursor - 1
            styles.forEach { span ->
                if (span.start <= lookBackIndex && span.end > lookBackIndex) {
                    val item = span.item
                    if (item.fontSize == 24.sp) newActiveStyles.add(SpanType.TITLE)
                    if (item.fontSize == 20.sp) newActiveStyles.add(SpanType.SUBTITLE)
                    if (item.fontSize == 16.sp) newActiveStyles.add(SpanType.BODY)
                    
                    if (item.fontWeight == FontWeight.Bold) newActiveStyles.add(SpanType.BOLD)
                    if (item.fontStyle == FontStyle.Italic) newActiveStyles.add(SpanType.ITALIC)
                    if (item.textDecoration == TextDecoration.Underline) newActiveStyles.add(SpanType.UNDERLINE)
                }
            }
        }
        // If cursor is at 0, we generally don't inherit styles unless manually toggled (which is handled by toggleStyle)
        // However, if we just deleted text and ended up at 0, we might want to reset (which this does).
        
        activeStyles = newActiveStyles
    }

    fun onValueChange(newValue: TextFieldValue) {
        val oldText = textFieldValue.text
        val newText = newValue.text
        val oldSelection = textFieldValue.selection
        
        // 0. Handle Selection Change Only (Preserve Spans)
        if (oldText == newText) {
            val preservedValue = textFieldValue.copy(
                selection = newValue.selection,
                composition = newValue.composition
            )
            updateText(preservedValue)
            updateActiveStyles(preservedValue)
            return
        }

        // 1. Handle Enter Key for Lists
        if (newText.length > oldText.length && newText.contains('\n') && newValue.selection.start == textFieldValue.selection.start + 1) {
             val addedChar = newText[newValue.selection.start - 1]
             if (addedChar == '\n') {
                 val cursor = newValue.selection.start
                 val prevLineEnd = cursor - 1
                 val prevLineStart = newText.lastIndexOf('\n', prevLineEnd - 1) + 1
                 val prevLine = newText.substring(prevLineStart, prevLineEnd)
                 
                 // Bullet List Logic
                 if (prevLine.trim() == "•") {
                     val cleanText = newText.removeRange(prevLineStart, cursor)
                     updateText(newValue.copy(text = cleanText, selection = androidx.compose.ui.text.TextRange(prevLineStart)))
                     return
                 } else if (prevLine.startsWith("• ")) {
                     val newTextWithBullet = newText.substring(0, cursor) + "• " + newText.substring(cursor)
                     updateText(newValue.copy(text = newTextWithBullet, selection = androidx.compose.ui.text.TextRange(cursor + 2)))
                     return
                 }
                 
                 // Numbered List Logic
                 val numberRegex = Regex("^(\\d+)\\. ")
                 val match = numberRegex.find(prevLine)
                 if (match != null) {
                     val number = match.groupValues[1].toInt()
                     if (prevLine.trim() == "$number.") {
                         val cleanText = newText.removeRange(prevLineStart, cursor)
                         updateText(newValue.copy(text = cleanText, selection = androidx.compose.ui.text.TextRange(prevLineStart)))
                         return
                     } else {
                         val nextNumber = number + 1
                         val prefix = "$nextNumber. "
                         val newTextWithNumber = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                         updateText(newValue.copy(text = newTextWithNumber, selection = androidx.compose.ui.text.TextRange(cursor + prefix.length)))
                         return
                     }
                 }
             }
        }

        // 2. Handle Text Changes (Insertion/Deletion/Replacement) to PRESERVE SPANS
        val oldAnnotatedString = textFieldValue.annotatedString
        var newAnnotatedString: AnnotatedString? = null
        
        // Case A: Replacement (Selection was not collapsed)
        if (!oldSelection.collapsed) {
            val insertedLength = newText.length - (oldText.length - (oldSelection.end - oldSelection.start))
            val insertedText = if (insertedLength > 0) {
                newText.substring(oldSelection.start, oldSelection.start + insertedLength)
            } else ""
            
            val builder = AnnotatedString.Builder(insertedText)
            if (insertedText.isNotEmpty()) {
                activeStyles.forEach { type ->
                    builder.addStyle(getSpanStyle(type), 0, insertedText.length)
                }
            }
            val insertedAnnotated = builder.toAnnotatedString()
            
            newAnnotatedString = oldAnnotatedString.subSequence(0, oldSelection.start) + 
                                 insertedAnnotated + 
                                 oldAnnotatedString.subSequence(oldSelection.end, oldAnnotatedString.length)
        }
        // Case B: Insertion (Cursor was collapsed, text grew)
        else if (newText.length > oldText.length) {
            val insertedLength = newText.length - oldText.length
            val calculatedInsertionIndex = newValue.selection.start - insertedLength
            
            if (calculatedInsertionIndex == oldSelection.start) {
                val insertedText = newText.substring(calculatedInsertionIndex, newValue.selection.start)
                
                val builder = AnnotatedString.Builder(insertedText)
                activeStyles.forEach { type ->
                    builder.addStyle(getSpanStyle(type), 0, insertedText.length)
                }
                val insertedAnnotated = builder.toAnnotatedString()
                
                newAnnotatedString = oldAnnotatedString.subSequence(0, calculatedInsertionIndex) + 
                                     insertedAnnotated + 
                                     oldAnnotatedString.subSequence(calculatedInsertionIndex, oldAnnotatedString.length)
            }
        }
        // Case C: Deletion (Cursor was collapsed, text shrank)
        else if (newText.length < oldText.length) {
            val deletedLength = oldText.length - newText.length
            
            if (newValue.selection.start < oldSelection.start) {
                 // Backspace
                 val start = newValue.selection.start
                 val end = oldSelection.start
                 newAnnotatedString = oldAnnotatedString.subSequence(0, start) + 
                                      oldAnnotatedString.subSequence(end, oldAnnotatedString.length)
            } else if (newValue.selection.start == oldSelection.start) {
                 // Forward Delete
                 val start = oldSelection.start
                 val end = start + deletedLength
                 newAnnotatedString = oldAnnotatedString.subSequence(0, start) + 
                                      oldAnnotatedString.subSequence(end, oldAnnotatedString.length)
            }
        }

        if (newAnnotatedString != null) {
            updateText(newValue.copy(annotatedString = newAnnotatedString))
            // If deletion occurred, update active styles based on new cursor position
            if (newText.length < oldText.length) {
                 updateActiveStyles(newValue.copy(annotatedString = newAnnotatedString))
            }
            return
        }
        
        updateText(newValue)
        updateActiveStyles(newValue)
    }

    fun toggleStyle(type: SpanType) {
        val typographyTypes = setOf(SpanType.TITLE, SpanType.SUBTITLE, SpanType.BODY)
        
        if (textFieldValue.selection.collapsed) {
            if (type in typographyTypes) {
                activeStyles = (activeStyles - typographyTypes) + type
            } else {
                activeStyles = if (activeStyles.contains(type)) {
                    activeStyles - type
                } else {
                    activeStyles + type
                }
            }
        } else {
            val selection = textFieldValue.selection
            val builder = AnnotatedString.Builder(textFieldValue.annotatedString)
            val style = getSpanStyle(type)
            
            builder.addStyle(style, selection.start, selection.end)
            val newAnnotatedString = builder.toAnnotatedString()
            updateText(textFieldValue.copy(annotatedString = newAnnotatedString))
            updateActiveStyles(textFieldValue.copy(annotatedString = newAnnotatedString))
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text("Title", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (title.isNotEmpty() && textFieldValue.text.isNotEmpty()) {
                        TextButton(onClick = {
                            val spans = extractSpans(textFieldValue.annotatedString)
                            val note = Note(title = title, content = textFieldValue.text, spans = spans)
                            NoteRepository.addNote(note)
                            onNavigateBack()
                        }) {
                            Text("Done")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (WindowInsets.isImeVisible) {
                FormattingToolbar(
                    activeStyles = activeStyles,
                    onToggleStyle = { toggleStyle(it) },
                    onUndo = {
                        if (undoStack.isNotEmpty()) {
                            redoStack.add(textFieldValue)
                            textFieldValue = undoStack.removeLast()
                        }
                    },
                    onRedo = {
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(textFieldValue)
                            textFieldValue = redoStack.removeLast()
                        }
                    },
                    onReset = {
                        if (textFieldValue.selection.collapsed) {
                            activeStyles = emptySet()
                        } else {
                            val selection = textFieldValue.selection
                            val builder = AnnotatedString.Builder(textFieldValue.annotatedString)
                            builder.addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Normal,
                                    fontStyle = FontStyle.Normal,
                                    textDecoration = TextDecoration.None,
                                    fontSize = 16.sp
                                ),
                                selection.start,
                                selection.end
                            )
                            val newAnnotatedString = builder.toAnnotatedString()
                            updateText(textFieldValue.copy(annotatedString = newAnnotatedString))
                            updateActiveStyles(textFieldValue.copy(annotatedString = newAnnotatedString))
                        }
                    },
                    textFieldValue = textFieldValue,
                    onValueChange = { onValueChange(it) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { onValueChange(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text("Add note here", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun FormattingToolbar(
    activeStyles: Set<SpanType>,
    onToggleStyle: (SpanType) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onReset: () -> Unit,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    var showTypographyMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
             TextButton(onClick = onReset) {
                 Text("Reset")
             }
        }
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                TextButton(onClick = { showTypographyMenu = true }) {
                    Text("Body")
                }
                DropdownMenu(
                    expanded = showTypographyMenu, 
                    onDismissRequest = { showTypographyMenu = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    DropdownMenuItem(text = { Text("Title") }, onClick = { 
                        onToggleStyle(SpanType.TITLE)
                        showTypographyMenu = false 
                    })
                    DropdownMenuItem(text = { Text("Subtitle") }, onClick = { 
                        onToggleStyle(SpanType.SUBTITLE)
                        showTypographyMenu = false 
                    })
                    DropdownMenuItem(text = { Text("Body") }, onClick = { 
                        onToggleStyle(SpanType.BODY)
                        showTypographyMenu = false 
                    })
                }
            }

            ToggleButton(
                isActive = activeStyles.contains(SpanType.BOLD),
                onClick = { onToggleStyle(SpanType.BOLD) },
                icon = { Icon(Icons.Default.FormatBold, "Bold") }
            )
            ToggleButton(
                isActive = activeStyles.contains(SpanType.ITALIC),
                onClick = { onToggleStyle(SpanType.ITALIC) },
                icon = { Icon(Icons.Default.FormatItalic, "Italic") }
            )
            ToggleButton(
                isActive = activeStyles.contains(SpanType.UNDERLINE),
                onClick = { onToggleStyle(SpanType.UNDERLINE) },
                icon = { Icon(Icons.Default.FormatUnderlined, "Underline") }
            )

            IconButton(onClick = onUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
            }
            IconButton(onClick = onRedo) {
                Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
            }
            
            IconButton(onClick = { 
                // Toggle Bullet
                val text = textFieldValue.text
                val selection = textFieldValue.selection
                val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
                val lineEnd = text.indexOf('\n', selection.start).takeIf { it != -1 } ?: text.length
                val lineContent = text.substring(lineStart, lineEnd)
                
                if (lineContent.startsWith("• ")) {
                    val newLineContent = lineContent.substring(2)
                    val newText = text.replaceRange(lineStart, lineEnd, newLineContent)
                    val offset = -2
                    val newSelectionStart = (selection.start + offset).coerceAtLeast(lineStart)
                    val newSelectionEnd = (selection.end + offset).coerceAtLeast(lineStart)
                    onValueChange(textFieldValue.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)))
                } else {
                    val numberRegex = Regex("^\\d+\\. ")
                    val cleanLineContent = lineContent.replace(numberRegex, "")
                    val newLineContent = "• " + cleanLineContent
                    val newText = text.replaceRange(lineStart, lineEnd, newLineContent)
                    val offset = 2 + (cleanLineContent.length - lineContent.length)
                    val newSelectionStart = selection.start + offset
                    val newSelectionEnd = selection.end + offset
                    onValueChange(textFieldValue.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)))
                }
            }) {
                Icon(Icons.Default.FormatListBulleted, "Bullet")
            }
            IconButton(onClick = { 
                // Toggle Number
                val text = textFieldValue.text
                val selection = textFieldValue.selection
                val lineStart = text.lastIndexOf('\n', selection.start - 1) + 1
                val lineEnd = text.indexOf('\n', selection.start).takeIf { it != -1 } ?: text.length
                val lineContent = text.substring(lineStart, lineEnd)
                
                val numberRegex = Regex("^\\d+\\. ")
                if (numberRegex.containsMatchIn(lineContent)) {
                    val newLineContent = lineContent.replace(numberRegex, "")
                    val newText = text.replaceRange(lineStart, lineEnd, newLineContent)
                    val offset = newLineContent.length - lineContent.length
                    val newSelectionStart = (selection.start + offset).coerceAtLeast(lineStart)
                    val newSelectionEnd = (selection.end + offset).coerceAtLeast(lineStart)
                    onValueChange(textFieldValue.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)))
                } else {
                    val cleanLineContent = if (lineContent.startsWith("• ")) lineContent.substring(2) else lineContent
                    val newLineContent = "1. " + cleanLineContent
                    val newText = text.replaceRange(lineStart, lineEnd, newLineContent)
                    val offset = 3 + (cleanLineContent.length - lineContent.length)
                    val newSelectionStart = selection.start + offset
                    val newSelectionEnd = selection.end + offset
                    onValueChange(textFieldValue.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)))
                }
            }) {
                Icon(Icons.Default.FormatListNumbered, "Number")
            }
        }
    }
}

@Composable
fun ToggleButton(isActive: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = if (isActive) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else IconButtonDefaults.iconButtonColors()
    ) {
        icon()
    }
}

fun getSpanStyle(type: SpanType): SpanStyle {
    return when (type) {
        SpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        SpanType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        SpanType.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
        SpanType.TITLE -> SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)
        SpanType.SUBTITLE -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)
        SpanType.BODY -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    }
}

fun extractSpans(text: AnnotatedString): List<NoteSpan> {
    return text.spanStyles.map { 
        val type = when {
            it.item.fontWeight == FontWeight.Bold && it.item.fontSize == 24.sp -> SpanType.TITLE
            it.item.fontWeight == FontWeight.Bold -> SpanType.BOLD
            it.item.fontStyle == FontStyle.Italic -> SpanType.ITALIC
            it.item.textDecoration == TextDecoration.Underline -> SpanType.UNDERLINE
            else -> SpanType.BODY
        }
        NoteSpan(it.start, it.end, type)
    }
}
