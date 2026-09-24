package com.example.astro

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class Paksha {
    SHUKLA, KRISHNA
}

data class EkadashiEvent(
    val id: String,
    val fastDate: LocalDate,
    val name: String,
    val paksha: Paksha,
    val isShuddha: Boolean,
    val isMahadvadashi: Boolean,
    val note: String,
    val tithiStart: ZonedDateTime,
    val tithiEnd: ZonedDateTime,
    val sunriseFastDay: ZonedDateTime,
    val arunodayaFastDay: ZonedDateTime,
    val paranaDate: LocalDate,
    val paranaStart: ZonedDateTime,
    val paranaEnd: ZonedDateTime,
    val paranaExplanation: String,
    val hariVasaraEnd: ZonedDateTime,
    val dvadashiEnd: ZonedDateTime
)

object EkadashiCalculator {

    private val EKADASHI_NAMES = mapOf(
        // Month ~ Solar sign or Vedic month
        Pair("Chaitra", Paksha.KRISHNA) to "Papamochani",
        Pair("Chaitra", Paksha.SHUKLA) to "Kamada",
        Pair("Vaishakha", Paksha.KRISHNA) to "Varuthini",
        Pair("Vaishakha", Paksha.SHUKLA) to "Mohini",
        Pair("Jyeshtha", Paksha.KRISHNA) to "Apara",
        Pair("Jyeshtha", Paksha.SHUKLA) to "Pandava Nirjala",
        Pair("Ashadha", Paksha.KRISHNA) to "Yogini",
        Pair("Ashadha", Paksha.SHUKLA) to "Sayana (Devashayani)",
        Pair("Shravana", Paksha.KRISHNA) to "Kamika",
        Pair("Shravana", Paksha.SHUKLA) to "Pavitra (Pavitropana)",
        Pair("Bhadrapada", Paksha.KRISHNA) to "Annada (Aja)",
        Pair("Bhadrapada", Paksha.SHUKLA) to "Parsva (Parivartini)",
        Pair("Ashvina", Paksha.KRISHNA) to "Indira",
        Pair("Ashvina", Paksha.SHUKLA) to "Pasankusa",
        Pair("Kartika", Paksha.KRISHNA) to "Rama",
        Pair("Kartika", Paksha.SHUKLA) to "Haribodhini (Utthana)",
        Pair("Margashirsha", Paksha.KRISHNA) to "Utpanna",
        Pair("Margashirsha", Paksha.SHUKLA) to "Mokshada",
        Pair("Pausha", Paksha.KRISHNA) to "Saphala",
        Pair("Pausha", Paksha.SHUKLA) to "Putrada",
        Pair("Magha", Paksha.KRISHNA) to "Shat-tila",
        Pair("Magha", Paksha.SHUKLA) to "Bhaimi (Jaya)",
        Pair("Phalguna", Paksha.KRISHNA) to "Vijaya",
        Pair("Phalguna", Paksha.SHUKLA) to "Amalaki"
    )

    /**
     * Determines the Vedic solar month approximation from Sun Longitude.
     */
    private fun getVedicMonth(sunLongDeg: Double): String {
        // Sidereal / Nirayana approximation (Lahiri Ayanamsha ~ 24.1°)
        var sidereal = sunLongDeg - 24.1
        if (sidereal < 0) sidereal += 360.0
        val sign = (sidereal / 30.0).toInt() % 12
        return when (sign) {
            0 -> "Vaishakha"   // Mesha
            1 -> "Jyeshtha"    // Vrishabha
            2 -> "Ashadha"     // Mithuna
            3 -> "Shravana"    // Karka
            4 -> "Bhadrapada"  // Simha
            5 -> "Ashvina"     // Kanya
            6 -> "Kartika"     // Tula
            7 -> "Margashirsha"// Vrishchika
            8 -> "Pausha"      // Dhanus
            9 -> "Magha"       // Makara
            10 -> "Phalguna"   // Kumbha
            else -> "Chaitra"  // Meena
        }
    }

    /**
     * Finds the next N Ekadashi events from a given date for the specified location.
     */
    fun getUpcomingEkadashis(
        startDate: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        count: Int = 12
    ): List<EkadashiEvent> {
        val results = mutableListOf<EkadashiEvent>()
        var curDate = startDate.minusDays(2)
        val maxDays = count * 20 + 30

        var day = 0
        while (results.size < count && day < maxDays) {
            val event = evaluateDayForEkadashi(curDate, latitude, longitude, zoneId)
            if (event != null && !event.fastDate.isBefore(startDate)) {
                // Prevent duplicate entries for same fast date
                if (results.none { it.fastDate == event.fastDate }) {
                    results.add(event)
                }
            }
            curDate = curDate.plusDays(1)
            day++
        }
        return results
    }

