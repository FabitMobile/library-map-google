package ru.fabit.google

import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline

class MapObject(private val id: String) {
    val markers: MutableList<Marker> = mutableListOf()
    val polylines: MutableList<Polyline> = mutableListOf()
    val polygons: MutableList<Polygon> = mutableListOf()

    fun remove() {
        markers.forEach { it.remove() }
        markers.clear()
        polylines.forEach { it.remove() }
        polylines.clear()
        polygons.forEach { it.remove() }
        polygons.clear()
    }
    fun isEmpty(): Boolean {
        return markers.isEmpty() && polylines.isEmpty() && polygons.isEmpty()
    }
}