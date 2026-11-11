package com.rnchartswrapper;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * X axis renderer placing the ticks of a date axis (`valueFormatter: 'date'`)
 * on calendar boundaries (see DateAxisTicks). Any other axis, or a forced /
 * centered label layout, keeps MPAndroidChart's default computation.
 * computeAxisValues receives the visible range, so it also applies when the
 * user pinch-zooms or pans.
 */
final class DateXAxisRenderer extends XAxisRenderer {

    DateXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, Transformer trans) {
        super(viewPortHandler, xAxis, trans);
    }

    @Override
    protected void computeAxisValues(float min, float max) {
        ValueFormatter formatter = mXAxis.getValueFormatter();
        if (!(formatter instanceof DateAxisValueFormatter)
                || mXAxis.isForceLabelsEnabled()
                || mXAxis.isCenterAxisLabelsEnabled()) {
            super.computeAxisValues(min, max);
            return;
        }
        DateAxisValueFormatter date = (DateAxisValueFormatter) formatter;
        long[] ticks = DateAxisTicks.compute(date.toMillis(min), date.toMillis(max), mXAxis.getLabelCount());
        int n = ticks.length;
        if (mXAxis.mEntries.length < n) {
            mXAxis.mEntries = new float[n];
        }
        for (int i = 0; i < n; i++) {
            mXAxis.mEntries[i] = date.toAxisValue(ticks[i]);
        }
        mXAxis.mEntryCount = n;
        mXAxis.mDecimals = 0;
        computeSize();
    }
}
