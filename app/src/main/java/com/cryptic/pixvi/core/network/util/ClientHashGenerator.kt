package com.cryptic.pixvi.core.network.util

import java.security.MessageDigest
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun clientHashGenerator(): HashAndTime{

     val HASH_SALT = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    val current = ZonedDateTime.now()

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    val timeStamp = current.format(formatter)
    val hash = MessageDigest.getInstance("MD5")

    hash.update((timeStamp + HASH_SALT).toByteArray())
    val digest = hash.digest()
    return HashAndTime(timeStamp, digest.joinToString("") { "%02x".format(it) })
}

data class HashAndTime(
    val timeStamp: String,
    val clientSecret: String
)