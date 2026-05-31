package com.apoorvdarshan.ai.data

import com.apoorvdarshan.ai.models.WeightEntry
import com.apoorvdarshan.ai.services.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/** Emitted when the user crosses their goal weight (under -> at/below for lose, over -> at/above for gain). */
data class WeightGoalReachedEvent(val reachedEntry: WeightEntry)

/**
 * CRUD + reactive reads for weight entries. Port of iOS WeightStore.
 * Also handles two cross-cutting behaviors:
 *
 * 1. After each add/delete, syncs [UserProfile.weightKg] to the latest entry
 *    so BMR/TDEE math and Settings stay in sync with Progress.
 * 2. Returns a [WeightGoalReachedEvent] from [addEntry] when the new weight
 *    crosses the user's goal, for the caller to surface as a celebration UI.
 */
class WeightRepository(
    private val prefs: PreferencesStore,
    private val profileRepository: ProfileRepository,
    private val health: HealthConnectManager? = null
) {
    val entries: Flow<List<WeightEntry>> = prefs.weightEntries.map { it.sortedBy { e -> e.date } }

    val latest: Flow<WeightEntry?> = prefs.weightEntries.map { list ->
        list.maxByOrNull { it.date }
    }

    /** Safe to call repeatedly — no-ops once any entries exist. */
    suspend fun seedInitialWeightIfEmpty(weightKg: Double) {
        if (prefs.weightEntries.first().isNotEmpty()) return
        addEntry(WeightEntry(weightKg = weightKg))
    }

    suspend fun addEntry(entry: WeightEntry): WeightGoalReachedEvent? {
        val current = prefs.weightEntries.first()
        val previousLatest = current.maxByOrNull { it.date }
        prefs.setWeightEntries(current + entry)

        syncProfileWeightToLatest()
        if (shouldSyncHealth()) {
            health?.writeWeight(entry)
        }

        val profile = profileRepository.current()
        val goal = profile?.goalWeightKg
        if (profile != null && goal != null && previousLatest != null) {
            val crossed = when (profile.goal) {
                com.apoorvdarshan.ai.models.WeightGoal.LOSE ->
                    previousLatest.weightKg > goal && entry.weightKg <= goal
                com.apoorvdarshan.ai.models.WeightGoal.GAIN ->
                    previousLatest.weightKg < goal && entry.weightKg >= goal
                com.apoorvdarshan.ai.models.WeightGoal.MAINTAIN -> false
            }
            if (crossed) return WeightGoalReachedEvent(entry)
        }
        return null
    }

    suspend fun deleteEntry(id: UUID) {
        val current = prefs.weightEntries.first()
        prefs.setWeightEntries(current.filter { it.id != id })
        syncProfileWeightToLatest()
        if (shouldSyncHealth()) {
            health?.deleteWeight(id)
        }
    }

    suspend fun replaceAll(entries: List<WeightEntry>) {
        prefs.setWeightEntries(entries)
        syncProfileWeightToLatest()
    }

    suspend fun clear() {
        prefs.setWeightEntries(emptyList())
    }

    suspend fun entriesInRange(from: Instant, to: Instant): List<WeightEntry> =
        prefs.weightEntries.first()
            .filter { it.date in from..to }
            .sortedBy { it.date }

    private suspend fun syncProfileWeightToLatest() {
        val profile = profileRepository.current() ?: return
        val newest = prefs.weightEntries.first().maxByOrNull { it.date } ?: return
        if (abs(profile.weightKg - newest.weightKg) > 0.01) {
            profileRepository.save(profile.copy(weightKg = newest.weightKg))
        }
    }

    private suspend fun shouldSyncHealth(): Boolean {
        val manager = health ?: return false
        return prefs.healthConnectEnabled.first() && manager.hasAllPermissions()
    }
}