    /**
     * Evaluates whether a given date is an Ekadashi fasting day.
     * Implements strict Vaishnava / Gaudiya Smriti rules (Hari-bhakti-vilasa):
     * - Checks Tithi at Sunrise.
     * - Checks Dashami Vedha at Arunodaya (96 min before sunrise).
     * - If Dashami is present at Arunodaya, Ekadashi is Viddha -> Fast shifted to Dvadashi.
     */
    private fun evaluateDayForEkadashi(
        candidateDate: LocalDate,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): EkadashiEvent? {
        val solarTimes = MeeusSolarLunar.calculateSolarTimes(candidateDate, latitude, longitude, zoneId)
        val jdSunrise = MeeusSolarLunar.toJulianDay(solarTimes.sunrise)
        val jdArunodaya = MeeusSolarLunar.toJulianDay(solarTimes.arunodaya)

        val tithiSunrise = MeeusSolarLunar.getTithiNumber(jdSunrise)
        val tithiArunodaya = MeeusSolarLunar.getTithiNumber(jdArunodaya)

        // Check if candidate date is Ekadashi at sunrise (11 = Shukla, 26 = Krishna)
        val isEkadashiSunrise = (tithiSunrise == 11 || tithiSunrise == 26)
        // Check if candidate date is Dvadashi at sunrise, but previous day had Dashami-Viddha Ekadashi
        val isDvadashiSunrise = (tithiSunrise == 12 || tithiSunrise == 27)

        var fastDate = candidateDate
        var isShuddha = true
        var isMahadvadashi = false
        var note = "Shuddha Ekadashi (Pure Vaishnava observance)"
        var paksha = if (tithiSunrise == 11 || tithiSunrise == 12) Paksha.SHUKLA else Paksha.KRISHNA

        if (isEkadashiSunrise) {
            val dashamiTithi = if (paksha == Paksha.SHUKLA) 10 else 25
            // If Dashami was active at Arunodaya, Ekadashi is Viddha (contaminated)
            if (tithiArunodaya == dashamiTithi) {
                // Viddha Ekadashi: Vaishnavas do not fast today; they fast on Dvadashi (tomorrow)
                fastDate = candidateDate.plusDays(1)
                isShuddha = false
                isMahadvadashi = true
                note = "Observed on Dvadashi (Previous day was Dashami-Viddha)"
            } else {
                // Shuddha Ekadashi
                fastDate = candidateDate
                isShuddha = true
                isMahadvadashi = false
                note = "Shuddha Ekadashi (Unmixed with Dashami at Arunodaya)"
            }
        } else if (isDvadashiSunrise) {
            // Check if yesterday was Dashami-Viddha Ekadashi to avoid missing it if starting check on Dvadashi
            val yDate = candidateDate.minusDays(1)
            val ySolar = MeeusSolarLunar.calculateSolarTimes(yDate, latitude, longitude, zoneId)
            val yJdSunrise = MeeusSolarLunar.toJulianDay(ySolar.sunrise)
            val yJdArun = MeeusSolarLunar.toJulianDay(ySolar.arunodaya)
            val yTithiSr = MeeusSolarLunar.getTithiNumber(yJdSunrise)
            val yTithiAr = MeeusSolarLunar.getTithiNumber(yJdArun)

            val dashami = if (tithiSunrise == 12) 10 else 25
            val ekadashi = if (tithiSunrise == 12) 11 else 26
            if (yTithiSr == ekadashi && yTithiAr == dashami) {
                fastDate = candidateDate
                isShuddha = false
                isMahadvadashi = true
                note = "Mahadvadashi fast (Due to Dashami-Vedha on previous day)"
            } else {
                return null
            }
        } else {
            return null
        }

        // Fast day solar calculations
        val fastSolar = MeeusSolarLunar.calculateSolarTimes(fastDate, latitude, longitude, zoneId)
        val jdFastSr = MeeusSolarLunar.toJulianDay(fastSolar.sunrise)

        // Find exact start and end of this Ekadashi tithi
        val targetStartDeg = if (paksha == Paksha.SHUKLA) 120.0 else 300.0
        val targetEndDeg = if (paksha == Paksha.SHUKLA) 132.0 else 312.0
        val targetHariVasaraDeg = if (paksha == Paksha.SHUKLA) 135.0 else 315.0
        val targetDvadashiEndDeg = if (paksha == Paksha.SHUKLA) 144.0 else 324.0

        val jdFastSearchStart = jdFastSr - 2.0
        val jdFastSearchEnd = jdFastSr + 2.0

        val jdEkadashiStart = MeeusSolarLunar.findElongationCrossing(targetStartDeg, jdFastSearchStart, jdFastSr + 0.5)
        val jdEkadashiEnd = MeeusSolarLunar.findElongationCrossing(targetEndDeg, jdFastSr - 0.5, jdFastSearchEnd)

        val tithiStart = MeeusSolarLunar.fromJulianDay(jdEkadashiStart, zoneId)
        val tithiEnd = MeeusSolarLunar.fromJulianDay(jdEkadashiEnd, zoneId)

        // Parana Day (day after fasting)
        val paranaDate = fastDate.plusDays(1)
        val paranaSolar = MeeusSolarLunar.calculateSolarTimes(paranaDate, latitude, longitude, zoneId)
        val jdParanaSr = MeeusSolarLunar.toJulianDay(paranaSolar.sunrise)

        // Hari Vasara (first quarter of Dvadashi tithi: spans 3 degrees from 132° to 135° or 312° to 315°)
        val jdHariVasaraEnd = MeeusSolarLunar.findElongationCrossing(
            targetHariVasaraDeg,
            jdEkadashiEnd - 0.1,
            jdEkadashiEnd + 1.2
        )
        val hariVasaraEnd = MeeusSolarLunar.fromJulianDay(jdHariVasaraEnd, zoneId)

        // End of Dvadashi tithi
        val jdDvadashiEnd = MeeusSolarLunar.findElongationCrossing(
            targetDvadashiEndDeg,
            jdHariVasaraEnd - 0.1,
            jdHariVasaraEnd + 1.5
        )
        val dvadashiEnd = MeeusSolarLunar.fromJulianDay(jdDvadashiEnd, zoneId)

        // Parana Calculation Rules:
        // 1. Fast CANNOT be broken before Sunrise on Parana day.
        // 2. Fast CANNOT be broken during Hari Vasara (first quarter of Dvadashi).
        // 3. Fast MUST be broken before Dvadashi tithi ends, if Dvadashi ends in daytime.
        // 4. If Dvadashi lasts all day, break in Pratah-kala (first 1/3 of daylight, or before solar noon).
        val paranaSr = paranaSolar.sunrise
        val paranaNoon = paranaSolar.noon
        val dayDurationSec = ChronoUnit.SECONDS.between(paranaSr, paranaSolar.sunset)
        val pratahKalaEnd = paranaSr.plusSeconds(dayDurationSec / 3)

        val paranaStart: ZonedDateTime
        val paranaEnd: ZonedDateTime
        val paranaExplanation: String

        if (hariVasaraEnd.isAfter(paranaSr)) {
            // Hari Vasara ends after sunrise: must wait until Hari Vasara finishes
            paranaStart = hariVasaraEnd
            if (dvadashiEnd.isBefore(paranaNoon) && dvadashiEnd.isAfter(paranaStart)) {
                paranaEnd = dvadashiEnd
                paranaExplanation = "Break after Hari Vasara ends, before Dvadashi tithi ends."
            } else if (dvadashiEnd.isBefore(paranaStart)) {
                // Rare astronomical overlap: break immediately after Hari Vasara
                paranaEnd = paranaStart.plusHours(2).coerceAtMost(paranaNoon)
                paranaExplanation = "Break immediately after Hari Vasara ends."
            } else {
                paranaEnd = paranaNoon
                paranaExplanation = "Break after Hari Vasara ends, before Solar Noon."
            }
        } else {
            // Hari Vasara completed before sunrise: can break right at sunrise
            paranaStart = paranaSr
            if (dvadashiEnd.isBefore(paranaNoon) && dvadashiEnd.isAfter(paranaSr)) {
                paranaEnd = dvadashiEnd
                paranaExplanation = "Break between Sunrise and end of Dvadashi tithi."
            } else {
                paranaEnd = pratahKalaEnd
                paranaExplanation = "Break during Pratah-kala (morning period after Sunrise)."
            }
        }

        // Determine Ekadashi Name
        val sunLong = MeeusSolarLunar.getSunLongitude(jdFastSr)
        val masa = getVedicMonth(sunLong)
        val baseName = EKADASHI_NAMES[Pair(masa, paksha)] ?: "Vedic"
        val fullName = "$baseName Ekadashi"

        val eventId = "${fastDate.year}_${fastDate.monthValue}_${fastDate.dayOfMonth}_${paksha.name}"

        return EkadashiEvent(
            id = eventId,
            fastDate = fastDate,
            name = fullName,
            paksha = paksha,
            isShuddha = isShuddha,
            isMahadvadashi = isMahadvadashi,
            note = note,
            tithiStart = tithiStart,
            tithiEnd = tithiEnd,
            sunriseFastDay = fastSolar.sunrise,
            arunodayaFastDay = fastSolar.arunodaya,
            paranaDate = paranaDate,
            paranaStart = paranaStart,
            paranaEnd = paranaEnd,
            paranaExplanation = paranaExplanation,
            hariVasaraEnd = hariVasaraEnd,
            dvadashiEnd = dvadashiEnd
        )
    }

    private fun ZonedDateTime.coerceAtMost(other: ZonedDateTime): ZonedDateTime {
        return if (this.isAfter(other)) other else this
    }
}
