package com.nora.ecosystem.core

enum class SpaceType { HOST, MANAGED_PROFILE, MANAGED_USER, VIRTUAL_MACHINE }
enum class IsolationLevel { APPLICATION, PROFILE, USER, SYSTEM, VIRTUAL_MACHINE }
enum class SpaceStatus { CREATED, RUNNING, PAUSED, STOPPED, ERROR, UNSUPPORTED }
enum class ManagementMode { UNMANAGED, PROFILE_OWNER, DEVICE_OWNER, OEM_MANAGED, UNKNOWN }
enum class NetworkBackend {
    SAME_HOST_NETWORK, MANAGED_PROFILE_POLICY, OEM_NETWORK_POLICY, DEDICATED_INTERFACE,
    DEDICATED_CELLULAR_SUBSCRIPTION, OEM_ROUTING, CARRIER_ROUTING, VIRTUAL_NETWORK,
    VIRTUAL_MACHINE_NETWORK, UNSUPPORTED
}
enum class CapabilityStatus {
    AVAILABLE, LIMITED, OEM_ONLY, SYSTEM_PRIVILEGED, CARRIER_DEPENDENT, HARDWARE_DEPENDENT, NOT_AVAILABLE
}

data class VirtualSpace(
    val id: String, val name: String, val type: SpaceType,
    val isolationLevel: IsolationLevel, val networkBackend: NetworkBackend, val status: SpaceStatus
)

data class SpaceNetworkConfig(
    val spaceId: String, val backend: NetworkBackend, val interfaceName: String?,
    val subscriptionId: Int?, val networkHandle: Long?, val ipv4: List<String>, val ipv6: List<String>,
    val dnsServers: List<String>, val gateway: String?, val routingPolicy: String?, val natMode: String?,
    val connectivityState: String, val capabilityFlags: Set<String>
)

data class CapabilityResult(
    val feature: String, val status: CapabilityStatus, val reason: String,
    val requiredDeployment: String?, val requiredPermission: String?
)

/** Structured result. Never report success for something the platform did not do. */
sealed class NoraResult<out T> {
    data class Success<T>(val data: T) : NoraResult<T>()
    data class Failure(val code: String, val message: String, val requiredTier: String) : NoraResult<Nothing>()
}

object Codes {
    const val UNSUPPORTED = "FEATURE_UNSUPPORTED"
    const val OEM = "FEATURE_OEM_REQUIRED"
    const val CARRIER = "FEATURE_CARRIER_REQUIRED"
    const val PRIVILEGED = "FEATURE_PRIVILEGED_PERMISSION_REQUIRED"
    const val HARDWARE = "FEATURE_HARDWARE_REQUIRED"
    const val HYPERVISOR = "FEATURE_HYPERVISOR_REQUIRED"
    const val DEVICE_OWNER = "FEATURE_DEVICE_OWNER_REQUIRED"
    const val PROFILE_OWNER = "FEATURE_PROFILE_OWNER_REQUIRED"
}
const val PUBLIC_IP_TEXT = "Not directly determined"
