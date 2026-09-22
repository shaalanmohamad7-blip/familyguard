package com.familyguard.app.ui.disclosure

import androidx.lifecycle.ViewModel
import com.familyguard.app.data.local.SecureStorage

class DisclosureViewModel(private val secureStorage: SecureStorage) : ViewModel() {

    fun acceptDisclosure() {
        secureStorage.setDisclosureAccepted(true)
    }
}
