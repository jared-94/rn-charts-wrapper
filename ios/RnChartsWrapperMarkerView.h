#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@import DGCharts;

NS_ASSUME_NONNULL_BEGIN

/**
 * Canvas-drawn tooltip shown on tap — the iOS counterpart of Android's
 * TextMarkerView.java, same visual contract: a rounded bubble showing the
 * per-point `marker` string (see ChartValue.marker in index.tsx, e.g.
 * "21°C\n14:32 - 26/08"), split on "\n" into centered lines.
 *
 * NOTE: written without the ability to compile against the real DGCharts
 * headers on this machine (no Xcode here). The IMarker protocol's exact
 * Swift-bridged Objective-C selectors (offsetForDrawingAtPoint:,
 * refreshContentWithEntry:highlight:, drawWithContext:point:) should be
 * double-checked against Xcode's generated interface for the resolved
 * DGCharts version on the first real build — see DGCharts.swiftinterface /
 * Cmd-click on IMarker.
 */
@interface RnChartsWrapperMarkerView : NSObject <IMarker>

@property(nonatomic, strong) UIColor *markerColor;
@property(nonatomic, strong) UIColor *markerTextColor;
@property(nonatomic, assign) NSTextAlignment textAlignment;

@end

NS_ASSUME_NONNULL_END
