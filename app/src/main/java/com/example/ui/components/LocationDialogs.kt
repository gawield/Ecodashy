package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.EditLocation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.GeoPoint
import com.example.location.LocationHelper
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    currentLocation: GeoPoint,
    onSelectLocation: (GeoPoint) -> Unit,
    onDetectGps: () -> Unit,
    onOpenCustomDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("location_picker_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Auto GPS
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDetectGps()
                        onDismiss()
                    }
                    .testTag("detect_gps_action")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Auto-detect via Device GPS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Reads coordinates directly from hardware GPS (offline)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Custom Coordinates
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenCustomDialog()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_custom_coords_button")
            ) {
                Icon(imageVector = Icons.Outlined.EditLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter Exact Coordinates Manually")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preset Cities",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
            ) {
                items(LocationHelper.PRESET_LOCATIONS) { point ->
                    val isSelected = point.name == currentLocation.name
                    ListItem(
                        headlineContent = {
                            Text(
                                text = point.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${point.latitude}°, ${point.longitude}° • ${point.zoneId.id}",
                                fontSize = 12.sp
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .clickable {
                                onSelectLocation(point)
                                onDismiss()
                            }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CustomCoordinatesDialog(
    initialPoint: GeoPoint,
    onConfirm: (name: String, lat: Double, lon: Double, zone: ZoneId) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialPoint.name) }
    var latText by remember { mutableStateOf(initialPoint.latitude.toString()) }
    var lonText by remember { mutableStateOf(initialPoint.longitude.toString()) }
    var zoneIdText by remember { mutableStateOf(initialPoint.zoneId.id) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Custom Coordinates", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Specify any location on Earth to calculate Ekadashi and Parana times completely offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_name_field")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude (-90 to +90)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_lat_field")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lonText,
                    onValueChange = { lonText = it },
                    label = { Text("Longitude (-180 to +180)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_lon_field")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = zoneIdText,
                    onValueChange = { zoneIdText = it },
                    label = { Text("Time Zone ID (e.g. Europe/Moscow)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_zone_field")
                )

                errorText?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lon = lonText.toDoubleOrNull()
                    if (lat == null || lat < -90.0 || lat > 90.0) {
                        errorText = "Invalid Latitude. Must be between -90 and 90."
                        return@Button
                    }
                    if (lon == null || lon < -180.0 || lon > 180.0) {
                        errorText = "Invalid Longitude. Must be between -180 and 180."
                        return@Button
                    }
                    val zone = try {
                        ZoneId.of(zoneIdText.trim())
                    } catch (e: Exception) {
                        errorText = "Invalid Timezone. Use valid IANA format (e.g. Europe/Moscow, Asia/Kolkata)."
                        return@Button
                    }
                    onConfirm(name.trim(), lat, lon, zone)
                },
                modifier = Modifier.testTag("confirm_custom_coords_button")
            ) {
                Text("Calculate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FastingGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekadashi Fasting & Parana", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                Text(
                    text = "What is Ekadashi?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ekadashi is the 11th lunar day (tithi) of each fortnight. In the Vaishnava tradition, it is dedicated to spiritual remembrance and fasting from all grains, beans, and lentils.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Items to Avoid:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "• Grains & flour (wheat, rice, oats, barley, corn)\n• Beans, peas, lentils, dal, chickpeas\n• Mustard, sesame seeds, hing (asafoetida)",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Permitted Items (Anukalpa):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• Fresh fruits, dry fruits, nuts\n• Milk, yogurt, ghee, paneer\n• Potatoes, carrots, pumpkin, ginger, rock salt (sendha namak), black pepper\n• Buckwheat (kuttu), tapioca (sabudana), water chestnut (singhara)",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Breaking the Fast (Parana):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Fast must be broken strictly within the calculated Parana window on the next morning (Dvadashi), after sunrise and after Hari Vasara ends, ideally by honoring grains or water.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}
