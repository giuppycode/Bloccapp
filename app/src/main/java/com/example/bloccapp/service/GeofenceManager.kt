package com.example.bloccapp.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.bloccapp.data.db.entity.Block
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

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
        if (block.geofenceLat == null || block.geofenceLng == null || block.geofenceRadius == null) return

        val geofence = Geofence.Builder()
            .setRequestId(block.id.toString())
            .setCircularRegion(block.geofenceLat, block.geofenceLng, block.geofenceRadius)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence added for block ${block.id}")
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to add geofence for block ${block.id}", it)
            }
        }
    }

    fun removeGeofence(blockId: Long) {
        geofencingClient.removeGeofences(listOf(blockId.toString())).run {
            addOnSuccessListener {
                Log.d("GeofenceManager", "Geofence removed for block $blockId")
            }
            addOnFailureListener {
                Log.e("GeofenceManager", "Failed to remove geofence for block $blockId", it)
            }
        }
    }
}
