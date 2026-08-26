#import "RnChartsWrapperChartConfigApplier.h"
#import "RnChartsWrapperDateAxisValueFormatter.h"

#import <React/RCTConvert.h>

static BOOL RCWHasKey(NSDictionary *dict, NSString *key)
{
  return dict != nil && dict[key] != nil && ![dict[key] isKindOfClass:[NSNull class]];
}

static NSDictionary *_Nullable RCWMap(NSDictionary *_Nullable dict, NSString *key)
{
  if (!RCWHasKey(dict, key)) {
    return nil;
  }
  id value = dict[key];
  return [value isKindOfClass:[NSDictionary class]] ? (NSDictionary *)value : nil;
}

static BOOL RCWBool(NSDictionary *dict, NSString *key, BOOL def)
{
  return RCWHasKey(dict, key) ? [dict[key] boolValue] : def;
}

static double RCWDouble(NSDictionary *dict, NSString *key, double def)
{
  return RCWHasKey(dict, key) ? [dict[key] doubleValue] : def;
}

static NSString *_Nullable RCWString(NSDictionary *dict, NSString *key)
{
  return RCWHasKey(dict, key) ? [dict[key] description] : nil;
}

static UIColor *_Nullable RCWColor(NSDictionary *dict, NSString *key)
{
  return RCWHasKey(dict, key) ? [RCTConvert UIColor:dict[key]] : nil;
}

@implementation RnChartsWrapperChartConfigApplier

+ (void)apply:(BarLineChartViewBase *)chart
     chartKind:(NSString *)chartKind
        config:(nullable NSDictionary *)config
        marker:(RnChartsWrapperMarkerView *)marker
{
  if (chart == nil) {
    return;
  }

  chart.chartDescription.enabled = NO;

  if (config == nil) {
    chart.data = nil;
    [chart notifyDataSetChanged];
    return;
  }

  [self applyLegend:chart.legend config:RCWMap(config, @"legend")];
  [self applyXAxis:chart.xAxis config:RCWMap(config, @"xAxis")];
  NSDictionary *yAxisConfig = RCWMap(config, @"yAxis");
  [self applyYAxis:chart.leftAxis config:RCWMap(yAxisConfig, @"left")];
  [self applyYAxis:chart.rightAxis config:RCWMap(yAxisConfig, @"right")];
  [self applyMarker:chart marker:marker config:RCWMap(config, @"marker")];

  chart.autoScaleMinMaxEnabled = RCWBool(config, @"autoScaleMinMaxEnabled", NO);

  NSDictionary *dataMap = RCWMap(config, @"data");
  if ([chartKind isEqualToString:@"bar"]) {
    [self applyBarData:(BarChartView *)chart data:dataMap];
  } else if ([chartKind isEqualToString:@"combined"]) {
    [self applyCombinedData:(CombinedChartView *)chart data:dataMap];
  } else {
    [self applyLineData:(LineChartView *)chart data:dataMap];
  }

  [self applyAnimation:chart config:RCWMap(config, @"animation")];

  [chart notifyDataSetChanged];
  [chart setNeedsDisplay];
}

#pragma mark - Legend / marker

+ (void)applyLegend:(Legend *)legend config:(nullable NSDictionary *)cfg
{
  if (cfg == nil) {
    legend.enabled = NO;
    return;
  }
  legend.enabled = RCWBool(cfg, @"enabled", YES);
  UIColor *textColor = RCWColor(cfg, @"textColor");
  if (textColor) {
    legend.textColor = textColor;
  }
  if (RCWHasKey(cfg, @"textSize")) {
    legend.font = [UIFont systemFontOfSize:RCWDouble(cfg, @"textSize", 12)];
  }
  if (RCWHasKey(cfg, @"formSize")) {
    legend.formSize = RCWDouble(cfg, @"formSize", 12);
  }
  if (RCWHasKey(cfg, @"drawInside")) {
    legend.drawInside = RCWBool(cfg, @"drawInside", NO);
  }
  if (RCWHasKey(cfg, @"wordWrapEnabled")) {
    legend.wordWrapEnabled = RCWBool(cfg, @"wordWrapEnabled", NO);
  }
  NSString *form = RCWString(cfg, @"form");
  if ([form isEqualToString:@"SQUARE"]) {
    legend.form = LegendFormSquare;
  } else if ([form isEqualToString:@"LINE"]) {
    legend.form = LegendFormLine;
  } else if ([form isEqualToString:@"NONE"]) {
    legend.form = LegendFormNone;
  } else if (form != nil) {
    legend.form = LegendFormCircle;
  }
}

