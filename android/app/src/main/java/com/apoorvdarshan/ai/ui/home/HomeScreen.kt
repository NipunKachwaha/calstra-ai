package com.apoorvdarshan.ai.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.models.FoodEntry
import com.apoorvdarshan.ai.models.MealType
import com.apoorvdarshan.ai.ui.components.MacroCard
import com.apoorvdarshan.ai.ui.components.WeekEnergyStrip
import com.apoorvdarshan.ai.ui.settings.DarkGlass
import com.apoorvdarshan.ai.ui.settings.LightGlass
import com.apoorvdarshan.ai.ui.settings.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.AppColors
import com.apoorvdarshan.ai.ui.theme.glowShadow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
// 1. Smart Theme Detection (Light/Dark mode) ke liye
import androidx.compose.ui.graphics.luminance

// 2. Text ke peeche Glowing Shadow aur uski positioning ke liye
import androidx.compose.ui.graphics.Shadow

// 3. 3D Press/Tap animation (pointerInput aur detectTapGestures) ke liye
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: AppContainer) {
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container))
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    val weekStartsOnMonday by container.prefs.weekStartsOnMonday.collectAsState(initial = false)
    val allEntries by container.foodRepository.entries.collectAsState(initial = emptyList())

    // 1. Theme Configuration (Reads from user settings and falls back to system)
    val appearance by container.prefs.appearanceMode.collectAsState(initial = "system")
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val glassTheme = remember(isDarkTheme) { if (isDarkTheme) DarkGlass else LightGlass }
    
    // 2. Scroll State for Ambient Orbs Parallax effect
    val listState = rememberLazyListState()

    var showText by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCopyFromDay by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FoodEntry?>(null) }
    var showNutritionDetail by remember { mutableStateOf(false) }

    var pendingCaptureFile by remember { mutableStateOf<File?>(null) }
    var pendingCaptureWantsNote by remember { mutableStateOf(false) }
    var pendingNoteImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingPickedPhotoWantsNote by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val wantsNote = pendingPickedPhotoWantsNote
        pendingPickedPhotoWantsNote = false
        if (uri != null) {
            val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                if (wantsNote) {
                    pendingNoteImageBytes = bytes
                } else {
                    vm.analyzePhoto(bytes)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved: Boolean ->
        val file = pendingCaptureFile
        val wantsNote = pendingCaptureWantsNote
        pendingCaptureFile = null
        pendingCaptureWantsNote = false
        if (saved && file != null && file.exists()) {
            val bytes = file.readBytes()
            if (bytes.isNotEmpty()) {
                if (wantsNote) {
                    pendingNoteImageBytes = bytes
                } else {
                    vm.analyzePhoto(bytes)
                }
            }
        }
    }

    fun launchCamera(withNote: Boolean = false) {
        val dir = File(ctx.cacheDir, "capture").apply { mkdirs() }
        val file = File(dir, "shot-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        pendingCaptureFile = file
        pendingCaptureWantsNote = withNote
        cameraLauncher.launch(uri)
    }

    var permissionWantsNote by remember { mutableStateOf(false) }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera(withNote = permissionWantsNote)
        permissionWantsNote = false
    }

    fun openCamera(withNote: Boolean = false) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera(withNote = withNote)
        } else {
            permissionWantsNote = withNote
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    val barcodePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showBarcodeScanner = true
    }

    fun openBarcodeScanner() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showBarcodeScanner = true
        } else {
            barcodePermission.launch(Manifest.permission.CAMERA)
        }
    }

    val today = LocalDate.now()
    val selectedDate = ui.date
    val isToday = selectedDate == today
    val mealGroups = remember(ui.todayEntries, ui.foodLogSortOrder) {
        foodLogMealGroups(ui.todayEntries, ui.foodLogSortOrder)
    }

    // 3. Applying the Theme locally for the entire screen
    CompositionLocalProvider(LocalGlassTheme provides glassTheme) {
        val glass = LocalGlassTheme.current 
        
        // Glass Background Wrapper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.bgTop, glass.bgBottom)))
        ) {
            // 4. Ambient Orbs Background (Ditto Copy from AboutScreen)
            AmbientOrbs(listState = listState)

            Scaffold(
                containerColor = Color.Transparent, // Let Glass background show through
                topBar = {
                    TopAppBar(
                        title = {},
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                val pressed = remember { mutableStateOf(false) }
                                val scale by animateFloatAsState(
                                    targetValue = if (pressed.value) 0.92f else 1f,
                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 220f),
                                    label = "plusPress"
                                )
                                val isDark = glass.bgTop.luminance() < 0.5f
                                val plusBg = if (isDark) {
                                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.08f)))
                                } else {
                                    Brush.verticalGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd))
                                }
                                val plusBorder = if (isDark) {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f)))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.35f)))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer { scaleX = scale; scaleY = scale }
                                        .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                                        .clip(CircleShape)
                                        .background(plusBg)
                                        .border(0.7.dp, plusBorder, CircleShape)
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            pressed.value = true
                                            showAddMenu = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add food", tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                LaunchedEffect(pressed.value) {
                                    if (pressed.value) {
                                        kotlinx.coroutines.delay(120)
                                        pressed.value = false
                                    }
                                }
                                DropdownMenu(
                                    expanded = showAddMenu,
                                    onDismissRequest = { showAddMenu = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = glass.bgTop,
                                    shadowElevation = 12.dp,
                                    modifier = Modifier.border(1.dp, glass.border, RoundedCornerShape(16.dp))
                                ) {
                                    MenuRow(label = "Camera", icon = Icons.Filled.CameraAlt) { showAddMenu = false; openCamera() }
                                    MenuRow(label = "Camera + Note", icon = Icons.AutoMirrored.Filled.Note) { showAddMenu = false; openCamera(withNote = true) }
                                    MenuRow(label = "Nutrition Label", icon = Icons.Filled.QrCodeScanner) { showAddMenu = false; openCamera() }
                                    MenuRow(label = "Barcode", icon = Icons.Filled.QrCodeScanner) { showAddMenu = false; openBarcodeScanner() }
                                    MenuRow(label = "From Photos", icon = Icons.Filled.PhotoLibrary) {
                                        showAddMenu = false
                                        pendingPickedPhotoWantsNote = false
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    MenuRow(label = "From Photos + Note", icon = Icons.AutoMirrored.Filled.Note) {
                                        showAddMenu = false
                                        pendingPickedPhotoWantsNote = true
                                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    MenuRow(label = "Text Input", icon = Icons.Filled.Edit) { showAddMenu = false; showText = true }
                                    MenuRow(label = "Voice", icon = Icons.Filled.Mic) { showAddMenu = false; showVoice = true }
                                    MenuRow(label = "Manual Entry", icon = Icons.Filled.Calculate) { showAddMenu = false; showManual = true }
                                    MenuRow(label = "Saved Meals", icon = Icons.Filled.Bookmark) { showAddMenu = false; showSaved = true }
                                    MenuRow(label = "Copy from Day", icon = Icons.Filled.CalendarMonth) { showAddMenu = false; showCopyFromDay = true }
                                }
                            }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    item {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            WeekEnergyStrip(
                                selectedDate = selectedDate,
                                onSelect = { vm.setSelectedDate(it) },
                                weekStartsOnMonday = weekStartsOnMonday
                            )
                        }
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                    item { CalorieHero(current = ui.caloriesToday, goal = ui.profile?.effectiveCalories ?: 2000) }

                    item { Spacer(Modifier.height(12.dp)) }
                    item {
                        // Horizontally scrollable: nutrient cards + "View More" card at end
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ui.homeTopNutrients) { nutrient ->
                                MacroGlassCard(
                                    label = nutrient.displayName,
                                    current = nutrient.current(ui.todayEntries).toInt(),
                                    goal = nutrient.goal(ui.profile, ui.optionalNutrientGoals).toInt(),
                                    unit = nutrient.unit
                                )
                            }
                            item {
                                ViewMoreCard(onClick = { showNutritionDetail = true })
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                    if (mealGroups.isEmpty()) {
                        item { SectionHeader("Today's Food") }
                        item {
                            SectionCardWrapper(isFirst = true, isLast = true) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                                    Text(
                                        "No foods logged",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = glass.textPrimary.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    } else {
                        for ((groupIndex, group) in mealGroups.withIndex()) {
                            item(key = "header-${group.id}") {
                                MealSectionHeader(
                                    meal = group.meal,
                                    showSortMenu = groupIndex == 0,
                                    sortOrder = ui.foodLogSortOrder,
                                    sortMenuExpanded = showSortMenu,
                                    onSortClick = { showSortMenu = true },
                                    onSortDismiss = { showSortMenu = false },
                                    onSortOrderSelected = { order ->
                                        showSortMenu = false
                                        vm.setFoodLogSortOrder(order)
                                    }
                                )
                            }
                            items(group.entries, key = { it.id }) { entry ->
                                val index = group.entries.indexOf(entry)
                                SectionCardWrapper(isFirst = index == 0, isLast = index == group.entries.lastIndex) {
                                    val isFav = ui.isFavorite(entry)
                                    SwipeableFoodRow(
                                        entry = entry,
                                        isFavorite = isFav,
                                        onTap = { editingEntry = entry },
                                        onDelete = { vm.deleteEntry(entry.id) },
                                        onToggleFavorite = { vm.toggleFavorite(entry) }
                                    )
                                    if (index != group.entries.lastIndex) Divider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal & Dialog rendering (Glassmorphism integration)

        if (showText) {
            TextInputDialog(
                onDismiss = { showText = false },
                onSubmit = { showText = false; vm.analyzeText(it) }
            )
        }

        if (showVoice) {
            VoiceInputSheet(
                container = container,
                onDismiss = { showVoice = false },
                onSubmit = { showVoice = false; vm.analyzeText(it) }
            )
        }

        if (showManual) {
            ManualEntryDialog(
                onDismiss = { showManual = false },
                onSave = { name, kcal, p, c, f, meal ->
                    showManual = false
                    vm.saveManualEntry(name, kcal, p, c, f, meal)
                }
            )
        }

        if (showSaved) {
            SavedMealsSheet(
                container = container,
                onDismiss = { showSaved = false },
                onRelogEntry = { vm.reviewSavedMeal(it) }
            )
        }

        if (showCopyFromDay) {
            CopyFromDaySheet(
                targetDate = ui.date,
                allEntries = allEntries,
                onCopy = { entries ->
                    vm.copyEntriesToSelectedDay(entries)
                    showCopyFromDay = false
                },
                onDismiss = { showCopyFromDay = false }
            )
        }

        if (showBarcodeScanner) {
            BarcodeScannerSheet(
                onBarcode = { barcode ->
                    showBarcodeScanner = false
                    vm.lookupBarcode(barcode)
                },
                onDismiss = { showBarcodeScanner = false }
            )
        }

        editingEntry?.let { entry ->
            EditFoodEntrySheet(
                entry = entry,
                onSave = { updated ->
                    vm.updateEntry(updated)
                    editingEntry = null
                },
                onDismiss = { editingEntry = null }
            )
        }

        if (showNutritionDetail) {
            NutritionDetailSheet(
                entries = ui.todayEntries,
                profile = ui.profile,
                homeTopNutrients = ui.homeTopNutrients,
                optionalGoals = ui.optionalNutrientGoals,
                onHomeTopNutrientsChange = vm::setHomeTopNutrients,
                onDismiss = { showNutritionDetail = false }
            )
        }

        pendingNoteImageBytes?.let { bytes ->
            ContextNoteSheet(
                imageBytes = bytes,
                onAnalyze = { note ->
                    pendingNoteImageBytes = null
                    vm.analyzePhotoWithNote(bytes, note)
                },
                onDismiss = { pendingNoteImageBytes = null }
            )
        }

        if (ui.analyzing) AnalyzingOverlay(imageBytes = ui.pendingImageBytes)

        ui.pendingAnalysis?.let { analysis ->
            FoodResultSheet(
                analysis = analysis,
                imageBytes = ui.pendingImageBytes,
                onSave = { name, grams, scale, mealType, selectedServingUnit, selectedServingQuantity ->
                    vm.saveAnalysis(name, grams, scale, mealType, selectedServingUnit, selectedServingQuantity)
                },
                onDismiss = { vm.dismissPending() }
            )
        }

        ui.error?.let { err ->
            val dialogShape = RoundedCornerShape(24.dp)
            AlertDialog(
                onDismissRequest = { vm.dismissPending() },
                title = { Text("Something went wrong") },
                text = { Text(err) },
                confirmButton = { TextButton(onClick = { vm.dismissPending() }) { Text("OK", color = AppColors.Calorie) } },
                containerColor = glass.bgBottom,
                titleContentColor = glass.textPrimary,
                textContentColor = glass.textPrimary.copy(alpha = 0.8f),
                shape = dialogShape,
                modifier = Modifier.border(1.dp, glass.border, dialogShape)
            )
        }
    }
}

// ── Ambient Orbs Animation ──────────────────────────────────────────────────

@Composable
private fun AmbientOrbs(listState: LazyListState) {
    val glass = LocalGlassTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val parallax = listState.firstVisibleItemScrollOffset * 0.15f
        val orbDrift = drift * 30f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbA, Color.Transparent),
                center = Offset(size.width * 0.15f, 180f - parallax + orbDrift),
                radius = 340f
            ),
            radius = 340f,
            center = Offset(size.width * 0.15f, 180f - parallax + orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbB, Color.Transparent),
                center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift),
                radius = 260f
            ),
            radius = 260f,
            center = Offset(size.width * 0.82f, 320f - parallax * 0.5f - orbDrift)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.orbC, Color.Transparent),
                center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f),
                radius = 300f
            ),
            radius = 300f,
            center = Offset(size.width * 0.6f, size.height * 0.75f + parallax * 0.3f)
        )
    }
}

