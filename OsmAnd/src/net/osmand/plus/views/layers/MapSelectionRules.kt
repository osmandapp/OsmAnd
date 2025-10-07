package net.osmand.plus.views.layers

/**
 * Defines additional filtering rules applied on individual map layers
 * to determine which objects should be collected.
 *
 * @param isUnknownLocation        If true, selection is considered for unknown locations
 * @param isOnlyTouchableObjects   If true, only touchable objects are collected.
 *                                 Touchable objects are those that visually react when tapped on the map.
 * @param isOnlyPoints             If true, only point-type objects are collected (e.g. POIs, markers).
 */
data class MapSelectionRules(
    var isUnknownLocation: Boolean = false,
    var isOnlyTouchableObjects: Boolean = false,
    var isOnlyPoints: Boolean = false
)

