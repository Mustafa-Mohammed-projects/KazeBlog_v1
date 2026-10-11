package com.kaze.blog.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaze.blog.data.NotesViewModel
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun KazeApp(vm: NotesViewModel = viewModel()) {
    val p = LocalPalette.current
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var filter by rememberSaveable { mutableStateOf("All") }
    var query by rememberSaveable { mutableStateOf("") }
    var gridMode by rememberSaveable { mutableStateOf(true) }

    val notes by vm.notes.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Keep status/navigation bar icons readable on the black splash and on both themes.
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val lightBars = !showSplash && !p.dark
        controller.isAppearanceLightStatusBars = lightBars
        controller.isAppearanceLightNavigationBars = lightBars
    }

    val trashWithUndo: (List<Long>) -> Unit = { ids ->
        vm.trash(ids)
        scope.launch {
            snack.currentSnackbarData?.dismiss()
            val result = snack.showSnackbar(
                message = if (ids.size == 1) "Note moved to Trash" else "${ids.size} notes moved to Trash",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.restore(ids)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = showSplash, animationSpec = tween(450), label = "splash") { splash ->
            if (splash) {
                SplashScreen { showSplash = false }
            } else {
                AnimatedContent(
                    targetState = editingId,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                                (fadeOut(tween(200)) + scaleOut(tween(300), targetScale = 0.96f))
                        } else {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.96f)) togetherWith
                                (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(250)))
                        }
                    },
                    label = "nav",
                ) { id ->
                    if (id == null) {
                        HomeScreen(
                            notes = notes,
                            folders = folders,
                            vm = vm,
                            snack = snack,
                            filter = filter,
                            onFilter = { filter = it },
                            query = query,
                            onQuery = { query = it },
                            gridMode = gridMode,
                            onGridMode = { gridMode = it },
                            onOpen = { editingId = it },
                            onNew = { editingId = System.currentTimeMillis() },
                            onTrash = trashWithUndo,
                        )
                    } else {
                        EditorScreen(
                            noteId = id,
                            existing = notes.firstOrNull { it.id == id },
                            folders = folders,
                            onAddFolder = { vm.addFolder(it) },
                            onCommit = { vm.commit(it) },
                            onTrash = { n ->
                                val blank = n.title.isBlank() && n.body.isBlank()
                                vm.commit(n)
                                editingId = null
                                if (!blank) trashWithUndo(listOf(n.id))
                            },
                            onClose = { editingId = null },
                        )
                    }
                }
            }
        }
    }
}
