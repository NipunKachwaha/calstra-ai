import SwiftUI
import WidgetKit

struct ProteinWidgetView: View {
    let entry: ProteinEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .systemSmall:          SmallProteinView(snapshot: entry.snapshot)
        case .systemMedium:         MediumProteinView(snapshot: entry.snapshot)
        case .accessoryCircular:    CircularProteinView(snapshot: entry.snapshot)
        case .accessoryRectangular: RectangularProteinView(snapshot: entry.snapshot)
        case .accessoryInline:      InlineProteinView(snapshot: entry.snapshot)
        default:                    SmallProteinView(snapshot: entry.snapshot)
        }
    }
}

// MARK: - Home Screen

private struct SmallProteinView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Image(systemName: "bolt.fill")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(WidgetPalette.calorieGradient)
                Text("Protein")
                    .font(.system(.caption, design: .rounded, weight: .semibold))
                    .foregroundStyle(.secondary)
                Spacer()
            }

            ZStack {
                Circle()
                    .stroke(WidgetPalette.calorie.opacity(0.15), lineWidth: 10)
                Circle()
                    .trim(from: 0, to: snapshot.proteinProgress)
                    .stroke(WidgetPalette.calorieGradient, style: StrokeStyle(lineWidth: 10, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                VStack(spacing: 0) {
                    Text("\(snapshot.protein)g")
                        .font(.system(.title3, design: .rounded, weight: .bold))
                        .minimumScaleFactor(0.7)
                        .lineLimit(1)
                    Text("/ \(snapshot.proteinGoal)g")
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(.secondary)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            Text("\(snapshot.proteinRemaining)g left")
                .font(.system(.caption2, design: .rounded, weight: .medium))
                .foregroundStyle(.secondary)
        }
    }
}

private struct MediumProteinView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                Circle()
                    .stroke(WidgetPalette.calorie.opacity(0.15), lineWidth: 9)
                Circle()
                    .trim(from: 0, to: snapshot.proteinProgress)
                    .stroke(WidgetPalette.calorieGradient, style: StrokeStyle(lineWidth: 9, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                VStack(spacing: 0) {
                    Text("\(snapshot.protein)")
                        .font(.system(.title2, design: .rounded, weight: .bold))
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    Text("/ \(snapshot.proteinGoal)")
                        .font(.system(.caption2, design: .rounded))
                        .foregroundStyle(.secondary)
                    Text("protein g")
                        .font(.system(.caption2, design: .rounded, weight: .medium))
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 92, height: 92)

            VStack(alignment: .leading, spacing: 8) {
                ProteinMacroBar(label: "Calories", value: snapshot.calories, goal: snapshot.calorieGoal, progress: snapshot.calorieProgress, unit: "")
                ProteinMacroBar(label: "Carbs",    value: snapshot.carbs,    goal: snapshot.carbsGoal,   progress: snapshot.carbsProgress, unit: "g")
                ProteinMacroBar(label: "Fat",      value: snapshot.fat,      goal: snapshot.fatGoal,     progress: snapshot.fatProgress,   unit: "g")
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private struct ProteinMacroBar: View {
    let label: String
    let value: Int
    let goal: Int
    let progress: Double
    let unit: String

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack {
                Text(label)
                    .font(.system(.caption2, design: .rounded, weight: .semibold))
                    .foregroundStyle(.secondary)
                Spacer()
                Text("\(value)\(unit) / \(goal)\(unit)")
                    .font(.system(.caption2, design: .rounded, weight: .medium))
                    .foregroundStyle(.primary)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(WidgetPalette.calorie.opacity(0.15))
                    Capsule()
                        .fill(WidgetPalette.calorieGradient)
                        .frame(width: max(4, geo.size.width * progress))
                }
            }
            .frame(height: 6)
        }
    }
}

// MARK: - Lock Screen

private struct CircularProteinView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        ZStack {
            AccessoryWidgetBackground()
            Circle()
                .trim(from: 0, to: snapshot.proteinProgress)
                .stroke(style: StrokeStyle(lineWidth: 3.5, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .padding(3)
            VStack(spacing: 0) {
                Text("\(snapshot.protein)")
                    .font(.system(.caption, design: .rounded, weight: .bold))
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                Text("prot")
                    .font(.system(size: 8, weight: .medium, design: .rounded))
                    .foregroundStyle(.secondary)
            }
        }
        .widgetAccentable()
    }
}

private struct RectangularProteinView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            ZStack {
                Circle()
                    .stroke(.secondary.opacity(0.3), lineWidth: 3)
                Circle()
                    .trim(from: 0, to: snapshot.proteinProgress)
                    .stroke(style: StrokeStyle(lineWidth: 3, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                Image(systemName: "bolt.fill")
                    .font(.system(size: 13, weight: .bold))
            }
            .frame(width: 42, height: 42)
            .widgetAccentable()

            VStack(alignment: .leading, spacing: 2) {
                Text("\(snapshot.protein)g")
                    .font(.system(.title3, design: .rounded, weight: .bold))
                    .widgetAccentable()
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text("of \(snapshot.proteinGoal)g protein")
                    .font(.system(.caption2, design: .rounded))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Text("\(snapshot.calories) kcal · C\(snapshot.carbs) · F\(snapshot.fat)")
                    .font(.system(.caption2, design: .rounded, weight: .medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
            }
            Spacer(minLength: 0)
        }
    }
}

private struct InlineProteinView: View {
    let snapshot: WidgetSnapshot

    var body: some View {
        Text("\(snapshot.protein)g / \(snapshot.proteinGoal)g protein · \(snapshot.proteinRemaining)g left")
    }
}
