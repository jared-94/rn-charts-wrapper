package com.rnchartswrapper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

/**
 * Canvas-drawn tooltip shown on tap — mirrors the original
 * react-native-charts-wrapper `marker` config: a rounded bubble with the
 * per-point `marker` string (see ChartValue.marker, e.g. "21°C\n14:32 - 26/08"),
 * split on "\n" into up to two centered lines. Implemented as a raw IMarker
 * (not a MarkerView/layout resource) so this library ships no Android
 * resources of its own.
 */
class TextMarkerView implements IMarker {
    private static final float PADDING_H = 16f;
    private static final float PADDING_V = 10f;
    private static final float CORNER_RADIUS = 8f;
    private static final float LINE_SPACING = 4f;
    private static final float VERTICAL_GAP = 24f;

    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBackgroundRect = new RectF();
    private final MPPointF mOffset = new MPPointF();

    private String[] mLines = new String[0];
    private Paint.Align mAlign = Paint.Align.CENTER;

    TextMarkerView() {
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(Color.DKGRAY);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(28f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setMarkerColor(int color) {
        mBackgroundPaint.setColor(color);
    }

    void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    void setTextAlign(String align) {
        if ("left".equals(align)) {
            mAlign = Paint.Align.LEFT;
        } else if ("right".equals(align)) {
            mAlign = Paint.Align.RIGHT;
        } else {
            mAlign = Paint.Align.CENTER;
        }
        mTextPaint.setTextAlign(mAlign);
    }

    @Override
    public MPPointF getOffset() {
        return mOffset;
    }

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
        float width = measureWidth();
        float height = measureHeight();
        return new MPPointF(-width / 2f, -height - VERTICAL_GAP);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        Object data = e.getData();
        String text = data instanceof String ? (String) data : String.valueOf(e.getY());
        mLines = text.split("\n");
    }

    private float measureWidth() {
        float max = 0f;
        for (String line : mLines) {
            max = Math.max(max, mTextPaint.measureText(line));
        }
        return max + PADDING_H * 2;
    }

    private float measureHeight() {
        float lineHeight = mTextPaint.descent() - mTextPaint.ascent();
        int lineCount = Math.max(mLines.length, 1);
        return lineHeight * lineCount + LINE_SPACING * Math.max(0, lineCount - 1) + PADDING_V * 2;
    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        if (mLines.length == 0) {
            return;
        }
        MPPointF offset = getOffsetForDrawingAtPoint(posX, posY);
        float width = measureWidth();
        float height = measureHeight();
        float left = posX + offset.x;
        float top = posY + offset.y;

        mBackgroundRect.set(left, top, left + width, top + height);
        canvas.drawRoundRect(mBackgroundRect, CORNER_RADIUS, CORNER_RADIUS, mBackgroundPaint);

        float lineHeight = mTextPaint.descent() - mTextPaint.ascent();
        float textX = mAlign == Paint.Align.LEFT ? left + PADDING_H
                : mAlign == Paint.Align.RIGHT ? left + width - PADDING_H
                : left + width / 2f;
        float baseline = top + PADDING_V - mTextPaint.ascent();
        for (String line : mLines) {
            canvas.drawText(line, textX, baseline, mTextPaint);
            baseline += lineHeight + LINE_SPACING;
        }
    }
}
