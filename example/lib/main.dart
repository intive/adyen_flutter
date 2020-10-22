import 'dart:convert';

import 'package:flutter/material.dart';
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
            dropInResponse = await FlutterAdyen.openDropIn(
              paymentMethods: jsonEncode(examplePaymentMethods),
              baseUrl: 'https://xxxxxxxxx/payment/',
              authToken: 'Bearer AAABBBCCCDDD222111',
              merchantAccount: 'YOURMERCHANTACCOUNTCOM',
              publicKey: pubKey,
              amount: '1230',
              currency: 'EUR',
              shopperReference:
                  DateTime.now().millisecondsSinceEpoch.toString(),
              reference: DateTime.now().millisecondsSinceEpoch.toString(),
            );

            setState(() {
              _payment_result = dropInResponse;
            });
            setState(() {});
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
