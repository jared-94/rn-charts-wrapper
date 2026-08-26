package com.rnchartswrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Dynamic;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewManagerDelegate;
import com.facebook.react.viewmanagers.RnChartsWrapperViewManagerDelegate;
import com.facebook.react.viewmanagers.RnChartsWrapperViewManagerInterface;

@ReactModule(name = RnChartsWrapperManager.REACT_CLASS)
public class RnChartsWrapperManager extends SimpleViewManager<RnChartsWrapperView>
        implements RnChartsWrapperViewManagerInterface<RnChartsWrapperView> {

    public static final String REACT_CLASS = "RnChartsWrapperView";

    private final RnChartsWrapperViewManagerDelegate<RnChartsWrapperView, RnChartsWrapperManager> mDelegate =
            new RnChartsWrapperViewManagerDelegate<>(this);

    @Nullable
    @Override
    protected ViewManagerDelegate<RnChartsWrapperView> getDelegate() {
        return mDelegate;
    }

    @NonNull
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected RnChartsWrapperView createViewInstance(@NonNull ThemedReactContext context) {
        return new RnChartsWrapperView(context);
    }

    // --- Props ---

    @Override
    public void setChartKind(RnChartsWrapperView view, @Nullable String value) {
        view.setChartKind(value);
    }

    @Override
    public void setDisallowInterceptTouch(RnChartsWrapperView view, boolean value) {
        view.setDisallowInterceptTouch(value);
    }

    @Override
    public void setChartConfig(RnChartsWrapperView view, @Nullable Dynamic value) {
        view.setChartConfig(value != null && !value.isNull() ? value.asMap() : null);
    }

    // --- Commands ---

    @Override
    public void clearHighlights(RnChartsWrapperView view) {
        view.clearHighlights();
    }

    @Override
    public void fitScreen(RnChartsWrapperView view) {
        view.fitScreen();
    }
}
