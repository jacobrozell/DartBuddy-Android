package com.jacobrozell.dartbuddy.support.store

import com.jacobrozell.dartbuddy.BuildConfig
import com.jacobrozell.dartbuddy.data.prefs.PlayStoreUpdateStore
import com.jacobrozell.dartbuddy.support.version.AppVersionComparator
import javax.inject.Inject
import javax.inject.Singleton

data class PlayStoreUpdateOffer(
    val storeVersion: String,
    val storeUrl: String,
)

@Singleton
class PlayStoreUpdateChecker @Inject constructor(
    private val updateStore: PlayStoreUpdateStore,
    private val versionFetcher: PlayStoreVersionFetcher,
) {
    private val packageName: String = BuildConfig.APPLICATION_ID
    private val installedVersion: String = BuildConfig.VERSION_NAME

    suspend fun checkForUpdate(): PlayStoreUpdateOffer? {
        if (!isEnabled) return null
        val lookup = versionFetcher.fetchVersion(packageName) ?: return null
        if (!AppVersionComparator.isStoreVersionNewer(lookup.version, installedVersion)) return null
        if (!shouldPrompt(forStoreVersion = lookup.version)) return null
        return PlayStoreUpdateOffer(storeVersion = lookup.version, storeUrl = lookup.storeUrl)
    }

    fun recordDismissal(offer: PlayStoreUpdateOffer) {
        updateStore.recordDismissal(offer.storeVersion)
    }

    private fun shouldPrompt(forStoreVersion: String): Boolean {
        val dismissed = updateStore.dismissedStoreVersion() ?: return true
        return AppVersionComparator.isStoreVersionNewer(forStoreVersion, dismissed)
    }

    companion object {
        val isEnabled: Boolean
            get() = !BuildConfig.DEBUG
    }
}
