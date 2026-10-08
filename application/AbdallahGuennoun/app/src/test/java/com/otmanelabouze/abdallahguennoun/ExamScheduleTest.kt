package com.otmanelabouze.abdallahguennoun

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExamScheduleTest {
    @Test fun datesFollowLevelAndPublishedYearOnly() {
        assertEquals(LocalDate.of(2027,5,28),examDate("1BAC",2027))
        assertEquals(LocalDate.of(2027,6,1),examDate("2BAC",2027))
        assertNull(examDate("TC",2027))
        assertNull(examDate("2BAC",2028))
    }
    @Test fun schoolYearChangesAtLocalSeptemberBoundary() {
        val boundary=LocalDate.of(2026,9,1).atStartOfDay(SchoolTimeZone).toInstant()
        assertEquals(2026,examYearAt(boundary.minusSeconds(1)))
        assertEquals(2027,examYearAt(boundary))
    }
    @Test fun remainingTimeUsesMoroccanMidnight() {
        val date=LocalDate.of(2027,6,1)
        val now=date.atStartOfDay(SchoolTimeZone).toInstant().minusSeconds((2*1440+3*60+4)*60L)
        assertEquals(ExamRemaining(2,3,4,false),examRemaining(date,now))
    }
    @Test fun elapsedDatesNeverProduceNegativeValues() {
        assertEquals(ExamRemaining(0,0,0,true),examRemaining(LocalDate.of(2027,6,1),Instant.parse("2027-06-02T00:00:00Z")))
    }
}
