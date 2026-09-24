package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.astro.EkadashiCalculator
import com.example.astro.EkadashiEvent
import com.example.astro.MeeusSolarLunar
import com.example.location.GeoPoint
import com.example.location.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.cos

data class AstronomicalSnapshot(
    val currentTime: ZonedDateTime,
    val julianDay: Double,
    val sunLongitudeDeg: Double,
    val moonLongitudeDeg: Double,
    val elongationDeg: Double,
    val tithiNumber: Int,
    val tithiName: String,
    val tithiProgressPercent: Int,
    val moonIlluminationPercent: Int,
    val todaySunrise: ZonedDateTime,
    val todayArunodaya: ZonedDateTime,
    val todaySunset: ZonedDateTime
)

data class EkadashiUiState(
    val selectedLocation: GeoPoint = LocationHelper.PRESET_LOCATIONS[0],
    val isLoading: Boolean = false,
    val upcomingEvents: List<EkadashiEvent> = emptyList(),
    val nextEvent: EkadashiEvent? = null,
    val astroSnapshot: AstronomicalSnapshot? = null,
    val countdownText: String = "",
    val locationStatusMessage: String? = null,
    val showLocationPicker: Boolean = false,
    val showCustomCoordsDialog: Boolean = false,
    val showAstroInspector: Boolean = false,
    val showFastingGuide: Boolean = false
)

class EkadashiViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(EkadashiUiState())
    val uiState: StateFlow<EkadashiUiState> = _uiState.asStateFlow()

    init {
        // Initial setup: try to get GPS, else default preset
        tryDetectGps(fallbackToPreset = true)

        // Live ticker for countdown & celestial state
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateLiveStatus()
                delay(1000)
            }
        }
    }

    fun setLocation(point: GeoPoint) {
        _uiState.update { it.copy(selectedLocation = point, locationStatusMessage = null) }
        recalculateCalendar()
    }

    fun tryDetectGps(fallbackToPreset: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            val context: Context = getApplication()
            val gpsPoint = LocationHelper.getLastKnownGpsLocation(context)
            if (gpsPoint != null) {
                _uiState.update {
                    it.copy(
                        selectedLocation = gpsPoint,
                        locationStatusMessage = "GPS coordinates updated successfully",
                        isLoading = false
                    )
                }
                recalculateCalendar()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locationStatusMessage = if (!fallbackToPreset) {
                            "GPS location not available (check permissions or GPS switch)"
                        } else null
                    )
                }
                if (fallbackToPreset && _uiState.value.upcomingEvents.isEmpty()) {
                    recalculateCalendar()
                }
            }
        }
    }

    fun setCustomCoordinates(name: String, lat: Double, lon: Double, zoneId: ZoneId) {
        val customPoint = GeoPoint(
            name = if (name.isNotBlank()) name else "Custom (${String.format("%.2f", lat)}, ${String.format("%.2f", lon)})",
            latitude = lat,
            longitude = lon,
            zoneId = zoneId,
            isAutoGps = false
        )
        setLocation(customPoint)
    }

    fun toggleLocationPicker(show: Boolean) {
        _uiState.update { it.copy(showLocationPicker = show) }
    }

    fun toggleCustomCoordsDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomCoordsDialog = show) }
    }

    fun toggleAstroInspector(show: Boolean) {
        _uiState.update { it.copy(showAstroInspector = show) }
    }

    fun toggleFastingGuide(show: Boolean) {
        _uiState.update { it.copy(showFastingGuide = show) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(locationStatusMessage = null) }
    }

    fun recalculateCalendar() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isLoading = true) }
            val loc = _uiState.value.selectedLocation
            val today = LocalDate.now(loc.zoneId)

            val events = EkadashiCalculator.getUpcomingEkadashis(
                startDate = today,
                latitude = loc.latitude,
                longitude = loc.longitude,
                zoneId = loc.zoneId,
                count = 14
            )

            val next = events.firstOrNull()

            _uiState.update {
                it.copy(
                    upcomingEvents = events,
                    nextEvent = next,
                    isLoading = false
                )
            }
            updateLiveStatus()
        }
    }

    private fun updateLiveStatus() {
        val loc = _uiState.value.selectedLocation
        val now = ZonedDateTime.now(loc.zoneId)
        val jd = MeeusSolarLunar.toJulianDay(now)

        val sunLong = MeeusSolarLunar.getSunLongitude(jd)
        val moonLong = MeeusSolarLunar.getMoonLongitude(jd)
        val elong = MeeusSolarLunar.getMoonElongation(jd)
        val tithiNum = MeeusSolarLunar.getTithiNumber(jd)
        val tithiName = MeeusSolarLunar.getTithiName(tithiNum)
        val tithiProgress = (MeeusSolarLunar.getTithiProgress(jd) * 100).toInt()

        // Phase illumination % approx: (1 - cos(elong)) / 2 * 100
        val moonIllum = ((1.0 - cos(elong * Math.PI / 180.0)) / 2.0 * 100.0).toInt().coerceIn(0, 100)

        val todaySolar = MeeusSolarLunar.calculateSolarTimes(now.toLocalDate(), loc.latitude, loc.longitude, loc.zoneId)

        val snapshot = AstronomicalSnapshot(
            currentTime = now,
            julianDay = jd,
            sunLongitudeDeg = sunLong,
            moonLongitudeDeg = moonLong,
            elongationDeg = elong,
            tithiNumber = tithiNum,
            tithiName = tithiName,
            tithiProgressPercent = tithiProgress,
            moonIlluminationPercent = moonIllum,
            todaySunrise = todaySolar.sunrise,
            todayArunodaya = todaySolar.arunodaya,
            todaySunset = todaySolar.sunset
        )

        // Calculate countdown to next Ekadashi or active Parana
        val next = _uiState.value.nextEvent
        val countdown = if (next != null) {
            val fastStart = next.sunriseFastDay
            val paranaStart = next.paranaStart
            val paranaEnd = next.paranaEnd

            when {
                now.isBefore(fastStart) -> {
                    val dur = Duration.between(now, fastStart)
                    formatDuration(dur, "Fasting starts in")
                }
                now.isBefore(paranaStart) -> {
                    val dur = Duration.between(now, paranaStart)
                    formatDuration(dur, "Parana begins in")
                }
                now.isBefore(paranaEnd) -> {
                    val dur = Duration.between(now, paranaEnd)
                    formatDuration(dur, "Parana window ends in")
                }
                else -> {
                    "Parana window completed"
                }
            }
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                astroSnapshot = snapshot,
                countdownText = countdown
            )
        }
    }

    private fun formatDuration(duration: Duration, prefix: String): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return if (days > 0) {
            "$prefix $days d ${hours}h ${minutes}m"
        } else {
            "$prefix ${hours}h ${minutes}m ${seconds}s"
        }
    }
}
