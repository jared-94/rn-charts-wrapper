#import <Foundation/Foundation.h>
#import "RnChartsWrapperMarkerView.h"

@import DGCharts;

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS counterpart of ChartConfigApplier.java — reads the `chartConfig`
 * NSDictionary (converted from the C++ Props' folly::dynamic, see
 * RnChartsWrapperView.mm) and applies it to a live DGCharts view. Same
 * field names/semantics as the Android version; this is the single source
 * of truth for the config schema on this platform.
 *
 * UNVERIFIED AGAINST A REAL BUILD (no Xcode on the machine this was written
 * on) — highest-risk spots to check first if the build fails here:
 *  - LineChartDataSet.Mode / YAxis.AxisDependency enum bridging names
 *    (assumed LineChartDataSetModeLinear/Stepped/CubicBezier/HorizontalBezier
 *    and YAxisAxisDependencyLeft/Right below — confirm via Cmd-click in Xcode).
 *  - Fill's linear-gradient initializer signature (assumed
 *    -initWithLinearGradient:angle: taking a CGGradientRef).
 *  - IMarker protocol selectors (see RnChartsWrapperMarkerView.h).
 */
@interface RnChartsWrapperChartConfigApplier : NSObject

+ (void)apply:(BarLineChartViewBase *)chart
     chartKind:(NSString *)chartKind
        config:(nullable NSDictionary *)config
        marker:(RnChartsWrapperMarkerView *)marker;

@end

NS_ASSUME_NONNULL_END
