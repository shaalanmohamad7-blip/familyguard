package com.familyguard.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A tiny generic ViewModelProvider.Factory so screens can construct their ViewModel with
 * plain constructor arguments from [com.familyguard.app.ServiceLocator], without a DI
 * framework.
 */
class GenericViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}
