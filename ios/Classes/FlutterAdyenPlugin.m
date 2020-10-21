#import "FlutterAdyenPlugin.h"
#import <flutter_adyen/flutter_adyen-Swift.h>

@implementation FlutterAdyenPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterAdyenPlugin registerWithRegistrar:registrar];
}
@end
