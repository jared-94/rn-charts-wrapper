package com.rnchartswrapper;

import android.graphics.drawable.GradientDrawable;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the single bundled `chartConfig` object (see
 * RnChartsWrapperNativeComponent.ts) and applies it to a live MPAndroidChart
 * instance. Covers exactly the subset of the original react-native-charts-
 * wrapper config surface this library reimplements — see index.tsx for the
 * full field list. This is the single source of truth for the config
 * schema; there is no codegen-side typing to keep in sync.
 */
final class ChartConfigApplier {

    private ChartConfigApplier() {
    }

    static void apply(BarLineChartBase<?> chart, String chartKind, ReadableMap config, TextMarkerView marker) {
        if (chart == null) {
            return;
        }

        chart.getDescription().setEnabled(false);

        if (config == null) {
            chart.clear();
            chart.invalidate();
            return;
        }

        applyLegend(chart.getLegend(), getMap(config, "legend"));
        applyXAxis(chart.getXAxis(), getMap(config, "xAxis"));
        ReadableMap yAxisConfig = getMap(config, "yAxis");
        applyYAxis(chart.getAxisLeft(), yAxisConfig != null ? getMap(yAxisConfig, "left") : null);
        applyYAxis(chart.getAxisRight(), yAxisConfig != null ? getMap(yAxisConfig, "right") : null);
        applyMarker(chart, marker, getMap(config, "marker"));

        chart.setAutoScaleMinMaxEnabled(config.hasKey("autoScaleMinMaxEnabled") && config.getBoolean("autoScaleMinMaxEnabled"));

        ReadableMap dataMap = getMap(config, "data");
        if ("bar".equals(chartKind)) {
            applyBarChartData((BarChart) chart, dataMap);
        } else if ("combined".equals(chartKind)) {
            applyCombinedChartData((CombinedChart) chart, dataMap);
        } else {
            applyLineChartData((LineChart) chart, dataMap);
        }

        applyAnimation(chart, getMap(config, "animation"));

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private static ReadableMap getMap(ReadableMap map, String key) {
        return map != null && map.hasKey(key) && !map.isNull(key) ? map.getMap(key) : null;
    }

    // --- Legend / description / marker ---

    private static void applyLegend(Legend legend, ReadableMap cfg) {
        if (cfg == null) {
            legend.setEnabled(false);
            return;
        }
        legend.setEnabled(!cfg.hasKey("enabled") || cfg.getBoolean("enabled"));
        if (cfg.hasKey("textColor")) {
            legend.setTextColor(cfg.getInt("textColor"));
        }
        if (cfg.hasKey("textSize")) {
            legend.setTextSize((float) cfg.getDouble("textSize"));
        }
        if (cfg.hasKey("formSize")) {
            legend.setFormSize((float) cfg.getDouble("formSize"));
        }
        if (cfg.hasKey("drawInside")) {
            legend.setDrawInside(cfg.getBoolean("drawInside"));
        }
        if (cfg.hasKey("wordWrapEnabled")) {
            legend.setWordWrapEnabled(cfg.getBoolean("wordWrapEnabled"));
        }
        if (cfg.hasKey("form")) {
            legend.setForm(parseLegendForm(cfg.getString("form")));
        }
    }

    private static Legend.LegendForm parseLegendForm(String value) {
        if (value == null) {
            return Legend.LegendForm.CIRCLE;
        }
        switch (value) {
            case "SQUARE":
                return Legend.LegendForm.SQUARE;
            case "LINE":
                return Legend.LegendForm.LINE;
            case "NONE":
                return Legend.LegendForm.NONE;
            default:
                return Legend.LegendForm.CIRCLE;
        }
    }

    private static void applyMarker(BarLineChartBase<?> chart, TextMarkerView marker, ReadableMap cfg) {
        boolean enabled = cfg != null && (!cfg.hasKey("enabled") || cfg.getBoolean("enabled"));
        if (!enabled) {
            chart.setMarker(null);
            return;
        }
        if (cfg.hasKey("markerColor")) {
            marker.setMarkerColor(cfg.getInt("markerColor"));
        }
        if (cfg.hasKey("textColor")) {
            marker.setTextColor(cfg.getInt("textColor"));
        }
        if (cfg.hasKey("textAlign")) {
            marker.setTextAlign(cfg.getString("textAlign"));
        }
        chart.setMarker(marker);
    }

    // --- Axes ---

    private static void applyXAxis(XAxis axis, ReadableMap cfg) {
        if (cfg == null) {
            axis.setEnabled(false);
            return;
        }
        axis.setEnabled(!cfg.hasKey("enabled") || cfg.getBoolean("enabled"));
        if (cfg.hasKey("position")) {
            axis.setPosition(parseXAxisPosition(cfg.getString("position")));
        }
        if (cfg.hasKey("drawGridLines")) {
            axis.setDrawGridLines(cfg.getBoolean("drawGridLines"));
        }
        if (cfg.hasKey("drawLabels")) {
            axis.setDrawLabels(cfg.getBoolean("drawLabels"));
        }
        if (cfg.hasKey("textColor")) {
            axis.setTextColor(cfg.getInt("textColor"));
        }
        if (cfg.hasKey("textSize")) {
            axis.setTextSize((float) cfg.getDouble("textSize"));
        }
        if (cfg.hasKey("axisMinimum")) {
            axis.setAxisMinimum((float) cfg.getDouble("axisMinimum"));
        } else {
            axis.resetAxisMinimum();
        }
        if (cfg.hasKey("axisMaximum")) {
            axis.setAxisMaximum((float) cfg.getDouble("axisMaximum"));
        } else {
            axis.resetAxisMaximum();
        }
        if (cfg.hasKey("valueFormatter") && "date".equals(cfg.getString("valueFormatter"))) {
            String pattern = cfg.hasKey("valueFormatterPattern") ? cfg.getString("valueFormatterPattern") : "HH:mm";
            String locale = cfg.hasKey("locale") ? cfg.getString("locale") : null;
            long since = cfg.hasKey("since") ? (long) cfg.getDouble("since") : 0L;
            String timeUnit = cfg.hasKey("timeUnit") ? cfg.getString("timeUnit") : "SECONDS";
            axis.setValueFormatter(new DateAxisValueFormatter(pattern, locale, since, timeUnit));
        } else {
            axis.setValueFormatter(null);
        }
        applyGridDashedLine(axis, getMap(cfg, "gridDashedLine"));
    }

    private static XAxis.XAxisPosition parseXAxisPosition(String value) {
        if (value == null) {
            return XAxis.XAxisPosition.BOTTOM;
        }
        switch (value) {
            case "TOP":
                return XAxis.XAxisPosition.TOP;
            case "BOTH_SIDED":
                return XAxis.XAxisPosition.BOTH_SIDED;
            case "TOP_INSIDE":
                return XAxis.XAxisPosition.TOP_INSIDE;
            case "BOTTOM_INSIDE":
                return XAxis.XAxisPosition.BOTTOM_INSIDE;
            default:
                return XAxis.XAxisPosition.BOTTOM;
        }
    }

    private static void applyYAxis(YAxis axis, ReadableMap cfg) {
        if (cfg == null) {
            axis.setEnabled(false);
            return;
        }
        axis.setEnabled(!cfg.hasKey("enabled") || cfg.getBoolean("enabled"));
        if (cfg.hasKey("drawGridLines")) {
            axis.setDrawGridLines(cfg.getBoolean("drawGridLines"));
        }
        if (cfg.hasKey("drawLabels")) {
            axis.setDrawLabels(cfg.getBoolean("drawLabels"));
        }
        if (cfg.hasKey("textColor")) {
            axis.setTextColor(cfg.getInt("textColor"));
        }
        if (cfg.hasKey("axisMinimum")) {
            axis.setAxisMinimum((float) cfg.getDouble("axisMinimum"));
        } else {
            axis.resetAxisMinimum();
        }
        if (cfg.hasKey("axisMaximum")) {
            axis.setAxisMaximum((float) cfg.getDouble("axisMaximum"));
        } else {
            axis.resetAxisMaximum();
        }
        if (cfg.hasKey("labelCount")) {
            boolean force = cfg.hasKey("labelCountForce") && cfg.getBoolean("labelCountForce");
            axis.setLabelCount((int) cfg.getDouble("labelCount"), force);
        }
        applyGridDashedLine(axis, getMap(cfg, "gridDashedLine"));
    }

    private static void applyGridDashedLine(AxisBase axis, ReadableMap cfg) {
        if (cfg == null) {
            return;
        }
        float lineLength = cfg.hasKey("lineLength") ? (float) cfg.getDouble("lineLength") : 10f;
        float spaceLength = cfg.hasKey("spaceLength") ? (float) cfg.getDouble("spaceLength") : 10f;
        float phase = cfg.hasKey("phase") ? (float) cfg.getDouble("phase") : 0f;
        axis.enableGridDashedLine(lineLength, spaceLength, phase);
    }

    // --- Animation ---

    private static void applyAnimation(BarLineChartBase<?> chart, ReadableMap cfg) {
        int durationX = cfg != null && cfg.hasKey("durationX") ? (int) cfg.getDouble("durationX") : 0;
        int durationY = cfg != null && cfg.hasKey("durationY") ? (int) cfg.getDouble("durationY") : 0;
        if (durationX > 0 || durationY > 0) {
            chart.animateXY(durationX, durationY);
        }
    }

    // --- Data ---

    private static void applyLineChartData(LineChart chart, ReadableMap dataMap) {
        ReadableArray dataSets = dataMap != null ? dataMap.getArray("dataSets") : null;
        if (dataSets == null) {
            chart.setData(new LineData());
            return;
        }
        List<ILineDataSet> sets = new ArrayList<>(dataSets.size());
        for (int i = 0; i < dataSets.size(); i++) {
            sets.add(buildLineDataSet(dataSets.getMap(i)));
        }
        chart.setData(new LineData(sets));
    }

    private static void applyBarChartData(BarChart chart, ReadableMap dataMap) {
        ReadableArray dataSets = dataMap != null ? dataMap.getArray("dataSets") : null;
        if (dataSets == null) {
            chart.setData(new BarData());
            return;
        }
        List<IBarDataSet> sets = new ArrayList<>(dataSets.size());
        for (int i = 0; i < dataSets.size(); i++) {
            sets.add(buildBarDataSet(dataSets.getMap(i)));
        }
        BarData barData = new BarData(sets);
        applyBarDataConfig(barData, getMap(dataMap, "config"), sets.size());
        chart.setData(barData);
    }

    private static void applyCombinedChartData(CombinedChart chart, ReadableMap dataMap) {
        CombinedData combinedData = new CombinedData();
        ReadableMap barDataMap = getMap(dataMap, "barData");
        if (barDataMap != null) {
            ReadableArray dataSets = barDataMap.getArray("dataSets");
            List<IBarDataSet> sets = new ArrayList<>(dataSets.size());
            for (int i = 0; i < dataSets.size(); i++) {
                sets.add(buildBarDataSet(dataSets.getMap(i)));
            }
            BarData barData = new BarData(sets);
            applyBarDataConfig(barData, getMap(barDataMap, "config"), sets.size());
            combinedData.setData(barData);
        }
        ReadableMap lineDataMap = getMap(dataMap, "lineData");
        if (lineDataMap != null) {
            ReadableArray dataSets = lineDataMap.getArray("dataSets");
            List<ILineDataSet> sets = new ArrayList<>(dataSets.size());
            for (int i = 0; i < dataSets.size(); i++) {
                sets.add(buildLineDataSet(dataSets.getMap(i)));
            }
            combinedData.setData(new LineData(sets));
        }
        chart.setData(combinedData);
    }

    private static void applyBarDataConfig(BarData barData, ReadableMap dataConfig, int dataSetCount) {
        if (dataConfig == null) {
            return;
        }
        if (dataConfig.hasKey("barWidth")) {
            barData.setBarWidth((float) dataConfig.getDouble("barWidth"));
        }
        ReadableMap group = getMap(dataConfig, "group");
        if (group != null && dataSetCount > 1) {
            float fromX = (float) group.getDouble("fromX");
            float groupSpace = (float) group.getDouble("groupSpace");
            float barSpace = (float) group.getDouble("barSpace");
            barData.groupBars(fromX, groupSpace, barSpace);
        }
    }

    private static LineDataSet buildLineDataSet(ReadableMap dataSetMap) {
        List<Entry> entries = buildEntries(dataSetMap.getArray("values"));
        String label = dataSetMap.hasKey("label") ? dataSetMap.getString("label") : "";
        LineDataSet dataSet = new LineDataSet(entries, label);
        applyLineDataSetConfig(dataSet, getMap(dataSetMap, "config"));
        return dataSet;
    }

    private static BarDataSet buildBarDataSet(ReadableMap dataSetMap) {
        ReadableArray values = dataSetMap.getArray("values");
        List<BarEntry> entries = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            ReadableMap point = values.getMap(i);
            BarEntry entry = new BarEntry((float) point.getDouble("x"), (float) point.getDouble("y"));
            if (point.hasKey("marker")) {
                entry.setData(point.getString("marker"));
            }
            entries.add(entry);
        }
        String label = dataSetMap.hasKey("label") ? dataSetMap.getString("label") : "";
        BarDataSet dataSet = new BarDataSet(entries, label);
        applyBarDataSetConfig(dataSet, getMap(dataSetMap, "config"));
        return dataSet;
    }

