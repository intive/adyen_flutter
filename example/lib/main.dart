import 'dart:convert';

import 'package:adyen_dropin/flutter_adyen.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'mock_data.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _payment_result = 'Unknown';

  String? dropInResponse;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () async {
            try {
              dropInResponse = await FlutterAdyen.openDropIn(
                  paymentMethods: jsonEncode(myPaymentMethods),
                  baseUrl: '$baseUrl/',
                  clientKey: clientKey,
                  publicKey: publicKey,
                  locale: locale,
                  shopperReference: '',
                  returnUrl: 'adyendropin://',
                  amount: '100',
                  lineItem: {},
                  currency: currency,
                  additionalData: {},
                  headers: headers);
            } on PlatformException catch (e) {
              if (e.code == 'PAYMENT_CANCELLED')
                dropInResponse = 'Payment Cancelled';
              else
                dropInResponse = 'Payment Error';
            }

            setState(() {
              _payment_result = dropInResponse;
            });
          },
        ),
        appBar: AppBar(
          title: const Text('Flutter Adyen'),
        ),
        body: Center(
          child: Text('Payment Result: $_payment_result\n'),
        ),
      ),
    );
  }
}
