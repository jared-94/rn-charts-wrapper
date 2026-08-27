# rn-charts-wrapper

A New Architecture (Fabric + codegen) rewrite of [react-native-charts-wrapper](https://github.com/wuxudong/react-native-charts-wrapper), covering **Line**, **Bar**, and **Combined** (bar+line) charts on the same native engines as the original library: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) on Android and [DGCharts](https://github.com/danielgindi/Charts) on iOS.

This is a deliberate **subset** of the original API — no Pie/Radar/Scatter/Bubble/Candlestick charts, and only the config fields listed below. It grew out of migrating a real app off the old (bridge-only) `react-native-charts-wrapper`, so it covers exactly what a typical history/time-series chart screen needs, not full parity with the original library.

## Requirements

- React Native with the New Architecture enabled (Fabric).
- iOS 16.0+, Android minSdk 26+.
- `react-native-nitro-modules` is **not** required — this library uses plain Fabric codegen (`codegenNativeComponent` / `codegenNativeCommands`), not Nitro.

## Installation

```sh
npm install github:jared-94/rn-charts-wrapper#master
# or pin to a tag once one exists:
# npm install github:jared-94/rn-charts-wrapper#v1.0.0
```

**iOS**: `cd ios && pod install`. Pulls in `DGCharts` (~> 5.1) automatically via the podspec.

**Android**: no extra setup — `com.github.PhilJay:MPAndroidChart` is fetched via JitPack, but your app's root `build.gradle` needs a JitPack repository declared in its `allprojects { repositories { ... } }` block:

```gradle
allprojects {
  repositories {
    // ...
    maven { url 'https://www.jitpack.io' }
  }
}
```

(Most RN apps that pull in any JitPack-hosted native dependency already have this.)

## Usage

```tsx
import { LineChart, BarChart, CombinedChart } from 'rn-charts-wrapper';
import { processColor } from 'react-native';

<LineChart
  style={{ width: 350, height: 250 }}
  data={{
    dataSets: [{
      label: 'Temperature',
      values: [{ x: 0, y: 20 }, { x: 1, y: 22, marker: '22°C\n14:00' }],
      config: {
        mode: 'LINEAR',
        color: processColor('blue'),
        lineWidth: 2,
        drawFilled: true,
        fillGradient: {
          colors: [processColor('#0000ffcc'), processColor('#0000ff10')],
          positions: [0, 1],
          angle: 270,
          orientation: 'TOP_BOTTOM',
        },
      },
    }],
  }}
  legend={{ enabled: true, form: 'CIRCLE' }}
  marker={{ enabled: true, textAlign: 'center' }}
  xAxis={{ position: 'BOTTOM', drawGridLines: true }}
  yAxis={{ left: { enabled: true }, right: { enabled: false } }}
  autoScaleMinMaxEnabled
/>
```

`BarChart` takes the same `data` shape. `CombinedChart` takes `{ barData?: ChartData, lineData?: ChartData }` instead — see [`groupHistoryChart` pattern](#combinedchart) below.

### CombinedChart

```tsx
<CombinedChart
  style={{ width: 350, height: 250 }}
  data={{
    barData: {
      dataSets: [{ label: 'Rain (mm)', values: rainValues, config: { color: processColor('cyan'), axisDependency: 'RIGHT' } }],
      config: { barWidth: 0.3 },
    },
    lineData: {
      dataSets: [{ label: 'Temperature', values: tempValues, config: { color: processColor('orange'), axisDependency: 'LEFT' } }],
    },
  }}
  yAxis={{ left: { enabled: true }, right: { enabled: true } }}
/>
```

### Imperative handle

```tsx
const chartRef = useRef<ChartHandle>(null);

chartRef.current?.highlights([]); // clears the active tooltip/highlight
chartRef.current?.fitScreen();    // resets zoom/pan to fit all data
```

### Events

- `onChange` — fired on pinch-zoom/pan, `{ action: 'chartScaled' | 'chartTranslated' }`.
- `onSelect` — fired when a data point is tapped or the selection is cleared, `{ action: 'select' | 'none', x, y, dataSetIndex }`.

## Props reference

| Prop | Type | Notes |
|---|---|---|
| `data` | `ChartData` (Line/Bar) or `CombinedChartData` (Combined) | Required. |
| `legend` | `LegendConfig` | `enabled`, `textColor`, `form`, `formSize`, `drawInside`, `textSize`, `wordWrapEnabled`. |
| `marker` | `MarkerConfig` | Tooltip shown on tap — `enabled`, `markerColor`, `textColor`, `textAlign`. |
| `xAxis` | `XAxisConfig` | Includes a built-in date formatter (`valueFormatter: 'date'` + `since`/`timeUnit`/`valueFormatterPattern`/`locale`). |
| `yAxis` | `{ left?: YAxisSideConfig, right?: YAxisSideConfig }` | Independent left/right axis config, incl. `axisMinimum`/`axisMaximum`/`labelCount`. |
| `chartDescription` | `{ text?: string }` | Passed through; typically left as `{ text: '' }` to hide it. |
| `autoScaleMinMaxEnabled` | `boolean` | |
| `animation` | `{ durationX?: number, durationY?: number }` | Milliseconds. |
| `disallowInterceptTouch` | `boolean` | Android only for now — see [Known limitations](#known-limitations). |

Full field-level types are in [`src/index.tsx`](src/index.tsx).

## Known limitations

- **No Pie/Radar/Scatter/Bubble/Candlestick charts** — only Line, Bar, and Combined.
- **`disallowInterceptTouch` is Android-only.** On iOS it's accepted but currently a no-op — pinch/pan on the chart can be stolen by an ancestor `ScrollView`. See the TODO in `ios/RnChartsWrapperView.mm`.
- **iOS `CombinedChart` draw order**: the bar-before-line draw order is set via a DGCharts API whose exact Objective-C bridging wasn't 100% certain when written (see the `NOTE` in `ios/RnChartsWrapperView.mm`). It matches DGCharts' own default either way, so this only matters if you need a *non-default* order.
- Config is passed as one opaque object per chart (`chartConfig` internally) rather than individually-typed Fabric props, so prop-level diffing/animation isn't granular — fine for the common "rebuild the whole config on data change" pattern this library was built around.

## License

MIT
