package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AstronomicalSnapshot
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroInspectorSheet(
    snapshot: AstronomicalSnapshot?,
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
                .verticalScroll(rememberScrollState())
                .testTag("astro_inspector_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Astronomical Core (Offline)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "100% on-device ephemeris based on Jean Meeus Astronomical Algorithms. Zero internet requests or external APIs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (snapshot != null) {
                // Live Values
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LIVE CELESTIAL COORDINATES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        AstroRow("Julian Ephemeris Day (JD)", String.format("%.6f", snapshot.julianDay))
                        AstroRow("Sun Ecliptic Longitude (λ☉)", String.format("%.4f°", snapshot.sunLongitudeDeg))
                        AstroRow("Moon Ecliptic Longitude (λ☽)", String.format("%.4f°", snapshot.moonLongitudeDeg))
                        AstroRow("Moon-Sun Elongation (E)", String.format("%.4f°", snapshot.elongationDeg))
                        AstroRow("Current Tithi (#${snapshot.tithiNumber})", snapshot.tithiName)
                        AstroRow("Tithi Progress", "${snapshot.tithiProgressPercent}%")
                        AstroRow("Moon Phase Illumination", "${snapshot.moonIlluminationPercent}%")
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(6.dp))
                        AstroRow("Today Arunodaya (Dawn)", snapshot.todayArunodaya.format(TIME_FORMATTER))
                        AstroRow("Today Sunrise (Refraction 90°50')", snapshot.todaySunrise.format(TIME_FORMATTER))
                        AstroRow("Today Sunset", snapshot.todaySunset.format(TIME_FORMATTER))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Formula explanations
            Text(
                text = "Algorithmic Rules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            FormulaCard(
                title = "1. Tithi (Lunar Day)",
                formula = "Tithi = floor((λ_Moon - λ_Sun) / 12°) + 1",
                explanation = "• Shukla Ekadashi: 120° ≤ Elongation < 132°\n• Shukla Dvadashi: 132° ≤ Elongation < 144°\n• Krishna Ekadashi: 300° ≤ Elongation < 312°\n• Krishna Dvadashi: 312° ≤ Elongation < 324°"
            )

            Spacer(modifier = Modifier.height(10.dp))

            FormulaCard(
                title = "2. Dashami Vedha (Vaishnava Standard)",
                formula = "Arunodaya = Sunrise - 96 minutes (4 ghatikas)",
                explanation = "According to Hari-bhakti-vilasa, if the 10th tithi (Dashami) extends into Arunodaya, the Ekadashi is Viddha (contaminated). Fasting is deferred to Dvadashi (Mahadvadashi)."
            )

            Spacer(modifier = Modifier.height(10.dp))

            FormulaCard(
                title = "3. Parana & Hari Vasara Window",
                formula = "Hari Vasara = 1st Quarter of Dvadashi (12° × 1/4 = 3°)",
                explanation = "• Parana Start: max(Sunrise, Hari Vasara End)\n• Parana End: Dvadashi End or Pratah-kala (1/3 of daylight / midday).\n• Breaking fast during Hari Vasara or before Sunrise is strictly prohibited."
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AstroRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormulaCard(
    title: String,
    formula: String,
    explanation: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formula,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
