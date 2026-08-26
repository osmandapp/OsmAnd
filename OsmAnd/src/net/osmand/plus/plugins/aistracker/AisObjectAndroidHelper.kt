package net.osmand.plus.plugins.aistracker

import net.osmand.Location
import net.osmand.shared.aistracker.AisLocation

fun Location.toAisLocation() = AisLocation(
    latitude = latitude,
    longitude = longitude,
    speed = speed,
    bearing = bearing,
    hasSpeed = hasSpeed(),
    hasBearing = hasBearing()
)
