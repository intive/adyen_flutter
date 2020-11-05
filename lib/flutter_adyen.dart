import 'dart:async';

import 'package:flutter/services.dart';

class FlutterAdyen {
  static const MethodChannel _channel = const MethodChannel('flutter_adyen');

  static Future<String> openDropIn(
      {paymentMethods,
      String baseUrl,
      String clientKey,
      String publicKey,
      lineItem,
      String locale,
      String amount,
      String currency,
      environment = 'TEST'}) async {
    Map<String, dynamic> args = {};
    args.putIfAbsent('paymentMethods', () => paymentMethods);
    args.putIfAbsent('baseUrl', () => baseUrl);
    args.putIfAbsent('clientKey', () => clientKey);
    args.putIfAbsent('publicKey', () => publicKey);
    args.putIfAbsent('amount', () => amount);
    args.putIfAbsent('locale', () => locale);
    args.putIfAbsent('currency', () => currency);
    args.putIfAbsent('lineItem', () => lineItem);
    args.putIfAbsent('environment', () => environment);

    final String response = await _channel.invokeMethod('openDropIn', args);
    return response;
  }
}
