/// <reference path="./react-native-codegen-shims.d.ts" />
import type * as React from 'react';
import type { HostComponent, ViewProps } from 'react-native';
import type {
    Double,
    Int32,
    UnsafeMixed,
    WithDefault,
    DirectEventHandler,
} from 'react-native/Libraries/Types/CodegenTypes';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';

// Fired on chart scale/translate (pinch-zoom, pan) — mirrors the old
// react-native-charts-wrapper `onChange` event. `action` is one of
// 'chartScaled' | 'chartTranslated'.
export type ChartChangeEvent = Readonly<{
    action: string;
}>;

// Fired when a data point is tapped (value-selected) or the selection is
// cleared (nothing-selected) — mirrors the old lib's `onSelect`. A consumer
// can use this purely as a "something happened" signal (e.g. to restart a
// tooltip auto-clear timer) without reading the payload, but the fields
// mirror MPAndroidChart/DGCharts' own Highlight object for parity.
export type ChartSelectEvent = Readonly<{
    action: string;
    x: Double;
    y: Double;
    dataSetIndex: Int32;
}>;

export interface NativeProps extends ViewProps {
    // 'line' | 'bar' | 'combined' — selects the underlying MPAndroidChart/
    // DGCharts renderer. Set by the LineChart/BarChart/CombinedChart JS
    // wrappers in index.tsx, not meant to be passed directly by consumers.
    chartKind?: WithDefault<string, 'line'>;

    disallowInterceptTouch?: WithDefault<boolean, false>;

    // Single bundled config object: { data, legend, marker, xAxis, yAxis,
    // chartDescription, autoScaleMinMaxEnabled, animation }, mirroring the
    // shape of the equivalent top-level props from the original
    // react-native-charts-wrapper API. Declared as UnsafeMixed rather than
    // exhaustively typed in codegen — the config tree is deeply
    // nested/optional, a consumer typically rebuilds it wholesale on every
    // render anyway, and the native ChartConfigApplier on each platform is
    // the single source of truth for the schema actually read.
    // (UnsafeObject/Object — the module-side generic-object escape hatch —
    // is NOT accepted for component props by this codegen version; verified
    // by running combine-js-to-schema against this exact spec. UnsafeMixed
    // compiles to `Dynamic` on Java — call `.asMap()` — and to
    // `folly::dynamic` on the C++ Props struct. Converting that to
    // NSDictionary on iOS needs a local folly::dynamic->id walker rather
    // than RN's own `facebook::react::convertFollyDynamicToId()` — that
    // header isn't exposed on third-party pods' header search paths — see
    // RnChartsWrapperManager.java / RnChartsWrapperView.mm.)
    chartConfig?: UnsafeMixed;

    // Named onChartChange (not onChange) on the native side: RN's
    // iOS/Android BaseViewConfig injects a generic bubbling "onChange"
    // (topChange) into every native view's merged view config. A custom
    // direct event also named onChange collides with that on the same
    // "topChange" key, throwing "Event cannot be both direct and bubbling"
    // on first mount. The public onChange prop (index.tsx) is unaffected —
    // only this internal wire name changed.
    onChartChange?: DirectEventHandler<ChartChangeEvent>;
    onSelect?: DirectEventHandler<ChartSelectEvent>;
}

type NativeType = HostComponent<NativeProps>;

interface NativeCommands {
    clearHighlights: (viewRef: React.ElementRef<NativeType>) => void;
    fitScreen: (viewRef: React.ElementRef<NativeType>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
    supportedCommands: ['clearHighlights', 'fitScreen'],
});

export default codegenNativeComponent<NativeProps>(
    'RnChartsWrapperView',
) as NativeType;
