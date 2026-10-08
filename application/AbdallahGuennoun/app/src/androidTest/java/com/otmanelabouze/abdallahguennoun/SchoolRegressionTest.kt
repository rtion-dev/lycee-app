package com.otmanelabouze.abdallahguennoun

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchoolRegressionTest {
    @Test fun imageExpiryIsFixedAt24Hours() {
        val downloaded=1_700_000_000_000L
        assertTrue(SchoolMediaCache.fresh(downloaded,downloaded))
        assertTrue(SchoolMediaCache.fresh(downloaded,downloaded+SchoolMediaCache.TTL-1))
        assertFalse(SchoolMediaCache.fresh(downloaded,downloaded+SchoolMediaCache.TTL))
        assertFalse(SchoolMediaCache.fresh(downloaded,downloaded-1))
        assertFalse(SchoolMediaCache.fresh(0,downloaded))
    }

    @Test fun officialSchoolPagesStayEmbedded() {
        assertTrue(isSchoolWebUrl(SchoolWebsite.MASSAR.home))
        assertTrue(isSchoolWebUrl(SchoolWebsite.TELMIDTICE.home))
        assertTrue(isSchoolWebUrl("https://login.microsoftonline.com/common/oauth2/authorize"))
    }
    @Test fun lookalikeAndUnsafeUrlsAreNotEmbedded() {
        listOf("http://massarservice.men.gov.ma/", "https://men.gov.ma.evil.example/",
            "https://user@men.gov.ma/", "javascript:alert(1)", "file:///etc/passwd")
            .forEach { assertFalse(it,isSchoolWebUrl(it)) }
    }
    @Test fun bothArabicDigitSetsNormalize() {
        assertEquals("D144141647",normalizeDigits("D١٤٤١٤١٦٤٧"))
        assertEquals("1234567890",normalizeDigits("۱۲۳۴۵۶۷۸۹۰"))
    }
}
