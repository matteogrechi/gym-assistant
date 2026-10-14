package it.matteogrechi.gymassystant.feature

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.matteogrechi.gymassystant.R
import it.matteogrechi.gymassystant.app.GymViewModel
import it.matteogrechi.gymassystant.ui.*

// GYM-007/008/015/022/025/027: shared chrome, native Back and retained per-route presentation.
@Composable
fun GymApp(vm: GymViewModel) {
    val dataset by vm.dataset.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var history by rememberSaveable { mutableStateOf(arrayListOf("train")) }
    var menu by remember { mutableStateOf(false) }
    val route = history.last()
    val holder = rememberSaveableStateHolder()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun leaveInput() { focus.clearFocus(); keyboard?.hide() }
    fun navigate(next: String) { leaveInput(); history = ArrayList(history + next) }
    fun back() { if (history.size > 1) { leaveInput(); history = ArrayList(history.dropLast(1)) } }
    BackHandler(history.size > 1) { back() }
    val theme = dataset?.settings?.theme?.name?.lowercase() ?: vm.launchTheme
    GymTheme(theme) {
        val colors = GymColors
        val activity = LocalActivity.current
        val view = LocalView.current
        val dark = when (theme) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }
        LaunchedEffect(activity, view, dark) {
            if (activity != null) WindowCompat.getInsetsController(activity.window, view).apply {
                if (isAppearanceLightStatusBars != !dark) isAppearanceLightStatusBars = !dark
                if (isAppearanceLightNavigationBars != !dark) isAppearanceLightNavigationBars = !dark
            }
        }
        val side = if (LocalConfiguration.current.screenWidthDp < 350) 16.dp else 24.dp
        Surface(Modifier.fillMaxSize(), color = colors.surface) {
            if (dataset == null) LoadingScreen() else Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = side).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("gym assystant", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold)
                    Text(" ●", color = colors.focus, fontSize = 9.sp)
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton({ menu = true }, Modifier.size(48.dp)) {
                            Icon(GymIcons.MoreVert, stringResource(R.string.more_options), tint = colors.primary)
                        }
                        DropdownMenu(menu, { menu = false }, containerColor = colors.surfaceBright) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.settings)) },
                                modifier = Modifier.heightIn(min = 48.dp), onClick = {
                                    menu = false
                                    if (route != "settings") navigate("settings")
                                })
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(start = side, end = side, top = 8.dp)) {
                    val data = requireNotNull(dataset)
                    holder.SaveableStateProvider(route) {
                        when (route.substringBefore(':')) {
                            "train" -> TrainFeature(data, vm, ::navigate)
                            "session" -> SessionFeature(route.substringAfter(':'), data, vm, ::navigate, ::back)
                            "exercise" -> {
                                val parts = route.split(':')
                                ExerciseFeature(parts[1], parts[2], data, vm, ::back)
                            }
                            "plan", "template", "definition" -> PlanningFeature(route, data, vm, ::navigate, ::back)
                            "progress" -> ProgressFeature(data, vm)
                            "settings" -> SettingsFeature(data, vm, ::back)
                        }
                    }
                }
                val destination = when (route.substringBefore(':')) {
                    "train", "session", "exercise" -> "train"
                    "plan", "template", "definition" -> "plan"
                    "progress" -> "progress"
                    else -> ""
                }
                NavigationBar(containerColor = colors.surfaceContainer, tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.heightIn(min = 80.dp)) {
                    Spacer(Modifier.width(20.dp))
                    listOf(Triple("train", R.string.train, GymIcons.Train),
                        Triple("plan", R.string.plan, GymIcons.Plan),
                        Triple("progress", R.string.progress, GymIcons.Progress)).forEach { (name, label, icon) ->
                        NavigationBarItem(selected = destination == name, onClick = { leaveInput(); history = arrayListOf(name) },
                            icon = { Box(Modifier.width(32.dp).height(24.dp), contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(24.dp)) } },
                            label = { Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (destination == name) FontWeight.Bold else FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = colors.accent,
                                selectedIconColor = colors.onAccent, selectedTextColor = colors.onSurface,
                                unselectedIconColor = colors.primary, unselectedTextColor = colors.onSurface))
                    }
                    Spacer(Modifier.width(20.dp))
                }
            }
            if (error) AlertDialog(onDismissRequest = vm::clearError,
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(stringResource(R.string.error_message)) },
                confirmButton = { GymButton(stringResource(R.string.close), vm::clearError) })
        }
    }
}
