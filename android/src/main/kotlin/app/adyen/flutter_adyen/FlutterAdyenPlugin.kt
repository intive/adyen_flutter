package app.adyen.flutter_adyen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.components.model.paymentmethods.Item
import com.adyen.checkout.components.model.payments.Amount
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.model.toStringPretty
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.dropin.service.DropInServiceResult
import com.adyen.checkout.redirect.RedirectComponent
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
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
import java.util.*

class FlutterAdyenPlugin :
    MethodCallHandler, PluginRegistry.ActivityResultListener, FlutterPlugin, ActivityAware {

    private var methodChannel: MethodChannel? = null

    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null

    var flutterResult: Result? = null

    companion object {

        const val CHANNEL_NAME = "flutter_adyen"

        /**
         * For EmbeddingV1
         */
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            FlutterAdyenPlugin().apply {
                onAttachedToEngine(registrar.messenger())
                activity = registrar.activity()
                addActivityResultListener(registrar)
            }
        }
    }

    override fun onMethodCall(call: MethodCall, res: Result) {
        when (call.method) {
            "openDropIn" -> {

                if (activity == null) {
                    res.error(
                        "1",
                        "Activity is null",
                        "The activity is probably not attached"
                    )
                    return
                }

                val nonNullActivity = activity!!

                val additionalData =
                    call.argument<Map<String, String>>("additionalData") ?: emptyMap()
                val paymentMethods = call.argument<String>("paymentMethods")
                val baseUrl = call.argument<String>("baseUrl")
                val clientKey = call.argument<String>("clientKey")
                val apiKey = call.argument<String>("publicKey")
                val amount = call.argument<String>("amount")
                val currency = call.argument<String>("currency")
                val env = call.argument<String>("environment")
                val lineItem = call.argument<Map<String, String>>("lineItem")
                val shopperReference = call.argument<String>("shopperReference")

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                val lineItemString = JSONObject(lineItem).toString()
                val additionalDataString = JSONObject(additionalData).toString()
                val localeString = call.argument<String>("locale") ?: "de_DE"
                val countryCode = localeString.split("_").last()

                /*
                Log.e("[Flutter Adyen]", "Client Key from Flutter: $clientKey")
                Log.e("[Flutter Adyen]", "Environment from Flutter: $env")
                Log.e("[Flutter Adyen]", "Locale String from Flutter: $localeString")
                Log.e("[Flutter Adyen]", "Locale String from Flutter: $paymentMethods")
                Log.e("[Flutter Adyen]", "Country Code from Flutter: $countryCode")
                Log.e("[Flutter Adyen]", "Base URL from Flutter: $baseUrl")
                Log.e("[Flutter Adyen]", "Currency from Flutter: $currency")
                Log.e("[Flutter Adyen]", "Shopper Reference from Flutter: $shopperReference")
                 */

                val environment = when (env) {
                    "LIVE_US" -> Environment.UNITED_STATES
                    "LIVE_AUSTRALIA" -> Environment.AUSTRALIA
                    "LIVE_EUROPE" -> Environment.EUROPE
                    else -> Environment.TEST
                }

                // Log.e("[Flutter Adyen] ENVIRONMENT", "Resolved environment: $environment")

                try {
                    val jsonObject = JSONObject(paymentMethods ?: "")
                    val paymentMethodsApiResponse =
                        PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObject)
                    val shopperLocale = Locale.GERMANY
                    // val shopperLocale = if (LocaleUtil.isValidLocale(locale)) locale else LocaleUtil.getLocale(nonNullActivity)
                    // Log.e("[Flutter Adyen] SHOPPER LOCALE", "Shopper Locale from localeString $localeString: $shopperLocale")
                    val cardConfiguration = CardConfiguration.Builder(nonNullActivity, clientKey!!)
                        .setHolderNameRequired(true)
                        .setShopperLocale(shopperLocale)
                        .setEnvironment(environment)
                        .build()

                    val sharedPref =
                        nonNullActivity.getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        remove("AdyenResultCode")
                        putString("baseUrl", baseUrl)
                        putString("amount", "$amount")
                        putString("countryCode", countryCode)
                        putString("currency", currency)
                        putString("lineItem", lineItemString)
                        putString("additionalData", additionalDataString)
                        putString("shopperReference", shopperReference)
                        putString("apiKey", apiKey)
                        commit()
                    }

                    val dropInConfiguration = DropInConfiguration.Builder(
                        nonNullActivity,
                        AdyenDropinService::class.java,
                        clientKey
                    )
                        .addCardConfiguration(cardConfiguration)
                        .setEnvironment(environment)
                        .build()

                    DropIn.startPayment(
                        nonNullActivity,
                        paymentMethodsApiResponse,
                        dropInConfiguration
                    )

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

    //region lifecycle
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (activity == null) return false

        val sharedPref = activity!!.getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val storedResultCode = sharedPref.getString("AdyenResultCode", "PAYMENT_CANCELLED")
        flutterResult?.success(storedResultCode)
        flutterResult = null
        return true
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(binding.binaryMessenger)
    }

    private fun onAttachedToEngine(messenger: BinaryMessenger) {
        this.methodChannel = MethodChannel(messenger, CHANNEL_NAME)
        this.methodChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        unbindActivityBinding()
        this.methodChannel = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        bindActivityBinding(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        unbindActivityBinding()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        bindActivityBinding(binding)
    }

    override fun onDetachedFromActivity() {
        unbindActivityBinding()
    }

    private fun bindActivityBinding(binding: ActivityPluginBinding) {
        this.activity = binding.activity
        this.activityBinding = binding
        addActivityResultListener(binding)
    }

    private fun unbindActivityBinding() {
        activityBinding?.removeActivityResultListener(this)
        this.activity = null
        this.activityBinding = null
    }

    private fun addActivityResultListener(activityBinding: ActivityPluginBinding) {
        activityBinding.addActivityResultListener(this)
    }

    private fun addActivityResultListener(registrar: PluginRegistry.Registrar) {
        registrar.addActivityResultListener(this)
    }
    //endregion
}

/**
 * This is just an example on how to make network calls on the [DropInService].
 * You should make the calls to your own servers and have additional data or processing if necessary.
 */
@Throws(JsonSyntaxException::class)
inline fun <reified T> Gson.fromJson(json: String): T? =
    fromJson<T>(json, object : TypeToken<T>() {}.type)

class AdyenDropinService : DropInService() {

    override fun makePaymentsCall(paymentComponentJson: JSONObject): DropInServiceResult {
        val sharedPref = getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val baseUrl = sharedPref.getString("baseUrl", "UNDEFINED_STR")
        val apiKey: String = sharedPref.getString("apiKey", "") ?: ""
        val amount = sharedPref.getString("amount", "UNDEFINED_STR")
//        val currency = sharedPref.getString("currency", "UNDEFINED_STR")
        val currency = "HKD"
        val countryCode = sharedPref.getString("countryCode", "DE")
        val lineItemString = sharedPref.getString("lineItem", "UNDEFINED_STR")
        val additionalDataString = sharedPref.getString("additionalData", "UNDEFINED_STR")
        val uuid: UUID = UUID.randomUUID()
        val reference: String = uuid.toString()
        val shopperReference = sharedPref.getString("shopperReference", null)

        val moshi = Moshi.Builder().build()
        val jsonAdapter = moshi.adapter(LineItem::class.java)
        val lineItem: LineItem? = jsonAdapter.fromJson(lineItemString ?: "")

        val gson = Gson()

        val additionalData =
            gson.fromJson<Map<String, String>>(additionalDataString ?: "") ?: emptyMap()
        val serializedPaymentComponentData =
            PaymentComponentData.SERIALIZER.deserialize(paymentComponentJson)

        if (serializedPaymentComponentData.paymentMethod == null)
            return DropInServiceResult.Error(errorMessage = "Empty payment data")

        val paymentsRequest = createPaymentRequestV69(
            paymentComponentData = paymentComponentJson,
            shopperReference = "",
            amount = amount ?: "",
            currency = currency ?: "",
//            countryCode = "HK",
            merchantAccount = "LegatoTechECOM",
            redirectUrl = RedirectComponent.getReturnUrl(applicationContext),
            isThreeds2Enabled = true,
            isExecuteThreeD = false,
            shopperEmail = null,
        )
        val paymentsRequestJson = serializePaymentsRequestV69(paymentsRequest)
        Log.e("TAG", "paymentsRequestJson $paymentsRequestJson")
        val requestBody =
            RequestBody.create(MediaType.parse("application/json"), paymentsRequestJson.toString())

        val headers: HashMap<String, String> = HashMap()
        headers["x-API-key"] = apiKey
        headers["content-type"] = "application/json"
        val re = paymentsRequest.combineToJSONObject()
        val call = getService(headers, baseUrl ?: "").payments(re)
        call.request().headers()
        return try {
            val response = call.execute()
            val paymentsResponse = response.body()
            return handleResponse(paymentsResponse)
        } catch (e: IOException) {
            with(sharedPref.edit()) {
                putString("AdyenResultCode", "ERROR")
                commit()
            }
            DropInServiceResult.Error(errorMessage = "IOException")
        }
    }

    override fun makeDetailsCall(actionComponentJson: JSONObject): DropInServiceResult {
        val sharedPref = getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val apiKey: String = sharedPref.getString("apiKey", "") ?: ""
        val baseUrl = sharedPref.getString("baseUrl", "UNDEFINED_STR")
        val requestBody =
            RequestBody.create(MediaType.parse("application/json"), actionComponentJson.toString())
        val headers: HashMap<String, String> = HashMap()
        headers["x-API-key"] = apiKey
        headers["content-type"] = "application/json"
        val call = getService(headers, baseUrl ?: "").details(actionComponentJson)
        return try {
            val response = call.execute()
            val paymentsResponse = response.body()
            return handleResponse(paymentsResponse)
        } catch (e: IOException) {
            with(sharedPref.edit()) {
                putString("AdyenResultCode", "ERROR")
                commit()
            }
            DropInServiceResult.Error(errorMessage = "IOException")
        }
    }

    @Suppress("NestedBlockDepth")
    private fun handleResponse(detailsResponse: JSONObject?): DropInServiceResult {
        val sharedPref = getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        return if (detailsResponse != null) {
            if (detailsResponse.has("action")) {
                val action = Action.SERIALIZER.deserialize(detailsResponse.getJSONObject("action"))
                with(sharedPref.edit()) {
                    putString("AdyenResultCode", action.toString())
                    commit()
                }
                DropInServiceResult.Action(action)
            } else {
                Log.e("TAG", "Final result - ${detailsResponse.toStringPretty()}")

                val resultCode = if (detailsResponse.has("resultCode")) {
                    detailsResponse.get("resultCode").toString()
                } else {
                    "EMPTY"
                }
                with(sharedPref.edit()) {
                    putString("AdyenResultCode", resultCode)
                    commit()
                }
                DropInServiceResult.Finished(resultCode)
            }
        } else {
            with(sharedPref.edit()) {
                putString("AdyenResultCode", "ERROR")
                commit()
            }
            DropInServiceResult.Error(reason = "IOException")
        }
    }
}

fun createPaymentsRequest(
    context: Context, lineItem: LineItem?,
    paymentComponentData: PaymentComponentData<out PaymentMethodDetails>,
    amount: String, currency: String,
    reference: String, shopperReference: String?,
    countryCode: String,
    additionalData: Map<String, String>
): PaymentsRequest {
    @Suppress("UsePropertyAccessSyntax")
    return PaymentsRequest(
        payment = Payment(
            paymentComponentData.getPaymentMethod() as PaymentMethodDetails,
            countryCode,
            paymentComponentData.isStorePaymentMethodEnable,
            getAmount(amount, currency),
            reference,
            RedirectComponent.getReturnUrl(context),
            lineItems = listOf(lineItem),
            shopperReference = shopperReference
        ),
        additionalData = additionalData
    )
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

data class PaymentsRequestDataV69(
    val shopperReference: String,
    val amount: Amount,
//    val countryCode: String,
    val merchantAccount: String,
    val returnUrl: String,
    val additionalData: AdditionalDataV69,
    val threeDSAuthenticationOnly: Boolean,
    val shopperIP: String,
    val reference: String,
    val channel: String,
    val lineItems: List<Item>,
    val shopperEmail: String? = null,
    val threeDS2RequestData: ThreeDS2RequestDataRequest?
)

data class ThreeDS2RequestDataRequest(
    val deviceChannel: String = "app",
    val challengeIndicator: String = "requestChallenge"
)

private fun getAmount(amount: String, currency: String) = createAmount(amount.toInt(), currency)

fun createAmount(value: Int, currency: String): Amount {
    val amount = Amount()
    amount.currency = currency
    amount.value = value
    return amount
}

//region data classes
data class Payment(
    val paymentMethod: PaymentMethodDetails,
    val countryCode: String = "DE",
    val storePaymentMethod: Boolean,
    val amount: Amount,
    val reference: String,
    val returnUrl: String,
    val channel: String = "Android",
    val lineItems: List<LineItem?>,
    val additionalData: AdditionalData = AdditionalData(allow3DS2 = "true"),
    val shopperReference: String?
) : Serializable

data class PaymentsRequest(
    val payment: Payment,
    val additionalData: Map<String, String>
) : Serializable

/**
 * Data inside this class will not be sent as shown, instead paymentComponentData and requestData will
 * both be merged into the same JSON object. Check [PaymentsRepositoryImpl] for implementation.
 */
data class PaymentsRequestV69(
    val paymentComponentData: JSONObject,
    val requestData: PaymentsRequestDataV69
) : Serializable

data class LineItem(
    val id: String,
    val description: String
) : Serializable

data class AdditionalData(val allow3DS2: String = "true")
//endregion


data class AdditionalDataV69(
    val allow3DS2: String = "false",
    val executeThreeD: String = "false"
)


private fun serializePaymentsRequest(paymentsRequest: PaymentsRequest): JSONObject {
    val gson = Gson()
    val jsonString = gson.toJson(paymentsRequest)
    val request = JSONObject(jsonString)
    print(request)
    return request
}

private fun serializePaymentsRequestV69(paymentsRequest: PaymentsRequestV69): JSONObject {
    val gson = Gson()
    val jsonString = gson.toJson(paymentsRequest)
    val request = JSONObject(jsonString)
    print(request)
    return request
}

private fun PaymentsRequestV69.combineToJSONObject(): JSONObject {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(PaymentsRequestDataV69::class.java)
    val requestDataJson = JSONObject(adapter.toJson(this.requestData))

    return requestDataJson
        // This will override any already existing fields in requestDataJson
        .putAll(this.paymentComponentData)
}

private fun JSONObject.putAll(other: JSONObject): JSONObject {
    val keys = other.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = other.get(key)
        put(key, value)
    }
    return this
}

private const val SHOPPER_IP = "142.12.31.22"
private const val CHANNEL = "android"
private val LINE_ITEMS = listOf(Item())
private fun getReference() = "android-test-components_${System.currentTimeMillis()}"
private fun getAdditionalDataV69(isThreeds2Enabled: Boolean, isExecuteThreeD: Boolean) =
    AdditionalDataV69(
        allow3DS2 = isThreeds2Enabled.toString(),
        executeThreeD = isExecuteThreeD.toString()
    )