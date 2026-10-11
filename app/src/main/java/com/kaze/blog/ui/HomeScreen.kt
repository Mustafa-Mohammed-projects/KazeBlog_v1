package com.kaze.blog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaze.blog.data.CATEGORIES
import com.kaze.blog.data.Note
import com.kaze.blog.data.NotesViewModel

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    notes: List<Note>,
    vm: NotesViewModel,
    snack: SnackbarHostState,
    filter: String,
    onFilter: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    gridMode: Boolean,
    onGridMode: (Boolean) -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onTrash: (List<Long>) -> Unit,
) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val inTrash = filter == "Trash"

    BackHandler(enabled = selected.isNotEmpty()) { selected = emptySet() }
    LaunchedEffect(filter) { selected = emptySet() }

    val visible = remember(notes, filter, query) {
        notes
            .filter { n ->
                if (filter == "Trash") n.trashed
                else !n.trashed && when (filter) {
                    "All" -> true
                    "Favorites" -> n.favorite
                    else -> n.category == filter
                }
            }
            .filter { n ->
                query.isBlank() ||
                    n.title.contains(query, ignoreCase = true) ||
                    n.body.contains(query, ignoreCase = true)
            }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
    }
    val activeCount = remember(notes) { notes.count { !it.trashed } }
    val trashCount = remember(notes) { notes.count { it.trashed } }
    val filters = remember { listOf("All", "Favorites") + CATEGORIES + "Trash" }

    Scaffold(
        containerColor = p.bg,
        snackbarHost = {
            SnackbarHost(snack) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = p.surface,
                    contentColor = p.onBg,
                    actionColor = p.accent,
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selected.isEmpty() && !inTrash,
                enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    containerColor = p.accent,
                    contentColor = p.onAccent,
                    shape = RoundedCornerShape(20.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New note", fontWeight = FontWeight.SemiBold) },
                )
            }
        },
    ) { padding ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(if (gridMode) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Crossfade(targetState = selected.isNotEmpty(), animationSpec = tween(200), label = "header") { selecting ->
                        if (!selecting) {
                            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        buildAnnotatedString {
                                            append("Kaze")
                                            withStyle(SpanStyle(color = p.accent)) { append("Blog") }
                                        },
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.onBg,
                                    )
                                    Text(
                                        if (inTrash) "$trashCount in Trash" else "$activeCount notes",
                                        fontSize = 14.sp,
                                        color = p.muted,
                                    )
                                }
                                if (inTrash && visible.isNotEmpty()) {
                                    TextButton(onClick = { confirmEmpty = true }) {
                                        Text("Empty", color = p.accent, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                IconButton(onClick = { onGridMode(!gridMode) }) {
                                    ViewToggleIcon(gridMode, p.onBg)
                                }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selected = emptySet() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = p.onBg)
                                }
                                Text(
                                    "${selected.size} selected",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = p.onBg,
                                )
                                if (inTrash) {
                                    IconButton(onClick = { vm.restore(selected); selected = emptySet() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = p.onBg)
                                    }
                                    IconButton(onClick = { confirmDelete = true }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete forever", tint = p.onBg)
                                    }
                                } else {
                                    IconButton(onClick = { vm.togglePin(selected); selected = emptySet() }) {
                                        Icon(PinIcon, contentDescription = "Pin", tint = p.onBg)
                                    }
                                    IconButton(onClick = { vm.toggleFavorite(selected); selected = emptySet() }) {
                                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = p.onBg)
                                    }
                                    IconButton(onClick = { onTrash(selected.toList()); selected = emptySet() }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Move to Trash", tint = p.onBg)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    SearchField(query, onQuery)
                }
            }

            item(key = "chips", span = StaggeredGridItemSpan.FullLine) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filters) { f ->
                        KazeChip(label = f, selected = f == filter, onClick = { onFilter(f) })
                    }
                }
            }

            if (visible.isEmpty()) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    EmptyState(
                        title = when {
                            query.isNotBlank() -> "No results"
                            inTrash -> "Trash is empty"
                            filter == "Favorites" -> "No favorites yet"
                            else -> "No notes yet"
                        },
                        subtitle = when {
                            query.isNotBlank() -> "Try a different keyword."
                            inTrash -> "Deleted notes show up here."
                            filter == "Favorites" -> "Tap the heart on a note to find it here."
                            else -> "Tap New note to start writing."
                        },
                    )
                }
            }

            items(visible, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    selected = note.id in selected,
                    maxBodyLines = if (gridMode) 7 else 3,
                    onClick = {
                        if (selected.isNotEmpty() || inTrash) selected = selected.toggle(note.id)
                        else onOpen(note.id)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selected = selected + note.id
                    },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = p.surface,
            titleContentColor = p.onBg,
            textContentColor = p.muted,
            title = { Text("Delete forever?") },
            text = { Text("The selected notes will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteForever(selected); selected = emptySet(); confirmDelete = false
                }) { Text("Delete", color = Color(0xFFE5675C)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = p.accent) }
            },
        )
    }
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            containerColor = p.surface,
            titleContentColor = p.onBg,
            textContentColor = p.muted,
            title = { Text("Empty Trash?") },
            text = { Text("All notes in Trash will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) {
                    Text("Empty", color = Color(0xFFE5675C))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text("Cancel", color = p.accent) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    maxBodyLines: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    val bg = p.noteColors[note.colorIndex.coerceIn(0, p.noteColors.lastIndex)]
    val borderColor by animateColorAsState(
        when {
            selected -> p.accent
            note.colorIndex == 0 -> p.outline
            else -> Color.Transparent
        },
        tween(180), label = "cardBorder",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (note.title.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.onBg,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(8.dp))
                        Icon(PinIcon, contentDescription = "Pinned", tint = p.accent, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (note.pinned) {
                Icon(PinIcon, contentDescription = "Pinned", tint = p.accent, modifier = Modifier.size(16.dp))
            }
            if (note.body.isNotBlank()) {
                if (note.title.isNotBlank() || note.pinned) Spacer(Modifier.height(6.dp))
                Text(
                    note.body.trim(),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = p.onBg.copy(alpha = 0.72f),
                    maxLines = maxBodyLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.category, fontSize = 11.sp, color = p.accent, fontWeight = FontWeight.Medium)
                Text("  ·  ${formatCardDate(note.updatedAt)}", fontSize = 11.sp, color = p.muted)
                Spacer(Modifier.weight(1f))
                if (note.favorite) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = p.accent, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(p.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(p.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = p.accent, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = p.onBg)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = p.muted, textAlign = TextAlign.Center)
    }
}
