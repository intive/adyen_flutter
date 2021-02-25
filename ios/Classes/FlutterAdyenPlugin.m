#import "FlutterAdyenPlugin.h"
#import <adyen_dropin/adyen_dropin-Swift.h>

@implementation FlutterAdyenPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    [SwiftFlutterAdyenPlugin registerWithRegistrar:registrar];
}
@end
