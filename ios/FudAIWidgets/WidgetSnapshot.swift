import Foundation

// NOTE: This file is a **duplicate** of calorietracker/Services/WidgetSnapshot.swift.
// The widget extension is a separate target and can't see the main app's sources,
// so we maintain two copies. Keep the struct layout identical or decoding breaks.
struct WidgetSnapshot: Codable, Equatable {
    let date: Date
    let dayStart: Date
    let calories: Int
    let calorieGoal: Int
    let protein: Int
    let proteinGoal: Int
    let carbs: Int
    let carbsGoal: Int
    let fat: Int
    let fatGoal: Int

    static var appGroupID: String {
        Bundle.main.object(forInfoDictionaryKey: "AppGroupIdentifier") as? String
            ?? "group.com.apoorvdarshan.calorietracker"
    }
    private static let key = "widget_snapshot_v1"

    static var sharedDefaults: UserDefaults? {
        UserDefaults(suiteName: appGroupID)
    }

    static func read() -> WidgetSnapshot? {
        guard let data = sharedDefaults?.data(forKey: key),
              let snapshot = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return nil }
        // If the snapshot's dayStart is not today, treat it as stale. The main app
        // will refresh on its next scene-active, but until then we should show an
        // empty today rather than yesterday's totals. Returning nil lets the timeline
        // provider substitute `.empty` (zeroed today).
        let today = Calendar.current.startOfDay(for: Date())
        guard Calendar.current.isDate(snapshot.dayStart, inSameDayAs: today) else {
            return nil
        }
        return snapshot
    }

    static var placeholder: WidgetSnapshot {
        let now = Date()
        return WidgetSnapshot(
            date: now,
            dayStart: Calendar.current.startOfDay(for: now),
            calories: 1247, calorieGoal: 2000,
            protein: 84, proteinGoal: 150,
            carbs: 132, carbsGoal: 220,
            fat: 42, fatGoal: 70
        )
    }

    static var empty: WidgetSnapshot {
        let now = Date()
        return WidgetSnapshot(
            date: now,
            dayStart: Calendar.current.startOfDay(for: now),
            calories: 0, calorieGoal: 2000,
            protein: 0, proteinGoal: 150,
            carbs: 0, carbsGoal: 220,
            fat: 0, fatGoal: 70
        )
    }

    var caloriesRemaining: Int { max(0, calorieGoal - calories) }
    var proteinRemaining: Int { max(0, proteinGoal - protein) }
    var carbsRemaining: Int { max(0, carbsGoal - carbs) }
    var fatRemaining: Int { max(0, fatGoal - fat) }
    var calorieProgress: Double {
        guard calorieGoal > 0 else { return 0 }
        return min(1.0, Double(calories) / Double(calorieGoal))
    }
    var proteinProgress: Double {
        guard proteinGoal > 0 else { return 0 }
        return min(1.0, Double(protein) / Double(proteinGoal))
    }
    var carbsProgress: Double {
        guard carbsGoal > 0 else { return 0 }
        return min(1.0, Double(carbs) / Double(carbsGoal))
    }
    var fatProgress: Double {
        guard fatGoal > 0 else { return 0 }
        return min(1.0, Double(fat) / Double(fatGoal))
    }
}
