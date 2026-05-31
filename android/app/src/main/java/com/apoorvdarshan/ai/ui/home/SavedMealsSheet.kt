package com.apoorvdarshan.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.data.FrequentFoodGroup
import com.apoorvdarshan.ai.models.FoodEntry
import com.apoorvdarshan.ai.services.FoodImageStore
import com.apoorvdarshan.ai.ui.theme.LocalGlassTheme
import com.apoorvdarshan.ai.ui.theme.AppColors
import kotlinx.coroutines.launch
import androidx.compose.material3.ModalBottomSheet
import com.apoorvdarshan.ai.ui.theme.glowShadow

enum class SavedTab { RECENTS, FREQUENT, FAVORITES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMealsSheet(
    container: AppContainer,
    onDismiss: () -> Unit,
    onRelogEntry: (FoodEntry) -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    // Applying Glass Theme
    val glass = LocalGlassTheme.current

    val savedSegment by container.prefs.lastSavedMealsSegment.collectAsState(initial = SavedTab.RECENTS.name)
    var tab by remember(savedSegment) {
        mutableStateOf(
            runCatching { SavedTab.valueOf(savedSegment) }.getOrDefault(SavedTab.RECENTS)
        )
    }
    var recents by remember { mutableStateOf<List<FoodEntry>>(emptyList()) }
    var frequent by remember { mutableStateOf<List<FrequentFoodGroup>>(emptyList()) }

    val favorites by container.foodRepository.favorites.collectAsState(initial = emptyList())
    val favKeys by container.foodRepository.favoriteKeys.collectAsState(initial = emptySet())

    var searchQuery by remember(tab) { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    
    val filteredRecents = remember(recents, searchQuery) {
        if (!isSearching) recents
        else recents.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val filteredFrequent = remember(frequent, searchQuery) {
        if (!isSearching) frequent
        else frequent.filter { it.template.name.contains(searchQuery.trim(), ignoreCase = true) }
    }
    val filteredFavorites = remember(favorites, searchQuery) {
        if (!isSearching) favorites
        else favorites.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    LaunchedEffect(Unit) { container.foodRepository.migratedFavorites() }

    LaunchedEffect(tab, favKeys) {
        when (tab) {
            SavedTab.RECENTS -> recents = container.foodRepository.recent(50)
            SavedTab.FREQUENT -> frequent = container.foodRepository.frequent()
            SavedTab.FAVORITES -> Unit 
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = glass.bgTop // Glass UI specific background
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                "Saved Meals",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = glass.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                textAlign = TextAlign.Center
            )
            
            SegmentedTabs(selected = tab, onSelect = { newTab ->
                tab = newTab
                scope.launch { container.prefs.setLastSavedMealsSegment(newTab.name) }
            })
            
            Spacer(Modifier.height(12.dp))

            // Glassmorphism Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.saved_meals_search_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = glass.textPrimary,
                    unfocusedTextColor = glass.textPrimary,
                    focusedBorderColor = AppColors.Calorie,
                    unfocusedBorderColor = glass.border,
                    focusedContainerColor = glass.surface,
                    unfocusedContainerColor = glass.surface.copy(alpha = 0.3f),
                    cursorColor = AppColors.Calorie,
                    focusedLeadingIconColor = glass.textPrimary.copy(alpha = 0.6f),
                    unfocusedLeadingIconColor = glass.textPrimary.copy(alpha = 0.6f),
                    focusedPlaceholderColor = glass.textPrimary.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = glass.textPrimary.copy(alpha = 0.4f),
                ),
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = if (isSearching) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = glass.textPrimary.copy(alpha = 0.6f))
                        }
                    }
                } else null,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))

            when (tab) {
                SavedTab.RECENTS -> {
                    if (filteredRecents.isEmpty()) {
                        val msg = if (isSearching) stringResource(R.string.saved_meals_no_match)
                                  else "No foods logged yet"
                        EmptyState(icon = if (isSearching) Icons.Outlined.Search else Icons.Outlined.Schedule, text = msg)
                    } else {
                        SavedList(items = filteredRecents) { entry ->
                            SavedMealRow(
                                entry = entry,
                                isFavorite = entry.favoriteKey in favKeys,
                                subtitle = null,
                                imageStore = container.imageStore,
                                onClick = { onRelogEntry(entry); onDismiss() }
                            )
                        }
                    }
                }
                SavedTab.FREQUENT -> {
                    if (filteredFrequent.isEmpty()) {
                        val msg = if (isSearching) stringResource(R.string.saved_meals_no_match)
                                  else "No foods logged yet"
                        EmptyState(icon = if (isSearching) Icons.Outlined.Search else Icons.Outlined.Refresh, text = msg)
                    } else {
                        SavedList(items = filteredFrequent) { group ->
                            SavedMealRow(
                                entry = group.template,
                                isFavorite = group.template.favoriteKey in favKeys,
                                subtitle = "${group.count}× logged",
                                imageStore = container.imageStore,
                                onClick = { onRelogEntry(group.template); onDismiss() }
                            )
                        }
                    }
                }
                SavedTab.FAVORITES -> {
                    if (favorites.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Favorite,
                            text = "No favorites yet\nSwipe left on any food to add it"
                        )
                    } else if (filteredFavorites.isEmpty()) {
                        EmptyState(icon = Icons.Outlined.Search, text = stringResource(R.string.saved_meals_no_match))
                    } else if (isSearching) {
                        SavedList(items = filteredFavorites) { entry ->
                            SavedMealRow(
                                entry = entry,
                                isFavorite = true,
                                subtitle = null,
                                imageStore = container.imageStore,
                                onClick = { onRelogEntry(entry); onDismiss() }
                            )
                        }
                    } else {
                        FavoritesReorderableList(
                            favorites = favorites,
                            imageStore = container.imageStore,
                            onTap = { entry -> onRelogEntry(entry); onDismiss() },
                            onRemove = { entry ->
                                scope.launch { container.foodRepository.toggleFavorite(entry) }
                            },
                            onMove = { from, to ->
                                scope.launch { container.foodRepository.moveFavorite(from, to) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(selected: SavedTab, onSelect: (SavedTab) -> Unit) {
    val glass = LocalGlassTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(glass.surface.copy(alpha = 0.5f))
            .border(1.dp, glass.border, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        for (t in SavedTab.entries) { // Optimized: Used .entries instead of .values()
            val isSel = t == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSel) Brush.linearGradient(listOf(AppColors.CalorieStart, AppColors.CalorieEnd))
                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable { onSelect(t) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (t) {
                        SavedTab.RECENTS -> "Recents"
                        SavedTab.FREQUENT -> "Frequent"
                        SavedTab.FAVORITES -> "Favorites"
                    },
                    color = if (isSel) Color.White else glass.textPrimary.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun <T> SavedList(items: List<T>, row: @Composable (T) -> Unit) {
    LazyColumn(
        Modifier.fillMaxWidth().heightConstraint(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { row(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesReorderableList(
    favorites: List<FoodEntry>,
    imageStore: FoodImageStore,
    onTap: (FoodEntry) -> Unit,
    onRemove: (FoodEntry) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightConstraint()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        favorites.forEachIndexed { idx, entry ->
            @Suppress("DEPRECATION")
            val swipeState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onRemove(entry)
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = swipeState,
                backgroundContent = { FavoriteRemoveBackground(swipeState) },
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            ) {
                SavedMealRow(
                    entry = entry,
                    isFavorite = true,
                    subtitle = null,
                    imageStore = imageStore,
                    onClick = { onTap(entry) },
                    trailing = {
                        MoveButtons(
                            canMoveUp = idx > 0,
                            canMoveDown = idx < favorites.size - 1,
                            onMoveUp = { onMove(idx, idx - 1) },
                            onMoveDown = { onMove(idx, idx + 1) }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun MoveButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val glass = LocalGlassTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier
                .size(width = 32.dp, height = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (canMoveUp) glass.surfaceHover else Color.Transparent)
                .clickable(enabled = canMoveUp, onClick = onMoveUp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Move up",
                tint = glass.textPrimary.copy(alpha = if (canMoveUp) 0.75f else 0.18f),
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            Modifier
                .size(width = 32.dp, height = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (canMoveDown) glass.surfaceHover else Color.Transparent)
                .clickable(enabled = canMoveDown, onClick = onMoveDown),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Move down",
                tint = glass.textPrimary.copy(alpha = if (canMoveDown) 0.75f else 0.18f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteRemoveBackground(state: SwipeToDismissBoxState) {
    val active = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val rawOffset = runCatching { state.requireOffset() }.getOrDefault(0f)
    val revealWidthPx = if (active) (-rawOffset).coerceAtLeast(0f) else 0f
    val revealWidthDp = with(LocalDensity.current) { revealWidthPx.toDp() }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(revealWidthDp)
                .background(Color(0xFFD32F2F)),
            contentAlignment = Alignment.Center
        ) {
            if (active && revealWidthPx > 24f) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove favorite", tint = Color.White)
            }
        }
    }
}

@Composable
private fun SavedMealRow(
    entry: FoodEntry,
    isFavorite: Boolean,
    subtitle: String?,
    imageStore: FoodImageStore,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val glass = LocalGlassTheme.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(glass.surface)
            .border(
                1.dp, 
                Brush.linearGradient(listOf(glass.borderStrong, glass.border, glass.shimmer)), 
                RoundedCornerShape(16.dp)
            )
            .glowShadow(AppColors.Calorie.copy(alpha = 0.04f), radius = 20.dp, offsetY = 6.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Thumbnail(emoji = entry.emoji, imageFilename = entry.imageFilename, imageStore = imageStore)

        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    entry.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = glass.textPrimary,
                    maxLines = 2
                )
                if (isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = AppColors.Calorie,
                        modifier = Modifier.size(11.dp)
                    )
                }
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
                if (subtitle != null) {
                    Text("·", color = glass.textPrimary.copy(alpha = 0.4f))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = glass.textPrimary.copy(alpha = 0.6f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroTag("P", entry.protein.toInt())
                MacroTag("C", entry.carbs.toInt())
                MacroTag("F", entry.fat.toInt())
            }
        }

        if (trailing != null) {
            trailing()
        } else {
            Icon(
                Icons.Filled.AddCircle,
                contentDescription = "Log",
                tint = AppColors.Calorie,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun Thumbnail(emoji: String?, imageFilename: String?, imageStore: FoodImageStore) {
    val glass = LocalGlassTheme.current
    val shape = RoundedCornerShape(12.dp)
    val bitmap = remember(imageFilename) { imageFilename?.let { imageStore.load(it) } }

    Box(
        Modifier
            .size(56.dp)
            .clip(shape)
            .background(glass.surface.copy(alpha = 0.4f))
            .border(1.dp, glass.border, shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape)
            )
            emoji != null -> Text(emoji, fontSize = 28.sp)
            else -> Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                tint = AppColors.Calorie.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MacroTag(label: String, value: Int) {
    val glass = LocalGlassTheme.current
    Box(
        Modifier
            .clip(CircleShape)
            .background(glass.surfaceHover)
            .border(0.5.dp, glass.border, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            "$label ${value}g",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = glass.textPrimary.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    val glass = LocalGlassTheme.current
    Box(
        Modifier.fillMaxWidth().heightConstraint(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(glass.surface)
                    .border(1.dp, glass.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AppColors.Calorie.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text,
                fontSize = 15.sp,
                color = glass.textPrimary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Modifier.heightConstraint(): Modifier = this.height(420.dp)