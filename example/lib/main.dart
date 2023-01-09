import 'dart:convert';

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
              dropInResponse = await AdyenDropInPlugin.openDropIn(
                  paymentMethods: jsonEncode(examplePaymentMethods2),
                  baseUrl: 'https://checkout-test.adyen.com/v69/',
                  clientKey: 'test_SN3VYRCD5BGE7DGKOCLRVO2Y744KZIMC',
                  publicKey:
                      'AQElhmfuXNWTK0Qc+iScl2M5s+uvTYhFGKP8N1zUR10TF0LQ1jKYBxDBXVsNvuR83LVYjEgiTGAH-jNCgh/vvn6xbpxlu0g0IQR/Ta3fnvZHHmpf223MdtW8=-7U5<8,~MVEImAzZg',
                  locale: 'zh-rHK',
                  shopperReference: 'asdasda',
                  returnUrl: '',
                  amount: '1230',
                  lineItem: {'id': '1', 'description': 'adyen test'},
                  currency: 'HKD',
                  merchantAccount: 'LegatoTechECOM',
                  reference: '',
                  threeDS2RequestData: {
                    "deviceChannel": "app",
                    "challengeIndicator": "requestChallenge"
                  },
                  additionalData: {
                    "allow3DS2": "true",
                    "executeThreeD": "false"
                  },
                  storePaymentMethod: false);
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
