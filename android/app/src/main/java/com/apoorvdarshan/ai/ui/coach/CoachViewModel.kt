package com.apoorvdarshan.ai.ui.coach

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apoorvdarshan.ai.AppContainer
import com.apoorvdarshan.ai.R
import com.apoorvdarshan.ai.models.ChatMessage
import com.apoorvdarshan.ai.models.WeightGoal
import com.apoorvdarshan.ai.services.ai.AiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Base64

// ─────────────────────────────────────────────────────────────────────────────
// UI State & Error Models
// ─────────────────────────────────────────────────────────────────────────────

data class CoachUiState(
    val messages     : List<ChatMessage>  = emptyList(),
    val suggestions  : List<Int>          = emptyList(),
    val sending      : Boolean            = false,
    val error        : CoachError?        = null
) {
    val isEmpty: Boolean get() = messages.isEmpty() && !sending
    val canReset: Boolean get() = messages.isNotEmpty()
}

/** * Internal state just for tracking network/interaction status.
 * This keeps transient UI states separate from database streams.
 */
private data class InteractionState(
    val sending: Boolean = false,
    val error: CoachError? = null
)

sealed class CoachError {
    data class FromResource(@param:StringRes val resId: Int) : CoachError()
    data class Literal(
        val message : String,
        @param:StringRes val fallbackResId: Int? = null
    ) : CoachError()

    companion object {
        fun fromThrowable(t: Throwable): CoachError {
            val msg = t.localizedMessage
            return if (msg.isNullOrBlank())
                FromResource(R.string.coach_chat_failed)
            else
                Literal(msg)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class CoachViewModel(private val container: AppContainer) : ViewModel() {

    // ── state ─────────────────────────────────────────────────────────────────

    // Transient interaction state (loading/errors)
    private val _interaction = MutableStateFlow(InteractionState())

    private var sendJob: Job? = null

    // 🚀 OPTIMIZATION: Combine flows declaratively. 
    // UI reacts automatically whenever DB (messages/profile) OR network interaction changes.
    val ui: StateFlow<CoachUiState> = combine(
        container.chatRepository.messages,
        container.profileRepository.profile,
        _interaction
    ) { msgs, profile, interaction ->
        CoachUiState(
            messages    = msgs,
            suggestions = chipsFor(profile?.goal),
            sending     = interaction.sending,
            error       = interaction.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Resource efficient for Compose
        initialValue = CoachUiState()
    )

    // ── chip derivation ───────────────────────────────────────────────────────

    private fun chipsFor(goal: WeightGoal?): List<Int> = when (goal) {
        WeightGoal.LOSE -> listOf(
            R.string.coach_chip_predict_30_days,
            R.string.coach_chip_lose_faster,
            R.string.coach_chip_eating_too_much,
            R.string.coach_chip_what_dinner
        )
        WeightGoal.GAIN -> listOf(
            R.string.coach_chip_predict_30_days,
            R.string.coach_chip_gain_healthy,
            R.string.coach_chip_eating_enough,
            R.string.coach_chip_high_protein
        )
        WeightGoal.MAINTAIN -> listOf(
            R.string.coach_chip_holding_weight,
            R.string.coach_chip_average_intake,
            R.string.coach_chip_macro_suggestions,
            R.string.coach_chip_trend
        )
        else -> listOf(
            R.string.coach_chip_doing_this_week,
            R.string.coach_chip_predict_30_days,
            R.string.coach_chip_log_advice
        )
    }

    // ── public API ────────────────────────────────────────────────────────────

    fun send(
        userText      : String,
        imageBytes    : ByteArray? = null,
        thumbnailBytes: ByteArray? = null
    ) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() && imageBytes == null) return
        if (_interaction.value.sending) return

        val displayText = trimmed.ifEmpty { "Analyze this image." }

        sendJob?.cancel()
        sendJob = viewModelScope.launch {
            // 1. Optimistic UI update
            val userMsg = ChatMessage(
                role                   = ChatMessage.Role.USER,
                content                = displayText,
                attachmentImageBase64  = thumbnailBytes?.let { Base64.getEncoder().encodeToString(it) }
            )
            container.chatRepository.append(userMsg)
            
            // Start loading state
            _interaction.update { it.copy(sending = true, error = null) }

            try {
                // 2. Gather context
                val history = container.chatRepository
                    .contextMessages(limit = 20)
                    .dropLast(1)

                val profile = container.profileRepository.current()
                    ?: return@launch setError(CoachError.FromResource(R.string.coach_no_profile_error))

                val weights   = container.weightRepository.entries.first()
                val bodyFats  = container.bodyFatRepository.entries.first()
                val foods     = container.foodRepository.entries.first()
                val useMetric = container.prefs.useMetric.first()

                // 3. Remote AI call
                val reply = container.chatService.sendMessage(
                    history        = history,
                    newUserMessage = displayText,
                    profile        = profile,
                    weights        = weights,
                    bodyFats       = bodyFats,
                    foods          = foods,
                    useMetric      = useMetric,
                    imageBytes     = imageBytes
                )

                // 4. Save response & clear loading
                container.chatRepository.append(
                    ChatMessage(role = ChatMessage.Role.ASSISTANT, content = reply.trim())
                )
                _interaction.update { it.copy(sending = false) }

            } catch (e: AiError) {
                setError(CoachError.Literal(e.message ?: "", fallbackResId = R.string.coach_chat_failed))
            } catch (e: Throwable) {
                setError(CoachError.fromThrowable(e))
            }
        }
    }

    fun resetConversation() {
        viewModelScope.launch { container.chatRepository.clear() }
    }

    fun dismissError() {
        _interaction.update { it.copy(error = null) }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun setError(error: CoachError) {
        _interaction.update { it.copy(sending = false, error = error) }
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        sendJob?.cancel()
    }

    // ── factory ───────────────────────────────────────────────────────────────

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CoachViewModel(container) as T
    }
}