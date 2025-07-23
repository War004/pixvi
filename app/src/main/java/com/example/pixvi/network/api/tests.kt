import okhttp3.*
import java.io.IOException


//Not an actual test

fun makeRequestAndDisplayRealError() {
    val client = OkHttpClient()

    val acceptLanguage = "en-US"
    val appAcceptLanguage = "en-US"
    val osName = "Android"
    val osVersion = "12" // Example OS Version
    val appVersion = "6.137.0" // Example App Version
    val userAgent = "PixivAndroidApp/6.137.0 (Android 9; SM-S908E)" // Example User Agent
    val currentTime = "2025-05-11T00:07:34+05:30" // Use a dynamically generated current time for real tests
    val clientHash = "96a69cefa4fd4451368852440b98a291" // Use a dynamically generated hash for real tests
    val referer = "https://app-api.pixiv.net/"
    val bearerToken = "oL2wrKrlLI4glJ1e6MVL4tfWfRvbweB4yyMXtuCDM2g" // This is your potentially expired/invalid token

    val request = Request.Builder()
        .url("https://app-api.pixiv.net/v1/illust/recommended?filter=for_android&include_ranking_illusts=true&include_privacy_policy=true")
        .header("Accept-Language", acceptLanguage)
        .header("app-accept-language", appAcceptLanguage)
        .header("App-OS", osName)
        .header("App-OS-Version", osVersion)
        .header("App-Version", appVersion)
        .header("User-Agent", userAgent)
        .header("X-Client-Time", currentTime)
        .header("X-Client-Hash", clientHash)
        .header("Authorization", "Bearer $bearerToken")
        .header("Referer", referer)
        .build()

    println("--- Sending Request ---")
    println("URL: ${request.url}")
    println("Headers: ${request.headers}")
    println("-----------------------")

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("--- Request Failed ---")
            e.printStackTrace()
            println("----------------------")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val responseBodyString = it.body?.string() ?: "[No Body]"
                if (!it.isSuccessful) {
                    println("--- Received Error Response ---")
                    println("Code: ${it.code}")
                    println("Message: ${it.message}")
                    println("Headers:\n${it.headers}")
                    println("Body:\n$responseBodyString")
                    println("-----------------------------")
                } else {
                    println("--- Received Successful Response ---")
                    println("Code: ${it.code}")
                    println("Headers:\n${it.headers}")
                    println("Body:\n$responseBodyString")
                    println("--------------------------------")
                }
            }
        }
    })
}


fun main() {
    makeRequestAndDisplayRealError()
}