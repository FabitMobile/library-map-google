package ru.fabit.google

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper.getMainLooper
import android.view.View
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import ru.fabit.map.dependencies.factory.GeometryColorFactory
import ru.fabit.map.dependencies.factory.MarkerBitmapFactory
import ru.fabit.map.internal.domain.entity.*
import ru.fabit.map.internal.domain.entity.marker.*
import ru.fabit.map.internal.domain.entity.marker.Marker
import ru.fabit.map.internal.domain.listener.*
import ru.fabit.map.internal.protocol.MapProtocol

class GoogleMapWrapper(
    private val context: Context,
    private val mapStyleProvider: GoogleMapStyleProviderImpl,
    private val permissionProvider: PermissionProvider,
    private val markerCalculator: MarkerCalculator,
    private val markerBitmapFactory: MarkerBitmapFactory,
    private val isEnabledFreeSpaces: Boolean,
    private val geometryColorFactory: GeometryColorFactory,
    private val isEnabledPolylineMapObject: Boolean,
    private val locationSource: LocationSource,
    private val uiSettings: UiSettings
) : MapProtocol, DiffCallback {

    private var isDebug: Boolean = false
    private var mapView: MapView? = null
    private var visibleMapRegionListeners = mutableListOf<VisibleMapRegionListener>()
    private var mapListeners = mutableListOf<MapListener>()
    private var mapLocationListeners = mutableListOf<MapLocationListener>()
    private var layoutChangeListeners = mutableListOf<View.OnLayoutChangeListener>()
    private var sizeChangedListeners = mutableListOf<SizeChangeListener>()
    private var googleMap: GoogleMap? = null
    private val uiThreadHandler: Handler
    private val DURATION_200MS: Int = 200
    private val mapObjectHashMap: MutableMap<String, MapObject> = mutableMapOf()
    private var payableZones: List<String> = listOf()
    private var isColoredMarkersEnabled: Boolean = false
    private var isDisabledOn: Boolean = false
    private var isRadarOn: Boolean = false
    private var bitmapHashMap: MutableMap<String, Bitmap> = mutableMapOf()
    private var isEnableLocation: Boolean = false
    private var userLocation: LatLng? = null
    private val DEFAULT_ZOOM: Float = 18f
    private var offsetBottom: Float = 0f
    private var isAnimationRun: Boolean = false

    init {
        val mainLooper = getMainLooper()
        this.uiThreadHandler = Handler(mainLooper)
    }

    override fun isDebugMode(): Boolean = isDebug

    override fun setDebugMode(isDebug: Boolean) {
        this.isDebug = isDebug
    }

    override fun setPayableZones(payableZones: List<String>) {
        this.payableZones = payableZones
    }

    override fun setUniqueColorForComParking(uniqueColorForComParking: Boolean) {

    }

    override fun createGeoJsonLayer() {

    }

    override fun disableMap() {
        mapView = null
    }

    override fun enableMap() {
        mapView = MapView(context)
    }

    override fun init(style: String) {
        initializedMap(style)
    }

    private fun initializedMap(
        style: String,
        onInitializedListener: (googleMap: GoogleMap) -> Unit = {}
    ) {
        mapView?.getMapAsync { googleMap ->
            this.googleMap = googleMap
            googleMap.uiSettings.isRotateGesturesEnabled = uiSettings.isRotateGesturesEnabled
            googleMap.uiSettings.isTiltGesturesEnabled = uiSettings.isTiltGesturesEnabled
            googleMap.uiSettings.isCompassEnabled = uiSettings.isCompassEnabled
            googleMap.setOnCameraMoveListener(OnCameraMoveListener(googleMap))
            googleMap.setOnMapClickListener { latLng ->
                mapListeners.forEach { listener ->
                    listener.onMapTap(MapPoint(latLng.longitude, latLng.latitude))
                }
            }
            googleMap.setOnMarkerClickListener { mapMarker ->
                mapListeners.forEach { listener ->
                    val marker = mapMarker.tag as Marker
                    listener.onMarkerClicked(marker)
                }
                true
            }
            googleMap.setOnPolygonClickListener { polygon ->
                mapListeners.forEach { listener ->
                    val marker = polygon.tag as Marker
                    listener.onMarkerClicked(marker)
                }
            }
            enableLocation(isEnableLocation)
            onInitializedListener(googleMap)
        }
    }

    override fun start() {
        mapView?.onStart()
    }

    override fun stop() {
        mapView?.onStop()
    }

    override fun getMapView(): View? = mapView

    override fun setFocusRect(
        topLeftX: Float,
        topLeftY: Float,
        bottomRightX: Float,
        bottomRightY: Float
    ) {
        offsetBottom = (mapView?.height ?: 0) - bottomRightY
        googleMap?.cameraPosition?.let {
            emitUpdateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    it.target,
                    it.zoom
                )
            )
        }
    }

    override fun clearCache(id: String) {

    }

    override fun updateVersionCache(time: String) {

    }

    @SuppressLint("MissingPermission")
    override fun destroy() {
        visibleMapRegionListeners.clear()
        mapListeners.clear()
        mapLocationListeners.clear()
        layoutChangeListeners.clear()
        mapView?.onPause()
        mapView?.onStop()
        mapView?.onDestroy()
    }

    override fun create(savedInstanceState: Bundle?) {
        mapView?.onCreate(savedInstanceState)
    }

    override fun resume() {
        mapView?.onResume()
    }

    override fun pause() {
        mapView?.onPause()
    }

    override fun saveInstanceState(outState: Bundle) {
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        mapView?.onLowMemory()
    }

    override fun addVisibleMapRegionListener(visibleMapRegionListener: VisibleMapRegionListener) {
        visibleMapRegionListeners.add(visibleMapRegionListener)
    }

    override fun removeVisibleRegionListeners() {
        visibleMapRegionListeners.clear()
    }

    override fun removeVisibleRegionListener(visibleMapRegionListener: VisibleMapRegionListener) {
        visibleMapRegionListeners.remove(visibleMapRegionListener)
    }

    override fun addMapListener(mapListener: MapListener) {
        mapListeners.add(mapListener)
    }

    override fun removeMapListeners() {
        mapListeners.clear()
    }

    override fun removeMapListener(mapListener: MapListener) {
        mapListeners.remove(mapListener)
    }

    override fun addMapLocationListener(mapLocationListener: MapLocationListener) {
        this.mapLocationListeners.add(mapLocationListener)
    }

    override fun removeMapLocationListeners() {
        this.mapLocationListeners.clear()
    }

    override fun removeMapLocationListener(mapLocationListener: MapLocationListener) {
        this.mapLocationListeners.remove(mapLocationListener)
    }

    override fun addLayoutChangeListener(layoutChangeListener: View.OnLayoutChangeListener) {
        this.layoutChangeListeners.add(layoutChangeListener)
    }

    override fun removeLayoutChangeListeners() {
        this.layoutChangeListeners.clear()
    }

    override fun removeLayoutChangeListener(layoutChangeListener: View.OnLayoutChangeListener) {
        this.layoutChangeListeners.remove(layoutChangeListener)
    }

    override fun addSizeChangeListener(sizeChangeListener: SizeChangeListener) {
        this.sizeChangedListeners.add(sizeChangeListener)
    }

    override fun removeSizeChangeListeners() {
        this.sizeChangedListeners.clear()
    }

    override fun removeSizeChangeListener(sizeChangeListener: SizeChangeListener) {
        this.sizeChangedListeners.remove(sizeChangeListener)
    }

    override fun drawQuad(key: String, rect: Rect, color: Int) {

    }

    @SuppressLint("MissingPermission")
    override fun enableLocation(enable: Boolean?) {
        isEnableLocation = permissionProvider.isLocationPermissionGranted() && enable == true
        googleMap?.isMyLocationEnabled = isEnableLocation
        if (isEnableLocation) {
            locationSource.startLocationUpdates(object : LocationListener {
                override fun onNewLocation(latLng: LatLng) {
                    userLocation = latLng
                    mapLocationListeners.forEach { listener ->
                        listener.onLocationUpdate(MapCoordinates(latLng.latitude, latLng.longitude))
                    }
                }
            }
            )
        } else {
            locationSource.stopLocationUpdates()
        }
    }

    override fun onMarkersUpdated(
        oldMarkers: MutableMap<String, Marker>,
        newMarkers: MutableMap<String, Marker>,
        zoom: Float
    ) {
        markerCalculator.calculateDiff(oldMarkers, newMarkers, this)
    }

    override fun isAnimatedMarkersEnabled(): Boolean = true

    override fun moveCameraPosition(latitude: Double, longitude: Double) {
        uiThreadHandler.post {
            moveCameraPosition(latitude, longitude, DEFAULT_ZOOM)
        }
    }

    override fun moveCameraPositionWithZoom(latitude: Double, longitude: Double, zoom: Float) {
        moveCameraPosition(latitude, longitude, zoom)
    }

    override fun moveCameraPositionWithBounds(mapBounds: MapBounds) {
        val southWest = LatLng(mapBounds.minLat, mapBounds.minLon)
        val northEast = LatLng(mapBounds.maxLat, mapBounds.maxLon)
        val latLngBounds = LatLngBounds(southWest, northEast)
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 0)
        updateCamera(cameraUpdate)
    }

    override fun moveCameraZoomAndPosition(latitude: Double, longitude: Double, zoom: Float) {
        moveCameraPosition(latitude, longitude, zoom)
    }

    override fun moveToUserLocation(defaultCoordinates: MapCoordinates?) {
        defaultCoordinates?.let { mapCoordinates ->
            moveCameraPosition(mapCoordinates.latitude, mapCoordinates.longitude, DEFAULT_ZOOM)
        }
    }

    override fun moveToUserLocation(zoom: Float, defaultCoordinates: MapCoordinates?) {
        defaultCoordinates?.let { mapCoordinates ->
            moveCameraPosition(mapCoordinates.latitude, mapCoordinates.longitude, zoom)
        } ?: userLocation?.let { latLng ->
            moveCameraPosition(latLng.latitude, latLng.longitude, zoom)
        }
    }

    override fun tryMoveToUserLocation(
        zoom: Float,
        defaultCoordinates: MapCoordinates,
        mapCallback: MapCallback
    ) {
        moveCameraPosition(defaultCoordinates.latitude, defaultCoordinates.longitude, zoom)
    }

    override fun deselect(markerToDeselect: Marker) {
        uiThreadHandler.post {
            val mapObject = mapObjectHashMap[markerToDeselect.id]
            if (mapObject != null) {
                onUpdated(listOf(markerToDeselect))
            }
        }
    }

    override fun selectMarker(markerToSelect: Marker) {
        uiThreadHandler.post {
            val mapObject = mapObjectHashMap[markerToSelect.id]
            if (mapObject != null) {
                onUpdated(listOf(markerToSelect))
            }
        }
    }

    override fun zoomIn() {
        updateCamera(CameraUpdateFactory.zoomIn())
    }

    override fun zoomOut() {
        updateCamera(CameraUpdateFactory.zoomOut())
    }

    override fun getVisibleRegionByZoomAndPoint(
        zoom: Float,
        latitude: Double,
        longitude: Double
    ): MapBounds {
        return MapBounds(0.0, 0.0, 0.0, 0.0)
    }

    override fun radarStateChange(
        isRadarOn: Boolean,
        isColoredMarkersEnabled: Boolean,
        markers: Collection<Marker>
    ) {
        this.isRadarOn = isRadarOn
        this.isColoredMarkersEnabled = isColoredMarkersEnabled
    }

    override fun onDisabledChange(isDisabledOn: Boolean) {
        this.isDisabledOn = isDisabledOn
    }

    override fun setAnimationMarkerListener(animationMarkerListener: AnimationMarkerListener) {

    }

    override fun setStyle(style: String) {
    }

    override fun getMapStyleProvider() = mapStyleProvider

    override fun onAdded(markers: List<Marker>) {
        createBitmaps(markers)
        uiThreadHandler.post {
            for (marker in markers) {
                val mapObject = MapObject(marker.id)

                if (marker.type == MarkerType.ANIMATION && (marker as AnimationMarker).showAnimation) {
//                drawAnimationMarker()
                } else {
                    if (marker.type != MarkerType.NO_MARKER) {
                        val id = getImageProviderId(marker)
                        googleMap?.addMarker(MarkerOptions().apply {
                            position(LatLng(marker.latitude, marker.longitude))
                            anchor(0.5f, 0.5f)
                            bitmapHashMap[id]?.let { icon(BitmapDescriptorFactory.fromBitmap(it)) }
                        })?.let {
                            mapObject.markers.add(it)
                            it.tag = marker
                        }
                    }
                    addShape(marker, mapObject)
                    if (!mapObject.isEmpty()) mapObjectHashMap[marker.id] = mapObject
                }
            }
        }
    }

    override fun onRemoved(markers: List<Marker>) {
        uiThreadHandler.post {
            markers.forEach { marker ->
                mapObjectHashMap[marker.id]?.remove()
            }
        }
    }

    override fun onUpdated(markers: List<Marker>) {
        createBitmaps(markers)
        uiThreadHandler.post {
            for (marker in markers) {
                val mapObject = mapObjectHashMap[marker.id]
                mapObject?.let {
                    if (marker.type == MarkerType.ANIMATION && (marker as AnimationMarker).showAnimation) {
//                drawAnimationMarker()
                    } else {
                        if (marker.type != MarkerType.NO_MARKER) {
                            val id = getImageProviderId(marker)
                            mapObject.markers.firstOrNull()?.apply {
                                bitmapHashMap[id]?.let {
                                    setIcon(
                                        BitmapDescriptorFactory.fromBitmap(
                                            it
                                        )
                                    )
                                }
                                tag = marker
                            }
                        }
                        updateShape(marker, mapObject)
                    }
                }
            }
        }
    }

    inner class OnCameraMoveListener(private val googleMap: GoogleMap) :
        GoogleMap.OnCameraMoveListener {
        override fun onCameraMove() {
            visibleMapRegionListeners.forEach { listener ->
                val visibleRegion = googleMap.projection.visibleRegion
                val visibleMapRegion = VisibleMapRegion(
                    MapPoint(visibleRegion.farLeft.longitude, visibleRegion.farLeft.latitude),
                    MapPoint(visibleRegion.farRight.longitude, visibleRegion.farRight.latitude),
                    MapPoint(visibleRegion.nearLeft.longitude, visibleRegion.nearLeft.latitude),
                    MapPoint(visibleRegion.nearRight.longitude, visibleRegion.nearRight.latitude),
                    googleMap.cameraPosition.zoom
                )
                listener.onRegionChange(visibleMapRegion)
            }
        }
    }

    private fun updateCamera(cameraUpdate: CameraUpdate) {
        googleMap?.setPadding(0, 0, 0, offsetBottom.toInt())
        isAnimationRun = true
        googleMap?.animateCamera(
            cameraUpdate,
            DURATION_200MS,
            object : GoogleMap.CancelableCallback {
                override fun onCancel() {
                }

                override fun onFinish() {
                    isAnimationRun = false
                }

            }
        )
    }

    private fun emitUpdateCamera(cameraUpdate: CameraUpdate) {
        if (!isAnimationRun) {
            updateCamera(cameraUpdate)
        }
    }

    private fun getImageProviderId(marker: Marker): String {
        val needUniqueColor = payableZones.contains(marker.data?.zoneNumber)
        return markerBitmapFactory.getBitmapMapObjectId(
            context,
            marker,
            isRadarOn,
            isDisabledOn,
            isColoredMarkersEnabled,
            needUniqueColor
        )
    }

    private fun getImageProviderBitmap(marker: Marker): Bitmap? {
        val needUniqueColor = payableZones.contains(marker.data?.zoneNumber)
        return markerBitmapFactory.getBitmapMapObject(
            context,
            marker,
            isRadarOn,
            isDisabledOn,
            isColoredMarkersEnabled,
            needUniqueColor
        )
    }

    private fun moveCameraPosition(latitude: Double, longitude: Double, zoom: Float?) {
        if (googleMap != null) {
            val latLng = LatLng(latitude, longitude)
            val cameraUpdate = if (zoom != null) {
                CameraUpdateFactory.newLatLngZoom(latLng, zoom)
            } else {
                CameraUpdateFactory.newLatLng(latLng)
            }
            updateCamera(cameraUpdate)
        }
    }

    private fun addShape(
        marker: Marker,
        forObject: MapObject
    ) {
        marker.data?.let { markerData ->
            if (MapItemType.fromString(markerData.type) == MapItemType.CLUSTER) {
                if (markerData.listLocation.count() > 0) {
                    for (locationCluster in markerData.listLocation) {
                        addRelativeObject(locationCluster, marker, forObject);
                    }
                }
            } else {
                val location = markerData.location
                addRelativeObject(location, marker, forObject)
            }
        }
    }

    private fun updateShape(
        marker: Marker,
        forObject: MapObject
    ) {
        marker.data?.let { markerData ->
            if (MapItemType.fromString(markerData.type) == MapItemType.CLUSTER) {
                if (markerData.listLocation.count() > 0) {
                    for (locationCluster in markerData.listLocation) {
                        updateRelativeObject(locationCluster, marker, forObject);
                    }
                }
            } else {
                val location = markerData.location
                updateRelativeObject(location, marker, forObject)
            }
        }
    }

    private fun addRelativeObject(
        location: Location?,
        marker: Marker,
        forObject: MapObject
    ) {
        if (location != null) {
            val type = location.type
            if (type != null) {
                if (type == Location.LINE_STRING) {
                    if (isVisiblePolylineMapObject(marker)) {
                        addPolylineShape(location, marker, forObject)
                    }
                } else if (type == Location.POLYGON) {
                    setPolygonShape(location, marker, forObject)
                }
            }
        }
    }

    private fun updateRelativeObject(
        location: Location?,
        marker: Marker,
        forObject: MapObject
    ) {
        if (location != null) {
            val type = location.type
            if (type != null) {
                if (type == Location.LINE_STRING) {
                    if (isVisiblePolylineMapObject(marker)) {
                        updatePolylineShape(location, marker, forObject)
                    }
                } else if (type == Location.POLYGON) {
                    updatePolygonShape(location, marker, forObject)
                }
            }
        }
    }

    private fun addPolylineShape(
        location: Location,
        marker: Marker,
        forObject: MapObject
    ) {
        val lineStringMapCoordinates = location.lineStringMapCoordinates
        if (lineStringMapCoordinates != null) {
            val linePoints: MutableList<LatLng> = ArrayList()
            for (mapCoordinates in lineStringMapCoordinates) {
                linePoints.add(LatLng(mapCoordinates.latitude, mapCoordinates.longitude))
            }
            googleMap?.addPolyline(PolylineOptions().apply {
                addAll(linePoints)
                width(10f)
                color(
                    geometryColorFactory
                        .getColorStrokeGeometry(
                            context,
                            marker,
                            isRadarOn,
                            isDisabledOn
                        )
                )
            })?.let {
                forObject.polylines.add(it)
                it.tag = marker
            }
        }
    }

    private fun updatePolylineShape(
        location: Location,
        marker: Marker,
        forObject: MapObject
    ) {
        val lineStringMapCoordinates = location.lineStringMapCoordinates
        if (lineStringMapCoordinates != null) {
            val linePoints: MutableList<LatLng> = ArrayList()
            for (mapCoordinates in lineStringMapCoordinates) {
                linePoints.add(LatLng(mapCoordinates.latitude, mapCoordinates.longitude))
            }
            forObject.polylines.firstOrNull()?.apply {
                points = linePoints
                width = 10f
                color = geometryColorFactory
                    .getColorStrokeGeometry(
                        context,
                        marker,
                        isRadarOn,
                        isDisabledOn
                    )
                tag = marker
            }
        }
    }

    private fun isVisiblePolylineMapObject(marker: Marker): Boolean {
        return isEnabledPolylineMapObject || marker.state === MarkerState.SELECTED || isDisabledOn || isRadarOn
    }

    private fun setPolygonShape(
        location: Location,
        marker: Marker,
        forObject: MapObject
    ) {
        val polygonMapCoordinate = location.polygonMapCoordinate
        if (polygonMapCoordinate != null && polygonMapCoordinate.size > 0) {
            val linePoints: MutableList<LatLng> = ArrayList()
            val firstMapCoordinates = polygonMapCoordinate[0]
            for ((latitude, longitude) in firstMapCoordinates) {
                linePoints.add(LatLng(latitude, longitude))
            }
            googleMap?.addPolygon(PolygonOptions().apply {
                addAll(linePoints)
                strokeWidth(4f)
                strokeColor(
                    geometryColorFactory.getColorStrokeGeometry(
                        context,
                        marker,
                        isRadarOn,
                        isDisabledOn
                    )
                )
                fillColor(
                    geometryColorFactory
                        .getColorFillGeometry(context, marker, isRadarOn, isDisabledOn)
                )
            })?.let {
                forObject.polygons.add(it)
                it.tag = marker
            }
        }
    }

    private fun updatePolygonShape(
        location: Location,
        marker: Marker,
        forObject: MapObject
    ) {
        val polygonMapCoordinate = location.polygonMapCoordinate
        if (polygonMapCoordinate != null && polygonMapCoordinate.size > 0) {
            val linePoints: MutableList<LatLng> = ArrayList()
            val firstMapCoordinates = polygonMapCoordinate[0]
            for ((latitude, longitude) in firstMapCoordinates) {
                linePoints.add(LatLng(latitude, longitude))
            }
            forObject.polygons.firstOrNull()?.apply {
                strokeColor = geometryColorFactory.getColorStrokeGeometry(
                    context,
                    marker,
                    isRadarOn,
                    isDisabledOn
                )
                fillColor = geometryColorFactory
                    .getColorFillGeometry(context, marker, isRadarOn, isDisabledOn)
                tag = marker
            }
        }
    }

    private fun createBitmaps(markers: List<Marker>) {
        markers.forEach { marker ->
            val id = getImageProviderId(marker)
            if (bitmapHashMap[id] == null) {
                val bitmap = getImageProviderBitmap(marker)
                bitmap?.let { bitmapHashMap[id] = bitmap }
            }
        }
    }
}
