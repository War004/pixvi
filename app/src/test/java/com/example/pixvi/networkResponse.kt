package com.example.pixvi

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.example.pixvi.withFilter

//quick test
/*
fun main() = runBlocking {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
    }

    val url = "https://app-api.pixiv.net/v1/illust/recommended"

    // Generate current timestamp in the required format
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    val currentTime = java.time.ZonedDateTime.now().format(formatter)

    val response: HttpResponse = client.get(url) {
        url {
            parameters.append("filter", "for_android")
            parameters.append("include_ranking_illusts", "true")
            parameters.append("include_privacy_policy", "true")
        }

        // Headers
        headers {
            append(HttpHeaders.AcceptLanguage, "en_US")
            append("app-accept-language", "en")
            append("App-OS", "android")
            append("App-OS-Version", "9")
            append("App-Version", "6.137.0")
            append(HttpHeaders.Authorization, "Bearer oGLbkaNtFudfmYeehetNJvPQBuCkz9lyz3KJvHlCrF4")
            append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            append("User-Agent", "PixivAndroidApp/6.137.0 (Android 9; SM-S908E)")
            append("X-Client-Time", currentTime)
            // You may need to regenerate this hash based on the current time
            append("X-Client-Hash", "33cbc7ff0ee3f39c8c35b7d4899d3f60")
        }
    }
    println(response)
    println("Status: ${response.status}")

    val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    if (response.status.isSuccess()) {
        val responseBody = response.bodyAsText()
        //println("Response body: $responseBody")
        try {
            val purifedText: withFilter = json.decodeFromString<com.example.pixvi.withFilter>(responseBody)
            println("Response body first id: ${purifedText.illusts[2].id}")
        } catch (e: Exception) {
            println("Error: ${e.message}")
        }
    }

    client.close()
}*/