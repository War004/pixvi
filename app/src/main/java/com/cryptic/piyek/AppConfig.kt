package com.cryptic.piyek

import android.os.Build
import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AppConfig {
    private  val appVersion = "6.184.0"
    private val clientId = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private val clientSecret = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private val refererUrl = "https://app-api.pixiv.net/"
    private val osVersion = Build.VERSION.RELEASE
    private val deviceModel = Build.MODEL
    private val osName = "Android"
    private val locale = Locale.getDefault()

    fun getAppVersion():String { return appVersion }
    fun getClientId():String { return clientId }
    fun getClientSecret ():String { return clientSecret }
    fun getRefererUrl():String { return refererUrl }
    fun getOsName():String {return osName}
    fun getOsVersion(): String {return osVersion}
    fun getUserAgent():String{
        return "PixivAndroid/$appVersion ($osName $osVersion; $deviceModel)"
    }
    fun getAcceptLan():String {
        return "${locale.language}_${locale.country}"
    }
    fun getAppAcceptLan(): String { return locale.language}


    fun clientHashGenerator(): HashAndTime {
        val hashSalt = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
        val timeStamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
        val digest = MessageDigest.getInstance("MD5").digest((timeStamp + hashSalt).toByteArray())
        return HashAndTime(timeStamp, digest.toHexString())
    }

    data class HashAndTime(
        val timeStamp: String,
        val clientSecret: String
    )
}