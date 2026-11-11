import DGCharts
import Foundation

/**
 * iOS counterpart of DateXAxisRenderer.java + DateAxisTicks.java (keep in
 * sync): ticks of a date x axis on calendar boundaries in the device time
 * zone, instead of DGCharts' decimal intervals (100 000 s over a week → skipped
 * day labels, bars drifting away from their day).
 *
 * Swift because DGCharts dispatches computeAxisValues through the Swift
 * vtable — an Objective-C override would never be called. The applier
 * (RnChartsWrapperChartConfigApplier.mm) declares this class by hand and
 * sets `dateAxis` / `sinceMillis` / `unitToMillis` from the xAxis config.
 */
@objc(RnChartsWrapperDateXAxisRenderer)
public final class RnChartsWrapperDateXAxisRenderer: XAxisRenderer {

    @objc public var dateAxis = false
    @objc public var sinceMillis: Double = 0
    @objc public var unitToMillis: Double = 1000

    public override func computeAxisValues(min: Double, max: Double) {
        guard dateAxis, unitToMillis > 0, !axis.isForceLabelsEnabled, !axis.centerAxisLabelsEnabled else {
            super.computeAxisValues(min: min, max: max)
            return
        }
        let ticks = DateAxisTicks.compute(
            minMs: sinceMillis + min * unitToMillis,
            maxMs: sinceMillis + max * unitToMillis,
            labelCount: axis.labelCount)
        // Entries are doubles here: no float rounding to compensate, unlike Android.
        axis.entries = ticks.map { ($0 - sinceMillis) / unitToMillis }
        axis.centeredEntries = []
        axis.decimals = 0
        computeSize()
    }
}

private enum DateAxisTicks {

    private enum Unit {
        case second, minute, hour, day, week, month, year

        var approxMs: Double {
            switch self {
            case .second: return 1_000
            case .minute: return 60_000
            case .hour: return 3_600_000
            case .day: return 86_400_000
            case .week: return 7 * 86_400_000
            case .month: return 30.44 * 86_400_000
            case .year: return 365.25 * 86_400_000
            }
        }
    }

    private static let steps: [(Unit, Int)] = [
        (.second, 1), (.second, 2), (.second, 5), (.second, 10), (.second, 15), (.second, 30),
        (.minute, 1), (.minute, 2), (.minute, 5), (.minute, 10), (.minute, 15), (.minute, 30),
        (.hour, 1), (.hour, 2), (.hour, 3), (.hour, 6), (.hour, 12),
        (.day, 1), (.day, 2), (.day, 3),
        (.week, 1), (.week, 2),
        (.month, 1), (.month, 2), (.month, 3), (.month, 6),
        (.year, 1), (.year, 2), (.year, 5), (.year, 10), (.year, 20), (.year, 50), (.year, 100),
    ]

    private static let stepTolerance = 0.8
    private static let maxTicks = 500

    static func compute(minMs: Double, maxMs: Double, labelCount: Int) -> [Double] {
        guard maxMs > minMs, labelCount > 0, minMs.isFinite, maxMs.isFinite else { return [] }
        let target = (maxMs - minMs) / Double(labelCount) * stepTolerance
        let (unit, amount) = steps.first { $0.0.approxMs * Double($0.1) >= target } ?? steps[steps.count - 1]

        let cal = Calendar.current
        guard var date = alignDown(Date(timeIntervalSince1970: minMs / 1000), unit, amount, cal) else { return [] }
        var ticks: [Double] = []
        var guardCount = 0
        while ticks.count < maxTicks, guardCount < maxTicks * 4 {
            guardCount += 1
            let t = date.timeIntervalSince1970 * 1000
            if t > maxMs { break }
            if t >= minMs { ticks.append(t) }
            guard let next = advance(date, unit, amount, cal), next > date else { break }
            date = next
        }
        return ticks
    }

    private static func floorTo(_ value: Int, _ amount: Int) -> Int {
        return value - ((value % amount) + amount) % amount
    }

    private static func floorMod(_ value: Int, _ amount: Int) -> Int {
        return ((value % amount) + amount) % amount
    }

    /** Days since 1970-01-01 of the local date of `day`. */
    private static func epochDay(_ day: Date, _ cal: Calendar) -> Int {
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        let c = cal.dateComponents([.year, .month, .day], from: day)
        let midnight = utc.date(from: c) ?? day
        return Int((midnight.timeIntervalSince1970 / 86_400).rounded(.down))
    }

    private static func alignDown(_ date: Date, _ unit: Unit, _ amount: Int, _ cal: Calendar) -> Date? {
        var c = cal.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        switch unit {
        case .second:
            c.second = floorTo(c.second ?? 0, amount)
            return cal.date(from: c)
        case .minute:
            c.second = 0
            c.minute = floorTo(c.minute ?? 0, amount)
            return cal.date(from: c)
        case .hour:
            c.second = 0
            c.minute = 0
            c.hour = floorTo(c.hour ?? 0, amount)
            return cal.date(from: c)
        default:
            break
        }
        let midnight = cal.startOfDay(for: date)
        switch unit {
        case .day:
            // Anchored on the epoch day number, so 2-day ticks don't change parity while panning.
            return cal.date(byAdding: .day, value: -floorMod(epochDay(midnight, cal), amount), to: midnight)
        case .week:
            let back = (cal.component(.weekday, from: midnight) - cal.firstWeekday + 7) % 7
            guard let weekStart = cal.date(byAdding: .day, value: -back, to: midnight) else { return nil }
            let week = Int((Double(epochDay(weekStart, cal)) / 7).rounded(.down))
            return cal.date(byAdding: .day, value: -7 * floorMod(week, amount), to: weekStart)
        case .month:
            let index = (c.year ?? 0) * 12 + (c.month ?? 1) - 1
            let first = cal.date(from: DateComponents(year: c.year, month: c.month, day: 1))
            return first.flatMap { cal.date(byAdding: .month, value: -floorMod(index, amount), to: $0) }
        default:
            let year = c.year ?? 1970
            return cal.date(from: DateComponents(year: year - floorMod(year, amount), month: 1, day: 1))
        }
    }

    private static func advance(_ date: Date, _ unit: Unit, _ amount: Int, _ cal: Calendar) -> Date? {
        switch unit {
        case .second:
            return date.addingTimeInterval(Double(amount))
        case .minute:
            return date.addingTimeInterval(Double(amount) * 60)
        case .hour:
            // Wall-clock hours (00, 06, 12, 18) even on DST change days.
            let next = cal.component(.hour, from: date) + amount
            if next >= 24 {
                return cal.date(byAdding: .day, value: 1, to: cal.startOfDay(for: date))
                    .map { cal.startOfDay(for: $0) }
            }
            return cal.date(bySettingHour: next, minute: 0, second: 0, of: date)
        case .day:
            return cal.date(byAdding: .day, value: amount, to: date).map { cal.startOfDay(for: $0) }
        case .week:
            return cal.date(byAdding: .day, value: 7 * amount, to: date).map { cal.startOfDay(for: $0) }
        case .month:
            return cal.date(byAdding: .month, value: amount, to: date).map { cal.startOfDay(for: $0) }
        case .year:
            return cal.date(byAdding: .year, value: amount, to: date).map { cal.startOfDay(for: $0) }
        }
    }
}
