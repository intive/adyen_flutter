package app.adyen.flutter_adyen.network.intercepters

import okhttp3.Interceptor
import okhttp3.Response

class HeaderInterceptor(private val headers: HashMap<String, String>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        val builder = request().newBuilder()
        headers.keys.forEach { builder.addHeader(it, headers[it] ?: "") }
        proceed(builder.build())
    }
}