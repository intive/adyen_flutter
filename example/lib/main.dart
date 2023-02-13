import 'dart:convert';
import 'dart:io';

import 'package:adyen_drop_in_plugin/adyen_drop_in_plugin.dart';
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
              final List<Map<String, String>> items = [];
              items.add({'id': '1', 'quantity': '1'});

              dropInResponse = await AdyenDropInPlugin.openDropIn(
                paymentMethods: jsonEncode(examplePaymentMethods2),
                baseUrl: //'https://checkout-test.adyen.com/v69/',
                    'https://venchi-crm-progress.legato.co/api/v1/general/',
                clientKey: 'test_SN3VYRCD5BGE7DGKOCLRVO2Y744KZIMC',
                publicKey:
                    'AQElhmfuXNWTK0Qc+iScl2M5s+uvTYhFGKP8N1zUR10TF0LQ1jKYBxDBXVsNvuR83LVYjEgiTGAH-vkwDN9XLxb+X04zNqgWvSxWMedghO9+pMgNa2hs9dhI=-wHJ38#+FDCuSG[>8',
                locale: 'HK',
                accessToken: '2F2ko01b7RwUozq6tAiMePzkksDQv2osGdxd9VnSrD',
                shopperReference: 'asdasda',
                returnUrl: 'appscheme://payment',
                amount: '120',
                lineItem: items,
                currency: 'HKD',
                merchantAccount: 'LegatoTechECOM',
                reference:
                    '${Platform.isIOS ? 'ios' : 'android'}-components_${DateTime.now().millisecondsSinceEpoch}',
                threeDS2RequestData: {
                  "deviceChannel": "app",
                  "challengeIndicator": "requestChallenge"
                },
                additionalData: {"allow3DS2": "true", "executeThreeD": "false"},
                storePaymentMethod: false,
              );
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
