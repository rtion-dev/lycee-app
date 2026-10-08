package com.otmanelabouze.abdallahguennoun

import java.net.URI

internal fun secureWebHost(url:String):String? = runCatching {
    val uri=URI(url)
    uri.host?.lowercase()?.takeIf {uri.scheme.equals("https",true) && uri.rawUserInfo==null && (uri.port==-1 || uri.port==443)}
}.getOrNull()

internal fun isSchoolWebUrl(url:String):Boolean {
    val host=secureWebHost(url) ?: return false
    return host=="men.gov.ma" || host.endsWith(".men.gov.ma") || host=="taalim.ma" || host.endsWith(".taalim.ma") || host=="login.microsoftonline.com" || host=="login.live.com"
}