+ (void)applyMarker:(BarLineChartViewBase *)chart
              marker:(RnChartsWrapperMarkerView *)marker
              config:(nullable NSDictionary *)cfg
{
  BOOL enabled = cfg != nil && RCWBool(cfg, @"enabled", YES);
  if (!enabled) {
    chart.marker = nil;
    return;
  }
  UIColor *markerColor = RCWColor(cfg, @"markerColor");
  if (markerColor) {
    marker.markerColor = markerColor;
  }
  UIColor *textColor = RCWColor(cfg, @"textColor");
  if (textColor) {
    marker.markerTextColor = textColor;
  }
  NSString *align = RCWString(cfg, @"textAlign");
  if ([align isEqualToString:@"left"]) {
    marker.textAlignment = NSTextAlignmentLeft;
  } else if ([align isEqualToString:@"right"]) {
    marker.textAlignment = NSTextAlignmentRight;
  } else {
    marker.textAlignment = NSTextAlignmentCenter;
  }
  chart.marker = marker;
}

#pragma mark - Axes

+ (void)applyXAxis:(XAxis *)axis config:(nullable NSDictionary *)cfg
{
  if (cfg == nil) {
    axis.enabled = NO;
    return;
  }
  axis.enabled = RCWBool(cfg, @"enabled", YES);
  NSString *position = RCWString(cfg, @"position");
  if ([position isEqualToString:@"TOP"]) {
    axis.labelPosition = XAxisLabelPositionTop;
  } else if ([position isEqualToString:@"BOTH_SIDED"]) {
    axis.labelPosition = XAxisLabelPositionBothSided;
  } else if ([position isEqualToString:@"TOP_INSIDE"]) {
    axis.labelPosition = XAxisLabelPositionTopInside;
  } else if ([position isEqualToString:@"BOTTOM_INSIDE"]) {
    axis.labelPosition = XAxisLabelPositionBottomInside;
  } else if (position != nil) {
    axis.labelPosition = XAxisLabelPositionBottom;
  }
  if (RCWHasKey(cfg, @"drawGridLines")) {
    axis.drawGridLinesEnabled = RCWBool(cfg, @"drawGridLines", YES);
  }
  if (RCWHasKey(cfg, @"drawLabels")) {
    axis.drawLabelsEnabled = RCWBool(cfg, @"drawLabels", YES);
  }
  UIColor *textColor = RCWColor(cfg, @"textColor");
  if (textColor) {
    axis.labelTextColor = textColor;
  }
  if (RCWHasKey(cfg, @"textSize")) {
    axis.labelFont = [UIFont systemFontOfSize:RCWDouble(cfg, @"textSize", 10)];
  }
  if (RCWHasKey(cfg, @"axisMinimum")) {
    axis.axisMinimum = RCWDouble(cfg, @"axisMinimum", 0);
  } else {
    [axis resetCustomAxisMin];
  }
  if (RCWHasKey(cfg, @"axisMaximum")) {
    axis.axisMaximum = RCWDouble(cfg, @"axisMaximum", 0);
  } else {
    [axis resetCustomAxisMax];
  }
  if ([RCWString(cfg, @"valueFormatter") isEqualToString:@"date"]) {
    NSString *pattern = RCWString(cfg, @"valueFormatterPattern") ?: @"HH:mm";
    NSString *locale = RCWString(cfg, @"locale");
    double since = RCWDouble(cfg, @"since", 0);
    NSString *timeUnit = RCWString(cfg, @"timeUnit") ?: @"SECONDS";
    axis.valueFormatter = [[RnChartsWrapperDateAxisValueFormatter alloc] initWithPattern:pattern
                                                                                   locale:locale
                                                                                    since:since
                                                                                 timeUnit:timeUnit];
  } else {
    axis.valueFormatter = nil;
  }
  [self applyGridDashedLine:axis config:RCWMap(cfg, @"gridDashedLine")];
}

