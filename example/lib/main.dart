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
                  baseUrl: 'https://api.dev.juhapp.de',
                  clientKey: 'test_3NCK7SJH55H2TD2LHTGJR7ZCPIQN2C7U',
                  publicKey: '10001|D28B4325E83D975A28F3957208C4CA32512DA8E963B0E1B5663F672499C511868B330A2A77E023D0447AF3389B00E6C91CF5FB9093D21B69CF7AC9E0F211331E23AB2881D4727F00079DBF319EF2FC9F4A25F0D00B697AFFA668EE3F189902461755492560F7D2E5875D9277A8E6C81D53E1255440AFD803919585364E862FC469474297E2FF62295E47B38E1E5900389FC1FF76F49914C6EB505B80202D5E2A6B2359968A486674CBFB8A6331C8CC3BCF5FEDD9CC5B85FF9E5B6B285E53174B902A7CEBCBD5BBFACF692CB320BF693BE02AC35DC764EE1038AF1D1AE8792933A4DBFA782BB01C87B47956C252499D1A085B1CFBFEB05A12C3B575421B62A451',
                  locale: 'de_DE',
                  reference: 'asd',
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
