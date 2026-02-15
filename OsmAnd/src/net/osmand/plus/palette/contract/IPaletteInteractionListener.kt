package net.osmand.plus.palette.contract

import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.shared.palette.domain.PaletteItem

/**
 * Interface for User Actions.
 * These events originate from the UI (Adapter/View) and are sent to the Controller.
 */
interface IPaletteInteractionListener {

	/**
	 * Called when user selects an item in the UI.
	 * @param item The selected item.
	 * @param markAsUsed If true, this item should be moved to the top of "Last Used" list.
	 * Use 'false' for preview selection (e.g. in a card),
	 * and 'true' for final selection (e.g. in a dialog/fragment).
	 */
	fun onPaletteItemClick(item: PaletteItem, markAsUsed: Boolean)

	fun onPaletteItemLongClick(anchorView: View, item: PaletteItem)

	fun onAddButtonClick(activity: FragmentActivity)

	fun onShowAllClick(activity: FragmentActivity)
}