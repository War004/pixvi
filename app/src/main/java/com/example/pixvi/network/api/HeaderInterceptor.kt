import android.os.Build
import com.example.pixvi.login.AuthManager
import com.example.pixvi.utils.PixivAuthUtils
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class HeaderInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // --- Get Dynamic Values ---
        val currentTime = PixivAuthUtils.getCurrentTimeFormatted()
        val clientHash = PixivAuthUtils.generateClientHash(currentTime)
        val currentAuthToken = AuthManager.getCurrentAccessToken() // Get token from manager

        // --- Other Headers ---
        // TODO: Replace hardcoded "6.144.0" with BuildConfig.VERSION_NAME if possible
        val appVersion = "6.144.0"
        val osVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL
        val osName = "Android"
        val userAgent = "PixivAndroidApp/$appVersion ($osName $osVersion; $deviceModel)"
        val locale = Locale.getDefault()
        val acceptLanguage = "${locale.language}_${locale.country}"
        val appAcceptLanguage = locale.language
        val referer = "https://app-api.pixiv.net/"

        // --- Build Request ---
        val requestBuilder = originalRequest.newBuilder()

        // Add all headers
        requestBuilder.header("Accept-Language", acceptLanguage)
        requestBuilder.header("app-accept-language", appAcceptLanguage)
        requestBuilder.header("App-OS", osName)
        requestBuilder.header("App-OS-Version", osVersion)
        requestBuilder.header("App-Version", appVersion)
        requestBuilder.header("User-Agent", userAgent)
        requestBuilder.header("X-Client-Time", currentTime)
        requestBuilder.header("X-Client-Hash", clientHash) // Always add hash
        requestBuilder.header("Referer",referer)

        // Conditionally add Authorization
        if (!currentAuthToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $currentAuthToken")
        }

        requestBuilder.method(originalRequest.method, originalRequest.body)
        val newRequest = requestBuilder.build()

        return chain.proceed(newRequest)
    }
}