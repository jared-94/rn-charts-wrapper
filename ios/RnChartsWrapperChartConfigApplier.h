#import <Foundation/Foundation.h>
#import "RnChartsWrapperMarkerView.h"

#import <DGCharts/DGCharts-Swift.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS counterpart of ChartConfigApplier.java — reads the `chartConfig`
 * NSDictionary (converted from the C++ Props' folly::dynamic, see
 * RnChartsWrapperView.mm) and applies it to a live DGCharts view. Same
 * field names/semantics as the Android version; this is the single source
 * of truth for the config schema on this platform.
 */
@interface RnChartsWrapperChartConfigApplier : NSObject

+ (void)apply:(BarLineChartViewBase *)chart
     chartKind:(NSString *)chartKind
        config:(nullable NSDictionary *)config
        marker:(RnChartsWrapperMarkerView *)marker;

@end

NS_ASSUME_NONNULL_END
