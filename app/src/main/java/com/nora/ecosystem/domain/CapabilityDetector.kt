package com.nora.ecosystem.domain

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.nora.ecosystem.core.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CapabilityDetector @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val dpm: DevicePolicyManager
) {
    private val pm get() = ctx.packageManager

    fun detectAll(): List<CapabilityResult> = listOf(
        deviceOwner(), profileOwner(), managedProfile(), virtualization(),
        perSpaceCellular(), independentPublicIp(), oemBackend()
    )

    fun deviceOwner(): CapabilityResult {
        val on = dpm.isDeviceOwnerApp(ctx.packageName)
        return CapabilityResult("DEVICE_OWNER", if (on) CapabilityStatus.AVAILABLE else CapabilityStatus.NOT_AVAILABLE,
            if (on) "App is Device Owner" else "Not provisioned as Device Owner (needs QR/NFC/adb provisioning on a reset, authorized device)",
            "TIER_3_DEVICE_OWNER", null)
    }
    fun profileOwner(): CapabilityResult {
        val on = dpm.isProfileOwnerApp(ctx.packageName)
        return CapabilityResult("PROFILE_OWNER", if (on) CapabilityStatus.AVAILABLE else CapabilityStatus.NOT_AVAILABLE,
            if (on) "App is Profile Owner" else "Not a Profile Owner", "TIER_2_PROFILE_OWNER", null)
    }
    fun managedProfile(): CapabilityResult {
        val f = pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)
        return CapabilityResult("MANAGED_PROFILE", if (f) CapabilityStatus.LIMITED else CapabilityStatus.NOT_AVAILABLE,
            if (f) "Device supports managed users; provisioning via official flow only" else "FEATURE_MANAGED_USERS absent",
            "TIER_2_PROFILE_OWNER", null)
    }
    fun virtualization(): CapabilityResult {
        val feature = pm.hasSystemFeature("android.software.virtualization_framework")
        val kvm = File("/dev/kvm").exists()
        return when {
            Build.VERSION.SDK_INT < 33 -> CapabilityResult("AVF_PVM", CapabilityStatus.NOT_AVAILABLE,
                "AVF needs Android 13+; this is API ${Build.VERSION.SDK_INT}", "TIER_5", null)
            !feature -> CapabilityResult("AVF_PVM", CapabilityStatus.HARDWARE_DEPENDENT,
                "android.software.virtualization_framework not advertised (kvm node present=$kvm)", "TIER_5", null)
            ctx.checkSelfPermission("android.permission.MANAGE_VIRTUAL_MACHINE") != PackageManager.PERMISSION_GRANTED ->
                CapabilityResult("AVF_PVM", CapabilityStatus.SYSTEM_PRIVILEGED,
                    "AVF present but MANAGE_VIRTUAL_MACHINE not granted to this app", "TIER_5", "android.permission.MANAGE_VIRTUAL_MACHINE")
            else -> CapabilityResult("AVF_PVM", CapabilityStatus.AVAILABLE, "AVF present and permission granted", "TIER_5", null)
        }
    }
    fun perSpaceCellular() = CapabilityResult("PER_SPACE_CELLULAR", CapabilityStatus.CARRIER_DEPENDENT,
        "SIM-to-profile assignment is not exposed to apps; needs OEM telephony + carrier support",
        "TIER_4_OEM_CARRIER", "android.permission.MODIFY_PHONE_STATE (privileged)")
    fun independentPublicIp() = CapabilityResult("INDEPENDENT_PUBLIC_IP", CapabilityStatus.CARRIER_DEPENDENT,
        "Different Space does not mean different public IP. $PUBLIC_IP_TEXT.", "TIER_4_OR_5_PLUS_NETWORK", null)
    fun oemBackend(): CapabilityResult {
        val priv = try {
            val f = ApplicationInfo::class.java.getDeclaredField("privateFlags").apply { isAccessible = true }
            val flag = ApplicationInfo::class.java.getDeclaredField("PRIVATE_FLAG_PRIVILEGED").apply { isAccessible = true }.getInt(null)
            (f.getInt(ctx.applicationInfo) and flag) != 0
        } catch (e: Throwable) { false }
        return CapabilityResult("OEM_BACKEND", if (priv) CapabilityStatus.AVAILABLE else CapabilityStatus.OEM_ONLY,
            if (priv) "Installed as privileged app" else "Not a privileged system app", "TIER_4_OEM", "signature|privileged")
    }
}
