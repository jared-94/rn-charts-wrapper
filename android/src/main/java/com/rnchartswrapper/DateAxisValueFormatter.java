package com.rnchartswrapper;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mirrors the original react-native-charts-wrapper xAxis date formatter:
 * `valueFormatter: 'date'` + `since`/`timeUnit`/`valueFormatterPattern`/`locale`.
 * Axis values are epoch offsets expressed in `timeUnit` — converted to
 * millis and formatted with the given pattern/locale (e.g. "fr-FR").
 */
class DateAxisValueFormatter extends ValueFormatter {
    private final SimpleDateFormat mFormat;
    private final long mSinceMillis;
    private final double mUnitToMillis;

    DateAxisValueFormatter(String pattern, String localeTag, long since, String timeUnit) {
        Locale locale = parseLocale(localeTag);
        mFormat = new SimpleDateFormat(pattern != null && !pattern.isEmpty() ? pattern : "HH:mm", locale);
        double unitToMillis = unitToMillis(timeUnit);
        mUnitToMillis = unitToMillis;
        mSinceMillis = Math.round(since * unitToMillis);
    }

    private static Locale parseLocale(String tag) {
        if (tag == null || tag.isEmpty()) {
            return Locale.getDefault();
        }
        String[] parts = tag.split("[-_]");
        if (parts.length >= 2) {
            return new Locale(parts[0], parts[1]);
        }
        return new Locale(parts[0]);
    }

    private static double unitToMillis(String unit) {
        if (unit == null) {
            return 1000.0;
        }
        switch (unit) {
            case "MILLISECONDS":
                return 1.0;
            case "MINUTES":
                return 60_000.0;
            case "HOURS":
                return 3_600_000.0;
            case "DAYS":
                return 86_400_000.0;
            case "SECONDS":
            default:
                return 1000.0;
        }
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        long millis = mSinceMillis + Math.round(value * mUnitToMillis);
        return mFormat.format(new Date(millis));
    }
}
