package com.example.bloccapp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error in geofencing event: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val isActive = geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
            val transitionType = if (isActive) "ENTER" else "EXIT"
            
            geofencingEvent.triggeringGeofences?.forEach { geofence ->
                val blockId = geofence.requestId.toLongOrNull() ?: return@forEach
                BlockingState.setGeofenceActive(blockId, isActive)
                Log.i("GeofenceReceiver", "Transition $transitionType detected for block $blockId. Active: $isActive")
            }
        } else {
            Log.w("GeofenceReceiver", "Unknown transition type: $geofenceTransition")
        }
    }
}
