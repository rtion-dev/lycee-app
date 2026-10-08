package com.otmanelabouze.abdallahguennoun

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentAnswersTest {
    @Test fun normalizesMassarId() { assertEquals("A123456789", documentAnswer("massarId", " a123456789 ")) }
    @Test fun acceptsLeapDay() { assertEquals("2008-02-29", documentAnswer("birthDate", "2008-02-29")) }
    @Test(expected = java.time.format.DateTimeParseException::class)
    fun rejectsImpossibleDate() { documentAnswer("birthDate", "2009-02-29") }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsFutureDate() { documentAnswer("birthDate", "2999-01-01") }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyName() { documentAnswer("nameAr", "   ") }
    @Test fun rollNumberIsNumeric() { assertEquals(7L, documentAnswer("ordinalNumber", "7")) }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroRollNumber() { documentAnswer("ordinalNumber", "0") }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidMassarId() { documentAnswer("massarId", "12345") }
}
