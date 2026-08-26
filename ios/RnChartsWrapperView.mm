/**
 * Fabric component glue — mirrors RnChartsWrapperView.java's structure
 * (recreate-chart-on-chartKind-change, apply bundled chartConfig via a
 * ChartConfigApplier, forward ChartViewDelegate callbacks as onChange/
 * onSelect). See RnChartsWrapperChartConfigApplier.h for the DGCharts-
 * specific parts that are unverified against a real Xcode build.
 */

#import "RnChartsWrapperView.h"
#import "RnChartsWrapperChartConfigApplier.h"
#import "RnChartsWrapperMarkerView.h"

#import <react/renderer/components/rnchartswrapper/ComponentDescriptors.h>
#import <react/renderer/components/rnchartswrapper/EventEmitters.h>
#import <react/renderer/components/rnchartswrapper/Props.h>
#import <react/renderer/components/rnchartswrapper/RCTComponentViewHelpers.h>
#import <react/utils/FollyConvert.h>

#import <React/RCTConversions.h>
#import <React/RCTFabricComponentsPlugins.h>

@import DGCharts;

using namespace facebook::react;

@interface RnChartsWrapperView () <RCTRnChartsWrapperViewViewProtocol, ChartViewDelegate>
@end

@implementation RnChartsWrapperView {
  NSString *_chartKind;
  BOOL _disallowInterceptTouch;
  BarLineChartViewBase *_chart;
  RnChartsWrapperMarkerView *_marker;
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RnChartsWrapperViewProps>();
    _props = defaultProps;
    _chartKind = @"line";
    _marker = [[RnChartsWrapperMarkerView alloc] init];
    [self recreateChart];
  }
  return self;
}

- (void)layoutSubviews
{
  [super layoutSubviews];
  _chart.frame = self.bounds;
}

#pragma mark - Chart lifecycle

- (void)recreateChart
{
  [_chart removeFromSuperview];
  if ([_chartKind isEqualToString:@"bar"]) {
    _chart = [[BarChartView alloc] initWithFrame:self.bounds];
  } else if ([_chartKind isEqualToString:@"combined"]) {
    // NOTE: unverified — CombinedChartView.DrawOrder is a Swift enum nested
    // in the class; the exact bridged ObjC array/enum spelling below is a
    // best guess (see RnChartsWrapperChartConfigApplier.h's disclaimer).
    // Drop this line entirely if it doesn't compile — DGCharts defaults to
    // bar-then-line anyway, which is what this app needs.
    CombinedChartView *combined = [[CombinedChartView alloc] initWithFrame:self.bounds];
    combined.drawOrder = @[ @(CombinedChartDrawOrderBar), @(CombinedChartDrawOrderLine) ];
    _chart = combined;
  } else {
    _chart = [[LineChartView alloc] initWithFrame:self.bounds];
  }
  _chart.delegate = self;
  _chart.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
  [self addSubview:_chart];
}

#pragma mark - RCTComponentViewProtocol

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<RnChartsWrapperViewComponentDescriptor>();
}

- (void)prepareForRecycle
{
  [super prepareForRecycle];
  _chartKind = @"line";
  [self recreateChart];
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
  const auto &oldViewProps = *std::static_pointer_cast<const RnChartsWrapperViewProps>(_props);
  const auto &newViewProps = *std::static_pointer_cast<const RnChartsWrapperViewProps>(props);

  NSString *newChartKind = RCTNSStringFromString(newViewProps.chartKind);
  if (newChartKind.length == 0) {
    newChartKind = @"line";
  }
  BOOL chartKindChanged = ![newChartKind isEqualToString:_chartKind];
  _chartKind = newChartKind;
  if (chartKindChanged) {
    [self recreateChart];
  }

  // TODO: Android's counterpart calls requestDisallowInterceptTouchEvent
  // from onInterceptTouchEvent so pinch/pan on the chart isn't stolen by an
  // ancestor scroll view (both historyChart.js and groupHistoryChart.js
  // always pass disallowInterceptTouch={true}). No iOS equivalent wired up
  // yet — UIScrollView touch-cancellation is a different mechanism
  // (touchesShouldCancelInView: on the ancestor, or its
  // canCancelContentTouches/delaysContentTouches). Currently a no-op here;
  // revisit if the on-device test shows a parent scroll view stealing the
  // chart's zoom/pan gesture.
  _disallowInterceptTouch = newViewProps.disallowInterceptTouch;

  if (chartKindChanged || oldViewProps.chartConfig != newViewProps.chartConfig) {
    id configValue = convertFollyDynamicToId(newViewProps.chartConfig);
    NSDictionary *config = [configValue isKindOfClass:[NSDictionary class]] ? (NSDictionary *)configValue : nil;
    [RnChartsWrapperChartConfigApplier apply:_chart chartKind:_chartKind config:config marker:_marker];
  }

  [super updateProps:props oldProps:oldProps];
}

#pragma mark - Commands (RCTRnChartsWrapperViewViewProtocol)

- (void)clearHighlights
{
  [_chart highlightValues:nil];
}

- (void)fitScreen
{
  [_chart fitScreen];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args
{
  RCTRnChartsWrapperViewHandleCommand(self, commandName, args);
}

#pragma mark - ChartViewDelegate

- (void)chartValueSelected:(ChartViewBase *)chartView entry:(ChartDataEntry *)entry highlight:(ChartHighlight *)highlight
{
  if (_eventEmitter == nullptr) {
    return;
  }
  auto emitter = std::dynamic_pointer_cast<const RnChartsWrapperViewEventEmitter>(_eventEmitter);
  if (!emitter) {
    return;
  }
  emitter->onSelect({
      .action = std::string("select"),
      .x = entry.x,
      .y = entry.y,
      .dataSetIndex = highlight != nil ? (int)highlight.dataSetIndex : 0,
  });
}

- (void)chartValueNothingSelected:(ChartViewBase *)chartView
{
  if (_eventEmitter == nullptr) {
    return;
  }
  auto emitter = std::dynamic_pointer_cast<const RnChartsWrapperViewEventEmitter>(_eventEmitter);
  if (!emitter) {
    return;
  }
  emitter->onSelect({
      .action = std::string("none"),
      .x = 0,
      .y = 0,
      .dataSetIndex = -1,
  });
}

- (void)chartScaled:(ChartViewBase *)chartView scaleX:(CGFloat)scaleX scaleY:(CGFloat)scaleY
{
  [self emitChange:@"chartScaled"];
}

- (void)chartTranslated:(ChartViewBase *)chartView dX:(CGFloat)dX dY:(CGFloat)dY
{
  [self emitChange:@"chartTranslated"];
}

- (void)emitChange:(NSString *)action
{
  if (_eventEmitter == nullptr) {
    return;
  }
  auto emitter = std::dynamic_pointer_cast<const RnChartsWrapperViewEventEmitter>(_eventEmitter);
  if (!emitter) {
    return;
  }
  emitter->onChange({.action = std::string([action UTF8String])});
}

@end

Class<RCTComponentViewProtocol> RnChartsWrapperViewCls(void)
{
  return RnChartsWrapperView.class;
}
