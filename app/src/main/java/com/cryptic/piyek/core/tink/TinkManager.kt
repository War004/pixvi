package com.cryptic.piyek.core.tink

import android.app.Application
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException

private const val PREF_FILE_NAME = "piyek_keyset_prefs"
private const val KEYSET_NAME = "master_keyset"
private const val MASTER_KEY_URI = "android-keystore://piyek_master_key"

class TinkManager(application: Application) {

    val aead: Aead by lazy {
        try {
            AeadConfig.register()

            AndroidKeysetManager.Builder()
                .withSharedPref(application, KEYSET_NAME, PREF_FILE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Failed to initialize Tink: Security Error", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to initialize Tink: IO Error", e)
        }
    }
}