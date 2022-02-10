package ru.fabit.google

import ru.fabit.map.dependencies.provider.MapStyleProvider

class GoogleMapStyleProviderImpl(
    private val defaultStyle: String,
    private val monochromeStyle: String
) : MapStyleProvider {

    override fun getDefaultStyle() = defaultStyle

    override fun getCongestionStyle() = monochromeStyle
}