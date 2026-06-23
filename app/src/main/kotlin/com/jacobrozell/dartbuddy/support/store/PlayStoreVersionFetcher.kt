package com.jacobrozell.dartbuddy.support.store

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class PlayStoreLookupResult(
    val version: String,
    val storeUrl: String,
)

fun interface PlayStoreVersionFetcher {
    suspend fun fetchVersion(packageName: String): PlayStoreLookupResult?
}

@Singleton
class HttpPlayStoreVersionFetcher @Inject constructor() : PlayStoreVersionFetcher {
    override suspend fun fetchVersion(packageName: String): PlayStoreLookupResult? = withContext(Dispatchers.IO) {
        val storeUrl = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
        try {
            val connection = (URL(storeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "DartBuddy-Android")
            }
            val html = connection.inputStream.bufferedReader().use { it.readText() }
            val version = SOFTWARE_VERSION_REGEX.find(html)?.groupValues?.get(1)
                ?: BRACKET_VERSION_REGEX.find(html)?.groupValues?.get(1)
            version?.let { PlayStoreLookupResult(it, storeUrl) }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val SOFTWARE_VERSION_REGEX = Regex(""""softwareVersion":"([^"]+)"""")
        private val BRACKET_VERSION_REGEX = Regex("""\[\[\["([0-9.]+?)"\]""")
    }
}
