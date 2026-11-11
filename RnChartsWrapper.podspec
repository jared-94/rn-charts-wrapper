require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "RnChartsWrapper"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = "MIT"
  s.homepage     = "https://github.com/jared-94/rn-charts-wrapper"
  s.authors      = "jared-94"

  s.platforms    = { :ios => "16.0" }

  s.source       = { :git => "https://github.com/jared-94/rn-charts-wrapper.git", :tag => "v#{s.version}" }
  s.static_framework = true

  # One Swift file: RnChartsWrapperDateXAxisRenderer.swift (DGCharts' axis
  # renderer can only be overridden from Swift).
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.swift_version = "5.0"
  # Nothing outside the pod imports these headers (the component is looked up
  # with NSClassFromString by codegen's RCTThirdPartyComponentsProvider).
  # Private = kept out of the module's umbrella header, which the Swift file
  # imports implicitly: RnChartsWrapperView.h pulls React's C++ headers.
  s.private_header_files = "ios/**/*.h"

  # DGCharts is the maintained continuation of danielgindi/Charts (formerly
  # consumed here as "ios-charts" by the original react-native-charts-wrapper)
  # — same underlying rendering engine as MPAndroidChart on Android. Its core
  # view/dataset/axis classes keep Objective-C compatibility (this library's
  # whole raison d'être depends on it), so this pod's .mm sources call it
  # directly via `@import DGCharts;` rather than needing a Swift shim.
  s.dependency "DGCharts", "~> 5.1"

  install_modules_dependencies(s)
end
