package com.familyguard.app

import android.app.Application
import com.familyguard.app.util.NotificationHelper
import com.familyguard.app.work.WorkScheduler

class FamilyGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        NotificationHelper.ensureChannels(this)

        // Background reporting (heartbeat, usage) only ever runs while the device is paired;
        // WorkScheduler checks pairing state itself before doing anything.
        WorkScheduler.scheduleBackgroundWorkIfPaired(this)
    }
}
