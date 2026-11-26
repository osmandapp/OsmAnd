package net.osmand.sensors.simulate

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: SimulatorViewModel by viewModels()

    private val bluetoothPermissions: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                SimulatorScreen()
            }
        }
    }

    private fun hasPermissions(): Boolean = bluetoothPermissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    private fun SimulatorScreen() {
        val simulator = viewModel.simulator
        val state by simulator.state.collectAsStateWithLifecycle()
        var services by rememberSaveable { mutableStateOf(listOf(SensorService.HEART_RATE)) }
        var advertiseMode by rememberSaveable { mutableStateOf(AdvertiseMode.ALL) }
        var options by rememberSaveable { mutableStateOf(SensorOption.defaults.toList()) }

        val config = SimulatorConfig(services, advertiseMode, options.toSet())
        val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (simulator.isBluetoothEnabled) simulator.start(config)
        }
        val startWithBluetooth = {
            if (simulator.isBluetoothEnabled) {
                simulator.start(config)
            } else {
                enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }
        val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (hasPermissions()) startWithBluetooth()
        }

        LaunchedEffect(state.running) {
            if (state.running) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("BLE Sensors Simulate", style = MaterialTheme.typography.headlineSmall)
                }
                item {
                    SettingsCard(
                        enabled = !state.running,
                        services = services, onServices = {
                            services = it
                            if (!AdvertiseMode.FIRST.isApplicable(it)) {
                                advertiseMode = AdvertiseMode.ALL
                            }
                        },
                        advertiseMode = advertiseMode, onAdvertiseMode = { advertiseMode = it },
                        options = options, onOptions = { options = it }
                    )
                }
                item {
                    if (state.running) {
                        OutlinedButton(onClick = { simulator.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Stop") }
                    } else {
                        Button(
                            onClick = { if (hasPermissions()) startWithBluetooth() else requestPermissions.launch(bluetoothPermissions) },
                            enabled = services.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Start") }
                    }
                }
                item { StatusCard(state) }
                if (state.running) {
                    val services = state.config?.services.orEmpty()
                    val sentOptions = state.config?.options.orEmpty()
                    state.readings?.let { item { ReadingsCard(it, services, sentOptions) } }
                }
                item { Text("Log", style = MaterialTheme.typography.titleMedium) }
                items(state.log) { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    enabled: Boolean,
    services: List<SensorService>, onServices: (List<SensorService>) -> Unit,
    advertiseMode: AdvertiseMode, onAdvertiseMode: (AdvertiseMode) -> Unit,
    options: List<SensorOption>, onOptions: (List<SensorOption>) -> Unit
) {
    var editedService by remember { mutableStateOf<SensorService?>(null) }
    editedService?.let { service ->
        SensorOptionsDialog(service, options, onOptions) { editedService = null }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Sensors", style = MaterialTheme.typography.titleMedium)
            Text("Advertised in the order they are checked, Battery is always added", style = MaterialTheme.typography.bodySmall)
            SensorService.sensors.forEach { item ->
                val index = services.indexOf(item)
                val title = if (index >= 0) "${index + 1}. ${item.sensorType}" else item.sensorType
                val onEdit = if (item.options.isNotEmpty()) ({ editedService = item }) else null
                CheckboxRow(title, item.title, index >= 0, enabled, onEdit) {
                    onServices(if (index >= 0) services - item else services + item)
                }
            }
            Text("Presets", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            SensorPreset.entries.forEach { preset ->
                TextButton(onClick = { onServices(preset.services) }, enabled = enabled) {
                    Text("${preset.title}: ${preset.description}")
                }
            }
            if (services.isNotEmpty()) {
                Text("Bluetooth name: \"${SensorService.bluetoothName(services)}\"", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Advertisement", style = MaterialTheme.typography.titleMedium)
            AdvertiseMode.entries.filter { it.isApplicable(services) }.forEach { item ->
                val services = item.advertisedServices(services).joinToString { it.title }.ifEmpty { "-" }
                RadioRow(item.title, services, item == advertiseMode, enabled) { onAdvertiseMode(item) }
            }
        }
    }
}

@Composable
private fun SensorOptionsDialog(
    service: SensorService,
    options: List<SensorOption>, onOptions: (List<SensorOption>) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text(service.sensorType) },
        text = {
            Column {
                service.options.forEach { option ->
                    val checked = option in options
                    CheckboxRow(option.title, option.description, checked, enabled = true) {
                        onOptions(if (checked) options - option else options + option)
                    }
                }
            }
        }
    )
}

@Composable
private fun CheckboxRow(
    title: String, subtitle: String, checked: Boolean, enabled: Boolean,
    onEdit: (() -> Unit)? = null, onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f).toggleable(value = checked, enabled = enabled, onValueChange = { onClick() }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit, enabled = enabled) {
                Icon(Icons.Filled.Edit, contentDescription = "Settings of $title")
            }
        }
    }
}

@Composable
private fun RadioRow(title: String, subtitle: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatusCard(state: SimulatorState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Status: ${state.status}", style = MaterialTheme.typography.titleMedium)
            state.deviceName?.let { Text("Seen by scanners as \"$it\", the Bluetooth name of this phone until Stop") }
            if (state.running) {
                if (state.centrals.isEmpty()) {
                    Text("No connected centrals")
                }
                state.centrals.forEach { central ->
                    Text("Connected: ${central.address}")
                    val subscriptions = central.subscriptions.entries.joinToString { "${it.key.shortName()} ${it.value}" }
                    Text("  subscriptions: ${subscriptions.ifEmpty { "none" }}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ReadingsCard(readings: Readings, services: List<SensorService>, options: Set<SensorOption>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Sent values", style = MaterialTheme.typography.titleMedium)
            if (SensorService.HEART_RATE in services) {
                Text("Heart rate: ${readings.heartRate} bpm")
            }
            if (SensorService.RUNNING_SPEED_CADENCE in services) {
                Text(String.format(Locale.US, "Running speed: %.2f m/s (%.1f km/h)", readings.speedMs, readings.speedMs * 3.6))
                Text("Running cadence: ${readings.cadence} steps/min")
                if (SensorOption.RUNNING_STRIDE_LENGTH in options) {
                    Text(String.format(Locale.US, "Stride length: %.2f m", readings.strideLengthM))
                }
                if (SensorOption.RUNNING_TOTAL_DISTANCE in options) {
                    Text(String.format(Locale.US, "Total distance: %.1f m", readings.totalDistanceM))
                }
            }
            if (SensorService.CYCLING_SPEED_CADENCE in services) {
                Text(String.format(Locale.US, "Bike speed: %.2f m/s (%.1f km/h), wheel 2086 mm", readings.bikeSpeedMs, readings.bikeSpeedMs * 3.6))
            }
            if (SensorService.CYCLING_SPEED_CADENCE in services || SensorService.CYCLING_POWER in services) {
                Text("Crank cadence: ${readings.crankCadence} rpm")
            }
            if (SensorService.CYCLING_POWER in services) {
                Text("Power: ${readings.powerW} W")
            }
            if (SensorService.HEALTH_THERMOMETER in services) {
                Text(String.format(Locale.US, "Temperature: %.2f °C", readings.temperatureC))
            }
            if (SensorService.BLOOD_PRESSURE in services) {
                Text("Blood pressure: ${readings.systolic}/${readings.diastolic} mmHg, pulse ${readings.heartRate}")
                Text("Cuff pressure: ${readings.cuffPressure} mmHg")
            }
            Text("Battery: ${readings.batteryLevel}%")
        }
    }
}