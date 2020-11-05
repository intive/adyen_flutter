package app.adyen.flutter_adyen

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.adyen.checkout.base.model.PaymentMethodsApiResponse
import com.adyen.checkout.base.model.payments.Amount
import com.adyen.checkout.base.model.payments.request.*
import com.adyen.checkout.base.model.payments.response.Action
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.util.LocaleUtil
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.service.CallResult
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.redirect.RedirectComponent
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.io.Serializable


class FlutterAdyenPlugin(private val activity: Activity) : MethodCallHandler, PluginRegistry.ActivityResultListener, PluginRegistry.NewIntentListener {
    var flutterResult: Result? = null
    var resultCode: String? = null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_adyen")
            val plugin = FlutterAdyenPlugin(registrar.activity())
            channel.setMethodCallHandler(plugin)
            registrar.addActivityResultListener(plugin)
            registrar.addNewIntentListener(plugin)
        }
    }

    override fun onMethodCall(call: MethodCall, res: Result) {
        when (call.method) {
            "openDropIn" -> {

                val paymentMethods = call.argument<String>("paymentMethods")
                val baseUrl = call.argument<String>("baseUrl")
                val clientKey = call.argument<String>("clientKey")
                val publicKey = call.argument<String>("publicKey")
                val amount = call.argument<String>("amount")
                val currency = call.argument<String>("currency")
                val env = call.argument<String>("environment")
                val lineItem = call.argument<Map<String, String>>("lineItem")

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                val lineItemString = JSONObject(lineItem).toString()
                val localeString = call.argument<String>("locale")

                var environment = Environment.TEST
                if (env == "LIVE_US") {
                    environment = Environment.UNITED_STATES
                } else if (env == "LIVE_AUSTRALIA") {
                    environment = Environment.AUSTRALIA
                } else if (env == "LIVE_EUROPE") {
                    environment = Environment.EUROPE
                }

                try {
                    val jsonObject = JSONObject(paymentMethods ?: "")
                    val paymentMethodsApiResponse = PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject)
                    val shopperLocale = LocaleUtil.fromLanguageTag(localeString ?: "")
                    val cardConfiguration = CardConfiguration.Builder(activity)
                            .setPublicKey(publicKey ?: "")
                            .setShopperLocale(shopperLocale)
                            .setEnvironment(environment)
                            .build()

                    val resultIntent = Intent(activity, activity::class.java)
                    resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                    val sharedPref = activity.getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("baseUrl", baseUrl)
                        putString("amount", "$amount")
                        putString("currency", currency)
                        putString("lineItem", lineItemString)
                        commit()
                    }

                    val dropInConfiguration = DropInConfiguration.Builder(activity, resultIntent, AdyenDropinService::class.java)
                            .setClientKey(clientKey ?: "")
                            .addCardConfiguration(cardConfiguration)
                            .build()
                    DropIn.startPayment(activity, paymentMethodsApiResponse, dropInConfiguration)
                    flutterResult = res
                } catch (e: Throwable) {
                    res.error("PAYMENT_ERROR", "${e.printStackTrace()}", "")
                }


            }
            else -> {
                res.notImplemented()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (this.resultCode == null) {
            flutterResult?.error("PAYMENT_CANCELLED", "PAYMENT_CANCELLED", "PAYMENT_CANCELLED")
            return true
        }
        return true
    }

    override fun onNewIntent(intent: Intent?): Boolean {
        if (intent?.hasExtra(DropIn.RESULT_KEY) == true) {
            resultCode = intent.getStringExtra(DropIn.RESULT_KEY)
            flutterResult?.success(resultCode)
        }
        return true
    }
}

/**
 * This is just an example on how to make network calls on the [DropInService].
 * You should make the calls to your own servers and have additional data or processing if necessary.
 */
class AdyenDropinService : DropInService() {

    companion object {
        private val TAG = LogUtil.getTag()
    }

