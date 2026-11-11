package com.rnchartswrapper;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Calendar-aligned tick positions for a date x axis, in the device time zone.
 *
 * MPAndroidChart / DGCharts pick "nice" decimal intervals (1, 2, 5 × 10^n) of
 * the axis unit. For an epoch-seconds axis that gives ticks every 100 000 s
 * (27.8 h) over a week or 200 000 s (2.31 days) over two weeks, labelled with
 * the day of an arbitrary instant: days get skipped ("mer. 04, jeu. 05,
 * ven. 06, dim. 08") and daily bars drift away from their labels. Here the
 * step is a calendar unit (seconds … years) and every tick sits on a local
 * boundary (midnight, the hour, the 1st of the month…), stable while panning.
 *
 * Mirrored by RnChartsWrapperDateAxisTicks.swift on iOS — keep both in sync.
 */
final class DateAxisTicks {

    private static final int SECOND = 0, MINUTE = 1, HOUR = 2, DAY = 3, WEEK = 4, MONTH = 5, YEAR = 6;

    private static final double[] UNIT_MS = {
            1_000d, 60_000d, 3_600_000d, 86_400_000d, 7 * 86_400_000d, 30.44 * 86_400_000d, 365.25 * 86_400_000d
    };

    /** {unit, amount}, smallest first. Sub-day amounts divide 60 / 24. */
    private static final int[][] STEPS = {
            {SECOND, 1}, {SECOND, 2}, {SECOND, 5}, {SECOND, 10}, {SECOND, 15}, {SECOND, 30},
            {MINUTE, 1}, {MINUTE, 2}, {MINUTE, 5}, {MINUTE, 10}, {MINUTE, 15}, {MINUTE, 30},
            {HOUR, 1}, {HOUR, 2}, {HOUR, 3}, {HOUR, 6}, {HOUR, 12},
            {DAY, 1}, {DAY, 2}, {DAY, 3},
            {WEEK, 1}, {WEEK, 2},
            {MONTH, 1}, {MONTH, 2}, {MONTH, 3}, {MONTH, 6},
            {YEAR, 1}, {YEAR, 2}, {YEAR, 5}, {YEAR, 10}, {YEAR, 20}, {YEAR, 50}, {YEAR, 100},
    };

    /**
     * A step may be a bit shorter than range / labelCount (MPAndroidChart's
     * own rounding does the same), so that a week gets one label per day.
     */
    private static final double STEP_TOLERANCE = 0.8;
    private static final int MAX_TICKS = 500;

    private DateAxisTicks() {
    }

    static long[] compute(long minMs, long maxMs, int labelCount) {
        if (maxMs <= minMs || labelCount <= 0) {
            return new long[0];
        }
        double target = (maxMs - minMs) / (double) labelCount * STEP_TOLERANCE;
        int[] step = STEPS[STEPS.length - 1];
        for (int[] s : STEPS) {
            if (UNIT_MS[s[0]] * s[1] >= target) {
                step = s;
                break;
            }
        }
        int unit = step[0];
        int amount = step[1];

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(minMs);
        alignDown(cal, unit, amount);

        long[] buffer = new long[MAX_TICKS];
        int n = 0;
        // MAX_TICKS * 4 bounds the loop even if alignDown landed far before minMs.
        for (int guard = 0; guard < MAX_TICKS * 4 && n < MAX_TICKS; guard++) {
            long t = cal.getTimeInMillis();
            if (t > maxMs) {
                break;
            }
            if (t >= minMs) {
                buffer[n++] = t;
            }
            advance(cal, unit, amount);
        }
        long[] ticks = new long[n];
        System.arraycopy(buffer, 0, ticks, 0, n);
        return ticks;
    }

    /** Moves cal back to the closest boundary of the step (local wall clock). */
    private static void alignDown(Calendar cal, int unit, int amount) {
        cal.set(Calendar.MILLISECOND, 0);
        switch (unit) {
            case SECOND:
                cal.set(Calendar.SECOND, floorTo(cal.get(Calendar.SECOND), amount));
                return;
            case MINUTE:
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MINUTE, floorTo(cal.get(Calendar.MINUTE), amount));
                return;
            case HOUR:
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.HOUR_OF_DAY, floorTo(cal.get(Calendar.HOUR_OF_DAY), amount));
                return;
            default:
                break;
        }
        toMidnight(cal);
        switch (unit) {
            case DAY:
                // Anchored on the epoch day number, so 2-day ticks don't change parity while panning.
                cal.add(Calendar.DAY_OF_MONTH, -(int) Math.floorMod(epochDay(cal), (long) amount));
                break;
            case WEEK: {
                int back = (cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek() + 7) % 7;
                cal.add(Calendar.DAY_OF_MONTH, -back);
                long week = Math.floorDiv(epochDay(cal), 7L);
                cal.add(Calendar.DAY_OF_MONTH, -7 * (int) Math.floorMod(week, (long) amount));
                break;
            }
            case MONTH: {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                int index = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH);
                cal.add(Calendar.MONTH, -Math.floorMod(index, amount));
                break;
            }
            case YEAR:
                cal.set(Calendar.DAY_OF_YEAR, 1);
                cal.add(Calendar.YEAR, -Math.floorMod(cal.get(Calendar.YEAR), amount));
                break;
            default:
                break;
        }
        toMidnight(cal);
    }

    private static void advance(Calendar cal, int unit, int amount) {
        switch (unit) {
            case SECOND:
                cal.add(Calendar.SECOND, amount);
                break;
            case MINUTE:
                cal.add(Calendar.MINUTE, amount);
                break;
            case HOUR: {
                // Wall-clock hours (00, 06, 12, 18) even on DST change days.
                int next = cal.get(Calendar.HOUR_OF_DAY) + amount;
                if (next >= 24) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    toMidnight(cal);
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, next);
                }
                break;
            }
            case DAY:
                cal.add(Calendar.DAY_OF_MONTH, amount);
                toMidnight(cal);
                break;
            case WEEK:
                cal.add(Calendar.DAY_OF_MONTH, 7 * amount);
                toMidnight(cal);
                break;
            case MONTH:
                cal.add(Calendar.MONTH, amount);
                toMidnight(cal);
                break;
            case YEAR:
            default:
                cal.add(Calendar.YEAR, amount);
                toMidnight(cal);
                break;
        }
    }

    private static void toMidnight(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private static int floorTo(int value, int amount) {
        return value - Math.floorMod(value, amount);
    }

    /** Days since 1970-01-01 of cal's local date. */
    private static long epochDay(Calendar cal) {
        Calendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        return Math.floorDiv(utc.getTimeInMillis(), 86_400_000L);
    }
}
