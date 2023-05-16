package net.osmand.plus.auto

interface NavigationListener {
    fun requestLocationNavigation(): Boolean
    fun updateNavigation(navigating: Boolean)
    fun stopNavigation()
}