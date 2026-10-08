package com.otmanelabouze.abdallahguennoun

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceErrorsTest {
    @Test fun permissionErrorDoesNotBlameGoogle() {
        val message = serviceErrorForCode("PERMISSION_DENIED", "en")
        assertTrue(message.startsWith("Signed in"))
        assertTrue(message.contains("school office"))
    }
    @Test fun networkErrorSuggestsConnectionRecovery() {
        assertTrue(serviceErrorForCode("NETWORK", "en").contains("internet connection"))
    }
    @Test fun internalErrorDetailsAreNotShown() {
        val message = serviceErrorForCode("private diagnostic", "en")
        assertTrue(!message.contains("private diagnostic"))
    }
}