    override fun makePaymentsCall(paymentComponentData: JSONObject): CallResult {
        val sharedPref = getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val baseUrl = sharedPref.getString("baseUrl", "UNDEFINED_STR")
        val amount = sharedPref.getString("amount", "UNDEFINED_STR")
        val currency = sharedPref.getString("currency", "UNDEFINED_STR")
        val lineItemString = sharedPref.getString("lineItem", "UNDEFINED_STR")

        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(LineItem::class.java)
        val lineItem: LineItem? = jsonAdapter.fromJson(lineItemString ?: "")

        val serializedPaymentComponentData = PaymentComponentData.SERIALIZER.deserialize(paymentComponentData)

        if (serializedPaymentComponentData.paymentMethod == null)
            return CallResult(CallResult.ResultType.ERROR, "Empty payment data")

        val paymentsRequest = createPaymentsRequest(this@AdyenDropinService, lineItem, serializedPaymentComponentData, amount
                ?: "", currency ?: "")
        val paymentsRequestJson = serializePaymentsRequest(paymentsRequest)

        val requestBody = RequestBody.create(MediaType.parse("application/json"), paymentsRequestJson.toString())

        val headers: HashMap<String, String> = HashMap()
        val call = getService(headers, baseUrl ?: "").payments(requestBody)
        call.request().headers()
        return try {
            val response = call.execute()
            val paymentsResponse = response.body()

            if (response.isSuccessful && paymentsResponse != null) {
                if (paymentsResponse.action != null) {
                    CallResult(CallResult.ResultType.ACTION, Action.SERIALIZER.serialize(paymentsResponse.action).toString())
                } else {
                    if (paymentsResponse.resultCode != null &&
                            (paymentsResponse.resultCode == "Authorised" || paymentsResponse.resultCode == "Received" || paymentsResponse.resultCode == "Pending")) {
                        CallResult(CallResult.ResultType.FINISHED, paymentsResponse.resultCode)
                    } else {
                        CallResult(CallResult.ResultType.FINISHED, paymentsResponse.resultCode
                                ?: "EMPTY")
                    }
                }
            } else {
                CallResult(CallResult.ResultType.ERROR, "IOException")
            }
        } catch (e: IOException) {
            CallResult(CallResult.ResultType.ERROR, "IOException")
        }
    }

    override fun makeDetailsCall(actionComponentData: JSONObject): CallResult {
        val sharedPref = getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val baseUrl = sharedPref.getString("baseUrl", "UNDEFINED_STR")
        val requestBody = RequestBody.create(MediaType.parse("application/json"), actionComponentData.toString())
        val headers: HashMap<String, String> = HashMap()

        val call = getService(headers, baseUrl ?: "").details(requestBody)
        return try {
            val response = call.execute()
            val detailsResponse = response.body()
            if (response.isSuccessful && detailsResponse != null) {
                if (detailsResponse.resultCode != null &&
                        (detailsResponse.resultCode == "Authorised" || detailsResponse.resultCode == "Received" || detailsResponse.resultCode == "Pending")) {
                    CallResult(CallResult.ResultType.FINISHED, detailsResponse.resultCode)
                } else {
                    CallResult(CallResult.ResultType.FINISHED, detailsResponse.resultCode
                            ?: "EMPTY")
                }
            } else {
                CallResult(CallResult.ResultType.ERROR, "IOException")
            }
        } catch (e: IOException) {
            CallResult(CallResult.ResultType.ERROR, "IOException")
        }
    }
}


fun createPaymentsRequest(context: Context, lineItem: LineItem?, paymentComponentData: PaymentComponentData<out PaymentMethodDetails>, amount: String, currency: String): PaymentsRequest {
    @Suppress("UsePropertyAccessSyntax")
    return PaymentsRequest(
            paymentComponentData.getPaymentMethod() as PaymentMethodDetails,
            paymentComponentData.isStorePaymentMethodEnable,
            getAmount(amount, currency),
            RedirectComponent.getReturnUrl(context),
            lineItems = listOf(lineItem)
    )
}

private fun getAmount(amount: String, currency: String) = createAmount(amount.toInt(), currency)

fun createAmount(value: Int, currency: String): Amount {
    val amount = Amount()
    amount.currency = currency
    amount.value = value
    return amount
}

data class PaymentsRequest(
        val paymentMethod: PaymentMethodDetails,
        val storePaymentMethod: Boolean,
        val amount: Amount,
        val returnUrl: String,
        val channel: String = "android",
        val lineItems: List<LineItem?>,
        val additionalData: AdditionalData = AdditionalData(allow3DS2 = "false")
)
data class LineItem(
        val id: String,
        val description: String
): Serializable

data class AdditionalData(val allow3DS2: String = "false")

private fun serializePaymentsRequest(paymentsRequest: PaymentsRequest): JSONObject {
    val moshi = Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(PaymentMethodDetails::class.java, PaymentMethodDetails.TYPE)
                    .withSubtype(CardPaymentMethod::class.java, CardPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(IdealPaymentMethod::class.java, IdealPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(EPSPaymentMethod::class.java, EPSPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(DotpayPaymentMethod::class.java, DotpayPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(EntercashPaymentMethod::class.java, EntercashPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(OpenBankingPaymentMethod::class.java, OpenBankingPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(GooglePayPaymentMethod::class.java, GooglePayPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(GenericPaymentMethod::class.java, "other")
            )
            .build()
    val jsonAdapter = moshi.adapter(PaymentsRequest::class.java)
    val requestString = jsonAdapter.toJson(paymentsRequest)
    val request = JSONObject(requestString)

    request.remove("paymentMethod")
    request.put("paymentMethod", PaymentMethodDetails.SERIALIZER.serialize(paymentsRequest.paymentMethod))

    return request
}