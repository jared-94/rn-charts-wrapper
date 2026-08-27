#import "RnChartsWrapperMarkerView.h"

static const CGFloat kPaddingH = 8.0;
static const CGFloat kPaddingV = 5.0;
static const CGFloat kCornerRadius = 4.0;
static const CGFloat kLineSpacing = 2.0;
static const CGFloat kVerticalGap = 12.0;

@implementation RnChartsWrapperMarkerView {
  NSArray<NSString *> *_lines;
}

- (instancetype)init
{
  if (self = [super init]) {
    _markerColor = [UIColor darkGrayColor];
    _markerTextColor = [UIColor whiteColor];
    _textAlignment = NSTextAlignmentCenter;
    _lines = @[];
  }
  return self;
}

- (NSDictionary<NSAttributedStringKey, id> *)textAttributes
{
  NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
  style.alignment = _textAlignment;
  return @{
    NSFontAttributeName : [UIFont systemFontOfSize:13],
    NSForegroundColorAttributeName : _markerTextColor,
    NSParagraphStyleAttributeName : style,
  };
}

- (CGSize)measure
{
  NSDictionary *attrs = [self textAttributes];
  CGFloat width = 0;
  CGFloat height = 0;
  for (NSString *line in _lines) {
    CGSize size = [line sizeWithAttributes:attrs];
    width = MAX(width, size.width);
    height += size.height;
  }
  if (_lines.count > 1) {
    height += kLineSpacing * (_lines.count - 1);
  }
  return CGSizeMake(width + kPaddingH * 2, height + kPaddingV * 2);
}

#pragma mark - ChartMarker

- (CGPoint)offset
{
  return CGPointZero;
}

- (CGPoint)offsetForDrawingAtPoint:(CGPoint)point
{
  CGSize size = [self measure];
  return CGPointMake(-size.width / 2.0, -size.height - kVerticalGap);
}

- (void)refreshContentWithEntry:(ChartDataEntry *)entry highlight:(ChartHighlight *)highlight
{
  NSString *text = [entry.data isKindOfClass:[NSString class]] ? (NSString *)entry.data
                                                                : [NSString stringWithFormat:@"%g", entry.y];
  _lines = [text componentsSeparatedByString:@"\n"];
}

- (void)drawWithContext:(CGContextRef)context point:(CGPoint)point
{
  if (_lines.count == 0) {
    return;
  }
  CGPoint offset = [self offsetForDrawingAtPoint:point];
  CGSize size = [self measure];
  CGRect rect = CGRectMake(point.x + offset.x, point.y + offset.y, size.width, size.height);

  UIGraphicsPushContext(context);
  UIBezierPath *path = [UIBezierPath bezierPathWithRoundedRect:rect cornerRadius:kCornerRadius];
  [_markerColor setFill];
  [path fill];

  NSDictionary *attrs = [self textAttributes];
  CGFloat y = rect.origin.y + kPaddingV;
  for (NSString *line in _lines) {
    CGSize lineSize = [line sizeWithAttributes:attrs];
    CGRect lineRect = CGRectMake(rect.origin.x + kPaddingH, y, rect.size.width - kPaddingH * 2, lineSize.height);
    [line drawInRect:lineRect withAttributes:attrs];
    y += lineSize.height + kLineSpacing;
  }
  UIGraphicsPopContext();
}

@end
