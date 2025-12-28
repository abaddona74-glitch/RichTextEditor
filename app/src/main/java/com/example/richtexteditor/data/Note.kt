package com.example.richtexteditor.data

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String, // Storing as HTML or Markdown or custom JSON? 
    // For this challenge, since we need to restore formatting, we need to store the spans.
    // A simple way is to store the text and a list of span ranges.
    // Or just store HTML if we can convert back and forth.
    // Given "No 3rd party libraries", manual span serialization is safest.
    val spans: List<NoteSpan> = emptyList()
)

data class NoteSpan(
    val start: Int,
    val end: Int,
    val type: SpanType
)

enum class SpanType {
    BOLD, ITALIC, UNDERLINE, TITLE, SUBTITLE, BODY
}
