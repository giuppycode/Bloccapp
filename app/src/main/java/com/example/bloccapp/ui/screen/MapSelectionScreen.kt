package com.example.bloccapp.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.bloccapp.PermissionManager
import com.google.android.gms.location.LocationServices
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

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
    val lifecycleOwner = LocalLifecycleOwner.current

    // Stato della posizione e del raggio
    var selectedPoint by remember {
        mutableStateOf(
            if (initialLat != null && initialLng != null) GeoPoint(initialLat, initialLng)
            else GeoPoint(41.9028, 12.4964) // Roma default
        )
    }
    var radius by remember { mutableFloatStateOf(initialRadius) }

    // Riferimento alla MapView (creata una volta sola)
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.5)
            // Se abbiamo già i dati iniziali, centriamo subito
            if (initialLat != null && initialLng != null) {
                controller.setCenter(GeoPoint(initialLat, initialLng))
            }
        }
    }

    // Ricerca posizione attuale all'avvio (solo se non abbiamo coordinate iniziali)
    LaunchedEffect(Unit) {
        if (initialLat == null || initialLng == null) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentPoint = GeoPoint(location.latitude, location.longitude)
                        selectedPoint = currentPoint
                        mapView.controller.animateTo(currentPoint)
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    // Gestione del ciclo di vita della mappa
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
                    modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Radius: ${radius.toInt()}m", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = radius,
                        onValueChange = { radius = it },
                        valueRange = 50f..1000f,
                        steps = 19
                    )
                    Button(
                        onClick = { onConfirm(selectedPoint.latitude, selectedPoint.longitude, radius) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Selection")
                    }
                }
            }
        }
    ) { innerPadding ->
        val circleFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toArgb()
        val circleOutlineColor = MaterialTheme.colorScheme.primary.toArgb()

        AndroidView(
            factory = { mapView },
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            update = { view ->
                view.overlays.clear()

                // Overlay per intercettare i click sulla mappa
                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        selectedPoint = p
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint): Boolean = false
                }
                view.overlays.add(MapEventsOverlay(eventsReceiver))

                // Cerchio del raggio
                val circle = Polygon(view)
                val circlePoints = Polygon.pointsAsCircle(selectedPoint, radius.toDouble())
                circle.points = circlePoints
                circle.fillPaint.color = circleFillColor
                circle.outlinePaint.color = circleOutlineColor
                circle.outlinePaint.strokeWidth = 2f
                view.overlays.add(circle)

                // Marker centrale
                val marker = Marker(view)
                marker.position = selectedPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Block Area Center"
                marker.isDraggable = true
                marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(marker: Marker) {
                        selectedPoint = marker.position
                    }
                    override fun onMarkerDragEnd(marker: Marker) {
                        selectedPoint = marker.position
                    }
                    override fun onMarkerDragStart(marker: Marker) {}
                })
                view.overlays.add(marker)

                // Gestione rotazione (opzionale)
                view.overlays.add(RotationGestureOverlay(view))

                view.invalidate()
            }
        )
    }
}
