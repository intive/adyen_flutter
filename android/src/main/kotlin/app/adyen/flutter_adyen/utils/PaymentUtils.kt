package app.adyen.flutter_adyen.utils

import app.adyen.flutter_adyen.*
import app.adyen.flutter_adyen.network.requests.PaymentsRequestV69
import com.adyen.checkout.components.model.paymentmethods.Item
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
    shopperReference: String,
    amount: String,
    currency: String,
//    countryCode: String,
    merchantAccount: String,
    redirectUrl: String,
    isThreeds2Enabled: Boolean,
    isExecuteThreeD: Boolean,
    shopperEmail: String? = null,
    force3DS2Challenge: Boolean = true,
    threeDSAuthenticationOnly: Boolean = false
): PaymentsRequestV69 {
    val paymentsRequestData = PaymentsRequestDataV69(
        shopperReference = shopperReference,
        amount = getAmount(amount, currency),
        merchantAccount = merchantAccount,
        returnUrl = redirectUrl,
//        countryCode = countryCode,
        shopperIP = SHOPPER_IP,
        reference = getReference(),
        channel = CHANNEL,
        additionalData = getAdditionalDataV69(
            isThreeds2Enabled = isThreeds2Enabled,
            isExecuteThreeD = isExecuteThreeD
        ),
        lineItems = LINE_ITEMS,
        shopperEmail = shopperEmail,
        threeDSAuthenticationOnly = threeDSAuthenticationOnly,
        threeDS2RequestData = if (force3DS2Challenge) ThreeDS2RequestDataRequest() else null
    )

    return PaymentsRequestV69(paymentComponentData, paymentsRequestData)
}

private fun getAmount(amount: String, currency: String) = createAmount(amount.toInt(), currency)

private const val SHOPPER_IP = "142.12.31.22"
private const val CHANNEL = "android"
private val LINE_ITEMS = listOf(Item())
private fun getReference() = "android-test-components_${System.currentTimeMillis()}"
private fun getAdditionalDataV69(isThreeds2Enabled: Boolean, isExecuteThreeD: Boolean) =
    AdditionalDataV69(
        allow3DS2 = isThreeds2Enabled.toString(),
        executeThreeD = isExecuteThreeD.toString()
    )