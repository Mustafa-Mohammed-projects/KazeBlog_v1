package com.kaze.blog

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kaze.blog.ui.CrashScreen
import com.kaze.blog.ui.KazeApp
import com.kaze.blog.ui.KazeTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashFile = File(filesDir, "crash.txt")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { crashFile.writeText(Log.getStackTraceString(e)) } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
        enableEdgeToEdge()
        setContent {
            KazeTheme {
                var report by remember { mutableStateOf(if (crashFile.exists()) crashFile.readText() else null) }
                val r = report
                if (r != null) {
                    CrashScreen(r) { crashFile.delete(); report = null }
                } else {
                    KazeApp()
                }
            }
        }
    }
}