// ── Week strip ────────────────────────────────────────────

// ── Week strip (Upgraded Card UI) ────────────────────────────────────────────

@Composable
private fun WeekStripSection(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val glass = LocalGlassTheme.current
    val firstDow = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekStart = remember(selectedDate, firstDow) {
        val offset = ((selectedDate.dayOfWeek.value - firstDow.value) + 7) % 7
        selectedDate.minusDays(offset.toLong())
    }
    val today = remember { LocalDate.now() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Creates equal spacing between cards
    ) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val isSel = date == selectedDate
            val isTdy = date == today

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp) // Gives the card a nice, touch-friendly height
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        // Selected gets the premium gradient, unselected gets a glass surface
                        if (isSel) AppColors.CalorieGradient 
                        else androidx.compose.ui.graphics.Brush.linearGradient(listOf(glass.surfaceHover, glass.surfaceHover))
                    )
                    .then(
                        // If it's today but NOT selected, give it a subtle colored border to stand out
                        if (isTdy && !isSel) Modifier.border(1.dp, AppColors.Calorie.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        else if (!isSel) Modifier.border(1.dp, glass.border, RoundedCornerShape(16.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(date) }
                    )
            ) {
                // Day of Week (S, M, T...)
                Text(
                    text = shortDay(date.dayOfWeek),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSel) Color.White.copy(alpha = 0.85f) else glass.textPrimary.copy(alpha = 0.5f)
                )
                
                Spacer(Modifier.height(4.dp))
                
                // Date Number (24, 25, 26...)
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.White else glass.textPrimary
                )
                
                // Optional: A tiny dot indicator under the number if it is exactly "Today" AND selected
                if (isTdy && isSel) {
                    Spacer(Modifier.height(2.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
    }
}

private fun shortDay(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}

// ── Calorie hero ─────────────────────────────────────────────────────

@Composable
private fun CalorieHero(current: Int, goal: Int) {
    val glass = LocalGlassTheme.current

    val ratio = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f

    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 50f),
        label = "calorieProgress"
    )
    val remaining = maxOf(0, goal - current)

    // 3D Press Animation — same as GlassUpdateCard in AboutScreen
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            // Exact same recipe as GlassCard in AboutScreen:
            // background = glass.surface (flat, no gradient)
            // border     = borderStrong → border → shimmer (3-stop linear)
            // glow       = subtle calorie ambient
            .clip(RoundedCornerShape(24.dp))
            .background(glass.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                RoundedCornerShape(24.dp)
            )
            .glowShadow(AppColors.Calorie.copy(alpha = 0.06f), radius = 28.dp, offsetY = 6.dp)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // --- TOP HERO TEXT ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$current",
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd)),
                        fontSize = 88.sp, 
                        fontWeight = FontWeight.ExtraBold,
                        shadow = Shadow(
                            color = AppColors.Calorie.copy(alpha = 0.5f),
                            offset = Offset(0f, 10f),
                            blurRadius = 20f
                        )
                    )
                )
                Text(
                    text = "of ${String.format(java.util.Locale.getDefault(), "%,d", goal)} kcal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = glass.textPrimary.copy(alpha = 0.6f)
                )
            }

            // --- ULTRA PROGRESS BAR ---
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp) // Thicker and more premium
            ) {
                val w = maxWidth
                
                // Track — uses glass.shimmer as bg (same token used in GlassCard border)
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(glass.shimmer)
                        .border(0.5.dp, glass.border, CircleShape)
                )

                val barWidth = (w * animatedRatio).coerceAtLeast(20.dp)
                
                // Filled Gradient Bar
                Box(
                    Modifier
                        .width(barWidth)
                        .height(20.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = AppColors.Calorie,
                            spotColor = AppColors.Calorie
                        )
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd)))
                ) {
                    // Glossy 3D Shine on the top edge of the bar
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )
                    
                    // Tip Indicator (ProgressBar ke end point par ek chhota highlight)
                    if (animatedRatio > 0.05f) {
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .shadow(8.dp, CircleShape, spotColor = Color.White)
                        )
                    }
                }
            }

            // --- BOTTOM TEXT ---
            Text(
                text = "${String.format(java.util.Locale.getDefault(), "%,d", remaining)} kcal left",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = glass.textPrimary.copy(alpha = 0.85f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Dropdown Menu Row ────────────────────────────────────────────

@Composable
private fun MenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val glass = LocalGlassTheme.current
    DropdownMenuItem(
        text = {
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = glass.textPrimary
            )
        },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = glass.textPrimary, modifier = Modifier.size(20.dp))
        },
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun ViewMoreButton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            "View More",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.Calorie.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = AppColors.Calorie.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// ── Macro Glass Card (scrollable nutrient card, GlassCard style) ────

