package ru.fabit.google

interface PermissionProvider {

    fun isLocationPermissionGranted(): Boolean
}