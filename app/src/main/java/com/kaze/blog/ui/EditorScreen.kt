package com.kaze.blog.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kaze.blog.data.Folder
import com.kaze.blog.data.Media
import com.kaze.blog.data.Note
import com.kaze.blog.data.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(
    noteId: Long,
    existing: Note?,
    folders: List<Folder>,
    onAddFolder: (String) -> Long,
    onCommit: (Note) -> Unit,
    onTrash: (Note) -> Unit,
    onClose: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val base = remember(noteId) { existing ?: Note(id = noteId) }

    var title by rememberSaveable(noteId) { mutableStateOf(base.title) }
    var body by rememberSaveable(noteId) { mutableStateOf(base.body) }
    var folderId by rememberSaveable(noteId) { mutableStateOf(base.folderId) }
    var colorIndex by rememberSaveable(noteId) { mutableStateOf(base.colorIndex) }
    var pinned by rememberSaveable(noteId) { mutableStateOf(base.pinned) }
    var favorite by rememberSaveable(noteId) { mutableStateOf(base.favorite) }
    var attachments by rememberSaveable(noteId) { mutableStateOf(ArrayList(base.attachments)) }
    var menuOpen by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var exportNote by remember { mutableStateOf<Note?>(null) }

    fun current() = base.copy(
        title = title.trim(),
        body = body.trimEnd(),
        folderId = folderId,
        colorIndex = colorIndex,
        pinned = pinned,
        favorite = favorite,
        attachments = attachments.toList(),
    )

    val commit = { onCommit(current()) }
    val saveNow by rememberUpdatedState(commit)
    val close = { onCommit(current()); onClose() }

    BackHandler { close() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(noteId) {
        if (existing == null) runCatching { titleFocus.requestFocus() }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = withContext(Dispatchers.IO) { Media.importImage(context, uri) }
                if (name != null) attachments = ArrayList(attachments + name)
                else Toast.makeText(context, "Couldn't add image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val createPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val n = exportNote
        if (uri != null && n != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)!!.use { out ->
                            PdfExporter.write(context, n, folders.firstOrNull { it.id == n.folderId }?.name, out)
                        }
                    }.isSuccess
                }
                Toast.makeText(context, if (ok) "PDF saved" else "Couldn't save PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val bg by animateColorAsState(
        p.noteColors[colorIndex.coerceIn(0, p.noteColors.lastIndex)], tween(300), label = "editorBg",
    )
    val iconTint = p.onBg.copy(alpha = 0.85f)

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { close() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iconTint)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { pinned = !pinned }) {
                    Icon(PinIcon, contentDescription = "Pin", tint = if (pinned) p.accent else iconTint)
                }
                IconButton(onClick = { favorite = !favorite }) {
                    Icon(
                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (favorite) p.accent else iconTint,
                    )
                }
                IconButton(onClick = {
                    val text = listOf(title, body).filter { it.isNotBlank() }.joinToString("\n\n")
                    if (text.isNotBlank()) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Share note"))
                    }
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = iconTint)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = iconTint)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Save as PDF") },
                            onClick = {
                                menuOpen = false
                                val n = current()
                                if (n.title.isBlank() && n.body.isBlank() && n.attachments.isEmpty()) {
                                    Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportNote = n
                                    val safe = n.title.ifBlank { "Note" }.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40)
                                    createPdf.launch("$safe.pdf")
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; onTrash(current()) },
                        )
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = p.onBg, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp,
                    ),
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    "Title", color = p.onBg.copy(alpha = 0.35f),
                                    fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatFullDate(base.updatedAt)}  ·  ${wordCount(body)} words",
                    fontSize = 12.sp,
                    color = p.muted,
                )
                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle = TextStyle(color = p.onBg, fontSize = 17.sp, lineHeight = 26.sp),
                    cursorBrush = SolidColor(p.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (attachments.isEmpty()) 320.dp else 120.dp),
                    decorationBox = { inner ->
                        Box {
                            if (body.isEmpty()) {
                                Text(
                                    "Start writing…", color = p.onBg.copy(alpha = 0.35f),
                                    fontSize = 17.sp, lineHeight = 26.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
                attachments.forEach { name ->
                    key(name) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            AttachmentImage(name, 1080, Modifier.fillMaxWidth(), ContentScale.FillWidth)
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { attachments = ArrayList(attachments - name) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(p.onBg.copy(alpha = 0.05f))
                    .padding(vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(ImageIcon, contentDescription = "Add image", tint = iconTint)
                    }
                    IconButton(onClick = { drawing = true }) {
                        Icon(BrushIcon, contentDescription = "Draw", tint = iconTint)
                    }
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(folders, key = { it.id }) { f ->
                            KazeChip(
                                label = f.name,
                                selected = f.id == folderId,
                                onClick = { folderId = if (folderId == f.id) 0L else f.id },
                            )
                        }
                        item { KazeChip("+ Folder", false) { showNewFolder = true } }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(p.noteColors) { i, color ->
                        val selected = i == colorIndex
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) p.accent else p.onBg.copy(alpha = 0.25f),
                                    CircleShape,
                                )
                                .clickable { colorIndex = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        AnimatedVisibility(
            visible = drawing,
            enter = slideInVertically(tween(280)) { it / 4 } + fadeIn(tween(280)),
            exit = slideOutVertically(tween(220)) { it / 4 } + fadeOut(tween(220)),
        ) {
            DrawingScreen(
                onCancel = { drawing = false },
                onSave = { bmp ->
                    drawing = false
                    scope.launch {
                        val name = withContext(Dispatchers.IO) { Media.savePng(context, bmp) }
                        attachments = ArrayList(attachments + name)
                    }
                },
            )
        }
    }

    if (showNewFolder) {
        FolderDialog(
            title = "New folder", initial = "",
            onConfirm = { folderId = onAddFolder(it); showNewFolder = false },
            onDelete = null,
            onDismiss = { showNewFolder = false },
        )
    }
}
