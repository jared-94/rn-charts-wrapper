import React from 'react';
import type { NativeSyntheticEvent, ProcessedColorValue, StyleProp, ViewStyle } from 'react-native';

import NativeRnChartsWrapperView, { Commands } from './RnChartsWrapperNativeComponent';
import type { ChartChangeEvent, ChartSelectEvent } from './RnChartsWrapperNativeComponent';

export type { ChartChangeEvent, ChartSelectEvent };

// --- Config shapes ---------------------------------------------------------
// These mirror the subset of the original react-native-charts-wrapper config
// API this library reimplements (Line/Bar/Combined charts) — not the full
// upstream API surface (no Pie/Radar/Scatter/Bubble/Candlestick support).

export type InterpolationMode = 'LINEAR' | 'STEPPED' | 'CUBIC_BEZIER' | 'HORIZONTAL_BEZIER';
export type AxisDependency = 'LEFT' | 'RIGHT';

/**
 * Output of React Native's own `processColor()` — which is how every color
 * value reaches this library. `processColor()` itself is typed to return
 * `null` for an invalid/unresolvable color, so every color field here
 * accepts that too rather than requiring a bare `number`. A `null`/`undefined`
 * color field is simply treated as "not set" natively (falls back to the
 * underlying chart engine's own default for that field).
 */
export type ProcessedColor = ProcessedColorValue | null | undefined;

export interface ChartValue {
    x: number;
    y: number;
    /** Tooltip text shown by the marker when this point is tapped, e.g. "21°C\n14:32 - 26/08". */
    marker?: string;
}

export interface FillGradientConfig {
    /** Two ARGB ints (processColor(...) output), start and end of the gradient. */
    colors: [ProcessedColor, ProcessedColor];
    positions: [number, number];
    angle?: number;
    orientation?: 'TOP_BOTTOM' | 'BOTTOM_TOP' | 'LEFT_RIGHT' | 'RIGHT_LEFT';
}

export interface DataSetConfig {
    mode?: InterpolationMode;
    color?: ProcessedColor;
    highlightColor?: ProcessedColor;
    highlightLineWidth?: number;
    highlightEnabled?: boolean;
    lineWidth?: number;
    drawFilled?: boolean;
    drawValues?: boolean;
    drawCircles?: boolean;
    circleColor?: ProcessedColor;
    circleRadius?: number;
    drawCircleHole?: boolean;
    fillGradient?: FillGradientConfig;
    fillAlpha?: number;
    /** CombinedChart only. */
    axisDependency?: AxisDependency;
}

export interface ChartDataSet {
    label?: string;
    values: ChartValue[];
    config?: DataSetConfig;
}

export interface BarGroupConfig {
    fromX: number;
    groupSpace: number;
    barSpace: number;
}

export interface ChartData {
    dataSets: ChartDataSet[];
    config?: {
        barWidth?: number;
        group?: BarGroupConfig;
    };
}

export interface CombinedChartData {
    barData?: ChartData;
    lineData?: ChartData;
}

export interface LegendConfig {
    enabled?: boolean;
    textColor?: ProcessedColor;
    form?: 'CIRCLE' | 'SQUARE' | 'LINE' | 'NONE';
    formSize?: number;
    drawInside?: boolean;
    textSize?: number;
    wordWrapEnabled?: boolean;
}

export interface MarkerConfig {
    enabled?: boolean;
    markerColor?: ProcessedColor;
    textColor?: ProcessedColor;
    textAlign?: 'left' | 'center' | 'right';
}

export interface GridDashedLineConfig {
    lineLength: number;
    spaceLength: number;
    phase: number;
}

export interface XAxisConfig {
    enabled?: boolean;
    position?: 'TOP' | 'BOTTOM' | 'BOTH_SIDED' | 'TOP_INSIDE' | 'BOTTOM_INSIDE';
    drawGridLines?: boolean;
    drawLabels?: boolean;
    textColor?: ProcessedColor;
    textSize?: number;
    valueFormatter?: 'date' | 'largeValue' | 'percent' | 'normal';
    since?: number;
    valueFormatterPattern?: string;
    locale?: string;
    timeUnit?: 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';
    axisMinimum?: number;
    axisMaximum?: number;
    gridDashedLine?: GridDashedLineConfig;
}

