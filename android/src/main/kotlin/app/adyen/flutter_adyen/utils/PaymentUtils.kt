package app.adyen.flutter_adyen.utils

import app.adyen.flutter_adyen.*
import app.adyen.flutter_adyen.network.requests.PaymentsRequestDataV69
import app.adyen.flutter_adyen.network.requests.PaymentsRequestV69
import com.adyen.checkout.components.model.payments.Amount
import com.google.gson.Gson
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject

fun PaymentsRequestV69.combineToJSONObject(): JSONObject {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(PaymentsRequestDataV69::class.java)
    val requestDataJson = JSONObject(adapter.toJson(this.requestData))

    return requestDataJson
        // This will override any already existing fields in requestDataJson
        .putAll(this.paymentComponentData)
}

fun PaymentsRequestV69.serializePaymentsRequestV69(): JSONObject {
    val gson = Gson()
    val jsonString = gson.toJson(this)
    val request = JSONObject(jsonString)
    print(request)
    return request
}

@Suppress("LongParameterList")
fun createPaymentRequestV69(
    paymentComponentData: JSONObject,
    shopperReference: String? = null,
    amount: String,
    currency: String,
    countryCode: String?,
    merchantAccount: String?,
    redirectUrl: String,
    shopperEmail: String? = null,
    force3DS2Challenge: Boolean = true,
    threeDSAuthenticationOnly: Boolean = false,
    additionalData: Map<String, String>,
    threeDS2RequestData: Map<String, String>,
    reference: String,
    items: List<Map<String, String>>,
): PaymentsRequestV69 {
    val paymentsRequestData = PaymentsRequestDataV69(
        shopperReference = shopperReference,
        amount = getAmount(amount, currency),
        merchantAccount = merchantAccount,
        returnUrl = redirectUrl,
        countryCode = countryCode,
//        shopperIP = if(BuildConfig.DEBUG) SHOPPER_IP else null,
        reference = reference,
        channel = CHANNEL,
        additionalData = additionalData,
        lineItems = items,
        shopperEmail = shopperEmail,
        threeDSAuthenticationOnly = threeDSAuthenticationOnly,
        threeDS2RequestData = if (force3DS2Challenge) threeDS2RequestData else null
    )

    return PaymentsRequestV69(paymentComponentData, paymentsRequestData)
}

fun getAmount(amount: String, currency: String) = createAmount(amount.toInt(), currency)

fun createAmount(value: Int, currency: String): Amount {
    val amount = Amount()
    amount.currency = currency
    amount.value = value
    return amount
}

private const val SHOPPER_IP = "142.12.31.22"
private const val CHANNEL = "android"