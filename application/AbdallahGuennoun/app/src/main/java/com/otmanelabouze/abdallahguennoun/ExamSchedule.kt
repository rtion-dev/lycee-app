package com.otmanelabouze.abdallahguennoun

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal val SchoolTimeZone:ZoneId=ZoneId.of("Africa/Casablanca")

// Regular session, 2026–2027 school calendar (ministerial decision 047.26).
// These are dates, not confirmed times of the first exam paper. Count to local midnight.
// Never advance this calendar automatically into an unpublished school year.
internal fun examDate(level:String,examYear:Int):LocalDate? = when {
    examYear!=2027 -> null
    level=="1BAC" -> LocalDate.of(2027,5,28)
    level=="2BAC" -> LocalDate.of(2027,6,1)
    else -> null
}

internal fun examYearAt(now:Instant):Int {
    val local=now.atZone(SchoolTimeZone)
    return local.year+if(local.monthValue>=9)1 else 0
}

internal data class ExamRemaining(val days:Long,val hours:Long,val minutes:Long,val started:Boolean)

internal fun examRemaining(date:LocalDate,now:Instant):ExamRemaining {
    val target=date.atStartOfDay(SchoolTimeZone).toInstant()
    val total=Duration.between(now,target).toMinutes().coerceAtLeast(0)
    return ExamRemaining(total/1440,(total%1440)/60,total%60,!now.isBefore(target))
}
