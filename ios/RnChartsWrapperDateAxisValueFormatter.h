#import <Foundation/Foundation.h>

#import <DGCharts/DGCharts-Swift.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS counterpart of DateAxisValueFormatter.java — see that file's doc
 * comment for the field semantics (since/timeUnit/pattern/locale).
 */
@interface RnChartsWrapperDateAxisValueFormatter : NSObject <ChartAxisValueFormatter>

- (instancetype)initWithPattern:(nullable NSString *)pattern
                          locale:(nullable NSString *)localeTag
                           since:(double)since
                        timeUnit:(nullable NSString *)timeUnit;

/** Epoch millis of axis value 0, and millis per axis unit (read by RnChartsWrapperDateXAxisRenderer). */
@property (nonatomic, readonly) double axisSinceMillis;
@property (nonatomic, readonly) double axisUnitToMillis;

@end

NS_ASSUME_NONNULL_END