+ (void)applyYAxis:(YAxis *)axis config:(nullable NSDictionary *)cfg
{
  if (cfg == nil) {
    axis.enabled = NO;
    return;
  }
  axis.enabled = RCWBool(cfg, @"enabled", YES);
  if (RCWHasKey(cfg, @"drawGridLines")) {
    axis.drawGridLinesEnabled = RCWBool(cfg, @"drawGridLines", YES);
  }
  if (RCWHasKey(cfg, @"drawLabels")) {
    axis.drawLabelsEnabled = RCWBool(cfg, @"drawLabels", YES);
  }
  UIColor *textColor = RCWColor(cfg, @"textColor");
  if (textColor) {
    axis.labelTextColor = textColor;
  }
  if (RCWHasKey(cfg, @"axisMinimum")) {
    axis.axisMinimum = RCWDouble(cfg, @"axisMinimum", 0);
  } else {
    [axis resetCustomAxisMin];
  }
  if (RCWHasKey(cfg, @"axisMaximum")) {
    axis.axisMaximum = RCWDouble(cfg, @"axisMaximum", 0);
  } else {
    [axis resetCustomAxisMax];
  }
  if (RCWHasKey(cfg, @"labelCount")) {
    BOOL force = RCWBool(cfg, @"labelCountForce", NO);
    [axis setLabelCount:(NSInteger)RCWDouble(cfg, @"labelCount", 2) force:force];
  }
  [self applyGridDashedLine:axis config:RCWMap(cfg, @"gridDashedLine")];
}

+ (void)applyGridDashedLine:(AxisBase *)axis config:(nullable NSDictionary *)cfg
{
  if (cfg == nil) {
    return;
  }
  double lineLength = RCWDouble(cfg, @"lineLength", 10);
  double spaceLength = RCWDouble(cfg, @"spaceLength", 10);
  double phase = RCWDouble(cfg, @"phase", 0);
  axis.gridLineDashLengths = @[ @(lineLength), @(spaceLength) ];
  axis.gridLineDashPhase = phase;
}

#pragma mark - Animation

+ (void)applyAnimation:(BarLineChartViewBase *)chart config:(nullable NSDictionary *)cfg
{
  double durationXMs = RCWDouble(cfg, @"durationX", 0);
  double durationYMs = RCWDouble(cfg, @"durationY", 0);
  if (durationXMs > 0 || durationYMs > 0) {
    [chart animateWithXAxisDuration:durationXMs / 1000.0 yAxisDuration:durationYMs / 1000.0];
  }
}

#pragma mark - Data

+ (void)applyLineData:(LineChartView *)chart data:(nullable NSDictionary *)dataMap
{
  NSArray *dataSets = RCWHasKey(dataMap, @"dataSets") ? dataMap[@"dataSets"] : nil;
  if (![dataSets isKindOfClass:[NSArray class]]) {
    chart.data = nil;
    return;
  }
  NSMutableArray<LineChartDataSet *> *sets = [NSMutableArray arrayWithCapacity:dataSets.count];
  for (NSDictionary *dataSetMap in dataSets) {
    [sets addObject:[self buildLineDataSet:dataSetMap]];
  }
  chart.data = [[LineChartData alloc] initWithDataSets:sets];
}

+ (void)applyBarData:(BarChartView *)chart data:(nullable NSDictionary *)dataMap
{
  NSArray *dataSets = RCWHasKey(dataMap, @"dataSets") ? dataMap[@"dataSets"] : nil;
  if (![dataSets isKindOfClass:[NSArray class]]) {
    chart.data = nil;
    return;
  }
  NSMutableArray<BarChartDataSet *> *sets = [NSMutableArray arrayWithCapacity:dataSets.count];
  for (NSDictionary *dataSetMap in dataSets) {
    [sets addObject:[self buildBarDataSet:dataSetMap]];
  }
  BarChartData *barData = [[BarChartData alloc] initWithDataSets:sets];
  [self applyBarDataConfig:barData config:RCWMap(dataMap, @"config") dataSetCount:sets.count];
  chart.data = barData;
}

