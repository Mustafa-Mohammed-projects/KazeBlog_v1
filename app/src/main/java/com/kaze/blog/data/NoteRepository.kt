package com.kaze.blog.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NoteRepository(private val file: File) {

    fun load(): Pair<List<Folder>, List<Note>> {
        if (!file.exists()) return emptyList<Folder>() to emptyList<Note>()
        return try {
            val text = file.readText().trim()
            if (text.startsWith("[")) {
                emptyList<Folder>() to parseNotes(JSONArray(text))
            } else {
                val o = JSONObject(text)
                val fa = o.optJSONArray("folders") ?: JSONArray()
                val folders = (0 until fa.length()).map {
                    val f = fa.getJSONObject(it)
                    Folder(f.getLong("id"), f.optString("name"))
                }
                folders to parseNotes(o.optJSONArray("notes") ?: JSONArray())
            }
        } catch (e: Exception) {
            emptyList<Folder>() to emptyList<Note>()
        }
    }

    private fun parseNotes(arr: JSONArray): List<Note> = (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        val att = o.optJSONArray("attachments")
        Note(
            id = o.getLong("id"),
            title = o.optString("title"),
            body = o.optString("body"),
            folderId = o.optLong("folder", 0L),
            colorIndex = o.optInt("color", 0),
            pinned = o.optBoolean("pinned"),
            favorite = o.optBoolean("favorite"),
            trashed = o.optBoolean("trashed"),
            createdAt = o.optLong("created"),
            updatedAt = o.optLong("updated"),
            attachments = if (att == null) emptyList() else (0 until att.length()).map { att.getString(it) },
        )
    }

    fun save(folders: List<Folder>, notes: List<Note>) {
        val root = JSONObject()
        root.put("v", 2)
        root.put("folders", JSONArray().apply {
            folders.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach { n ->
                put(
                    JSONObject().apply {
                        put("id", n.id)
                        put("title", n.title)
                        put("body", n.body)
                        put("folder", n.folderId)
                        put("color", n.colorIndex)
                        put("pinned", n.pinned)
                        put("favorite", n.favorite)
                        put("trashed", n.trashed)
                        put("created", n.createdAt)
                        put("updated", n.updatedAt)
                        put("attachments", JSONArray(n.attachments))
                    }
                )
            }
        })
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(root.toString())
        tmp.renameTo(file)
    }
}
