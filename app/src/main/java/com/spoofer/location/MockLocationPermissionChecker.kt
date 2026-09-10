package com.spoofer.location

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MockLocationPermissionChecker internal constructor(
    private val appOpsManager: AppOpsManager,
    private val uid: Int,
    private val packageName: String,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(
        appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager,
        uid = Process.myUid(),
        packageName = context.packageName,
    )

    @Suppress("DEPRECATION")
    fun isAllowed(): Boolean =
        runCatching {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                uid,
                packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
}
