package com.apoorvdarshan.ai

import android.app.Application
import com.apoorvdarshan.ai.data.BodyFatRepository
import com.apoorvdarshan.ai.data.ChatRepository
import com.apoorvdarshan.ai.data.FoodRepository
import com.apoorvdarshan.ai.data.KeyStore
import com.apoorvdarshan.ai.data.PreferencesStore
import com.apoorvdarshan.ai.data.ProfileRepository
import com.apoorvdarshan.ai.data.WeightRepository
import com.apoorvdarshan.ai.services.FoodImageStore
import com.apoorvdarshan.ai.services.NotificationService
import com.apoorvdarshan.ai.services.TestDataSeeder
import com.apoorvdarshan.ai.services.WidgetSnapshotWriter
import com.apoorvdarshan.ai.services.ai.ChatService
import com.apoorvdarshan.ai.services.ai.FoodAnalysisService
import com.apoorvdarshan.ai.services.health.HealthConnectManager
import com.apoorvdarshan.ai.services.speech.SpeechService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

/**
 * Application-scoped singleton wiring. Manual DI (no Hilt) — repositories and
 * services are instantiated once and handed to ViewModels via [container].
 */
class FudAIApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannels()
        container.widgetSnapshotWriter.observe().launchIn(appScope)
        // Re-arm the daily weight-log alarm on every cold start. AlarmManager
        // drops scheduled alarms on device reboot and (sometimes) on app
        // updates — without this, a user who enabled Notifications once would
        // silently stop receiving the reminder after the next reboot.
        appScope.launch {
            if (container.prefs.notificationsEnabled.first() &&
                container.notifications.canPostNotifications()
            ) {
                container.notifications.scheduleWeightReminder()
                // Body-fat reminder only fires for users who've actually opted
                // into body-fat tracking — the toggle in Settings is the same
                // master Notifications switch, but the body-fat ping is gated
                // on the profile having a current body-fat value so we don't
                // ping users who haven't entered one.
                val profile = container.profileRepository.current()
                if (profile?.bodyFatPercentage != null) {
                    container.notifications.scheduleBodyFatReminder()
                }
            }
        }
    }
}

class AppContainer(app: FudAIApp) {
    val appContext = app.applicationContext
    val prefs = PreferencesStore(app)
    val keyStore = KeyStore(app)
    val imageStore = FoodImageStore(app)
    val notifications = NotificationService(app)
    val health = HealthConnectManager(app)

    val profileRepository = ProfileRepository(prefs)
    val foodRepository = FoodRepository(prefs)
    val weightRepository = WeightRepository(prefs, profileRepository)
    val bodyFatRepository = BodyFatRepository(prefs, profileRepository)
    val chatRepository = ChatRepository(prefs)

    val foodAnalysis = FoodAnalysisService(prefs, keyStore)
    val chatService = ChatService(prefs, keyStore)
    val speechService = SpeechService(prefs, keyStore)

    val widgetSnapshotWriter = WidgetSnapshotWriter(app, prefs, foodRepository, profileRepository)
    val testDataSeeder = TestDataSeeder(this)

    /**
     * App-scoped flag set by [HomeViewModel] while a food analysis request is
     * in flight. The bottom nav reads this so the bar can hide during the
     * AnalyzingOverlay (matches iOS, where the analyzing sheet covers the
     * tab bar).
     */
    val analyzingFood: MutableStateFlow<Boolean> = MutableStateFlow(false)
}