@Composable
private fun MacroGlassCard(
    label: String,
    current: Int,
    goal: Int,
    unit: String = "g"
) {
    val glass = LocalGlassTheme.current
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(glass.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        MacroCard(
            label = label,
            current = current,
            goal = goal,
            unit = unit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── View More Card (last card in scrollable row) ─────────────────────

@Composable
private fun ViewMoreCard(onClick: () -> Unit) {
    val glass = LocalGlassTheme.current
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(glass.surface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "View more nutrients",
                tint = AppColors.Calorie.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                "More",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.Calorie.copy(alpha = 0.75f)
            )
        }
    }
}

// ── Section headers / cards / rows ──────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    val glass = LocalGlassTheme.current
    Text(
        title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = glass.textPrimary,
        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun MealSectionHeader(
    meal: MealType,
    showSortMenu: Boolean = false,
    sortOrder: FoodLogSortOrder = FoodLogSortOrder.STANDARD,
    sortMenuExpanded: Boolean = false,
    onSortClick: () -> Unit = {},
    onSortDismiss: () -> Unit = {},
    onSortOrderSelected: (FoodLogSortOrder) -> Unit = {}
) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 30.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            mealIcon(meal),
            contentDescription = null,
            tint = glass.textPrimary.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(meal.displayNameRes),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = glass.textPrimary.copy(alpha = 0.85f)
        )
        if (showSortMenu) {
            Spacer(Modifier.weight(1f))
            Box {
                Row(
                    modifier = Modifier.clickable { onSortClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = null,
                        tint = AppColors.Calorie,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Sort",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Calorie
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = onSortDismiss,
                    containerColor = glass.bgTop,
                    modifier = Modifier.border(1.dp, glass.border, RoundedCornerShape(12.dp))
                ) {
                    for (order in FoodLogSortOrder.entries) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    order.displayName,
                                    fontWeight = if (order == sortOrder) FontWeight.SemiBold else FontWeight.Normal,
                                    color = glass.textPrimary
                                )
                            },
                            onClick = { onSortOrderSelected(order) }
                        )
                    }
                }
            }
        }
    }
}

