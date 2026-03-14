/*
 * ArchiveTune Project Original (2026)
 * Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package moe.koiverse.archivetune.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.koiverse.archivetune.constants.AccountChannelHandleKey
import moe.koiverse.archivetune.constants.AccountEmailKey
import moe.koiverse.archivetune.constants.AccountListKey
import moe.koiverse.archivetune.constants.AccountNameKey
import moe.koiverse.archivetune.constants.DataSyncIdKey
import moe.koiverse.archivetune.constants.InnerTubeCookieKey
import moe.koiverse.archivetune.constants.PoTokenKey
import moe.koiverse.archivetune.constants.VisitorDataKey
import moe.koiverse.archivetune.models.SavedAccount

object AccountManager {

    const val MAX_ACCOUNTS = 3

    private val json = Json { ignoreUnknownKeys = true }

    fun decodeAccounts(raw: String): List<SavedAccount> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<SavedAccount>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeAccounts(accounts: List<SavedAccount>): String =
        json.encodeToString(accounts)

    suspend fun switchAccount(context: Context, account: SavedAccount) {
        context.dataStore.edit { prefs ->
            prefs[InnerTubeCookieKey] = account.cookie
            prefs[PoTokenKey] = account.poToken
            prefs[VisitorDataKey] = account.visitorData
            prefs[DataSyncIdKey] = account.dataSyncId
            prefs[AccountNameKey] = account.name
            prefs[AccountEmailKey] = account.email
            prefs[AccountChannelHandleKey] = account.channelHandle
        }
    }

    suspend fun addOrUpdateInList(context: Context, account: SavedAccount) {
        context.dataStore.edit { prefs ->
            val existing = decodeAccounts(prefs[AccountListKey] ?: "").toMutableList()
            val idx = existing.indexOfFirst { it.id == account.id }
            when {
                idx >= 0 -> existing[idx] = account
                existing.size < MAX_ACCOUNTS -> existing.add(account)
            }
            prefs[AccountListKey] = encodeAccounts(existing)
        }
    }

    suspend fun removeAccount(context: Context, accountId: String) {
        context.dataStore.edit { prefs ->
            val existing = decodeAccounts(prefs[AccountListKey] ?: "").toMutableList()
            val removed = existing.firstOrNull { it.id == accountId } ?: return@edit
            val wasActive = removed.cookie == (prefs[InnerTubeCookieKey] ?: "")
            existing.remove(removed)
            prefs[AccountListKey] = encodeAccounts(existing)
            if (wasActive) {
                val next = existing.firstOrNull()
                if (next != null) {
                    prefs[InnerTubeCookieKey] = next.cookie
                    prefs[PoTokenKey] = next.poToken
                    prefs[VisitorDataKey] = next.visitorData
                    prefs[DataSyncIdKey] = next.dataSyncId
                    prefs[AccountNameKey] = next.name
                    prefs[AccountEmailKey] = next.email
                    prefs[AccountChannelHandleKey] = next.channelHandle
                } else {
                    prefs.remove(InnerTubeCookieKey)
                    prefs.remove(PoTokenKey)
                    prefs.remove(VisitorDataKey)
                    prefs.remove(DataSyncIdKey)
                    prefs.remove(AccountNameKey)
                    prefs.remove(AccountEmailKey)
                    prefs.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}
