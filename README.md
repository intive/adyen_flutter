# adyen_drop_in_plugin

Note: This library is not official from Adyen.

Flutter plugin to integrate with the Android and iOS libraries of Adyen.
This library enables you to open the **Drop-in** method of Adyen with just calling one function.

* [Adyen drop-in Android](https://docs.adyen.com/checkout/android/drop-in)
* [Adyen drop-in iOS](https://docs.adyen.com/checkout/ios/drop-in)

The Plugin supports 3dSecure v2 and one time payment. It was not tested in a recurring payment scenario.

## Prerequisites

### Credentials
#### You need to have the following information:
* publicKey (from Adyen)
* clientKey (from Adyen)
* amount & currency 
* shopperReference (e.g userId)
* baseUrl from your backend
* Adyen Environment (Test, LIVE_EU etc..)
* locale (de_DE, en_US etc..)
* return url after payment (ios URLScheme of you app) for redirecting back to the app

### Payment Methods

Before calling the plugin, make sure to get the **payment methods** from your backend. For this, call the [a /paymentMethods](https://docs.adyen.com/api-explorer/#/PaymentSetupAndVerificationService/v46/paymentMethods) endpoint:


An example response from payment methods can be seen here:

```
{
    "groups": [
        {
            "name": "Credit Card",
            "types": [
                "amex",
                "mc",
                "visa"
            ]
        }
    ],
    "paymentMethods": [
        {
            "brands": [
                "amex",
                "mc",
                "visa"
            ],
            "details": [
                {
                    "key": "encryptedCardNumber",
                    "type": "cardToken"
                },
                {
                    "key": "encryptedSecurityCode",
                    "type": "cardToken"
                },
                {
                    "key": "encryptedExpiryMonth",
                    "type": "cardToken"
                },
                {
                    "key": "encryptedExpiryYear",
                    "type": "cardToken"
                },
                {
                    "key": "holderName",
                    "optional": true,
                    "type": "text"
                }
            ],
            "name": "Credit Card",
            "type": "scheme"
        },
        {
            "name": "PayPal",
            "supportsRecurring": true,
            "type": "paypal"
        }
    ]
}
```





The app uses these endpoints for payment submit and payment details calls:
```
<your base url>/payment
<your base url>/payment/details
```
The plugin will send data for the payment submit call wrapped into another object like this:
```
{
  payment: <all data for payment which has to be sent to adyen>,
  additionalData: {key: value}
}

// additonalData can be used to send additional data to your own backend for payment


```


## Setup

### Android

And in the AndroidManifest.xml in your application tag add this service, this allows adyen to tell the android app the result of the payment.

```
<application ...>
    ...
 <service
            android:name="app.adyen.flutter_adyen.AdyenDropinService"
            android:permission="android.permission.BIND_JOB_SERVICE"/>

</application>
``` 

#### Proguard
you need to add this to your proguard rules

```  
-keep class com.adyen.** { *; }
-keep class app.adyen.flutter_adyen.**  { *; }
```

### iOS
You need to add a URL_SCHEME if you do not have one yet.

[Here is how to add one.](https://developer.apple.com/documentation/uikit/inter-process_communication/allowing_apps_and_websites_to_link_to_your_content/defining_a_custom_url_scheme_for_your_app)


## Flutter Implementation
To start a Payment you need to call the plugin like so:

```
 try {
      String dropInResponse = await FlutterAdyen.openDropIn(
          paymentMethods: paymentMethods,  // the result of your payment methods call
          baseUrl: 'https://your-server.com/',
          clientKey: <ADYEN_CLIENT_KEY>,
          amount: '1000', // amount in cents
          lineItem: {'id': 'your product ID', 'description': 'Your product description'},
          additionalData: {
            'someKey': 'Some String'
          },
          shopperReference: <YouShopperReference>,
          returnUrl: 'yourAppScheme://payment', //required for iOS
          environment: 'TEST',  // add you environment for produciton here: LIVE_US, LIVE_AUSTRALIA or LIVE_EUROPE
          locale: 'de_DE', // your preferred locale
          publicKey: <your adyen public key>,
          currency: 'EUR');  // your preferred currency


    } on PlatformException {
      // Network Error or other system errors
      return PaymentResponse.paymentError.rawValue;
    }
```

```
      // the dropin returns the following responses as string
      PAYMENT_CANCELLED
      PAYMENT_ERROR
      Authorised
      Refused
      Pending
      Cancelled
      Error
      Received
```

