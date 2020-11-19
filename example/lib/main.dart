import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_adyen/flutter_adyen.dart';

import 'mock_data.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _payment_result = 'Unknown';

  String dropInResponse;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () async {
            try {
              dropInResponse = await FlutterAdyen.openDropIn(
                  paymentMethods: jsonEncode(examplePaymentMethods),
                  baseUrl: 'https://yourdomain.com',
                  clientKey: 'clientkey',
                  publicKey: 'publickey',
                  locale: 'de_DE',
                  shopperReference: 'asdasda',
                  returnUrl: 'http://asd.de',
                  amount: '1230',
                  lineItem: {'id': '1', 'description': 'adyen test'},
                  currency: 'EUR');
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
