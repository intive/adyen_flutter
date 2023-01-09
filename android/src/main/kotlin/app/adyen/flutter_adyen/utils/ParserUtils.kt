package app.adyen.flutter_adyen.utils

import org.json.JSONObject

fun JSONObject.putAll(other: JSONObject): JSONObject {
    val keys = other.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = other.get(key)
        put(key, value)
    }
    return this
}