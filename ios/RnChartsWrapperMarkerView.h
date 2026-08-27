#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

#import <DGCharts/DGCharts-Swift.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Canvas-drawn tooltip shown on tap — the iOS counterpart of Android's
 * TextMarkerView.java, same visual contract: a rounded bubble showing the
 * per-point `marker` string (see ChartValue.marker in index.tsx, e.g.
 * "21°C\n14:32 - 26/08"), split on "\n" into centered lines.
 */
@interface RnChartsWrapperMarkerView : NSObject <ChartMarker>

@property(nonatomic, strong) UIColor *markerColor;
@property(nonatomic, strong) UIColor *markerTextColor;
@property(nonatomic, assign) NSTextAlignment textAlignment;

@end

NS_ASSUME_NONNULL_END
