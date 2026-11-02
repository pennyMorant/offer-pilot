package com.overud.geo

import android.location.Address
import android.os.SystemClock
import android.net.Uri
import com.overud.model.*
import com.overud.region.DestinationCoordinates
import com.overud.region.RegionCell
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

/** The address cache is bounded and in memory only. No address or result is written to Room. */
class DestinationResolver(private val provider: AndroidGeocodingProvider) {
    private data class Cached(val destination: ResolvedDestination, val expiresAt: Long)
    private val cache = linkedMapOf<String, Cached>()
    private val mutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun resolve(text: String): ResolvedDestination = mutex.withLock {
        val raw = text.trim().take(240)
        val named = if (raw.startsWith("geo:", ignoreCase = true)) {
            runCatching { Uri.parse(raw).getQueryParameter("q") }.getOrNull()?.takeIf(String::isNotBlank)
        } else null
        val query = named ?: raw
        DestinationCoordinates.parse(query)?.let { return@withLock it }
        if (query.length < 4) return@withLock ResolvedDestination(DestinationResolution.TOO_BROAD)
        val key = query.lowercase(Locale.ROOT)
        val now = SystemClock.elapsedRealtime()
        cache[key]?.takeIf { it.expiresAt > now }?.let { return@withLock it.destination }
        val wait = 5000 - (now - lastRequestAt)
        if (wait > 0) delay(wait)
        lastRequestAt = SystemClock.elapsedRealtime()
        val result = select(provider.lookup(query))
        val lifetime = if (result.status == DestinationResolution.RESOLVED) 6L * 3600 * 1000 else 30_000L
        cache.remove(key)
        cache[key] = Cached(result, SystemClock.elapsedRealtime() + lifetime)
        while (cache.size > 64) cache.remove(cache.keys.first())
        result
    }

    suspend fun clear() = mutex.withLock { cache.clear() }

    private fun select(addresses: List<Address>): ResolvedDestination {
        if (addresses.isEmpty()) return ResolvedDestination(DestinationResolution.UNAVAILABLE)
        val usable = addresses.filter { it.hasLatitude() && it.hasLongitude() &&
            !it.thoroughfare.isNullOrBlank() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
        if (usable.isEmpty()) return ResolvedDestination(DestinationResolution.TOO_BROAD)
        val regions = usable.groupBy { RegionCell.at(it.latitude, it.longitude).key }
        if (regions.size != 1 || usable.size != addresses.size) return ResolvedDestination(DestinationResolution.AMBIGUOUS)
        val precision = if (usable.all { !it.subThoroughfare.isNullOrBlank() }) DestinationPrecision.ADDRESS else DestinationPrecision.STREET
        return ResolvedDestination(DestinationResolution.RESOLVED, regions.keys.single(), precision)
    }
}
