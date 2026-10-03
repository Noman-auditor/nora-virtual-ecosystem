package com.nora.ecosystem

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.nora.ecosystem.core.PUBLIC_IP_TEXT
import com.nora.ecosystem.domain.*
import com.nora.ecosystem.security.SecurityManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardVm @Inject constructor(
    val caps: CapabilityDetector, val dmm: DeviceManagementManager,
    val net: NetworkIsolationManager, val sec: SecurityManager
) : ViewModel()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface { Dashboard() } } }
    }
}

@Composable
fun Dashboard(vm: DashboardVm = hiltViewModel()) {
    val net = vm.net.hostNetwork()
    val results = vm.caps.detectAll()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pad = if (maxWidth < 400.dp) 8.dp else 16.dp
        LazyColumn(contentPadding = PaddingValues(pad), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("NORA VIRTUAL ECOSYSTEM", style = MaterialTheme.typography.titleLarge) }
            item { InfoCard("Device", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n${Build.MANUFACTURER} ${Build.MODEL}") }
            item { InfoCard("Management", "Mode: ${vm.dmm.getManagementMode()}\nDEVICE OWNER: ${if (vm.dmm.isDeviceOwner()) "ACTIVE" else "INACTIVE"}\nPROFILE OWNER: ${if (vm.dmm.isProfileOwner()) "ACTIVE" else "INACTIVE"}") }
            item { InfoCard("Security", "SELinux: ${vm.sec.selinuxState()}\nVerified boot: ${vm.sec.verifiedBootState()}") }
            item { InfoCard("Network", "Backend: ${net.backend}\nInterface: ${net.interfaceName ?: "UNKNOWN"}\nIPv4: ${net.ipv4.ifEmpty { listOf("UNKNOWN") }}\nIPv6: ${net.ipv6.ifEmpty { listOf("UNKNOWN") }}\nDNS: ${net.dnsServers.ifEmpty { listOf("UNKNOWN") }}\nGateway: ${net.gateway ?: "UNKNOWN"}\nPUBLIC IP: $PUBLIC_IP_TEXT") }
            items(results) { r -> InfoCard(r.feature, "${r.status}\n${r.reason}\nRequires: ${r.requiredDeployment ?: "-"}") }
        }
    }
}

@Composable
fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