export interface YAxisSideConfig {
    enabled?: boolean;
    drawGridLines?: boolean;
    drawLabels?: boolean;
    textColor?: ProcessedColor;
    axisMinimum?: number;
    axisMaximum?: number;
    labelCount?: number;
    labelCountForce?: boolean;
    gridDashedLine?: GridDashedLineConfig;
}

export interface YAxisConfig {
    left?: YAxisSideConfig;
    right?: YAxisSideConfig;
}

export interface AnimationConfig {
    durationX?: number;
    durationY?: number;
}

export interface ChartDescriptionConfig {
    text?: string;
}

export interface HighlightSelector {
    x?: number;
    dataSetIndex?: number;
}

export interface BaseChartProps {
    style?: StyleProp<ViewStyle>;
    disallowInterceptTouch?: boolean;
    legend?: LegendConfig;
    marker?: MarkerConfig;
    xAxis?: XAxisConfig;
    yAxis?: YAxisConfig;
    chartDescription?: ChartDescriptionConfig;
    autoScaleMinMaxEnabled?: boolean;
    animation?: AnimationConfig;
    onChange?: (event: NativeSyntheticEvent<ChartChangeEvent>) => void;
    onSelect?: (event: NativeSyntheticEvent<ChartSelectEvent>) => void;
}

export interface LineBarChartProps extends BaseChartProps {
    data: ChartData;
}

export interface CombinedChartProps extends BaseChartProps {
    data: CombinedChartData;
}

export interface ChartHandle {
    /** Clears any active tooltip/highlight. Historically took a Highlight[] selector; only ever called with []. */
    highlights: (selectors?: HighlightSelector[]) => void;
    /** Resets zoom/pan to fit all data on screen. */
    fitScreen: () => void;
}

type NativeRef = React.ElementRef<typeof NativeRnChartsWrapperView>;

function useChartHandle(ref: React.ForwardedRef<ChartHandle>) {
    const nativeRef = React.useRef<NativeRef>(null);
    React.useImperativeHandle(
        ref,
        () => ({
            highlights() {
                if (nativeRef.current) {
                    Commands.clearHighlights(nativeRef.current);
                }
            },
            fitScreen() {
                if (nativeRef.current) {
                    Commands.fitScreen(nativeRef.current);
                }
            },
        }),
        [],
    );
    return nativeRef;
}

function buildChartConfig(props: BaseChartProps, data: ChartData | CombinedChartData) {
    return {
        data,
        legend: props.legend,
        marker: props.marker,
        xAxis: props.xAxis,
        yAxis: props.yAxis,
        chartDescription: props.chartDescription,
        autoScaleMinMaxEnabled: props.autoScaleMinMaxEnabled,
        animation: props.animation,
    };
}

function makeChartComponent(chartKind: 'line' | 'bar' | 'combined') {
    return React.forwardRef<ChartHandle, LineBarChartProps | CombinedChartProps>((props, ref) => {
        const nativeRef = useChartHandle(ref);
        const chartConfig = React.useMemo(
            () => buildChartConfig(props, props.data),
            // eslint-disable-next-line react-hooks/exhaustive-deps
            [props.data, props.legend, props.marker, props.xAxis, props.yAxis, props.chartDescription, props.autoScaleMinMaxEnabled, props.animation],
        );

        return (
            <NativeRnChartsWrapperView
                ref={nativeRef}
                style={props.style}
                chartKind={chartKind}
                disallowInterceptTouch={!!props.disallowInterceptTouch}
                chartConfig={chartConfig}
                onChange={props.onChange}
                onSelect={props.onSelect}
            />
        );
    });
}

export const LineChart = makeChartComponent('line');
LineChart.displayName = 'LineChart';

export const BarChart = makeChartComponent('bar');
BarChart.displayName = 'BarChart';

export const CombinedChart = makeChartComponent('combined');
CombinedChart.displayName = 'CombinedChart';
