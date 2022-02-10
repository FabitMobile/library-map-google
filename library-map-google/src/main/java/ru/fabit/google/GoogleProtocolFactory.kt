package ru.fabit.google

import android.content.Context
import ru.fabit.map.dependencies.factory.GeometryColorFactory
import ru.fabit.map.dependencies.factory.MarkerBitmapFactory
import ru.fabit.map.internal.protocol.MapProtocol

class GoogleProtocolFactory {
    companion object {
        fun create(
            context: Context,
            mapStyleProvider: GoogleMapStyleProviderImpl,
            permissionProvider: PermissionProvider,
            markerCalculator: MarkerCalculator,
            markerBitmapFactory: MarkerBitmapFactory,
            isEnabledFreeSpaces: Boolean,
            geometryColorFactory: GeometryColorFactory,
            isEnabledPolylineMapObject: Boolean,
            locationSource: LocationSource,
            uiSettings: UiSettings
        ): MapProtocol {
            return GoogleMapWrapper(
                context,
                mapStyleProvider,
                permissionProvider,
                markerCalculator,
                markerBitmapFactory,
                isEnabledFreeSpaces,
                geometryColorFactory,
                isEnabledPolylineMapObject,
                locationSource,
                uiSettings
            )
        }
    }
}