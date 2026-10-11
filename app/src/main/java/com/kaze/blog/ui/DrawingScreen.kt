package com.kaze.blog.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import android.graphics.Canvas as AndroidCanvas

class InkStroke(val points: List<Offset>, val color: Color, val width: Float, val eraser: Boolean)

private fun buildPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isEmpty()) return path
    path.moveTo(pts[0].x, pts[0].y)
    if (pts.size == 1) {
        path.lineTo(pts[0].x + 0.1f, pts[0].y)
    } else {
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            path.quadraticBezierTo(prev.x, prev.y, (prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
        }
        path.lineTo(pts.last().x, pts.last().y)
    }
    return path
}

private fun DrawScope.drawInk(s: InkStroke) {
    drawPath(
        path = buildPath(s.points),
        color = if (s.eraser) Color.Transparent else s.color,
        style = Stroke(width = s.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = if (s.eraser) BlendMode.Clear else BlendMode.SrcOver,
    )
}

/** Draws strokes onto a white paper bitmap (erasers clear earlier ink). */
private fun renderStrokes(strokes: List<InkStroke>, w: Int, h: Int): Bitmap {
    val layer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val lc = AndroidCanvas(layer)
    strokes.forEach { s ->
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = s.width
            if (s.eraser) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) else color = s.color.toArgb()
        }
        lc.drawPath(buildPath(s.points).asAndroidPath(), paint)
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val oc = AndroidCanvas(out)
    oc.drawColor(android.graphics.Color.WHITE)
    oc.drawBitmap(layer, 0f, 0f, null)
    layer.recycle()
    return out
}

private val InkColors = listOf(
    Color(0xFF111111), Color(0xFFE5484D), Color(0xFFF5A524), Color(0xFF46A758),
    Color(0xFF3E63DD), Color(0xFF8E4EC6), Color(0xFF8B5E3C),
)
private val InkWidths = listOf(4f, 9f, 18f)

@Composable
fun DrawingScreen(onSave: (Bitmap) -> Unit, onCancel: () -> Unit) {
    val p = LocalPalette.current
    val strokes = remember { mutableStateListOf<InkStroke>() }
    val redo = remember { mutableStateListOf<InkStroke>() }
    val currentPts = remember { mutableStateListOf<Offset>() }
    var color by remember { mutableStateOf(InkColors[0]) }
    var width by remember { mutableStateOf(InkWidths[1]) }
    var eraser by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    BackHandler { onCancel() }

    Column(
        Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = p.onBg) }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { if (strokes.isNotEmpty()) redo.add(strokes.removeAt(strokes.lastIndex)) },
                enabled = strokes.isNotEmpty(),
            ) {
                Icon(UndoIcon, contentDescription = "Undo", tint = p.onBg.copy(alpha = if (strokes.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(
                onClick = { if (redo.isNotEmpty()) strokes.add(redo.removeAt(redo.lastIndex)) },
                enabled = redo.isNotEmpty(),
            ) {
                Icon(RedoIcon, contentDescription = "Redo", tint = p.onBg.copy(alpha = if (redo.isNotEmpty()) 1f else 0.3f))
            }
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = {
                    val w = canvasSize.width
                    val h = canvasSize.height
                    if (strokes.isNotEmpty() && w > 0 && h > 0) onSave(renderStrokes(strokes.toList(), w, h)) else onCancel()
                },
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
            ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.size(8.dp))
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .onSizeChanged { canvasSize = it },
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .pointerInput(color, width, eraser) {
                        detectDragGestures(
                            onDragStart = { o -> currentPts.clear(); currentPts.add(o) },
                            onDrag = { change, _ -> change.consume(); currentPts.add(change.position) },
                            onDragEnd = {
                                if (currentPts.isNotEmpty()) {
                                    strokes.add(InkStroke(currentPts.toList(), color, if (eraser) width * 2.5f else width, eraser))
                                    redo.clear()
                                }
                                currentPts.clear()
                            },
                            onDragCancel = { currentPts.clear() },
                        )
                    },
            ) {
                strokes.forEach { drawInk(it) }
                if (currentPts.isNotEmpty()) {
                    drawInk(InkStroke(currentPts.toList(), color, if (eraser) width * 2.5f else width, eraser))
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KazeChip("Pen", !eraser) { eraser = false }
                KazeChip("Eraser", eraser) { eraser = true }
                Spacer(Modifier.weight(1f))
                InkWidths.forEach { wd ->
                    val sel = width == wd
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(if (sel) 2.dp else 1.dp, if (sel) p.accent else p.onBg.copy(alpha = 0.2f), CircleShape)
                            .clickable { width = wd },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size((wd * 1.1f + 4f).dp).clip(CircleShape).background(p.onBg))
                    }
                }
            }
            Spacer(Modifier.size(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(InkColors) { c ->
                    val sel = !eraser && color == c
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(if (sel) 3.dp else 1.dp, if (sel) p.accent else p.onBg.copy(alpha = 0.25f), CircleShape)
                            .clickable { color = c; eraser = false },
                    )
                }
            }
        }
    }
}
