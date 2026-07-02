package com.example.bloccapp.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.example.bloccapp.data.db.entity.Block
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(block: Block) {
        val lat = block.geofenceLat ?: return
        val lng = block.geofenceLng ?: return
        val radius = block.geofenceRadius ?: return

        val geofence = Geofence.Builder()
            .setRequestId(block.id.toString())
            .setCircularRegion(lat, lng, radius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence registered for block ${block.id}")
                // Controllo manuale immediato per attivazione istantanea se già dentro
                checkInitialInsideState(block.id, lat, lng, radius)
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to register geofence for block ${block.id}", it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkInitialInsideState(blockId: Long, targetLat: Double, targetLng: Double, radius: Float) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, targetLat, targetLng, results)
                val distance = results[0]
                val isInside = distance <= radius
                Log.d("GeofenceManager", "Initial check for block $blockId: distance=$distance, radius=$radius, isInside=$isInside")
                if (isInside) {
                    BlockingState.setGeofenceActive(blockId, true)
                }
            }
        }
    }

    fun removeGeofence(blockId: Long) {
        geofencingClient.removeGeofences(listOf(blockId.toString())).run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed for block $blockId")
                BlockingState.setGeofenceActive(blockId, false)
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to remove geofence for block $blockId", it)
            }
        }
    }
}
