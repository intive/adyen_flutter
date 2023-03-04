package app.adyen.flutter_adyen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import app.adyen.flutter_adyen.network.apis.getService
import app.adyen.flutter_adyen.utils.combineToJSONObject
import app.adyen.flutter_adyen.utils.createPaymentRequestV69
import app.adyen.flutter_adyen.utils.getAmount
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.response.Action
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.model.toStringPretty
import com.adyen.checkout.core.util.LocaleUtil
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.dropin.service.DropInService
import com.adyen.checkout.dropin.service.DropInServiceResult
import com.adyen.checkout.redirect.RedirectComponent
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
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
                val accessToken = call.argument<String>("accessToken")
                // https://docs.adyen.com/development-resources/currency-codes HKD
                val amount = call.argument<String>("amount").plus("00")
                val currency = call.argument<String>("currency")
                val env = call.argument<String>("environment")
                val lineItem = call.argument<ArrayList<Map<String, String>>>("lineItem")
                val shopperReference = call.argument<String>("shopperReference")
                val locale = call.argument<String>("locale") ?: ""

                val returnUrl = call.argument<String>("returnUrl")
                val merchantAccount = call.argument<String>("merchantAccount")
                val reference = call.argument<String>("reference")
                val threeDS2RequestData =
                    call.argument<Map<String, String>>("threeDS2RequestData") ?: emptyMap()
                val storePaymentMethod = call.argument<Boolean>("storePaymentMethod")
                val additionalParamsData =
                    call.argument<Map<String, String>>("additionalParams") ?: emptyMap()

                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
//                val lineItemString = JSONObject(lineItem).toString()
                val additionalDataString = JSONObject(additionalData).toString()
                val threeDS2RequestDataString = JSONObject(threeDS2RequestData).toString()
                val additionalParamsString = JSONObject(additionalParamsData).toString()
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
                    val cardConfiguration = CardConfiguration.Builder(nonNullActivity, clientKey!!)
                        .setHolderNameRequired(true)
                        .setShopperLocale(LocaleUtil.getLocale(nonNullActivity))
                        .setEnvironment(environment)
                        .build()

                    val sharedPref =
                        nonNullActivity.getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        remove("AdyenResultCode")
                        putString("baseUrl", baseUrl)
                        putString("amount", amount)
                        putString("countryCode", countryCode)
                        putString("locale", locale)
                        putString("currency", currency)
                        putString("lineItem", Gson().toJson(lineItem))
                        putString("additionalData", additionalDataString)
                        putString("shopperReference", shopperReference)
                        putString("apiKey", apiKey)
                        putString("accessToken", accessToken)
                        putString("returnUrl", returnUrl)
                        putString("merchantAccount", merchantAccount)
                        putString("reference", reference)
                        putString("threeDS2RequestData", threeDS2RequestDataString)
                        putString("additionalParams", additionalParamsString)
                        putBoolean("storePaymentMethod", storePaymentMethod ?: false)
                        commit()
                    }

                    val dropInConfiguration = DropInConfiguration.Builder(
                        nonNullActivity,
                        AdyenDropinService::class.java,
                        clientKey
                    ).addCardConfiguration(cardConfiguration)
                        .setAmount(getAmount(amount ?: "", currency ?: ""))
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

        val sharedPref = activity?.getSharedPreferences("ADYEN", Context.MODE_PRIVATE)
        val storedResultCode = sharedPref?.getString("AdyenResultCode", "PAYMENT_CANCELLED")
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
        val merchantAccount = sharedPref.getString("merchantAccount", "UNDEFINED_STR")
        val apiKey: String = sharedPref.getString("apiKey", "") ?: ""
        val accessToken: String = sharedPref.getString("accessToken", "") ?: ""
        val amount = sharedPref.getString("amount", "UNDEFINED_STR")
        val currency = sharedPref.getString("currency", "UNDEFINED_STR")
        val locale = sharedPref.getString("locale", "UNDEFINED_STR")
        val lineItemString = sharedPref.getString("lineItem", "[]")
        val additionalDataString = sharedPref.getString("additionalData", "UNDEFINED_STR")
        val threeDS2RequestDataString = sharedPref.getString("threeDS2RequestData", "UNDEFINED_STR")
        val shopperReference = sharedPref.getString("shopperReference", null)
        val reference = sharedPref.getString("reference", "UNDEFINED_STR")

        val gson = Gson()
        val item: List<Map<String, String>>? =
            gson.fromJson<List<Map<String, String>>>(lineItemString ?: "")

        val additionalData =
            gson.fromJson<Map<String, String>>(additionalDataString ?: "") ?: emptyMap()
        val threeDS2RequestData =
            gson.fromJson<Map<String, String>>(threeDS2RequestDataString ?: "") ?: emptyMap()
        val serializedPaymentComponentData =
            PaymentComponentData.SERIALIZER.deserialize(paymentComponentJson)

        if (serializedPaymentComponentData.paymentMethod == null)
            return DropInServiceResult.Error(errorMessage = "Empty payment data")

        val paymentsRequest = createPaymentRequestV69(
            paymentComponentData = paymentComponentJson,
            shopperReference = shopperReference,
            amount = amount ?: "",
            currency = currency ?: "",
            countryCode = locale,
            merchantAccount = merchantAccount,
            redirectUrl = RedirectComponent.getReturnUrl(applicationContext),
            threeDS2RequestData = threeDS2RequestData,
            shopperEmail = null,
            additionalData = additionalData,
            reference = reference ?: "",
            items = item ?: emptyList()
        )

        val headers: HashMap<String, String> = HashMap()
        headers["x-API-key"] = apiKey
        headers["Authorization"] = "Bearer $accessToken"
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
        val accessToken: String = sharedPref.getString("accessToken", "") ?: ""
        val baseUrl = sharedPref.getString("baseUrl", "UNDEFINED_STR")
        val lineItemString = sharedPref.getString("lineItem", "[]")
        val itemArr = JSONArray(lineItemString ?: "")
        if (itemArr.length() != 0)
            actionComponentJson.put("lineItems", itemArr)

        val headers: HashMap<String, String> = HashMap()
        headers["x-API-key"] = apiKey
        headers["Authorization"] = "Bearer $accessToken"
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