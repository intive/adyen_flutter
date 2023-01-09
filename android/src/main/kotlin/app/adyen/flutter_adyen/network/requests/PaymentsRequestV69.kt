package app.adyen.flutter_adyen.network.requests

import org.json.JSONObject
import java.io.Serializable

/**
 * Data inside this class will not be sent as shown, instead paymentComponentData and requestData will
 * both be merged into the same JSON object.
 */
data class PaymentsRequestV69(
    val paymentComponentData: JSONObject,
    val requestData: PaymentsRequestDataV69
) : Serializable