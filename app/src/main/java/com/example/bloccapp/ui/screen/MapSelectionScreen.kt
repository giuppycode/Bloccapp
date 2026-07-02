package com.example.bloccapp.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.bloccapp.PermissionManager
import androidx.compose.ui.platform.LocalContext

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectionScreen(
    initialLat: Double?,
    initialLng: Double?,
    initialRadius: Float,
    onBack: () -> Unit,
    onConfirm: (Double, Double, Float) -> Unit
) {
    val context = LocalContext.current
    var selectedLocation by remember {
        mutableStateOf(if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else LatLng(41.9028, 12.4964)) // Default Rome
    }
    var radius by remember { mutableFloatStateOf(initialRadius) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Area Selection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Radius: ${radius.toInt()}m", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..1000f,
                        steps = 19 // 50 to 1000 with 50m steps
                    )
                    Button(
                        onClick = { onConfirm(selectedLocation.latitude, selectedLocation.longitude, radius) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Selection")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { selectedLocation = it },
                properties = MapProperties(
                    isMyLocationEnabled = PermissionManager.hasLocationPermission(context)
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = selectedLocation),
                    title = "Block Area Center",
                    draggable = true
                )
                Circle(
                    center = selectedLocation,
                    radius = radius.toDouble(),
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    strokeColor = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2f
                )
            }
        }
    }
}
