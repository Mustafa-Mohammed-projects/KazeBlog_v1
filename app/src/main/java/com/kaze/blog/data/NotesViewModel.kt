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
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        if (file.exists()) {
            val (f, n) = repo.load()
            _folders.value = f
            _notes.value = n
        } else {
            seed()
            persist()
        }
    }

    private fun persist() {
        val f = _folders.value
        val n = _notes.value
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { repo.save(f, n) }
        }
    }

    private fun deleteFiles(names: Collection<String>) {
        if (names.isEmpty()) return
        val ctx = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) { names.forEach { Media.delete(ctx, it) } }
    }

    /** Saves an edited note. Empty notes are discarded; unchanged notes are left alone. */
    fun commit(note: Note) {
        val blank = note.title.isBlank() && note.body.isBlank() && note.attachments.isEmpty()
        val old = _notes.value.firstOrNull { it.id == note.id }
        if (blank) {
            if (old != null) {
                _notes.update { list -> list.filterNot { it.id == note.id } }
                deleteFiles(old.attachments)
                persist()
            }
            return
        }
        if (old != null &&
            old.title == note.title && old.body == note.body &&
            old.folderId == note.folderId && old.colorIndex == note.colorIndex &&
            old.pinned == note.pinned && old.favorite == note.favorite &&
            old.attachments == note.attachments
        ) return
        if (old != null) deleteFiles(old.attachments - note.attachments.toSet())
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
        deleteFiles(_notes.value.filter { it.id in set }.flatMap { it.attachments })
        _notes.update { list -> list.filterNot { it.id in set } }
        persist()
    }

    fun emptyTrash() {
        deleteFiles(_notes.value.filter { it.trashed }.flatMap { it.attachments })
        _notes.update { list -> list.filterNot { it.trashed } }
        persist()
    }

    // ---- folders ----
    fun addFolder(name: String): Long {
        var id = System.currentTimeMillis()
        while (_folders.value.any { it.id == id }) id++
        _folders.update { it + Folder(id, name.trim()) }
        persist()
        return id
    }

    fun renameFolder(id: Long, name: String) {
        _folders.update { list -> list.map { if (it.id == id) it.copy(name = name.trim()) else it } }
        persist()
    }

    fun deleteFolder(id: Long) {
        _folders.update { list -> list.filterNot { it.id == id } }
        _notes.update { list -> list.map { if (it.folderId == id) it.copy(folderId = 0L) else it } }
        persist()
    }

    private fun mutate(ids: Set<Long>, transform: (Note) -> Note) {
        _notes.update { list -> list.map { if (it.id in ids) transform(it) else it } }
        persist()
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val blog = Folder(now - 3, "Blog")
        val ideas = Folder(now - 2, "Ideas")
        val personal = Folder(now - 1, "Personal")
        _folders.value = listOf(blog, ideas, personal)
        _notes.value = listOf(
            Note(
                id = now,
                title = "Welcome to KazeBlog",
                body = "Your private space for notes, ideas and blog drafts.\n\n" +
                    "• Tap New note to start writing\n" +
                    "• Add photos or draw with the pen from the bottom bar\n" +
                    "• Create your own folders with + Folder (long-press a folder to rename it)\n" +
                    "• Export any note as a PDF from the ⋮ menu\n" +
                    "• Long-press a note to select, pin or delete",
                colorIndex = 5,
                pinned = true,
            ),
            Note(
                id = now + 1,
                title = "First blog post idea",
                body = "Write about a day in your life, a trip, or something you just learned.",
                folderId = blog.id,
                colorIndex = 4,
                updatedAt = now - 1000,
            ),
        )
    }
}
