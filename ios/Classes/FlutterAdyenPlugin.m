#import "FlutterAdyenPlugin.h"
#import <adyen_drop_in_plugin/adyen_drop_in_plugin-Swift.h>

@implementation FlutterAdyenPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    [SwiftFlutterAdyenPlugin registerWithRegistrar:registrar];
}
@end
