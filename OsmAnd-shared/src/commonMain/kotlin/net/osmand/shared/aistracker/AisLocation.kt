package net.osmand.shared.aistracker

data class AisLocation(
    var latitude: Double,
    var longitude: Double,
    var speed: Float,    // in m/s
    var bearing: Float,  // in degrees
    var hasSpeed: Boolean = true,
    var hasBearing: Boolean = true
)
