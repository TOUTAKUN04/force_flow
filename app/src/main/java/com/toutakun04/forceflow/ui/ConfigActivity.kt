package com.toutakun04.forceflow.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.glance.appwidget.updateAll
import com.toutakun04.forceflow.data.model.HeatmapData
import com.toutakun04.forceflow.data.repository.CodeforcesRepository
import com.toutakun04.forceflow.ui.theme.ThemeOption
import com.toutakun04.forceflow.widget.CodeforcesWidget
import com.toutakun04.forceflow.widget.CodeforcesWidgetReceiver
import com.toutakun04.forceflow.widget.HeatmapRenderer
import com.toutakun04.forceflow.worker.RefreshWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigActivity : ComponentActivity() {
    private var pendingLauncherColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureStableLaunchAliasEnabled()

        setContent {
            val prefs = getSharedPreferences("cf_widget_prefs", MODE_PRIVATE)
            val savedHandle = prefs.getString("handle", "") ?: ""
            val savedColor = prefs.getInt("theme_color", ThemeOption.themes[0].colorInt)
            val activity = this@ConfigActivity

            ConfigScreen(
                initialHandle = savedHandle,
                initialColorInt = savedColor,
                shouldShowWidgetHintAfterSave = { !isWidgetAdded() },
                onSave = { handle, colorInt, data ->
                    prefs.edit()
                        .putString("handle", handle)
                        .putInt("theme_color", colorInt)
                        .commit()

                    RefreshWorker.saveDataToPrefs(activity, data)

                    CodeforcesWidget().updateAll(activity)

                    pendingLauncherColor = colorInt

                    // Keep periodic refresh for future updates.
                    RefreshWorker.enqueuePeriodicRefresh(activity)
                },
                onMinimize = { activity.moveTaskToBack(true) }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        pendingLauncherColor?.let { color ->
            updateLauncherIcon(color)
            pendingLauncherColor = null
        }
    }

    private fun updateLauncherIcon(colorInt: Int) {
        ensureStableLaunchAliasEnabled()

        val selectedThemeName = ThemeOption.themes
            .firstOrNull { it.colorInt == colorInt }
            ?.name
            ?: "Emerald"
        val selectedAlias = when (selectedThemeName) {
            "Emerald" -> "$packageName.ui.LauncherIconEmerald"
            else -> "$packageName.ui.Launcher$selectedThemeName"
        }
        val prefs = getSharedPreferences("cf_widget_prefs", MODE_PRIVATE)
        val savedAlias = prefs.getString("launcher_alias", "$packageName.ui.LauncherIconEmerald")
        if (savedAlias == selectedAlias) return

        val allAliases = listOf(
            "$packageName.ui.LauncherIconEmerald",
            "$packageName.ui.LauncherOcean",
            "$packageName.ui.LauncherViolet",
            "$packageName.ui.LauncherAmber",
            "$packageName.ui.LauncherCrimson",
            "$packageName.ui.LauncherTeal",
            "$packageName.ui.LauncherRose",
            "$packageName.ui.LauncherGold"
        )

        allAliases.forEach { alias ->
            val state = if (alias == selectedAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(this, alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }

        prefs.edit().putString("launcher_alias", selectedAlias).apply()
    }

    private fun ensureStableLaunchAliasEnabled() {
        packageManager.setComponentEnabledSetting(
            ComponentName(this, "$packageName.ui.LauncherEmerald"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun isWidgetAdded(): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, CodeforcesWidgetReceiver::class.java)
        return appWidgetManager.getAppWidgetIds(provider).isNotEmpty()
    }
}

@Composable
private fun ConfigScreen(
    initialHandle: String,
    initialColorInt: Int,
    shouldShowWidgetHintAfterSave: () -> Boolean,
    onSave: suspend (String, Int, HeatmapData) -> Unit,
    onMinimize: () -> Unit
) {
    var handle by remember { mutableStateOf(initialHandle) }
    var selectedColorInt by remember { mutableIntStateOf(initialColorInt) }
    var isSaving by remember { mutableStateOf(false) }
    var showPostSaveWidgetHint by remember { mutableStateOf(false) }
    val repository = remember { CodeforcesRepository() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedComposeColor = Color(selectedColorInt)
    val previewBitmap = remember(selectedColorInt) {
        HeatmapRenderer.renderPreview(selectedColorInt)
    }

    if (showPostSaveWidgetHint) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Add the widget") },
            text = {
                Text(
                    text = "Add the ForceFlow widget on your home screen to check your Codeforces stats quickly."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPostSaveWidgetHint = false
                        onMinimize()
                    }
                ) {
                    Text("OK")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // App title
            Text(
                text = "CF",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = selectedComposeColor,
                letterSpacing = 4.sp
            )

            Text(
                text = "CODEFORCES WIDGET",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Handle input
            Text(
                text = "CODEFORCES HANDLE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                placeholder = {
                    Text("Enter your handle", color = Color.White.copy(alpha = 0.3f))
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = selectedComposeColor,
                    focusedBorderColor = selectedComposeColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = Color(0xFF111111),
                    unfocusedContainerColor = Color(0xFF111111),
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Theme color picker
            Text(
                text = "THEME COLOR",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeOption.themes.take(4).forEach { theme ->
                    ColorDot(
                        color = Color(theme.colorInt),
                        isSelected = selectedColorInt == theme.colorInt,
                        label = theme.name,
                        onClick = { selectedColorInt = theme.colorInt }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ThemeOption.themes.drop(4).forEach { theme ->
                    ColorDot(
                        color = Color(theme.colorInt),
                        isSelected = selectedColorInt == theme.colorInt,
                        label = theme.name,
                        onClick = { selectedColorInt = theme.colorInt }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preview card
            Text(
                text = "PREVIEW",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Statistics",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = "Solved: --",
                            color = selectedComposeColor,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Max: --",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Current: --",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Heatmap preview",
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    val trimmedHandle = handle.trim()
                    if (trimmedHandle.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter your Codeforces handle")
                        }
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        val fetchResult = withContext(Dispatchers.IO) {
                            repository.fetchHeatmapData(trimmedHandle)
                        }

                        fetchResult.fold(
                            onSuccess = { data ->
                                onSave(trimmedHandle, selectedColorInt, data)
                                isSaving = false
                                if (shouldShowWidgetHintAfterSave()) {
                                    showPostSaveWidgetHint = true
                                } else {
                                    snackbarHostState.showSnackbar("Saved successfully.")
                                }
                            },
                            onFailure = { error ->
                                isSaving = false
                                val message = when {
                                    error.message?.contains("not found", ignoreCase = true) == true -> {
                                        "Something went wrong. Check your Codeforces handle and try again."
                                    }
                                    else -> {
                                        "Something went wrong. Check your Codeforces handle and try again."
                                    }
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedComposeColor,
                    contentColor = Color.Black
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "SAVE & REFRESH",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorDot(
    color: Color,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Transparent,
        label = "border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(2.dp, animatedBorderColor, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

