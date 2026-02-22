package com.cryptic.pixvi.auth.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.cryptic.pixvi.auth.account.AccountMetaDataKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PixivAccountManager(context: Context){
    private val accountManager: AccountManager = AccountManager.get(context)

    fun createAccount(
        userId: Long,
        name: String,
        accountName: String,
        email: String,
        isPremium: Int,
        profilePicBig: String
    ): Boolean{
        val account = Account(email, AccountMetaDataKey.ACCOUNT_TYPE)
        val success = accountManager.addAccountExplicitly(account, null, null)
        if (success) {
            // Set the metadata
            accountManager.setUserData(account, AccountMetaDataKey.USER_ID, userId.toString())
            accountManager.setUserData(account, AccountMetaDataKey.NAME, name)
            accountManager.setUserData(account, AccountMetaDataKey.ACCOUNT_NAME, accountName)
            accountManager.setUserData(account, AccountMetaDataKey.EMAIL, email)
            accountManager.setUserData(account, AccountMetaDataKey.IS_PREMIUM, isPremium.toString())
            accountManager.setUserData(account, AccountMetaDataKey.PROFILE_PIC_BIG, profilePicBig)

            return true
        } else {
            return false
            // Something happened
        }
    }

    fun getAllAccountEmail(): List<String>{
        val accounts = accountManager.getAccountsByType(AccountMetaDataKey.ACCOUNT_TYPE)
        return accounts.map { it.name }
    }

    suspend fun getAccountInfo(email: String): Account?{
        val accounts = accountManager.getAccountsByType(AccountMetaDataKey.ACCOUNT_TYPE)
        return withContext(Dispatchers.IO){
            accounts.firstOrNull{ it.name == email}
        }
    }

    //if account exists
    suspend fun getAccountDetails(account: Account): AccountDetails = withContext(Dispatchers.IO) {
        val infoAbout = accountManager.getUserData(account, AccountMetaDataKey.IS_PREMIUM)?.toIntOrNull() ?: 0
        val isPremium = infoAbout != 0

        return@withContext AccountDetails(
            userId = accountManager.getUserData(account, AccountMetaDataKey.USER_ID),
            name = accountManager.getUserData(account, AccountMetaDataKey.NAME),
            accountName = accountManager.getUserData(account, AccountMetaDataKey.ACCOUNT_NAME),
            email = accountManager.getUserData(account, AccountMetaDataKey.EMAIL),
            isPremium = isPremium,
            profilePicUrlBig = accountManager.getUserData(account, AccountMetaDataKey.PROFILE_PIC_BIG),
        )
    }
}