+ (void)applyCombinedData:(CombinedChartView *)chart data:(nullable NSDictionary *)dataMap
{
  CombinedChartData *combinedData = [[CombinedChartData alloc] init];
  NSDictionary *barDataMap = RCWMap(dataMap, @"barData");
  if (barDataMap != nil) {
    NSArray *dataSets = barDataMap[@"dataSets"];
    NSMutableArray<BarChartDataSet *> *sets = [NSMutableArray arrayWithCapacity:dataSets.count];
    for (NSDictionary *dataSetMap in dataSets) {
      [sets addObject:[self buildBarDataSet:dataSetMap]];
    }
    BarChartData *barData = [[BarChartData alloc] initWithDataSets:sets];
    [self applyBarDataConfig:barData config:RCWMap(barDataMap, @"config") dataSetCount:sets.count];
    combinedData.barData = barData;
  }
  NSDictionary *lineDataMap = RCWMap(dataMap, @"lineData");
  if (lineDataMap != nil) {
    NSArray *dataSets = lineDataMap[@"dataSets"];
    NSMutableArray<LineChartDataSet *> *sets = [NSMutableArray arrayWithCapacity:dataSets.count];
    for (NSDictionary *dataSetMap in dataSets) {
      [sets addObject:[self buildLineDataSet:dataSetMap]];
    }
    combinedData.lineData = [[LineChartData alloc] initWithDataSets:sets];
  }
  chart.data = combinedData;
}

+ (void)applyBarDataConfig:(BarChartData *)barData config:(nullable NSDictionary *)cfg dataSetCount:(NSUInteger)count
{
  if (cfg == nil) {
    return;
  }
  if (RCWHasKey(cfg, @"barWidth")) {
    barData.barWidth = RCWDouble(cfg, @"barWidth", 0.5);
  }
  NSDictionary *group = RCWMap(cfg, @"group");
  if (group != nil && count > 1) {
    [barData groupBarsFromX:RCWDouble(group, @"fromX", 0)
                  groupSpace:RCWDouble(group, @"groupSpace", 0)
                    barSpace:RCWDouble(group, @"barSpace", 0)];
  }
}

+ (LineChartDataSet *)buildLineDataSet:(NSDictionary *)dataSetMap
{
  NSArray *values = dataSetMap[@"values"];
  NSMutableArray<ChartDataEntry *> *entries = [NSMutableArray arrayWithCapacity:values.count];
  for (NSDictionary *point in values) {
    ChartDataEntry *entry = [[ChartDataEntry alloc] initWithX:RCWDouble(point, @"x", 0) y:RCWDouble(point, @"y", 0)];
    if (RCWHasKey(point, @"marker")) {
      entry.data = point[@"marker"];
    }
    [entries addObject:entry];
  }
  NSString *label = RCWString(dataSetMap, @"label") ?: @"";
  LineChartDataSet *dataSet = [[LineChartDataSet alloc] initWithEntries:entries label:label];
  [self applyLineDataSetConfig:dataSet config:RCWMap(dataSetMap, @"config")];
  return dataSet;
}

+ (BarChartDataSet *)buildBarDataSet:(NSDictionary *)dataSetMap
{
  NSArray *values = dataSetMap[@"values"];
  NSMutableArray<BarChartDataEntry *> *entries = [NSMutableArray arrayWithCapacity:values.count];
  for (NSDictionary *point in values) {
    BarChartDataEntry *entry = [[BarChartDataEntry alloc] initWithX:RCWDouble(point, @"x", 0) y:RCWDouble(point, @"y", 0)];
    if (RCWHasKey(point, @"marker")) {
      entry.data = point[@"marker"];
    }
    [entries addObject:entry];
  }
  NSString *label = RCWString(dataSetMap, @"label") ?: @"";
  BarChartDataSet *dataSet = [[BarChartDataSet alloc] initWithEntries:entries label:label];
  [self applyBarDataSetConfig:dataSet config:RCWMap(dataSetMap, @"config")];
  return dataSet;
}

