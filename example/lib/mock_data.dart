var examplePaymentMethods = {"groups":[{"name":"Credit Card","types":["amex","mc","visa"]}],"paymentMethods":[{"brands":["amex","mc","visa"],"details":[{"key":"encryptedCardNumber","type":"cardToken"},{"key":"encryptedSecurityCode","type":"cardToken"},{"key":"encryptedExpiryMonth","type":"cardToken"},{"key":"encryptedExpiryYear","type":"cardToken"},{"key":"holderName","optional":true,"type":"text"}],"name":"Credit Card","type":"scheme"},{"name":"PayPal","supportsRecurring":true,"type":"paypal"}]};

String baseUrl = 'baseUrl';
String clientKey = 'clientKey';
String locale = 'nl_NL';
String currency = 'EUR';
String publicKey = 'publicKey';
Map<String, String> headers = {};