package com.cryptic.piyek

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.cryptic.piyek.core.DominantColorExtractor
import com.cryptic.piyek.core.database.AppDatabase
import com.cryptic.piyek.core.network.NetworkResultCallAdapterFactory
import com.cryptic.piyek.core.tink.TinkManager
import com.cryptic.piyek.core.tink.TinkMethod
import com.cryptic.piyek.feature.onboarding.OnboardingManager
import com.cryptic.piyek.core.auth.data.local.db.OAuthUserDao
import com.cryptic.piyek.core.auth.data.local.token.TokenManager
import com.cryptic.piyek.core.auth.data.local.token.TokenStorage
import com.cryptic.piyek.core.auth.data.remote.OAuthApiService
import com.cryptic.piyek.core.auth.data.repository.OAuthUserRepositoryImpl
import com.cryptic.piyek.core.auth.domain.repository.OAuthUserRepository
import com.cryptic.piyek.core.database.dataStore
import com.cryptic.piyek.core.network.interceptor.AuthInterceptor
import com.cryptic.piyek.core.network.interceptor.ImageInterceptor
import com.cryptic.piyek.feature.onboarding.domain.usecase.OAuthIntentProcessor
import com.cryptic.piyek.core.auth.SessionManager
import com.cryptic.piyek.core.content.data.model.ArtworkContentList
import com.cryptic.piyek.core.content.data.model.RecommendationNonNovelPara
import com.cryptic.piyek.core.content.domain.repo.CoreContentApiRepo
import com.cryptic.piyek.core.content.illust.data.remote.ILLustApiService
import com.cryptic.piyek.core.content.illust.data.repo.ILLustApiRepoImpl
import com.cryptic.piyek.feature.onboarding.data.remote.PixivWalkThroughApiService
import com.cryptic.piyek.feature.onboarding.data.repo.WalkthroughRepoImpl
import com.cryptic.piyek.feature.onboarding.domain.repo.WalkthroughRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(
    private val application: Application,
    private val applicationScope: CoroutineScope
) {

    private val networkJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val jsonConverter = networkJson.asConverterFactory("application/json".toMediaType())
    private val callAdapter = NetworkResultCallAdapterFactory()
    val appConfig = AppConfig()

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java,
            "piyek_database"
        ).build()
    }

    private val oAuthUserDao: OAuthUserDao by lazy {
        database.oAuthUserDao()
    }

    private val oauthOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val oauthRetrofit = Retrofit.Builder()
        .baseUrl("https://oauth.secure.pixiv.net/")
        .client(oauthOkHttpClient)
        .addCallAdapterFactory(callAdapter)
        .addConverterFactory(jsonConverter)
        .build()

    private val tinkManager: TinkManager by lazy {
        TinkManager(application)
    }

    private val dataStore: DataStore<Preferences> by lazy {
        application.dataStore
    }

    val tokenManager: TokenStorage by lazy {
        TokenManager()
    }

    private val authInterceptor by lazy {
        AuthInterceptor(
            tokenStorage = tokenManager,
            appConfig = appConfig,
            oAuthUserRepository = oAuthUserRepository
        )
    }

    private val piyekOkHttpsClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(authInterceptor)
            .build()
    }

    internal val piyekRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://app-api.pixiv.net/")
            .client(piyekOkHttpsClient)
            .addCallAdapterFactory(callAdapter)
            .addConverterFactory(jsonConverter)
            .build()
    }

    /*
    internal inline fun <reified T> createService(): T {
        return piyekRetrofit.create(T::class.java)
    }*/

    private val imageInterceptor: ImageInterceptor by lazy {
        ImageInterceptor(appConfig)
    }

    private val imageClient = OkHttpClient.Builder()
        .addInterceptor(imageInterceptor)
        .build()

    // =============================================================================================
    // PUBLIC MEMBERS
    // =============================================================================================

    val dominantColorExtractor: DominantColorExtractor by lazy {
        DominantColorExtractor()
    }

    val imageLoader by lazy {
        ImageLoader.Builder(application)
            .components { add(
                OkHttpNetworkFetcherFactory(
                    callFactory = {
                        imageClient
                    }
                )
            ) }.build()
    }

    val oAuthApi: OAuthApiService by lazy {
        oauthRetrofit.create(OAuthApiService::class.java)
    }

    private val onBoardingApiService: PixivWalkThroughApiService by lazy {
        piyekRetrofit.create(PixivWalkThroughApiService::class.java)
    }

    val iLLustApi: ILLustApiService by lazy {
        piyekRetrofit.create(ILLustApiService::class.java)
    }

    val iLLustRepo: CoreContentApiRepo<ArtworkContentList, RecommendationNonNovelPara, ArtworkContentList> by lazy {
        ILLustApiRepoImpl(iLLustApi)
    }

    val walkthroughRepo: WalkthroughRepo by lazy {
        WalkthroughRepoImpl(
            onBoardingApi = onBoardingApiService
        )
    }

    val tinkMethod: TinkMethod by lazy {
        TinkMethod(tinkManager)
    }

    val oAuthUserRepository: OAuthUserRepository by lazy {
        OAuthUserRepositoryImpl(
            dao = oAuthUserDao,
            dataStore = dataStore,
            oAuthApiService = oAuthApi,
            tinkMethod = tinkMethod,
            tokenStorage = tokenManager
        )
    }

    val oAuthIntentProcessor: OAuthIntentProcessor by lazy {
        OAuthIntentProcessor(
            repository = oAuthUserRepository,
            dataStore = dataStore,
            appConfig = appConfig,
            tokenManager = tokenManager
        )
    }

    val onBoardingManager: OnboardingManager by lazy {
        OnboardingManager()
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(
            dataStore = dataStore,
            oAuthUserRepository = oAuthUserRepository,
            tinkMethod = tinkMethod,
            appConfig = appConfig,
            applicationScope = applicationScope
        )
    }
}