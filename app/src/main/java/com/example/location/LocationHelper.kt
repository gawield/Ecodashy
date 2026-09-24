package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import java.time.ZoneId

data class GeoPoint(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
    val isAutoGps: Boolean = false
)

object LocationHelper {

    val PRESET_LOCATIONS = listOf(
        GeoPoint("Moscow, Russia", 55.7558, 37.6173, ZoneId.of("Europe/Moscow")),
        GeoPoint("Saint Petersburg, Russia", 59.9343, 30.3351, ZoneId.of("Europe/Moscow")),
        GeoPoint("Mayapur, India", 23.4233, 88.3888, ZoneId.of("Asia/Kolkata")),
        GeoPoint("Vrindavan, India", 27.5806, 77.7006, ZoneId.of("Asia/Kolkata")),
        GeoPoint("New Delhi, India", 28.6139, 77.2090, ZoneId.of("Asia/Kolkata")),
        GeoPoint("London, UK", 51.5074, -0.1278, ZoneId.of("Europe/London")),
        GeoPoint("New York, USA", 40.7128, -74.0060, ZoneId.of("America/New_York")),
        GeoPoint("San Francisco, USA", 37.7749, -122.4194, ZoneId.of("America/Los_Angeles")),
        GeoPoint("Tokyo, Japan", 35.6762, 139.6503, ZoneId.of("Asia/Tokyo")),
        GeoPoint("Sydney, Australia", -33.8688, 151.2093, ZoneId.of("Australia/Sydney"))
    )

    /**
     * Attempts to read last known location from GPS or Network provider (100% offline).
     */
    @SuppressLint("MissingPermission")
    fun getLastKnownGpsLocation(context: Context): GeoPoint? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }

            bestLocation?.let { loc ->
                val sysZone = ZoneId.systemDefault()
                GeoPoint(
                    name = "GPS (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})",
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    zoneId = sysZone,
                    isAutoGps = true
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
