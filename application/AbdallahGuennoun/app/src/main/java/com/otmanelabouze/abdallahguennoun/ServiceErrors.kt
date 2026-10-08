package com.otmanelabouze.abdallahguennoun

internal fun serviceErrorMessage(error: Exception, lang: String): String {
    val permission = error is com.google.firebase.firestore.FirebaseFirestoreException && error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
    val network = error is com.google.firebase.FirebaseNetworkException || (error is com.google.firebase.firestore.FirebaseFirestoreException && error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE)
    val configuration = error is com.google.firebase.auth.FirebaseAuthException && error.errorCode in listOf("ERROR_OPERATION_NOT_ALLOWED", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_API_KEY")
    val category = when {permission -> "PERMISSION_DENIED"; network -> "NETWORK"; configuration -> "CONFIGURATION"; else -> "OTHER"}
    return serviceErrorForCode(category, lang)
}

internal fun serviceErrorForCode(category: String, lang: String): String {
    return when {
        category == "PERMISSION_DENIED" -> appText(lang,"تم تسجيل الدخول، لكن خدمة المؤسسة غير متاحة لحسابك حالياً. تواصل مع الإدارة للتحقق من الصلاحية وتفعيل الخدمة.","Connexion réussie, mais ce service n'est pas accessible à votre compte. Contactez l'administration.","Signed in, but this school service is unavailable to your account. Contact the school office.")
        category == "NETWORK" -> appText(lang,"تعذر الاتصال. تحقق من الإنترنت ثم أعد المحاولة.","Connexion impossible. Vérifiez Internet puis réessayez.","Unable to connect. Check your internet connection and retry.")
        category == "CONFIGURATION" -> appText(lang,"إعداد تسجيل الدخول يحتاج مراجعة من إدارة التطبيق. جرّب لاحقاً أو تواصل مع الإدارة.","La configuration de connexion doit être vérifiée par l'administration.","The sign-in configuration needs to be checked by the app administrator.")
        else -> appText(lang,"تعذر إتمام العملية. أعد المحاولة أو تواصل مع الإدارة إذا استمر المشكل.","Opération impossible. Réessayez ou contactez l'administration.","Unable to complete the operation. Retry or contact the school office.")
    }
}
