package com.kaze.blog.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val file = File(app.filesDir, "notes.json")
    private val repo = NoteRepository(file)
    private val saveLock = Mutex()
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        val firstRun = !file.exists()
        _notes.value = if (firstRun) welcomeNotes() else repo.load()
        if (firstRun) persist()
    }

    private fun persist() {
        val snapshot = _notes.value
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { repo.save(snapshot) }
        }
    }

    /** Saves an edited note. Empty notes are discarded; unchanged notes are left alone. */
    fun commit(note: Note) {
        val blank = note.title.isBlank() && note.body.isBlank()
        val old = _notes.value.firstOrNull { it.id == note.id }
        if (blank) {
            if (old != null) {
                _notes.update { list -> list.filterNot { it.id == note.id } }
                persist()
            }
            return
        }
        if (old != null &&
            old.title == note.title && old.body == note.body &&
            old.category == note.category && old.colorIndex == note.colorIndex &&
            old.pinned == note.pinned && old.favorite == note.favorite
        ) return
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        _notes.update { list ->
            if (old == null) listOf(updated) + list
            else list.map { if (it.id == note.id) updated else it }
        }
        persist()
    }

    fun togglePin(ids: Set<Long>) {
        val allPinned = _notes.value.filter { it.id in ids }.all { it.pinned }
        mutate(ids) { it.copy(pinned = !allPinned) }
    }

    fun toggleFavorite(ids: Set<Long>) {
        val allFav = _notes.value.filter { it.id in ids }.all { it.favorite }
        mutate(ids) { it.copy(favorite = !allFav) }
    }

    fun trash(ids: Collection<Long>) = mutate(ids.toSet()) { it.copy(trashed = true, pinned = false) }

    fun restore(ids: Collection<Long>) = mutate(ids.toSet()) { it.copy(trashed = false) }

    fun deleteForever(ids: Collection<Long>) {
        val set = ids.toSet()
        _notes.update { list -> list.filterNot { it.id in set } }
        persist()
    }

    fun emptyTrash() {
        _notes.update { list -> list.filterNot { it.trashed } }
        persist()
    }

    private fun mutate(ids: Set<Long>, transform: (Note) -> Note) {
        _notes.update { list -> list.map { if (it.id in ids) transform(it) else it } }
        persist()
    }

    private fun welcomeNotes(): List<Note> {
        val now = System.currentTimeMillis()
        return listOf(
            Note(
                id = now,
                title = "Welcome to KazeBlog",
                body = "Your private space for notes, ideas and blog drafts.\n\n" +
                    "• Tap New note to start writing\n" +
                    "• Long-press a note to select, pin or delete\n" +
                    "• Pick a color and a category from the bottom bar\n" +
                    "• Everything is saved automatically on your device",
                category = "Notes",
                colorIndex = 5,
                pinned = true,
                createdAt = now,
                updatedAt = now,
            ),
            Note(
                id = now + 1,
                title = "First blog post idea",
                body = "Write about a day in your life, a trip, or something you just learned. " +
                    "Set the category to Blog to keep drafts together.",
                category = "Blog",
                colorIndex = 4,
                createdAt = now,
                updatedAt = now - 1000,
            ),
        )
    }
}
