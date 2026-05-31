import Foundation

enum OptionalNutrient: String, CaseIterable, Identifiable, Codable {
    case fiber
    case sugar
    case addedSugar
    case saturatedFat
    case cholesterol
    case sodium
    case potassium

    var id: String { rawValue }

    var jsonKey: String {
        switch self {
        case .fiber: "fiber"
        case .sugar: "sugar"
        case .addedSugar: "added_sugar"
        case .saturatedFat: "saturated_fat"
        case .cholesterol: "cholesterol"
        case .sodium: "sodium"
        case .potassium: "potassium"
        }
    }

    init?(jsonKey: String) {
        switch jsonKey {
        case "fiber": self = .fiber
        case "sugar": self = .sugar
        case "added_sugar": self = .addedSugar
        case "saturated_fat": self = .saturatedFat
        case "cholesterol": self = .cholesterol
        case "sodium": self = .sodium
        case "potassium": self = .potassium
        default: return nil
        }
    }

    var displayName: String {
        switch self {
        case .fiber: "Fiber"
        case .sugar: "Sugar"
        case .addedSugar: "Added Sugar"
        case .saturatedFat: "Saturated Fat"
        case .cholesterol: "Cholesterol"
        case .sodium: "Sodium"
        case .potassium: "Potassium"
        }
    }

    var shortDisplayName: String {
        switch self {
        case .saturatedFat: "Sat Fat"
        default: displayName
        }
    }

    var iconName: String {
        switch self {
        case .fiber: "leaf.fill"
        case .sugar: "cube.fill"
        case .addedSugar: "plus.circle.fill"
        case .saturatedFat: "circle.lefthalf.filled"
        case .cholesterol: "heart.fill"
        case .sodium: "circle.grid.2x2.fill"
        case .potassium: "bolt.fill"
        }
    }

    var unit: String {
        switch self {
        case .cholesterol, .sodium, .potassium: "mg"
        default: "g"
        }
    }

    var defaultGoal: Int {
        switch self {
        case .fiber: 30
        case .sugar: 50
        case .addedSugar: 25
        case .saturatedFat: 20
        case .cholesterol: 300
        case .sodium: 2_300
        case .potassium: 3_500
        }
    }

    var range: ClosedRange<Int> {
        switch self {
        case .fiber: 5...100
        case .sugar: 0...200
        case .addedSugar: 0...100
        case .saturatedFat: 0...80
        case .cholesterol: 0...1_000
        case .sodium: 500...6_000
        case .potassium: 1_000...6_000
        }
    }

    var step: Int {
        switch self {
        case .cholesterol, .sodium, .potassium: 50
        default: 5
        }
    }

    var goalStyle: String {
        switch self {
        case .fiber, .potassium:
            "target"
        case .sugar, .addedSugar, .saturatedFat, .cholesterol, .sodium:
            "limit"
        }
    }
}

struct OptionalNutrientGoals: Codable, Equatable {
    static let storageKey = "optionalNutrientGoals"

    private var values: [String: Int]

    init(values: [String: Int] = [:]) {
        self.values = values
    }

    static let defaults = OptionalNutrientGoals(
        values: Dictionary(uniqueKeysWithValues: OptionalNutrient.allCases.map { ($0.rawValue, $0.defaultGoal) })
    )

    static var current: OptionalNutrientGoals {
        guard let data = UserDefaults.standard.data(forKey: storageKey) else {
            return .defaults
        }
        return decoded(from: data)
    }

    static func decoded(from data: Data) -> OptionalNutrientGoals {
        guard !data.isEmpty,
              let goals = try? JSONDecoder().decode(OptionalNutrientGoals.self, from: data)
        else {
            return .defaults
        }
        return goals.mergedWithDefaults()
    }

    var encodedData: Data {
        (try? JSONEncoder().encode(mergedWithDefaults())) ?? Data()
    }

    func goal(for nutrient: OptionalNutrient) -> Int {
        values[nutrient.rawValue] ?? nutrient.defaultGoal
    }

    mutating func setGoal(_ value: Int, for nutrient: OptionalNutrient) {
        values[nutrient.rawValue] = Self.sanitized(value, for: nutrient)
    }

    func settingGoal(_ value: Int, for nutrient: OptionalNutrient) -> OptionalNutrientGoals {
        var copy = self
        copy.setGoal(value, for: nutrient)
        return copy
    }

    func mergedWithDefaults() -> OptionalNutrientGoals {
        var merged = OptionalNutrientGoals.defaults.values
        for nutrient in OptionalNutrient.allCases {
            if let value = values[nutrient.rawValue] {
                merged[nutrient.rawValue] = Self.sanitized(value, for: nutrient)
            }
        }
        return OptionalNutrientGoals(values: merged)
    }

    static func save(_ goals: OptionalNutrientGoals) {
        UserDefaults.standard.set(goals.encodedData, forKey: storageKey)
    }

    static func sanitized(_ value: Int, for nutrient: OptionalNutrient) -> Int {
        let clamped = min(max(value, nutrient.range.lowerBound), nutrient.range.upperBound)
        guard nutrient.step > 1 else { return clamped }
        let offset = clamped - nutrient.range.lowerBound
        let snappedOffset = Int((Double(offset) / Double(nutrient.step)).rounded()) * nutrient.step
        return min(max(nutrient.range.lowerBound + snappedOffset, nutrient.range.lowerBound), nutrient.range.upperBound)
    }
}
