import 'dart:async';

import 'package:adyen_dropin/enums/adyen_error.dart';
import 'package:adyen_dropin/enums/adyen_response.dart';
import 'package:adyen_dropin/exceptions/adyen_exception.dart';
import 'package:flutter/services.dart';

class FlutterAdyen {
  static const MethodChannel _channel = const MethodChannel('flutter_adyen');

  static Future<AdyenResponse> openDropIn(
      {paymentMethods,
      required String baseUrl,
      required String clientKey,
      required String publicKey,
      lineItem,
      required String locale,
      required String amount,
      required String currency,
      required String returnUrl,
      required String shopperReference,
      required Map<String, String> additionalData,
      Map<String, String>? headers,
      environment = 'TEST'}) async {
    Map<String, dynamic> args = {};
    args.putIfAbsent('paymentMethods', () => paymentMethods);
    args.putIfAbsent('additionalData', () => additionalData);
    args.putIfAbsent('baseUrl', () => baseUrl);
    args.putIfAbsent('clientKey', () => clientKey);
    args.putIfAbsent('publicKey', () => publicKey);
    args.putIfAbsent('amount', () => amount);
    args.putIfAbsent('locale', () => locale);
    args.putIfAbsent('currency', () => currency);
    args.putIfAbsent('lineItem', () => lineItem);
    args.putIfAbsent('returnUrl', () => returnUrl);
    args.putIfAbsent('environment', () => environment);
    args.putIfAbsent('shopperReference', () => shopperReference);
    args.putIfAbsent('headers', () => headers);
    try {
      final String response =  await _channel.invokeMethod('openDropIn', args);
      return AdyenResponse.values.firstWhere((element) => element.name == response,
          orElse: () => AdyenResponse.Unknown);
    } on PlatformException catch (e) {
      switch(e.code) {
        case 'PAYMENT_ERROR':
          throw AdyenException(AdyenError.PAYMENT_ERROR, e.message);
        case 'PAYMENT_CANCELLED':
          throw AdyenException(AdyenError.PAYMENT_CANCELLED, e.message);
        default:
          rethrow;
      }
    }
  }
}
