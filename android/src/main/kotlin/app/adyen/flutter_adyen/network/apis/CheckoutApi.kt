package app.adyen.flutter_adyen.network.apis

import app.adyen.flutter_adyen.network.adapters.JSONObjectAdapter
import app.adyen.flutter_adyen.network.intercepters.HeaderInterceptor
import com.adyen.checkout.components.model.payments.request.*
import com.adyen.checkout.components.model.payments.response.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckoutApi {
    @POST("payments")
    fun payments(@Body paymentsRequest: JSONObject): Call<JSONObject>

    @POST("payments/details")
    fun details(@Body detailsRequest: JSONObject): Call<JSONObject>
}

fun getService(headers: HashMap<String, String>, baseUrl: String): CheckoutApi {
    val moshi = Moshi.Builder()
        .add(
            PolymorphicJsonAdapterFactory.of(
                PaymentMethodDetails::class.java,
                PaymentMethodDetails.TYPE
            )
                .withSubtype(CardPaymentMethod::class.java, CardPaymentMethod.PAYMENT_METHOD_TYPE)
                .withSubtype(IdealPaymentMethod::class.java, IdealPaymentMethod.PAYMENT_METHOD_TYPE)
                .withSubtype(EPSPaymentMethod::class.java, EPSPaymentMethod.PAYMENT_METHOD_TYPE)
                .withSubtype(
                    DotpayPaymentMethod::class.java,
                    DotpayPaymentMethod.PAYMENT_METHOD_TYPE
                )
                .withSubtype(
                    EntercashPaymentMethod::class.java,
                    EntercashPaymentMethod.PAYMENT_METHOD_TYPE
                )
                .withSubtype(
                    OpenBankingPaymentMethod::class.java,
                    OpenBankingPaymentMethod.PAYMENT_METHOD_TYPE
                )
                .withSubtype(GenericPaymentMethod::class.java, "other")
        )
        .add(
            PolymorphicJsonAdapterFactory.of(Action::class.java, Action.TYPE)
                .withSubtype(RedirectAction::class.java, RedirectAction.ACTION_TYPE)
                .withSubtype(
                    Threeds2FingerprintAction::class.java,
                    Threeds2FingerprintAction.ACTION_TYPE
                )
                .withSubtype(
                    Threeds2ChallengeAction::class.java,
                    Threeds2ChallengeAction.ACTION_TYPE
                )
                .withSubtype(QrCodeAction::class.java, QrCodeAction.ACTION_TYPE)
                .withSubtype(VoucherAction::class.java, VoucherAction.ACTION_TYPE)
        )
        .add(JSONObjectAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()
    val converter = MoshiConverterFactory.create(moshi)

    val client = OkHttpClient.Builder().addInterceptor(HeaderInterceptor(headers)).build()

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converter)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .client(client)
        .build()

    return retrofit.create(CheckoutApi::class.java)
}