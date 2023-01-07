var examplePaymentMethods = {
  "groups": [
    {
      "name": "Credit Card",
      "types": ["amex", "mc", "visa"]
    }
  ],
  "paymentMethods": [
    {
      "brands": ["amex", "mc", "visa"],
      "details": [
        {"key": "encryptedCardNumber", "type": "cardToken"},
        {"key": "encryptedSecurityCode", "type": "cardToken"},
        {"key": "encryptedExpiryMonth", "type": "cardToken"},
        {"key": "encryptedExpiryYear", "type": "cardToken"},
        {"key": "holderName", "optional": true, "type": "text"}
      ],
      "name": "Credit Card",
      "type": "scheme"
    },
    {"name": "PayPal", "supportsRecurring": true, "type": "paypal"}
  ]
};

var examplePaymentMethods2 = {
  "paymentMethods": [
    {
      "brands": ["visa", "mc", "amex", "cup", "jcb"],
      "name": "Credit Card",
      "type": "scheme"
    },
    {"name": "AliPay HK", "type": "alipay_hk"},
    {"name": "WeChat Pay", "type": "wechatpayMiniProgram"},
    {"name": "WeChat Pay", "type": "wechatpayQR"},
    {"name": "WeChat Pay", "type": "wechatpayWeb"}
  ]
};
String clientKey = '10001XXXXXXXXXXXXXXXXXXXX';