private data class FoodLogMealGroup(
    val id: String,
    val meal: MealType,
    val entries: List<FoodEntry>
)

private fun foodLogMealGroups(
    entries: List<FoodEntry>,
    sortOrder: FoodLogSortOrder
): List<FoodLogMealGroup> = when (sortOrder) {
    FoodLogSortOrder.STANDARD -> {
        val grouped = entries.groupBy { it.mealType }
        listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK, MealType.OTHER)
            .mapNotNull { meal ->
                val mealEntries = grouped[meal].orEmpty()
                if (mealEntries.isEmpty()) null else FoodLogMealGroup(
                    id = "standard-${meal.name}",
                    meal = meal,
                    entries = mealEntries
                )
            }
    }
    FoodLogSortOrder.LATEST_MEALS_FIRST -> latestMealRuns(entries)
}

private fun latestMealRuns(entries: List<FoodEntry>): List<FoodLogMealGroup> {
    val sortedEntries = entries.sortedByDescending { it.timestamp }
    val groups = mutableListOf<FoodLogMealGroup>()
    var currentMeal: MealType? = null
    val currentEntries = mutableListOf<FoodEntry>()

    fun appendCurrentGroup() {
        val meal = currentMeal ?: return
        if (currentEntries.isEmpty()) return
        groups += FoodLogMealGroup(
            id = "latest-${groups.size}-${meal.name}-${currentEntries.first().id}",
            meal = meal,
            entries = currentEntries.toList()
        )
    }

    for (entry in sortedEntries) {
        if (entry.mealType == currentMeal) {
            currentEntries += entry
        } else {
            appendCurrentGroup()
            currentMeal = entry.mealType
            currentEntries.clear()
            currentEntries += entry
        }
    }

    appendCurrentGroup()
    return groups
}

