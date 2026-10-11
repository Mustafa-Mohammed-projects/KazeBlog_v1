package com.kaze.blog.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.kaze.blog.ui.formatFullDate
import java.io.OutputStream

/** Renders a note (title, text, images, drawings) into a multi-page A4 PDF. */
object PdfExporter {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 44

    private class Writer(val doc: PdfDocument) {
        var page: PdfDocument.Page? = null
        var canvas: Canvas = Canvas()
        var y = MARGIN.toFloat()
        var n = 0

        fun newPage() {
            page?.let { doc.finishPage(it) }
            n++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, n).create())
            page = p
            canvas = p.canvas
            y = MARGIN.toFloat()
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }
    }

    fun write(ctx: Context, note: Note, folderName: String?, out: OutputStream) {
        val doc = PdfDocument()
        val w = Writer(doc)
        val contentW = PAGE_W - 2 * MARGIN
        w.newPage()

        fun layout(text: String, paint: TextPaint, spacing: Float = 1.2f): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, contentW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, spacing)
                .build()

        fun drawLayout(l: StaticLayout) {
            var line = 0
            while (line < l.lineCount) {
                val avail = PAGE_H - MARGIN - w.y
                var end = line
                while (end < l.lineCount && l.getLineBottom(end) - l.getLineTop(line) <= avail) end++
                if (end == line) { w.newPage(); continue }
                val top = l.getLineTop(line).toFloat()
                val bottom = l.getLineBottom(end - 1).toFloat()
                w.canvas.save()
                w.canvas.translate(MARGIN.toFloat(), w.y - top)
                w.canvas.clipRect(0f, top, contentW.toFloat(), bottom)
                l.draw(w.canvas)
                w.canvas.restore()
                w.y += bottom - top
                line = end
                if (line < l.lineCount) w.newPage()
            }
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 22f; isFakeBoldText = true
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 10f }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 12f }

        if (note.title.isNotBlank()) {
            drawLayout(layout(note.title, titlePaint, 1.1f))
            w.y += 4f
        }
        val meta = formatFullDate(note.updatedAt) + (folderName?.let { "  ·  $it" } ?: "")
        drawLayout(layout(meta, metaPaint))
        w.y += 14f
        if (note.body.isNotBlank()) {
            drawLayout(layout(note.body, bodyPaint, 1.3f))
            w.y += 10f
        }

        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        note.attachments.forEach { name ->
            val bmp = Media.load(ctx, name, 1200) ?: return@forEach
            var scale = contentW.toFloat() / bmp.width
            if (scale > 1f) scale = 1f
            val maxH = (PAGE_H - 2 * MARGIN).toFloat()
            if (bmp.height * scale > maxH) scale = maxH / bmp.height
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            if (w.y + dh > PAGE_H - MARGIN) w.newPage()
            w.canvas.drawBitmap(bmp, null, RectF(MARGIN.toFloat(), w.y, MARGIN + dw, w.y + dh), imgPaint)
            w.y += dh + 12f
            bmp.recycle()
        }

        w.finish()
        doc.writeTo(out)
        doc.close()
    }
}
