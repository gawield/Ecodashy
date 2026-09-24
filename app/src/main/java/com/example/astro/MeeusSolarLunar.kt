package com.example.astro

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

/**
 * High-precision offline astronomical algorithms based on Jean Meeus ("Astronomical Algorithms").
 * Used for precise solar & lunar positions, tithi calculations, sunrise/sunset, and Hari Vasara.
 */
object MeeusSolarLunar {

    private const val DEG2RAD = Math.PI / 180.0
    private const val RAD2DEG = 180.0 / Math.PI

    private fun normDeg(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    /**
     * Converts a ZonedDateTime to Julian Ephemeris Date (Julian Day).
     */
    fun toJulianDay(zdt: ZonedDateTime): Double {
        val utc = zdt.withZoneSameInstant(ZoneId.of("UTC"))
        var y = utc.year
        var m = utc.monthValue
        val d = utc.dayOfMonth.toDouble() +
                (utc.hour + (utc.minute + (utc.second + utc.nano / 1e9) / 60.0) / 60.0) / 24.0

        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)

        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /**
     * Converts a Julian Day back to ZonedDateTime in the specified ZoneId.
     */
    fun fromJulianDay(jd: Double, zoneId: ZoneId): ZonedDateTime {
        val jdPlus = jd + 0.5
        val z = floor(jdPlus).toLong()
        val f = jdPlus - z

        val a = if (z < 2299161) z else {
            val alpha = floor((z - 1867216.25) / 36524.25).toLong()
            z + 1 + alpha - floor(alpha / 4.0).toLong()
        }

        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toLong()
        val d = floor(365.25 * c).toLong()
        val e = floor((b - d) / 30.6001).toLong()

        val dayFrac = b - d - floor(30.6001 * e) + f
        val day = floor(dayFrac).toInt()
        val month = (if (e < 14) e - 1 else e - 13).toInt()
        val year = (if (month > 2) c - 4716 else c - 4715).toInt()

        val timeFrac = (dayFrac - day) * 24.0
        val hour = floor(timeFrac).toInt().coerceIn(0, 23)
        val minFrac = (timeFrac - hour) * 60.0
        val minute = floor(minFrac).toInt().coerceIn(0, 59)
        val secFrac = (minFrac - minute) * 60.0
        val second = floor(secFrac).toInt().coerceIn(0, 59)
        val nano = ((secFrac - second) * 1e9).toInt().coerceIn(0, 999999999)

        val utcZdt = ZonedDateTime.of(year, month, day, hour, minute, second, nano, ZoneId.of("UTC"))
        return utcZdt.withZoneSameInstant(zoneId)
    }

    /**
     * Apparent geocentric ecliptic longitude of the Sun in degrees [0, 360).
     * Reference: Meeus Chapter 25.
     */
    fun getSunLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0

        // Geometric mean longitude of the Sun
        val l0 = normDeg(280.46646 + 36000.76983 * t + 0.0003032 * t * t)

        // Mean anomaly of the Sun
        val m = normDeg(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val mRad = m * DEG2RAD

        // Sun equation of center
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mRad) +
                (0.019993 - 0.000101 * t) * sin(2.0 * mRad) +
                0.000289 * sin(3.0 * mRad)

        // Sun true longitude
        val sunTrue = l0 + c

        // Apparent longitude (aberration + nutation correction)
        val omega = normDeg(125.04 - 1934.136 * t) * DEG2RAD
        val lambda = sunTrue - 0.00569 - 0.00478 * sin(omega)

        return normDeg(lambda)
    }

    /**
     * Apparent geocentric ecliptic longitude of the Moon in degrees [0, 360).
     * Reference: Meeus Chapter 47 (periodic terms for ~0.01° accuracy).
     */
    fun getMoonLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        // Moon's mean longitude
        val lp = normDeg(218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0)

        // Moon's mean elongation
        val d = normDeg(297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0)

        // Sun's mean anomaly
        val m = normDeg(357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0)

        // Moon's mean anomaly
        val mp = normDeg(134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0)

        // Moon's argument of latitude
        val f = normDeg(93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0)

        // Main periodic perturbations to longitude
        val dRad = d * DEG2RAD
        val mRad = m * DEG2RAD
        val mpRad = mp * DEG2RAD
        val fRad = f * DEG2RAD