private fun mealIcon(meal: MealType): ImageVector = when (meal) {
    MealType.BREAKFAST -> Icons.Filled.WbTwilight
    MealType.LUNCH -> Icons.Filled.WbSunny
    MealType.DINNER -> Icons.Filled.Bedtime
    MealType.SNACK -> Icons.Filled.Coffee
    MealType.OTHER -> Icons.Filled.Restaurant
}

@Composable
private fun SectionCardWrapper(isFirst: Boolean, isLast: Boolean, content: @Composable () -> Unit) {
    val glass = LocalGlassTheme.current
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(22.dp)
        isFirst -> RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
        isLast -> RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)
        else -> RoundedCornerShape(0.dp)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(glass.surface) // Glass UI Card
    ) { content() }
}

@Composable
private fun Divider() {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .padding(start = 102.dp, end = 14.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(glass.divider)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableFoodRow(
    entry: FoodEntry,
    isFavorite: Boolean,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    @Suppress("DEPRECATION")
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    
    val density = LocalDensity.current
    val triggerPx = with(density) { 120.dp.toPx() }
    var firedThisSwipe by remember { mutableStateOf(false) }
    
    LaunchedEffect(state) {
        snapshotFlow { runCatching { state.requireOffset() }.getOrDefault(0f) }
            .collect { offset ->
                if (offset > triggerPx && !firedThisSwipe) {
                    firedThisSwipe = true
                    onToggleFavorite()
                    state.reset()
                }
                if (offset <= 0f) firedThisSwipe = false
            }
    }
    
    SwipeToDismissBox(
        state = state,
        backgroundContent = { SwipeBackground(state, isFavorite) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.clickable(onClick = onTap)) {
            FoodRow(entry = entry, isFavorite = isFavorite)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: androidx.compose.material3.SwipeToDismissBoxState, isFavorite: Boolean) {
    val direction = state.dismissDirection
    if (direction == SwipeToDismissBoxValue.Settled) {
        Box(Modifier.fillMaxSize())
        return
    }
    val (bg, icon, label) = when (direction) {
        SwipeToDismissBoxValue.EndToStart -> Triple(Color(0xFFD32F2F), Icons.Filled.Delete, "Delete")
        SwipeToDismissBoxValue.StartToEnd -> Triple(AppColors.Calorie, if (isFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite, if (isFavorite) "Unfavorite" else "Favorite")
        else -> return
    }
    
    val offset = runCatching { state.requireOffset() }.getOrDefault(0f)
    val widthPx = kotlin.math.abs(offset)
    val widthDp = with(LocalDensity.current) { widthPx.toDp() }
    val alignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(alignment)
                .fillMaxHeight()
                .width(widthDp)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            if (widthPx > 24f) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
    }
}

@Composable
private fun FoodRow(entry: FoodEntry, isFavorite: Boolean = false) {
    val glass = LocalGlassTheme.current
    val timeFmt = DateTimeFormatter.ofPattern("h:mma", Locale.US).withZone(ZoneId.systemDefault())
    val ctx = LocalContext.current
    val container = (ctx.applicationContext as com.apoorvdarshan.ai.FudAIApp).container
    val bitmap = remember(entry.imageFilename) {
        entry.imageFilename?.let { container.imageStore.load(it) }
    }
    
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Use wrapper's background
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(glass.surfaceHover),
            contentAlignment = Alignment.Center
        ) {
            when {
                bitmap != null -> androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = entry.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
                entry.emoji != null -> Text(entry.emoji ?: "", fontSize = 36.sp)
                else -> Icon(
                    Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = AppColors.Calorie,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            Modifier.weight(1f).padding(top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        entry.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = glass.textPrimary,
                        maxLines = 2,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Favorited",
                            tint = AppColors.Calorie,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    timeFmt.format(entry.timestamp).lowercase(),
                    fontSize = 12.sp,
                    color = glass.textPrimary.copy(alpha = 0.45f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "${entry.calories} kcal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Calorie
                )
                entry.servingSizeGrams?.takeIf { it > 0 }?.let { grams ->
                    Text("·", color = glass.textPrimary.copy(alpha = 0.4f))
                    val gramsText = if (grams == grams.toInt().toDouble()) "${grams.toInt()}g" else String.format("%.1fg", grams)
                    Text(
                        gramsText,
                        fontSize = 12.sp,
                        color = glass.textPrimary.copy(alpha = 0.6f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroChip("P", entry.protein.toInt())
                MacroChip("C", entry.carbs.toInt())
                MacroChip("F", entry.fat.toInt())
            }
        }
    }
}

@Composable
private fun MacroChip(label: String, value: Int) {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .clip(CircleShape)
            .background(glass.surfaceHover)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$label ${value}g",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = glass.textPrimary.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyFromDaySheet(
    targetDate: LocalDate,
    allEntries: List<FoodEntry>,
    onCopy: (List<FoodEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalGlassTheme.current
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    var sourceDate by remember(targetDate) { mutableStateOf(targetDate.minusDays(1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val sourceEntries = remember(allEntries, sourceDate) {
        allEntries
            .filter { it.timestamp.atZone(zone).toLocalDate() == sourceDate }
            .sortedByDescending { it.timestamp }
    }
    val groups = remember(sourceEntries) {
        foodLogMealGroups(sourceEntries, FoodLogSortOrder.STANDARD)
    }
    val targetText = if (targetDate == LocalDate.now()) "today" else targetDate.format(dateFmt)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop
    ) {
        SheetReviewToolbar(
            title = "Copy from Day",
            primaryLabel = if (sourceEntries.isEmpty()) "Done" else "Copy All",
            onCancel = onDismiss,
            onPrimary = { if (sourceEntries.isEmpty()) onDismiss() else onCopy(sourceEntries) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SheetSectionHeader("Source")
                    SheetPillRow(onClick = { showDatePicker = true }) {
                        Text("Copy From", fontSize = 17.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                        Text(
                            sourceDate.format(dateFmt),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Calorie
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Foods will be copied to $targetText. Original entries stay unchanged.",
                        fontSize = 13.sp,
                        color = glass.textPrimary.copy(alpha = 0.55f),
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }

            if (sourceEntries.isEmpty()) {
                item {
                    SectionCardWrapper(isFirst = true, isLast = true) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = AppColors.Calorie.copy(alpha = 0.45f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                "No foods logged on this day",
                                fontSize = 15.sp,
                                color = glass.textPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = { onCopy(sourceEntries) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(50.dp)
                    ) {
                        Text(
                            "Copy ${sourceEntries.size} food${if (sourceEntries.size == 1) "" else "s"} to $targetText",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                groups.forEach { group ->
                    item(key = "copy-header-${group.id}") {
                        MealSectionHeader(meal = group.meal)
                    }
                    item(key = "copy-meal-${group.id}") {
                        Button(
                            onClick = { onCopy(group.entries) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(46.dp)
                        ) {
                            Text(
                                "Copy ${stringResource(group.meal.displayNameRes)}",
                                color = AppColors.Calorie,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    items(group.entries, key = { "copy-entry-${it.id}" }) { entry ->
                        val index = group.entries.indexOf(entry)
                        SectionCardWrapper(isFirst = index == 0, isLast = index == group.entries.lastIndex) {
                            Box(Modifier.clickable { onCopy(listOf(entry)) }) {
                                FoodRow(entry = entry)
                            }
                            if (index != group.entries.lastIndex) Divider()
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = sourceDate.atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        )
        val dialogShape = RoundedCornerShape(24.dp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = dialogShape,
            colors = DatePickerDefaults.colors(containerColor = glass.bgBottom),
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        sourceDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("Done", color = AppColors.Calorie)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = glass.textPrimary.copy(alpha = 0.6f))
                }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = AppColors.Calorie,
                    todayDateBorderColor = AppColors.Calorie,
                    currentYearContentColor = AppColors.Calorie,
                    selectedYearContainerColor = AppColors.Calorie,
                    dayContentColor = glass.textPrimary,
                    headlineContentColor = glass.textPrimary,
                    titleContentColor = glass.textPrimary.copy(alpha = 0.6f),
                    weekdayContentColor = glass.textPrimary.copy(alpha = 0.6f),
                    yearContentColor = glass.textPrimary
                )
            )
        }
    }
}

// ── Dialogs ──────────────────────────────

@Composable
private fun AnalyzingOverlay(imageBytes: ByteArray? = null) {
    val glass = LocalGlassTheme.current
    val bitmap = remember(imageBytes) {
        imageBytes?.let { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(glass.bgTop),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Icon(
                    Icons.Filled.ImageSearch,
                    contentDescription = null,
                    tint = AppColors.Calorie,
                    modifier = Modifier.size(64.dp)
                )
            }
            CircularProgressIndicator(
                color = AppColors.Calorie,
                strokeWidth = 4.dp,
                modifier = Modifier.size(40.dp)
            )
            Text(
                if (bitmap != null) "Analyzing your food..." else "Looking up nutrition...",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Calorie
            )
        }
    }
}

@Composable
private fun TextInputDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val glass = LocalGlassTheme.current
    val placeholders = listOf(
        "2 eggs, toast with butter and a coffee",
        "Chipotle burrito bowl with chicken and rice",
        "Domino's pepperoni pizza, 2 slices",
        "Greek yogurt with granola and blueberries"
    )
    var input by remember { mutableStateOf("") }
    var placeholderIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            if (input.isEmpty()) placeholderIdx = (placeholderIdx + 1) % placeholders.size
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(glass.bgBottom)
                .border(1.dp, glass.border, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        androidx.compose.animation.Crossfade(
                            targetState = placeholderIdx,
                            animationSpec = androidx.compose.animation.core.tween(300),
                            label = "placeholder"
                        ) { idx ->
                            Text(
                                placeholders[idx],
                                color = glass.textPrimary.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        }
                    },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        focusedContainerColor = glass.surface.copy(alpha = 0.3f),
                        unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                        focusedBorderColor = AppColors.Calorie,
                        unfocusedBorderColor = glass.border,
                        cursorColor = AppColors.Calorie
                    )
                )
                Button(
                    onClick = { if (input.isNotBlank()) onSubmit(input.trim()) },
                    enabled = input.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Analyze", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = glass.textPrimary.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealType: MealType) -> Unit
) {
    val glass = LocalGlassTheme.current
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.currentMeal) }
    var mealMenuExpanded by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && calories.toIntOrNull() != null

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(glass.bgBottom)
                .border(1.dp, glass.border, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Manual Entry", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = glass.textPrimary)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Homemade salad", color = glass.textPrimary.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = glass.textPrimary,
                        unfocusedTextColor = glass.textPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = AppColors.Calorie,
                        unfocusedBorderColor = glass.border,
                        cursorColor = AppColors.Calorie,
                        focusedLabelColor = AppColors.Calorie,
                        unfocusedLabelColor = glass.textPrimary.copy(alpha = 0.6f)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Calories", calories, { calories = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    NumberField("Protein (g)", protein, { protein = it.filter(Char::isDigit) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField("Carbs (g)", carbs, { carbs = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    NumberField("Fat (g)", fat, { fat = it.filter(Char::isDigit) }, Modifier.weight(1f))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(glass.surface)
                        .border(1.dp, glass.border, RoundedCornerShape(12.dp))
                        .clickable { mealMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Meal Type", fontSize = 16.sp, modifier = Modifier.weight(1f), color = glass.textPrimary)
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                sheetMealIcon(mealType),
                                contentDescription = null,
                                tint = AppColors.Calorie,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(mealType.displayNameRes),
                                fontSize = 16.sp,
                                color = AppColors.Calorie,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        DropdownMenu(
                            expanded = mealMenuExpanded,
                            onDismissRequest = { mealMenuExpanded = false },
                            containerColor = glass.bgTop,
                            modifier = Modifier.border(1.dp, glass.border, RoundedCornerShape(12.dp))
                        ) {
                            for (m in MealType.entries) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(sheetMealIcon(m), contentDescription = null, tint = AppColors.Calorie)
                                    },
                                    text = { Text(stringResource(m.displayNameRes), color = glass.textPrimary) },
                                    onClick = {
                                        mealType = m
                                        mealMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            calories.toIntOrNull() ?: 0,
                            protein.toIntOrNull() ?: 0,
                            carbs.toIntOrNull() ?: 0,
                            fat.toIntOrNull() ?: 0,
                            mealType
                        )
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Calorie),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Save", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = glass.textPrimary.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val glass = LocalGlassTheme.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("0", color = glass.textPrimary.copy(alpha = 0.4f)) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = glass.textPrimary,
            unfocusedTextColor = glass.textPrimary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = AppColors.Calorie,
            unfocusedBorderColor = glass.border,
            cursorColor = AppColors.Calorie,
            focusedLabelColor = AppColors.Calorie,
            unfocusedLabelColor = glass.textPrimary.copy(alpha = 0.6f)
        )
    )
}