var examplePaymentMethods = {
  "groups": [
    {
      "name": "Credit Card",
      "types": ["amex", "bcmc", "cartebancaire", "mc", "visa", "visadankort"]
    }
  ],
  "paymentMethods": [
    {
      "brands": ["amex", "bcmc", "cartebancaire", "mc", "visa", "visadankort"],
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
    {
      "details": [
        {"key": "encryptedCardNumber", "type": "cardToken"},
        {"key": "encryptedExpiryMonth", "type": "cardToken"},
        {"key": "encryptedExpiryYear", "type": "cardToken"},
        {"key": "holderName", "optional": true, "type": "text"}
      ],
      "name": "Bancontact card",
      "supportsRecurring": true,
      "type": "bcmc"
    },
    {
      "name": "Online bank transfer.",
      "supportsRecurring": true,
      "type": "directEbanking"
    },
    {
      "details": [
        {
          "items": [
            {"id": "66", "name": "Bank Nowy BFG S.A."},
            {"id": "92", "name": "Bank Spółdzielczy w Brodnicy"},
            {"id": "11", "name": "Bank transfer / postal"},
            {"id": "74", "name": "Banki Spółdzielcze"},
            {"id": "73", "name": "BLIK"},
            {"id": "90", "name": "BNP Paribas - płacę z Pl@net"},
            {"id": "59", "name": "CinkciarzPAY"},
            {"id": "87", "name": "Credit Agricole PBL"},
            {"id": "83", "name": "EnveloBank"},
            {"id": "76", "name": "Getin Bank PBL"},
            {"id": "81", "name": "Idea Cloud"},
            {"id": "7", "name": "ING Corporate customers"},
            {"id": "35", "name": "Kantor Polski"},
            {"id": "93", "name": "Kasa Stefczyka"},
            {"id": "44", "name": "Millennium - Płatności Internetowe"},
            {"id": "10", "name": "Millennium Corporate customers"},
            {"id": "68", "name": "mRaty"},
            {"id": "1", "name": "mTransfer"},
            {"id": "91", "name": "Nest Bank"},
            {"id": "80", "name": "Noble Pay"},
            {"id": "50", "name": "Pay Way Toyota Bank"},
            {"id": "45", "name": "Pay with Alior Bank"},
            {"id": "65", "name": "Paylink Idea Bank"},
            {"id": "36", "name": "Pekao24Przelew"},
            {"id": "70", "name": "Pocztowy24"},
            {"id": "6", "name": "Przelew24"},
            {"id": "46", "name": "Płacę z Citi Handlowy"},
            {"id": "38", "name": "Płacę z ING"},
            {"id": "2", "name": "Płacę z Inteligo"},
            {"id": "4", "name": "Płacę z iPKO"},
            {"id": "75", "name": "Płacę z Plus Bank"},
            {"id": "51", "name": "Płać z BOŚ"},
            {"id": "55", "name": "Raty z Alior Bankiem PLN"},
            {"id": "89", "name": "Santander"},
            {"id": "52", "name": "SkyCash"},
            {"id": "60", "name": "T-Mobile usługi bankowe"},
            {"id": "21", "name": "VIA - Moje Rachunki"},
            {"id": "84", "name": "Volkswagen Bank direct"}
          ],
          "key": "issuer",
          "type": "select"
        }
      ],
      "name": "Local Polish Payment Methods",
      "supportsRecurring": true,
      "type": "dotpay"
    },
    {
      "name": "Finnish E-Banking",
      "supportsRecurring": true,
      "type": "ebanking_FI"
    },
    {
      "details": [
        {
          "items": [
            {
              "id": "d5d5b133-1c0d-4c08-b2be-3c9b116dc326",
              "name": "Dolomitenbank"
            },
            {
              "id": "ee9fc487-ebe0-486c-8101-17dce5141a67",
              "name": "Raiffeissen Bankengruppe"
            },
            {
              "id": "6765e225-a0dc-4481-9666-e26303d4f221",
              "name": "Hypo Tirol Bank AG"
            },
            {
              "id": "8b0bfeea-fbb0-4337-b3a1-0e25c0f060fc",
              "name": "Sparda Bank Wien"
            },
            {
              "id": "1190c4d1-b37a-487e-9355-e0a067f54a9f",
              "name": "Schoellerbank AG"
            },
            {
              "id": "e2e97aaa-de4c-4e18-9431-d99790773433",
              "name": "Volksbank Gruppe"
            },
            {"id": "bb7d223a-17d5-48af-a6ef-8a2bf5a4e5d9", "name": "Immo-Bank"},
            {
              "id": "e6819e7a-f663-414b-92ec-cf7c82d2f4e5",
              "name": "Bank Austria"
            },
            {
              "id": "eff103e6-843d-48b7-a6e6-fbd88f511b11",
              "name": "Easybank AG"
            },
            {
              "id": "25942cc9-617d-42a1-89ba-d1ab5a05770a",
              "name": "VR-BankBraunau"
            },
            {
              "id": "4a0a975b-0594-4b40-9068-39f77b3a91f9",
              "name": "Volkskreditbank"
            },
            {
              "id": "3fdc41fc-3d3d-4ee3-a1fe-cd79cfd58ea3",
              "name": "Erste Bank und Sparkassen"
            },
            {
              "id": "ba7199cc-f057-42f2-9856-2378abf21638",
              "name": "BAWAG P.S.K. Gruppe"
            }
          ],
          "key": "issuer",
          "type": "select"
        }
      ],
      "name": "EPS",
      "supportsRecurring": true,
      "type": "eps"
    },
    {
      "details": [
        {"key": "bic", "optional": true, "type": "text"}
      ],
      "name": "GiroPay",
      "supportsRecurring": true,
      "type": "giropay"
    },
    {
      "details": [
        {
          "items": [
            {"id": "1121", "name": "Test Issuer"},
            {"id": "1154", "name": "Test Issuer 5"},
            {"id": "1153", "name": "Test Issuer 4"},
            {"id": "1152", "name": "Test Issuer 3"},
            {"id": "1151", "name": "Test Issuer 2"},
            {"id": "1162", "name": "Test Issuer Cancelled"},
            {"id": "1161", "name": "Test Issuer Pending"},
            {"id": "1160", "name": "Test Issuer Refused"},
            {"id": "1159", "name": "Test Issuer 10"},
            {"id": "1158", "name": "Test Issuer 9"},
            {"id": "1157", "name": "Test Issuer 8"},
            {"id": "1156", "name": "Test Issuer 7"},
            {"id": "1155", "name": "Test Issuer 6"}
          ],
          "key": "issuer",
          "type": "select"
        }
      ],
      "name": "iDEAL",
      "supportsRecurring": true,
      "type": "ideal"
    },
    {
      "name": "Pay later with Klarna.",
      "supportsRecurring": true,
      "type": "klarna"
    },
    {
      "name": "Slice it with Klarna.",
      "supportsRecurring": true,
      "type": "klarna_account"
    },
    {
      "name": "Pay now with Klarna.",
      "supportsRecurring": true,
      "type": "klarna_paynow"
    },
    {"name": "Multibanco", "supportsRecurring": true, "type": "multibanco"},
    {"name": "Paysafecard", "supportsRecurring": true, "type": "paysafecard"},
    {"name": "Swish", "supportsRecurring": true, "type": "swish"},
    {"name": "Trustly", "supportsRecurring": true, "type": "trustly"},
    {
      "details": [
        {"key": "telephoneNumber", "optional": true, "type": "tel"}
      ],
      "name": "Vipps",
      "supportsRecurring": true,
      "type": "vipps"
    }
  ]
};

String clientKey = '10001XXXXXXXXXXXXXXXXXXXX';
