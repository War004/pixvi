package com.example.pixvi.utils

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PixivAuthUtils {
    // Salt used in generateClientHash
    private const val HASH_SALT = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    //Thanks to https://github.com/DaRealFreak for finding the new logic for X-Client-Hash

    /**
     * Formats the current time into the string format required for Pixiv API requests.
     */
    fun getCurrentTimeFormatted(): String {
        // Pixiv seems to require US locale for the formatter potentially
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return formatter.format(Date())
    }

    /**
     * Generates the client hash required for Pixiv API.
     *
     * @param timeString The formatted timestamp string (from getCurrentTimeFormatted).
     * @return The MD5 client hash string.
     */
    fun generateClientHash(timeString: String): String {
        val hash = MessageDigest.getInstance("MD5")
        hash.update((timeString + HASH_SALT).toByteArray())
        val digest = hash.digest()
        // Convert byte array to hex string
        return digest.joinToString("") { "%02x".format(it) }
    }
}