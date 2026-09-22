package com.familyguard.app.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.familyguard.app.ServiceLocator
import com.familyguard.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging is used purely as a push-delivery transport — there is no Firebase
 * Auth and no Firestore anywhere in this app. The only data-message this app understands is
 * "request-location", which lets the parent dashboard's "refresh now" button trigger an
 * immediate one-time location fix instead of waiting for the next periodic report.
 */
class FamilyGuardMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!ServiceLocator.secureStorage.isPaired()) return
        scope.launch {
            ServiceLocator.deviceRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (!ServiceLocator.secureStorage.isPaired()) return

        val type = message.data[Constants.FCM_KEY_TYPE] ?: return
        if (type != Constants.FCM_TYPE_REQUEST_LOCATION) return

        scope.launch {
            val fix = ServiceLocator.locationHelper.getOneTimeLocation() ?: return@launch
            ServiceLocator.locationRepository.reportLocation(
                lat = fix.lat,
                lng = fix.lng,
                accuracyM = fix.accuracyM,
                source = Constants.SOURCE_PUSH_REQUEST
            )
        }
    }
}
