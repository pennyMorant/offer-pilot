package io.github.pennymorant.offerpilot.geo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class AndroidGeocodingProvider(context: Context) {
    private val geocoder = Geocoder(context.applicationContext)

    suspend fun lookup(query: String): List<Address> {
        if (!runCatching { Geocoder.isPresent() }.getOrDefault(false)) return emptyList()
        return withTimeoutOrNull(6000) {
            suspendCancellableCoroutine<List<Address>> { continuation ->
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
            }
        }.orEmpty()
    }
}
