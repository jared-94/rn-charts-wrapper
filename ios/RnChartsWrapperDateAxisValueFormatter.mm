#import "RnChartsWrapperDateAxisValueFormatter.h"

@implementation RnChartsWrapperDateAxisValueFormatter {
  NSDateFormatter *_formatter;
  double _sinceMillis;
  double _unitToMillis;
}

- (instancetype)initWithPattern:(nullable NSString *)pattern
                          locale:(nullable NSString *)localeTag
                           since:(double)since
                        timeUnit:(nullable NSString *)timeUnit
{
  if (self = [super init]) {
    _formatter = [[NSDateFormatter alloc] init];
    _formatter.dateFormat = pattern.length > 0 ? pattern : @"HH:mm";
    if (localeTag.length > 0) {
      NSString *normalized = [localeTag stringByReplacingOccurrencesOfString:@"-" withString:@"_"];
      _formatter.locale = [[NSLocale alloc] initWithLocaleIdentifier:normalized];
    }
    _unitToMillis = [self unitToMillis:timeUnit];
    _sinceMillis = since * _unitToMillis;
  }
  return self;
}

- (double)unitToMillis:(nullable NSString *)unit
{
  if ([unit isEqualToString:@"MILLISECONDS"]) return 1.0;
  if ([unit isEqualToString:@"MINUTES"]) return 60000.0;
  if ([unit isEqualToString:@"HOURS"]) return 3600000.0;
  if ([unit isEqualToString:@"DAYS"]) return 86400000.0;
  return 1000.0; // SECONDS, default
}

#pragma mark - ChartAxisValueFormatter

- (NSString *)stringForValue:(double)value axis:(nullable ChartAxisBase *)axis
{
  double millis = _sinceMillis + value * _unitToMillis;
  NSDate *date = [NSDate dateWithTimeIntervalSince1970:millis / 1000.0];
  return [_formatter stringFromDate:date];
}

@end
