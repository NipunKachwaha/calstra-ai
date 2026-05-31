package com.apoorvdarshan.ai.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.models.BodyFatEntry
import com.apoorvdarshan.ai.models.UserProfile
import com.apoorvdarshan.ai.models.WeightEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// UI State & Interaction Models
// ─────────────────────────────────────────────────────────────────────────────

data class ProgressUiState(
    val entries: List<WeightEntry> = emptyList(),
    val bodyFatEntries: List<BodyFatEntry> = emptyList(),
    val profile: UserProfile? = null,
    val goalReached: Boolean = false
)

/** * UI interactions (like showing a dialog) ko track karne ke liye alag state.
 * Isse database stream ke updates aur transient UI states aapas me clash nahi karte.
 */
private data class InteractionState(
    val goalReached: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class ProgressViewModel(private val container: AppContainer) : ViewModel() {
    
    // Transient interaction state
    private val _interaction = MutableStateFlow(InteractionState())

    // 🚀 OPTIMIZATION: Database streams aur UI interactions ka perfect synchronization
    val ui: StateFlow<ProgressUiState> = combine(
        container.profileRepository.profile,
        container.weightRepository.entries,
        container.bodyFatRepository.entries,
        _interaction
    ) { p, weights, bodyFats, interaction ->
        ProgressUiState(
            entries = weights,
            bodyFatEntries = bodyFats,
            profile = p,
            goalReached = interaction.goalReached
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // App minimize hone pe memory bachayega
        initialValue = ProgressUiState()
    )

    // ── Actions ──────────────────────────────────────────────────────────────

    fun addWeight(kg: Double) {
        viewModelScope.launch {
            val event = container.weightRepository.addEntry(WeightEntry(weightKg = kg))
            if (event != null) {
                // UI state seedha update karne ki jagah sirf interaction state update karenge
                _interaction.update { it.copy(goalReached = true) }
                container.notifications.showGoalReached()
            }
        }
    }

    fun deleteWeight(id: UUID) {
        viewModelScope.launch { container.weightRepository.deleteEntry(id) }
    }

    fun addBodyFat(fraction: Double) {
        viewModelScope.launch {
            container.bodyFatRepository.addEntry(BodyFatEntry(bodyFatFraction = fraction))
        }
    }

    fun deleteBodyFat(id: UUID) {
        viewModelScope.launch { container.bodyFatRepository.deleteEntry(id) }
    }

    fun dismissGoalReached() {
        _interaction.update { it.copy(goalReached = false) }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProgressViewModel(container) as T
    }
}