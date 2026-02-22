package com.cryptic.pixvi.core.tink

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException

class TinkManager(context: Context) {

    companion object {
        // file name for the SharedPreference that will store the encrypted keys
        private const val PREF_FILE_NAME = "pixvi_keyset_prefs"
        // name of the keyset inside that file
        private const val KEYSET_NAME = "master_keyset"
        // The URI for the master key in Android Keystore
        private const val MASTER_KEY_URI = "android-keystore://pixvi_master_key"
    }

    val aead: Aead by lazy {
        try {
            // 1. Register AEAD Config
            AeadConfig.register()

            // 2. Build the AndroidKeysetManager
            // This handles the "Envelope Encryption" automatically:
            // - Generates a new key if one doesn't exist.
            // - Reads the existing key if it does.
            // - Encrypts the key using the Android Keystore Master Key.
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
                // AES256_GCM is the recommended standard in the repo's templates
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(),Aead::class.java)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Failed to initialize Tink: Security Error", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to initialize Tink: IO Error", e)
        }
    }
}