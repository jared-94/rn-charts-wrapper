package com.rnchartswrapper;

import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.events.EventDispatcher;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

/**
 * Single Fabric view backing LineChart/BarChart/CombinedChart (see
 * index.tsx) — `chartKind` selects which MPAndroidChart subclass is actually
 * instantiated. Kept as one native view (rather than three) so
 * ChartConfigApplier's parsing logic isn't tripled.
 */
@SuppressWarnings("deprecation")
class RnChartsWrapperView extends FrameLayout implements OnChartGestureListener, OnChartValueSelectedListener {

    // "top"-prefixed internal event names codegen derives from the onXxx
    // props declared in RnChartsWrapperNativeComponent.ts — must match
    // exactly for JS to receive them (see normalizeInputEventName in
    // @react-native/codegen).
    //
    // Named onChartChange (not onChange): RN's BaseViewConfig injects a
    // generic bubbling "onChange" (topChange) into every native view's
    // merged view config, which collides with a custom direct event of the
    // same name and throws "Event cannot be both direct and bubbling" on
    // first mount. See RnChartsWrapperNativeComponent.ts.
    private static final String EVENT_ON_CHANGE = "topChartChange";
    private static final String EVENT_ON_SELECT = "topSelect";

    private String mChartKind = "line";
    private boolean mDisallowInterceptTouch = false;
    private ReadableMap mChartConfig;

    private BarLineChartBase<?> mChart;
    private final TextMarkerView mMarker = new TextMarkerView();

    RnChartsWrapperView(ThemedReactContext context) {
        super(context);
        recreateChart();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mDisallowInterceptTouch && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.onInterceptTouchEvent(ev);
    }

    // --- Prop setters (called from RnChartsWrapperManager) ---

    void setChartKind(String kind) {
        String normalized = kind == null ? "line" : kind;
        if (normalized.equals(mChartKind) && mChart != null) {
            return;
        }
        mChartKind = normalized;
        recreateChart();
        applyConfig();
    }

    void setDisallowInterceptTouch(boolean value) {
        mDisallowInterceptTouch = value;
    }

    void setChartConfig(ReadableMap config) {
        mChartConfig = config;
        applyConfig();
    }

    // --- Commands (called from RnChartsWrapperManager) ---

    void clearHighlights() {
        if (mChart != null) {
            mChart.highlightValues(null);
        }
    }

    void fitScreen() {
        if (mChart != null) {
            mChart.fitScreen();
        }
    }

    // --- Chart lifecycle ---

    private void recreateChart() {
        removeAllViews();
        if ("bar".equals(mChartKind)) {
            mChart = new BarChart(getContext());
        } else if ("combined".equals(mChartKind)) {
            CombinedChart combined = new CombinedChart(getContext());
            combined.setDrawOrder(new CombinedChart.DrawOrder[]{
                    CombinedChart.DrawOrder.BAR,
                    CombinedChart.DrawOrder.LINE,
            });
            mChart = combined;
        } else {
            mChart = new LineChart(getContext());
        }
        mChart.setOnChartGestureListener(this);
        mChart.setOnChartValueSelectedListener(this);
        addView(mChart, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void applyConfig() {
        ChartConfigApplier.apply(mChart, mChartKind, mChartConfig, mMarker);
    }

    // --- OnChartGestureListener: only scale/translate map to the old lib's onChange ---

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {
    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {
    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {
    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        emitChange("chartScaled");
    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {
        emitChange("chartTranslated");
    }

    // --- OnChartValueSelectedListener ---

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        WritableMap map = Arguments.createMap();
        map.putString("action", "select");
        map.putDouble("x", e.getX());
        map.putDouble("y", e.getY());
        map.putInt("dataSetIndex", h != null ? h.getDataSetIndex() : 0);
        emitEvent(EVENT_ON_SELECT, map);
    }

    @Override
    public void onNothingSelected() {
        WritableMap map = Arguments.createMap();
        map.putString("action", "none");
        map.putDouble("x", 0);
        map.putDouble("y", 0);
        map.putInt("dataSetIndex", -1);
        emitEvent(EVENT_ON_SELECT, map);
    }

    private void emitChange(String action) {
        WritableMap map = Arguments.createMap();
        map.putString("action", action);
        emitEvent(EVENT_ON_CHANGE, map);
    }

    private void emitEvent(String name, WritableMap payload) {
        ReactContext reactContext = (ReactContext) getContext();
        EventDispatcher dispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, getId());
        if (dispatcher != null) {
            int surfaceId = UIManagerHelper.getSurfaceId(reactContext);
            dispatcher.dispatchEvent(new RnChartsWrapperEvent(surfaceId, getId(), name, payload));
        }
    }
}
