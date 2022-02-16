package app.adyen.flutter_adyen_example

import android.os.Bundle
import app.adyen.flutter_adyen.FlutterAdyenPlugin

import io.flutter.app.FlutterActivity

class EmbeddingV1Activity: FlutterActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlutterAdyenPlugin.registerWith(registrarFor("app.adyen.flutter_adyen.FlutterAdyenPlugin"))
    }
}
