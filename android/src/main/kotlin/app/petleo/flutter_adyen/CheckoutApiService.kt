/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by caiof on 11/2/2019.
 */

package app.petleo.flutter_adyen

import com.adyen.checkout.base.model.paymentmethods.InputDetail
import com.adyen.checkout.base.model.payments.request.*
import com.adyen.checkout.base.model.payments.response.*
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface CheckoutApiService {
    @POST("payments/")
    fun payments(@Body paymentsRequest: RequestBody): Call<PaymentsApiResponse>

    @POST("payments/details/")
    fun details(@Body detailsRequest: RequestBody): Call<PaymentsApiResponse>
}

class HeaderInterceptor(private val headers: HashMap<String, String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val builder = request().newBuilder()
        headers.keys.forEach { builder.addHeader(it, headers[it] ?: "") }
        proceed(builder.build())
    }
}

fun getService(headers: HashMap<String, String>, baseUrl: String): CheckoutApiService {
    val moshi = Moshi.Builder()
            .add(PolymorphicJsonAdapterFactory.of(PaymentMethodDetails::class.java, PaymentMethodDetails.TYPE)
                    .withSubtype(CardPaymentMethod::class.java, CardPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(IdealPaymentMethod::class.java, IdealPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(MolpayPaymentMethod::class.java, MolpayPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(EPSPaymentMethod::class.java, EPSPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(DotpayPaymentMethod::class.java, DotpayPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(EntercashPaymentMethod::class.java, EntercashPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(OpenBankingPaymentMethod::class.java, OpenBankingPaymentMethod.PAYMENT_METHOD_TYPE)
                    .withSubtype(GenericPaymentMethod::class.java, "other")
            )
            .add(PolymorphicJsonAdapterFactory.of(Action::class.java, Action.TYPE)
                    .withSubtype(RedirectAction::class.java, RedirectAction.ACTION_TYPE)
                    .withSubtype(Threeds2FingerprintAction::class.java, Threeds2FingerprintAction.ACTION_TYPE)
                    .withSubtype(Threeds2ChallengeAction::class.java, Threeds2ChallengeAction.ACTION_TYPE)
                    .withSubtype(QrCodeAction::class.java, QrCodeAction.ACTION_TYPE)
                    .withSubtype(VoucherAction::class.java, VoucherAction.ACTION_TYPE)
            )
            .build()
    val converter = MoshiConverterFactory.create(moshi)

    val client = OkHttpClient.Builder().addInterceptor(HeaderInterceptor(headers)).build()

    val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(converter)
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(client)
            .build()

    return retrofit.create(CheckoutApiService::class.java)
}

data class PaymentsApiResponse(
        val resultCode: String? = null,
        val paymentData: String? = null,
        val details: List<InputDetail>? = null,
        val action: Action? = null
)