package com.overud.geo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.overud.analytics.RegionRecorder

class WorkLocationTracker(private val context: Context, private val regions: RegionRecorder) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val listener = LocationListener { location: Location -> regions.location(location) }
    @SuppressLint("MissingPermission")
    fun start() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).forEach { provider ->
            runCatching { manager.requestLocationUpdates(provider, 10_000L, 50f, listener, Looper.getMainLooper()) }
        }
    }
    fun stop() { manager.removeUpdates(listener) }
}
