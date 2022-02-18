package ru.fabit.google

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

interface LocationSource {
    suspend fun locationUpdateEvents(): Flow<LatLng>
}