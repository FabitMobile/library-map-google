package ru.fabit.google

interface LocationSource {
    fun startLocationUpdates(locationListener: LocationListener)
    fun stopLocationUpdates()
}