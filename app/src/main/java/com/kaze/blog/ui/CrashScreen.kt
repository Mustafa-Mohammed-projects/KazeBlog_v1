package com.kaze.blog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown on the next launch after a crash so the error can be copied and reported. */
@Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(
        Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().padding(16.dp),
    ) {
        Text("KazeBlog crashed last time", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text("Copy this error and send it to fix the problem.", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            report,
            color = Color(0xFFB7C32C),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        )
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { clipboard.setText(AnnotatedString(report)) }) { Text("Copy") }
            Spacer(Modifier.width(12.dp))
            Button(onClick = onContinue) { Text("Continue") }
        }
    }
}