        var sl = 6.288774 * sin(mpRad) +
                1.274027 * sin(2.0 * dRad - mpRad) +
                0.658314 * sin(2.0 * dRad) +
                0.213618 * sin(2.0 * mpRad) -
                0.185116 * sin(mRad) -
                0.114332 * sin(2.0 * fRad) +
                0.058793 * sin(2.0 * dRad - 2.0 * mpRad) +
                0.057066 * sin(2.0 * dRad - mRad - mpRad) +
                0.053322 * sin(2.0 * dRad + mpRad) +
                0.046153 * sin(2.0 * dRad - mRad) -
                0.034728 * sin(dRad) -
                0.030383 * sin(mRad + mpRad) +
                0.015327 * sin(2.0 * dRad - 2.0 * fRad) -
                0.012528 * sin(2.0 * dRad + mRad - mpRad) +
                0.010980 * sin(2.0 * dRad - mRad + mpRad) +
                0.010675 * sin(4.0 * dRad - mpRad) +
                0.010077 * sin(mpRad - 2.0 * fRad)

        // Nutation in longitude correction
        val omega = normDeg(125.04 - 1934.136 * t) * DEG2RAD
        sl += -0.00478 * sin(omega)

        return normDeg(lp + sl)
    }

    /**
     * Elongation between Moon and Sun (0 to 360 degrees).
     * 0° = New Moon (Amavasya end)
     * 180° = Full Moon (Purnima)
     */
    fun getMoonElongation(jd: Double): Double {
        val sun = getSunLongitude(jd)
        val moon = getMoonLongitude(jd)
        return normDeg(moon - sun)
    }

    /**
     * Tithi index from 1 to 30:
     * 1 to 15: Shukla Pratipat to Purnima (11 = Shukla Ekadashi, 12 = Shukla Dvadashi)
     * 16 to 30: Krishna Pratipat to Amavasya (26 = Krishna Ekadashi, 27 = Krishna Dvadashi)
     */
    fun getTithiNumber(jd: Double): Int {
        val elong = getMoonElongation(jd)
        val tithi = floor(elong / 12.0).toInt() + 1
        return tithi.coerceIn(1, 30)
    }

    /**
     * Fractional tithi (e.g. 11.45 means 45% into Shukla Ekadashi).
     */
    fun getTithiProgress(jd: Double): Double {
        val elong = getMoonElongation(jd)
        return (elong % 12.0) / 12.0
    }

    /**
     * Returns name of Tithi (1..30).
     */
    fun getTithiName(tithi: Int): String {
        val names = arrayOf(
            "Pratipat", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
            "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
            "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima"
        )
        return if (tithi <= 15) {
            "Shukla " + names[tithi - 1]
        } else {
            val idx = tithi - 16
            if (idx == 14) "Amavasya" else "Krishna " + names[idx]
        }
    }

    /**
     * Solves for the exact Julian Day when the Moon-Sun elongation hits [targetDegrees]
     * within the interval [jdStart, jdEnd] using high-precision bisection root finding.
     */
    fun findElongationCrossing(targetDegrees: Double, jdStart: Double, jdEnd: Double): Double {
        var low = jdStart
        var high = jdEnd

        fun angleDiff(jd: Double): Double {
            val elong = getMoonElongation(jd)
            var diff = elong - targetDegrees
            while (diff > 180.0) diff -= 360.0
            while (diff < -180.0) diff += 360.0
            return diff
        }

        var fLow = angleDiff(low)
        var fHigh = angleDiff(high)

        // If not bracketing root directly due to phase wrap, subdivide into 12 steps
        if (fLow * fHigh > 0) {
            val steps = 24
            val step = (jdEnd - jdStart) / steps
            for (i in 0 until steps) {
                val t0 = jdStart + i * step
                val t1 = t0 + step
                val d0 = angleDiff(t0)
                val d1 = angleDiff(t1)
                if (d0 * d1 <= 0) {
                    low = t0
                    high = t1
                    fLow = d0
                    fHigh = d1
                    break
                }
            }
        }

        // Bisection to within 1 second (~0.00001 days)
        for (iter in 0 until 50) {
            val mid = (low + high) / 2.0
            val fMid = angleDiff(mid)
            if (abs(fMid) < 0.0001 || (high - low) < 0.00001157) { // 1 sec = 1/86400 ~ 0.00001157
                return mid
            }
            if (fLow * fMid <= 0) {
                high = mid
                fHigh = fMid
            } else {
                low = mid
                fLow = fMid
            }
        }
        return (low + high) / 2.0
    }

    data class SolarTimes(
        val sunrise: ZonedDateTime,
        val noon: ZonedDateTime,
        val sunset: ZonedDateTime,
        val arunodaya: ZonedDateTime // 96 min (4 ghatikas) before sunrise
    )

    /**
     * Calculates exact Sunrise, Solar Noon, Sunset and Arunodaya for a given date and coordinates.
     * Incorporates standard solar zenith 90°50' (90.8333°) for atmospheric refraction & solar disc.
     */
    fun calculateSolarTimes(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): SolarTimes {
        // Approximate noon JD for the date
        val noonZdt = ZonedDateTime.of(date, LocalTime.of(12, 0), zoneId)
        val jdNoon = toJulianDay(noonZdt)
        val t = (jdNoon - 2451545.0) / 36525.0

        // Solar Mean Anomaly
        val m = normDeg(357.52911 + 35999.05029 * t - 0.0001537 * t * t) * DEG2RAD

        // Geometric Mean Longitude
        val l0 = normDeg(280.46646 + 36000.76983 * t + 0.0003032 * t * t)

        // Equation of Center
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
                (0.019993 - 0.000101 * t) * sin(2.0 * m) +
                0.000289 * sin(3.0 * m)

        val sunTrueLong = l0 + c
        val omega = normDeg(125.04 - 1934.136 * t) * DEG2RAD
        val sunAppLong = normDeg(sunTrueLong - 0.00569 - 0.00478 * sin(omega)) * DEG2RAD

        // Mean Obliquity of the Ecliptic
        val eps0 = 23.439291 - 0.0130042 * t - 0.00000016 * t * t + 0.000000504 * t * t * t
        val eps = (eps0 + 0.00256 * cos(omega)) * DEG2RAD

        // Solar Declination
        val sinDec = sin(eps) * sin(sunAppLong)
        val dec = asin(sinDec)

        // Equation of Time (EoT) in minutes
        val yVar = tan(eps / 2.0).pow(2.0)
        val l0Rad = l0 * DEG2RAD
        val eotRad = yVar * sin(2.0 * l0Rad) - 2.0 * 0.016708634 * sin(m) +
                4.0 * 0.016708634 * yVar * sin(m) * cos(2.0 * l0Rad) -
                0.5 * yVar * yVar * sin(4.0 * l0Rad) -
                1.25 * 0.016708634.pow(2.0) * sin(2.0 * m)
        val eotMinutes = eotRad * RAD2DEG * 4.0

        // Solar Noon in UTC minutes from midnight: 720 - 4 * longitude - EoT
        val solarNoonUtcMinutes = 720.0 - 4.0 * longitude - eotMinutes

        // Hour angle for sunrise/sunset (zenith 90.8333°)
        val latRad = latitude * DEG2RAD
        val zenithRad = 90.8333 * DEG2RAD
        val cosH0 = (cos(zenithRad) - sin(latRad) * sin(dec)) / (cos(latRad) * cos(dec))

        val h0Deg = when {
            cosH0 > 1.0 -> 0.0    // Polar night
            cosH0 < -1.0 -> 180.0 // Midnight sun
            else -> acos(cosH0) * RAD2DEG
        }

        val sunriseUtcMinutes = solarNoonUtcMinutes - h0Deg * 4.0
        val sunsetUtcMinutes = solarNoonUtcMinutes + h0Deg * 4.0

        fun minutesToZdt(minutesUtc: Double): ZonedDateTime {
            var min = minutesUtc
            var dayOffset = 0L
            while (min < 0) {
                min += 1440.0
                dayOffset -= 1
            }
            while (min >= 1440.0) {
                min -= 1440.0
                dayOffset += 1
            }
            val totalSeconds = (min * 60.0).roundToLong()
            val secOfDay = (totalSeconds % 86400).toInt()
            val utcDate = date.plusDays(dayOffset)
            val utcLdt = utcDate.atTime(LocalTime.ofSecondOfDay(secOfDay.toLong()))
            val utcZdt = utcLdt.atZone(ZoneId.of("UTC"))
            return utcZdt.withZoneSameInstant(zoneId)
        }

        val sunrise = minutesToZdt(sunriseUtcMinutes)
        val noon = minutesToZdt(solarNoonUtcMinutes)
        val sunset = minutesToZdt(sunsetUtcMinutes)
        val arunodaya = sunrise.minusMinutes(96) // 4 Vedic ghatikas

        return SolarTimes(sunrise, noon, sunset, arunodaya)
    }
}
