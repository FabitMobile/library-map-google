package ru.fabit.google

import com.google.android.gms.maps.model.LatLng

interface LocationListener {
    fun onNewLocation(latLng: LatLng)
}