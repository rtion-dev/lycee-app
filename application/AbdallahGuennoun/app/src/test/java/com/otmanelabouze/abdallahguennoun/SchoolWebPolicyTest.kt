package com.otmanelabouze.abdallahguennoun

import org.junit.Assert.*
import org.junit.Test

class SchoolWebPolicyTest {
    @Test fun officialSitesAndLoginRedirectsAreAccepted() {
        listOf("https://massarservice.men.gov.ma/moutamadris/Account","https://telmidtice.men.gov.ma/","https://taalim.ma/","https://login.microsoftonline.com/common/oauth2/authorize","https://login.live.com/").forEach {assertTrue(it,isSchoolWebUrl(it))}
    }
    @Test fun lookalikeDomainsAndUserInfoCannotBypassConfirmation() {
        listOf("https://men.gov.ma.attacker.test/","https://notmen.gov.ma/","https://men.gov.ma@attacker.test/","https://attacker.test@men.gov.ma/","https://login.microsoftonline.com.attacker.test/").forEach {assertFalse(it,isSchoolWebUrl(it))}
    }
    @Test fun unsafeSchemesAndNonStandardPortsAreRejected() {
        listOf("http://men.gov.ma/","file:///sdcard/file.html","content://private/file","javascript:alert(1)","intent://login","https://men.gov.ma:8443/","not a URL","https:///men.gov.ma").forEach {assertNull(it,secureWebHost(it))}
    }
    @Test fun externalHttpsLinksAreValidButNeedConfirmation() {
        assertEquals("example.org",secureWebHost("https://example.org/article"))
        assertFalse(isSchoolWebUrl("https://example.org/article"))
        assertTrue(isSchoolWebUrl("HTTPS://MEN.GOV.MA:443/"))
    }
}
