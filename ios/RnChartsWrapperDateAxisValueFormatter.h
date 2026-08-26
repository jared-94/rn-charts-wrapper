#import <Foundation/Foundation.h>

@import DGCharts;

NS_ASSUME_NONNULL_BEGIN

/**
 * iOS counterpart of DateAxisValueFormatter.java — see that file's doc
 * comment for the field semantics (since/timeUnit/pattern/locale).
 */
@interface RnChartsWrapperDateAxisValueFormatter : NSObject <AxisValueFormatter>

- (instancetype)initWithPattern:(nullable NSString *)pattern
                          locale:(nullable NSString *)localeTag
                           since:(double)since
                        timeUnit:(nullable NSString *)timeUnit;

@end

NS_ASSUME_NONNULL_END