+ (void)applyLineDataSetConfig:(LineChartDataSet *)dataSet config:(nullable NSDictionary *)cfg
{
  dataSet.drawValuesEnabled = NO;
  dataSet.drawCirclesEnabled = NO;
  dataSet.drawCircleHoleEnabled = NO;
  dataSet.highlightEnabled = YES;
  if (cfg == nil) {
    return;
  }

  NSString *mode = RCWString(cfg, @"mode");
  if ([mode isEqualToString:@"STEPPED"]) {
    dataSet.mode = LineChartDataSetModeStepped;
  } else if ([mode isEqualToString:@"CUBIC_BEZIER"]) {
    dataSet.mode = LineChartDataSetModeCubicBezier;
  } else if ([mode isEqualToString:@"HORIZONTAL_BEZIER"]) {
    dataSet.mode = LineChartDataSetModeHorizontalBezier;
  } else if (mode != nil) {
    dataSet.mode = LineChartDataSetModeLinear;
  }
  UIColor *color = RCWColor(cfg, @"color");
  if (color) {
    [dataSet setColor:color];
  }
  UIColor *highlightColor = RCWColor(cfg, @"highlightColor");
  if (highlightColor) {
    dataSet.highlightColor = highlightColor;
  }
  if (RCWHasKey(cfg, @"highlightLineWidth")) {
    dataSet.highlightLineWidth = RCWDouble(cfg, @"highlightLineWidth", 1);
  }
  if (RCWHasKey(cfg, @"highlightEnabled")) {
    dataSet.highlightEnabled = RCWBool(cfg, @"highlightEnabled", YES);
  }
  if (RCWHasKey(cfg, @"lineWidth")) {
    dataSet.lineWidth = RCWDouble(cfg, @"lineWidth", 1);
  }
  if (RCWHasKey(cfg, @"drawValues")) {
    dataSet.drawValuesEnabled = RCWBool(cfg, @"drawValues", NO);
  }
  if (RCWHasKey(cfg, @"drawFilled")) {
    dataSet.drawFilledEnabled = RCWBool(cfg, @"drawFilled", NO);
  }
  if (RCWHasKey(cfg, @"drawCircles")) {
    dataSet.drawCirclesEnabled = RCWBool(cfg, @"drawCircles", NO);
  }
  UIColor *circleColor = RCWColor(cfg, @"circleColor");
  if (circleColor) {
    [dataSet setCircleColor:circleColor];
  }
  if (RCWHasKey(cfg, @"circleRadius")) {
    dataSet.circleRadius = RCWDouble(cfg, @"circleRadius", 4);
  }
  if (RCWHasKey(cfg, @"drawCircleHole")) {
    dataSet.drawCircleHoleEnabled = RCWBool(cfg, @"drawCircleHole", NO);
  }
  if (RCWHasKey(cfg, @"fillAlpha")) {
    // Android's fillAlpha is on a 0-1000 scale (MPAndroidChart convention);
    // DGCharts' is a plain 0-1 CGFloat alpha — rescale to match visually.
    dataSet.fillAlpha = RCWDouble(cfg, @"fillAlpha", 1000) / 1000.0;
  }
  NSDictionary *fillGradient = RCWMap(cfg, @"fillGradient");
  if (fillGradient != nil) {
    dataSet.fill = [self buildGradientFill:fillGradient];
  }
  NSString *axisDependency = RCWString(cfg, @"axisDependency");
  if (axisDependency != nil) {
    dataSet.axisDependency = [axisDependency isEqualToString:@"RIGHT"] ? AxisDependencyRight : AxisDependencyLeft;
  }
}

+ (void)applyBarDataSetConfig:(BarChartDataSet *)dataSet config:(nullable NSDictionary *)cfg
{
  dataSet.drawValuesEnabled = NO;
  if (cfg == nil) {
    return;
  }
  UIColor *color = RCWColor(cfg, @"color");
  if (color) {
    [dataSet setColor:color];
  }
  UIColor *highlightColor = RCWColor(cfg, @"highlightColor");
  if (highlightColor) {
    dataSet.highlightColor = highlightColor;
  }
  if (RCWHasKey(cfg, @"highlightEnabled")) {
    dataSet.highlightEnabled = RCWBool(cfg, @"highlightEnabled", YES);
  }
  if (RCWHasKey(cfg, @"drawValues")) {
    dataSet.drawValuesEnabled = RCWBool(cfg, @"drawValues", NO);
  }
  NSString *axisDependency = RCWString(cfg, @"axisDependency");
  if (axisDependency != nil) {
    dataSet.axisDependency = [axisDependency isEqualToString:@"RIGHT"] ? AxisDependencyRight : AxisDependencyLeft;
  }
}

+ (Fill *)buildGradientFill:(NSDictionary *)cfg
{
  NSArray *colorsArr = cfg[@"colors"];
  NSMutableArray *cgColors = [NSMutableArray arrayWithCapacity:colorsArr.count];
  for (id colorValue in colorsArr) {
    UIColor *color = [RCTConvert UIColor:colorValue];
    [cgColors addObject:(id)color.CGColor];
  }
  CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
  CGGradientRef gradient = CGGradientCreateWithColors(colorSpace, (__bridge CFArrayRef)cgColors, NULL);
  CGColorSpaceRelease(colorSpace);

  double angle = RCWDouble(cfg, @"angle", 270);
  Fill *fill = [[Fill alloc] initWithLinearGradient:gradient angle:angle];
  CGGradientRelease(gradient);
  return fill;
}

@end
