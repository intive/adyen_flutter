package app.adyen.flutter_adyen.network.requests

import com.adyen.checkout.components.model.paymentmethods.Item
import com.adyen.checkout.components.model.payments.Amount

data class PaymentsRequestDataV69(
    val shopperReference: String? = null,
    val amount: Amount,
    val countryCode: String?,
    val merchantAccount: String?,
    val returnUrl: String,
    val additionalData: Map<String, String>,
    val threeDSAuthenticationOnly: Boolean,
    val shopperIP: String? = null,
    val reference: String,
    val channel: String,
    val lineItems: List<Item>,
    val shopperEmail: String? = null,
    val threeDS2RequestData: Map<String, String>?
)