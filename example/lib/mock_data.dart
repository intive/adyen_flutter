var examplePaymentMethods = {
  "groups": [
    {
      "name": "Credit Card",
      "types": ["visa", "mc", "amex"]
    }
  ],
  "paymentMethods": [
    {
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
    {"name": "Online bank transfer.", "supportsRecurring": true, "type": "directEbanking"},
    {"name": "Paysafecard", "supportsRecurring": true, "type": "paysafecard"},
    {
      "details": [
        {"key": "bic", "type": "text"}
      ],
      "name": "GiroPay",
      "supportsRecurring": true,
      "type": "giropay"
    },
  ]
};


String pubKey = '10001XXXXXXXXXXXXXXXXXXXX';
