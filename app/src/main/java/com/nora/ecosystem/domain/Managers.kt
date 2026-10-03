package com.nora.ecosystem.domain

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nora.ecosystem.core.*
import com.nora.ecosystem.data.SpaceDao
import com.nora.ecosystem.data.SpaceEntity
import com.nora.ecosystem.profile.NoraDeviceAdminReceiver
import com.nora.ecosystem.security.AuditLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceManagementManager @Inject constructor(
    @ApplicationContext private val ctx: Context, private val dpm: DevicePolicyManager
) {
    val admin get() = ComponentName(ctx, NoraDeviceAdminReceiver::class.java)
    fun isDeviceOwner() = dpm.isDeviceOwnerApp(ctx.packageName)
    fun isProfileOwner() = dpm.isProfileOwnerApp(ctx.packageName)
    fun getManagementMode() = when {
        isDeviceOwner() -> ManagementMode.DEVICE_OWNER
        isProfileOwner() -> ManagementMode.PROFILE_OWNER
        else -> ManagementMode.UNMANAGED
    }
    fun provisioningState() = when {
        isDeviceOwner() -> "DEVICE_OWNER_ACTIVE"
        dpm.isAdminActive(admin) -> "ADMIN_ACTIVE_NOT_OWNER"
        else -> "UNMANAGED"
    }
}

@Singleton
class NetworkIsolationManager @Inject constructor(private val cm: ConnectivityManager) {
    /** Host network only. Unknown values are null/UNKNOWN, never invented. */
    fun hostNetwork(): SpaceNetworkConfig {
        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val lp = net?.let { cm.getLinkProperties(it) }
        return SpaceNetworkConfig(
            spaceId = "HOST", backend = NetworkBackend.SAME_HOST_NETWORK,
            interfaceName = lp?.interfaceName, subscriptionId = null, networkHandle = net?.networkHandle,
            ipv4 = lp?.linkAddresses?.filter { it.address is Inet4Address }?.mapNotNull { it.address.hostAddress } ?: emptyList(),
            ipv6 = lp?.linkAddresses?.filter { it.address is Inet6Address }?.mapNotNull { it.address.hostAddress } ?: emptyList(),
            dnsServers = lp?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList(),
            gateway = lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress,
            routingPolicy = null, natMode = null,
            connectivityState = if (caps == null) "UNKNOWN" else "CONNECTED",
            capabilityFlags = buildSet {
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("WIFI")
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("CELLULAR")
                if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false) add("METERED")
            }
        )
    }
}

@Singleton
class SpaceManager @Inject constructor(
    private val dmm: DeviceManagementManager,
    private val caps: CapabilityDetector,
    private val dao: SpaceDao,
    private val audit: AuditLogger
) {
    suspend fun createSpace(name: String, type: SpaceType, iso: IsolationLevel, net: NetworkBackend): NoraResult<VirtualSpace> {
        if (type == SpaceType.VIRTUAL_MACHINE || iso == IsolationLevel.VIRTUAL_MACHINE) {
            val c = caps.virtualization()
            return NoraResult.Failure(Codes.HYPERVISOR, "VM spaces: ${c.reason}. VM lifecycle is not implemented in this scaffold.", "TIER_5")
        }
        if (iso == IsolationLevel.SYSTEM) return NoraResult.Failure(Codes.OEM, "SYSTEM isolation requires an OEM build", "TIER_4")
        if (net != NetworkBackend.SAME_HOST_NETWORK && net != NetworkBackend.MANAGED_PROFILE_POLICY) {
            val code = if (net == NetworkBackend.DEDICATED_CELLULAR_SUBSCRIPTION) Codes.CARRIER else Codes.OEM
            return NoraResult.Failure(code, "$net is CARRIER/OEM DEPENDENT. Status: NOT AVAILABLE", "TIER_4")
        }
        if (type == SpaceType.MANAGED_PROFILE && !dmm.isProfileOwner() && !dmm.isDeviceOwner())
            return NoraResult.Failure(Codes.PROFILE_OWNER, "Not a Profile/Device Owner", "TIER_2")
        if (type == SpaceType.MANAGED_USER && !dmm.isDeviceOwner())
            return NoraResult.Failure(Codes.DEVICE_OWNER, "Secondary users need Device Owner (and OEM allowance)", "TIER_3")
        val space = VirtualSpace(UUID.randomUUID().toString(), name, type, iso, net, SpaceStatus.CREATED)
        dao.insert(SpaceEntity.fromDomain(space))
        audit.log("SPACE_CREATE", "${space.id}:${space.type}")
        return NoraResult.Success(space)
    }
}

data class DevicePolicy(
    val cameraDisabled: Boolean? = null,
    val userRestrictions: Map<String, Boolean> = emptyMap(),
    val requireSeparatePublicIp: Boolean = false
)

@Singleton
class PolicyEngine @Inject constructor(
    private val dpm: DevicePolicyManager, private val dmm: DeviceManagementManager, private val audit: AuditLogger
) {
    suspend fun apply(p: DevicePolicy): NoraResult<Unit> {
        if (!dmm.isDeviceOwner() && !dmm.isProfileOwner())
            return NoraResult.Failure(Codes.DEVICE_OWNER, "Policies need Device Owner or Profile Owner", "TIER_2/3")
        val a = dmm.admin
        p.cameraDisabled?.let { dpm.setCameraDisabled(a, it) }
        p.userRestrictions.forEach { (k, on) -> if (on) dpm.addUserRestriction(a, k) else dpm.clearUserRestriction(a, k) }
        audit.log("POLICY_APPLY")
        if (p.requireSeparatePublicIp)
            return NoraResult.Failure(Codes.OEM, "Configured but not enforceable on this deployment", "TIER_4")
        return NoraResult.Success(Unit)
    }
}

@Singleton
class KioskManager @Inject constructor(
    private val dpm: DevicePolicyManager, private val dmm: DeviceManagementManager, private val audit: AuditLogger
) {
    suspend fun setAllowed(packages: List<String>): NoraResult<Unit> {
        if (!dmm.isDeviceOwner()) return NoraResult.Failure(Codes.DEVICE_OWNER, "Lock-task kiosk requires Device Owner", "TIER_3")
        dpm.setLockTaskPackages(dmm.admin, packages.toTypedArray())
        audit.log("KIOSK_SET", packages.joinToString())
        return NoraResult.Success(Unit)
    }
    fun allowed(): List<String> =
        if (dmm.isDeviceOwner()) dpm.getLockTaskPackages(dmm.admin).toList() else emptyList()
}
