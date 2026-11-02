package com.overud.geo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class AndroidGeocodingProvider(context: Context) {
    private val geocoder = Geocoder(context.applicationContext)
    private val legacy = ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(1),
        { task -> Thread(task, "overud-geocoder").apply { isDaemon = true } })

    @Suppress("DEPRECATION")
    suspend fun lookup(query: String): List<Address> {
        if (!runCatching { Geocoder.isPresent() }.getOrDefault(false)) return emptyList()
        return withTimeoutOrNull(6000) {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= 33) {
                    runCatching {
                        geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses.toList())
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(emptyList())
                            }
                        })
                    }.onFailure { if (continuation.isActive) continuation.resume(emptyList()) }
                } else {
                    runCatching {
                        val future = legacy.submit {
                            val result = runCatching { geocoder.getFromLocationName(query, 5).orEmpty() }.getOrDefault(emptyList())
                            if (continuation.isActive) continuation.resume(result)
                        }
                        continuation.invokeOnCancellation {
                            future.cancel(true)
                            (future as? Runnable)?.let { legacy.remove(it) }
                        }
                    }.onFailure { if (continuation.isActive) continuation.resume(emptyList()) }
                }
            }
        }.orEmpty()
    }
}