    private static List<Entry> buildEntries(ReadableArray values) {
        List<Entry> entries = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            ReadableMap point = values.getMap(i);
            Entry entry = new Entry((float) point.getDouble("x"), (float) point.getDouble("y"));
            if (point.hasKey("marker")) {
                entry.setData(point.getString("marker"));
            }
            entries.add(entry);
        }
        return entries;
    }

    private static void applyLineDataSetConfig(LineDataSet dataSet, ReadableMap cfg) {
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSet.setDrawCircleHole(false);
        dataSet.setHighlightEnabled(true);
        if (cfg == null) {
            return;
        }

        if (cfg.hasKey("mode")) {
            dataSet.setMode(parseLineMode(cfg.getString("mode")));
        }
        if (cfg.hasKey("color")) {
            dataSet.setColor(cfg.getInt("color"));
        }
        if (cfg.hasKey("highlightColor")) {
            dataSet.setHighLightColor(cfg.getInt("highlightColor"));
        }
        if (cfg.hasKey("highlightLineWidth")) {
            dataSet.setHighlightLineWidth((float) cfg.getDouble("highlightLineWidth"));
        }
        if (cfg.hasKey("highlightEnabled")) {
            dataSet.setHighlightEnabled(cfg.getBoolean("highlightEnabled"));
        }
        if (cfg.hasKey("lineWidth")) {
            dataSet.setLineWidth((float) cfg.getDouble("lineWidth"));
        }
        if (cfg.hasKey("drawValues")) {
            dataSet.setDrawValues(cfg.getBoolean("drawValues"));
        }
        if (cfg.hasKey("drawFilled")) {
            dataSet.setDrawFilled(cfg.getBoolean("drawFilled"));
        }
        if (cfg.hasKey("drawCircles")) {
            dataSet.setDrawCircles(cfg.getBoolean("drawCircles"));
        }
        if (cfg.hasKey("circleColor")) {
            dataSet.setCircleColor(cfg.getInt("circleColor"));
        }
        if (cfg.hasKey("circleRadius")) {
            dataSet.setCircleRadius((float) cfg.getDouble("circleRadius"));
        }
        if (cfg.hasKey("drawCircleHole")) {
            dataSet.setDrawCircleHole(cfg.getBoolean("drawCircleHole"));
        }
        if (cfg.hasKey("fillAlpha")) {
            dataSet.setFillAlpha((int) cfg.getDouble("fillAlpha"));
        }
        ReadableMap fillGradient = getMap(cfg, "fillGradient");
        if (fillGradient != null) {
            dataSet.setFillDrawable(buildGradientDrawable(fillGradient));
        }
        if (cfg.hasKey("axisDependency")) {
            dataSet.setAxisDependency(parseAxisDependency(cfg.getString("axisDependency")));
        }
    }

    private static void applyBarDataSetConfig(BarDataSet dataSet, ReadableMap cfg) {
        dataSet.setDrawValues(false);
        if (cfg == null) {
            return;
        }
        if (cfg.hasKey("color")) {
            dataSet.setColor(cfg.getInt("color"));
        }
        if (cfg.hasKey("highlightColor")) {
            dataSet.setHighLightColor(cfg.getInt("highlightColor"));
        }
        if (cfg.hasKey("highlightEnabled")) {
            dataSet.setHighlightEnabled(cfg.getBoolean("highlightEnabled"));
        }
        if (cfg.hasKey("drawValues")) {
            dataSet.setDrawValues(cfg.getBoolean("drawValues"));
        }
        if (cfg.hasKey("axisDependency")) {
            dataSet.setAxisDependency(parseAxisDependency(cfg.getString("axisDependency")));
        }
    }

    private static YAxis.AxisDependency parseAxisDependency(String value) {
        return "RIGHT".equals(value) ? YAxis.AxisDependency.RIGHT : YAxis.AxisDependency.LEFT;
    }

    private static GradientDrawable buildGradientDrawable(ReadableMap cfg) {
        ReadableArray colorsArr = cfg.getArray("colors");
        int[] colors = new int[colorsArr.size()];
        for (int i = 0; i < colorsArr.size(); i++) {
            colors[i] = (int) colorsArr.getDouble(i);
        }
        GradientDrawable drawable = new GradientDrawable(parseOrientation(cfg), colors);
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return drawable;
    }

    private static GradientDrawable.Orientation parseOrientation(ReadableMap cfg) {
        if (cfg.hasKey("orientation")) {
            try {
                return GradientDrawable.Orientation.valueOf(cfg.getString("orientation"));
            } catch (IllegalArgumentException ignored) {
                // fall through to default below
            }
        }
        return GradientDrawable.Orientation.TOP_BOTTOM;
    }

    private static LineDataSet.Mode parseLineMode(String value) {
        if (value == null) {
            return LineDataSet.Mode.LINEAR;
        }
        switch (value) {
            case "STEPPED":
                return LineDataSet.Mode.STEPPED;
            case "CUBIC_BEZIER":
                return LineDataSet.Mode.CUBIC_BEZIER;
            case "HORIZONTAL_BEZIER":
                return LineDataSet.Mode.HORIZONTAL_BEZIER;
            default:
                return LineDataSet.Mode.LINEAR;
        }
    }
}
