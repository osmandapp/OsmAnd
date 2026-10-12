package net.osmand.plus.auto

interface NavigationListener {
    fun requestLocationNavigation(): Boolean
    fun stopNavigation()
}