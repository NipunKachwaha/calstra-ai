import Foundation
import WidgetKit

/// Writes a WidgetSnapshot into the shared App Group container and asks
/// WidgetKit to refresh all timelines. Widgets can't read the main app's
/// private UserDefaults, so any data the widget needs has to go through here.
enum WidgetSnapshotWriter {
    /// Recomputes today's totals from the current FoodStore + ProfileStore and
    /// publishes them to the widget.
    static func publish(foods: [FoodEntry], profile: UserProfile) {
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        // Must be same-day — `timestamp >= startOfDay` alone would fold future-logged
        // entries (meals planned on tomorrow via the week strip) into today's totals.
        let today = foods.filter { calendar.isDate($0.timestamp, inSameDayAs: Date()) }

        let cal = today.reduce(0) { $0 + $1.calories }
        let p = today.reduce(0) { $0 + $1.protein }
        let c = today.reduce(0) { $0 + $1.carbs }
        let f = today.reduce(0) { $0 + $1.fat }

        let snapshot = WidgetSnapshot(
            date: Date(),
            dayStart: startOfDay,
            calories: cal,
            calorieGoal: profile.effectiveCalories,
            protein: p,
            proteinGoal: profile.effectiveProtein,
            carbs: c,
            carbsGoal: profile.effectiveCarbs,
            fat: f,
            fatGoal: profile.effectiveFat
        )

        let previous = WidgetSnapshot.read()
        guard previous != snapshot else { return }
        WidgetSnapshot.write(snapshot)
        WidgetCenter.shared.reloadAllTimelines()
    }
}
