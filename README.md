# flutter_adyen

Note: This library is not official from Adyen.

Flutter plugin to integrate with the Android and iOS libraries of Adyen.
This library enables you to open the **Drop-in** method of Adyen with just calling one function.

* [Adyen drop-in Android](https://docs.adyen.com/checkout/android/drop-in)
* [Adyen drop-in iOS](https://docs.adyen.com/checkout/ios/drop-in)

This should support One time payment and recurring payment. 

## Prerequisites

Before calling the plugin, make sure to get the **payment methods** from Adyen or better from your backend. For this, call the [a /paymentMethods](https://docs.adyen.com/api-explorer/#/PaymentSetupAndVerificationService/v46/paymentMethods) endpoint:


POST: https://checkout-test.adyen.com/v46/paymentMethods // Version number might be different
Payload should contain the merchant account
```
{
  "merchantAccount": "<YOUR ACCOUNT>"
}
```

Append your API key and a content-type to the headers:
```
Content-Type: application/json
X-API-KEY: AQEXXXXXXXXXXXXXXXX
```

It's not recommended to store the API key in the front-end for security reasons!

####You also need to have the:
* publicKey (from Adyen)
* merchantAccount (from Adyen)
* amount & currency 
* shopperReference (e.g userId)
* reference (e.g transactionId)

##Setup

###Android
Add this in your android/build.gradle

```
implementation "com.adyen.checkout:drop-in:3.2.1"
```

And in the AndroidManifest.xml in your application tag add this service, this allows adyen to tell the android app the result of the payment.

```
<application ...>
    ...
    <service
           android:name="app.petleo.flutter_adyen.MyDropInService"
           android:permission="android.permission.BIND_JOB_SERVICE" />

</application>
``` 

you also might need to set minifyEnabled & useProguard to false if it crashes on a release build

```  

buildTypes {
           release {
               signingConfig signingConfigs.release               
               minifyEnabled false
               useProguard false
               proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
           }
       }
```

###iOS
You need to add a URL_SCHEME if you do not have one yet.

[Here is how to add one.](https://developer.apple.com/documentation/uikit/inter-process_communication/allowing_apps_and_websites_to_link_to_your_content/defining_a_custom_url_scheme_for_your_app)

You might need to run this command `pod update Adyen/Card` in your ios folder.


##Usage
Just add this in your dart code
```
 const PAYMENT_SUCCESS = 'SUCCESS';
 const PAYMENT_CANCELLED = 'CANCELLED';
 
 String dropInResponse = await FlutterAdyen.openDropIn(
                paymentMethods: jsonEncode(examplePaymentMethods),
                baseUrl: 'https://YOURBACKEND/payment/',
                authToken: 'Bearer AAABBBCCCDDD222111',
                merchantAccount: 'YOURMERCHANTACCOUNTCOM',
                publicKey: pubKey,
                amount: '1230',
                currency: 'EUR',
                iosReturnUrl: 'YOURAPP://' <-- URL_SCHEME_FOR_YOUR_iOS_APP
                shopperReference: DateTime.now().millisecondsSinceEpoch.toString(),
                reference: DateTime.now().millisecondsSinceEpoch.toString(),
              );
              
  if(dropInResponse == 'PAYMENT_SUCCESS') ... 
  if(dropInResponse == 'PAYMENT_CANCELLED') ...
  else ... // you will get the error message here. (It is not translated to any languages)
```

### Important to know
The library expect your backend to provide the following endpoints as documented by Adyen:
 * https://YOURBACKEND/payment/payments/
 * https://YOURBACKEND/payment/payments/details/
