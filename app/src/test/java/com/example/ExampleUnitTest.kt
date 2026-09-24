package com.example

import com.example.astro.EkadashiCalculator
import com.example.astro.MeeusSolarLunar
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExampleUnitTest {

    @Test
    fun testJulianDay() {
        // Standard astronomical test point: 2000-01-01 12:00 UTC = JD 2451545.0
        val zdt = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneId.of("UTC"))
        val jd = MeeusSolarLunar.toJulianDay(zdt)
        assertEquals(2451545.0, jd, 0.001)

        val back = MeeusSolarLunar.fromJulianDay(jd, ZoneId.of("UTC"))
        assertEquals(2000, back.year)
        assertEquals(1, back.monthValue)
        assertEquals(1, back.dayOfMonth)
        assertEquals(12, back.hour)
    }

    @Test
    fun testSunAndMoonPositions() {
        val zdt = ZonedDateTime.of(2026, 9, 24, 8, 30, 0, 0, ZoneId.of("UTC"))
        val jd = MeeusSolarLunar.toJulianDay(zdt)

        val sunLong = MeeusSolarLunar.getSunLongitude(jd)
        assertTrue(sunLong in 0.0..360.0)

        val moonLong = MeeusSolarLunar.getMoonLongitude(jd)
        assertTrue(moonLong in 0.0..360.0)

        val elong = MeeusSolarLunar.getMoonElongation(jd)
        assertTrue(elong in 0.0..360.0)

        val tithi = MeeusSolarLunar.getTithiNumber(jd)
        assertTrue(tithi in 1..30)
    }

    @Test
    fun testSolarTimesAndArunodaya() {
        val date = LocalDate.of(2026, 10, 1)
        val lat = 55.7558 // Moscow
        val lon = 37.6173
        val zone = ZoneId.of("Europe/Moscow")

        val times = MeeusSolarLunar.calculateSolarTimes(date, lat, lon, zone)
        assertNotNull(times.sunrise)
        assertNotNull(times.sunset)
        assertTrue(times.sunrise.isBefore(times.sunset))

        // Arunodaya must be exactly 96 minutes (4 ghatikas) before sunrise
        val diffMinutes = java.time.Duration.between(times.arunodaya, times.sunrise).toMinutes()
        assertEquals(96L, diffMinutes)
    }

    @Test
    fun testEkadashiEventsCalculation() {
        val startDate = LocalDate.of(2026, 10, 1)
        val lat = 23.4233 // Mayapur
        val lon = 88.3888
        val zone = ZoneId.of("Asia/Kolkata")

        val events = EkadashiCalculator.getUpcomingEkadashis(startDate, lat, lon, zone, count = 4)
        assertEquals(4, events.size)

        for (event in events) {
            assertTrue(event.name.contains("Ekadashi"))
            assertTrue(event.paranaDate.isAfter(event.fastDate) || event.paranaDate.isEqual(event.fastDate.plusDays(1)))
            assertTrue(event.paranaStart.isBefore(event.paranaEnd))
            assertNotNull(event.paranaExplanation)
        }
    }
}
