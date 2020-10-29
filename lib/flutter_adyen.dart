import 'dart:async';

import 'package:flutter/services.dart';

class FlutterAdyen {
  static const MethodChannel _channel = const MethodChannel('flutter_adyen');

  static Future<String> openDropIn(
      {paymentMethods,
      baseUrl,
      authToken,
      iosReturnUrl,
      merchantAccount,
      clientKey,
      amount,
      currency,
      reference,
      shopperReference,
      environment = 'TEST'}) async {
    Map<String, dynamic> args = {};
    args.putIfAbsent('paymentMethods', () => paymentMethods);
    args.putIfAbsent('baseUrl', () => baseUrl);
    args.putIfAbsent('authToken', () => authToken);
    args.putIfAbsent('iosReturnUrl', () => iosReturnUrl);
    args.putIfAbsent('merchantAccount', () => merchantAccount);
    args.putIfAbsent('clientKey', () => clientKey);
    args.putIfAbsent('amount', () => amount);
    args.putIfAbsent('currency', () => currency);
    args.putIfAbsent('reference', () => reference);
    args.putIfAbsent('shopperReference', () => shopperReference);
    args.putIfAbsent('environment', () => environment);

    final String response = await _channel.invokeMethod('openDropIn', args);
    return response;
  }
}
