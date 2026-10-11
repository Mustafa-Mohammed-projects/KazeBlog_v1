package com.kaze.blog.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.kaze.blog.data.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val PinIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Pin",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser()
            .parsePathString("M16,9V4l1,0c0.55,0 1,-0.45 1,-1v0c0,-0.55 -0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3v0c0,0.55 0.45,1 1,1l1,0v5c0,1.66 -1.34,3 -3,3h0v2h5.97v7l1,1l1,-1v-7H19v-2h0C17.34,12 16,10.66 16,9z")
            .toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

@Composable
fun ViewToggleIcon(grid: Boolean, tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        val s = size.minDimension
        if (grid) {
            val gap = s * 0.14f
            val w = (s - gap) / 2f
            for (r in 0..1) for (c in 0..1) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(c * (w + gap), r * (w + gap)),
                    size = Size(w, w),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        } else {
            val h = s * 0.2f
            val gap = (s - 3 * h) / 2f
            for (i in 0..2) {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, i * (h + gap)),
                    size = Size(s, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
            }
        }
    }
}

fun formatCardDate(ts: Long): String {
    val now = Calendar.getInstance()
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val sameYear = now.get(Calendar.YEAR) == c.get(Calendar.YEAR)
    val pattern = when {
        sameYear && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) -> "h:mm a"
        sameYear -> "MMM d"
        else -> "MMM d, yyyy"
    }
    return SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(ts))
}

fun formatFullDate(ts: Long): String =
    SimpleDateFormat("EEE, MMM d  ·  h:mm a", Locale.ENGLISH).format(Date(ts))

fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KazeChip(label: String, selected: Boolean, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    val bg by animateColorAsState(
        if (selected) p.accent else p.onBg.copy(alpha = 0.08f), tween(200), label = "chipBg",
    )
    val fg by animateColorAsState(
        if (selected) p.onAccent else p.onBg.copy(alpha = 0.75f), tween(200), label = "chipFg",
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(p.surface)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = p.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search notes", color = p.muted, fontSize = 16.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = p.onBg, fontSize = 16.sp),
                cursorBrush = SolidColor(p.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = p.muted)
            }
        }
    }
}

private fun vec(name: String, path: String): ImageVector {
    fun build(d: String) = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black)).build()
    return try { build(path) } catch (e: Throwable) { build("M4,4h16v16H4z") }
}

val ImageIcon: ImageVector by lazy {
    vec("Image", "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z")
}
val BrushIcon: ImageVector by lazy {
    vec("Brush", "M7,14c-1.66,0 -3,1.34 -3,3 0,1.31 -1.16,2 -2,2 0.92,1.22 2.49,2 4,2 2.21,0 4,-1.79 4,-4 0,-1.66 -1.34,-3 -3,-3zM20.71,4.63l-1.34,-1.34c-0.39,-0.39 -1.02,-0.39 -1.41,0L9,12.25 11.75,15l8.96,-8.96c0.39,-0.39 0.39,-1.02 0,-1.41z")
}
val UndoIcon: ImageVector by lazy {
    vec("Undo", "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z")
}
val RedoIcon: ImageVector by lazy {
    vec("Redo", "M18.4,10.6C16.55,8.99 14.15,8 11.5,8c-4.65,0 -8.58,3.03 -9.96,7.22L3.9,16c1.05,-3.19 4.05,-5.5 7.6,-5.5 1.95,0 3.73,0.72 5.12,1.88L13,16h9V7l-3.6,3.6z")
}

/** Loads a saved image/drawing off the main thread and shows it. */
@Composable
fun AttachmentImage(name: String, targetWidth: Int, modifier: Modifier, contentScale: ContentScale) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(null, name) {
        value = withContext(Dispatchers.IO) { Media.load(ctx, name, targetWidth)?.asImageBitmap() }
    }
    val b = bmp
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier.height(120.dp).background(Color.Gray.copy(alpha = 0.2f)))
    }
}

@Composable
fun FolderDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        titleContentColor = p.onBg,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                placeholder = { Text("Folder name") },
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Save", color = p.accent)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFE5675C)) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = p.onBg) }
            }
        },
    )
}
