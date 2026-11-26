package net.osmand.plus.views.mapwidgets

/**
 * A map screen (a fragment of MapActivity) that hides the left and right widget panels while it is started.
 *
 * WidgetsVisibilityHelper tracks started fragments that implement this interface, so a screen
 * never has to show the panels again: when it is stopped or removed, it no longer hides them.
 */
interface SideWidgetsHider {

	/**
	 * Called on every update of the side panels while the fragment is started and not hidden.
	 */
	fun shouldHideSideWidgets(): Boolean
}
