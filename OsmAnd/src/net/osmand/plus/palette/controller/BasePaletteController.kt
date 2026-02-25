package net.osmand.plus.palette.controller

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.palette.contract.IExternalPaletteListener
import net.osmand.plus.palette.contract.IPaletteController
import net.osmand.plus.palette.contract.IPaletteInteractionListener
import net.osmand.plus.palette.contract.IPaletteView
import net.osmand.plus.palette.utils.IdMapper
import net.osmand.plus.utils.ColorUtilities
import net.osmand.shared.palette.data.PaletteRepository
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem
import java.lang.ref.WeakReference

abstract class BasePaletteController(
	protected val app: OsmandApplication,
	protected var paletteId: String
) : IPaletteController, IPaletteInteractionListener {

	protected val repository: PaletteRepository = app.paletteRepository

	protected val views = ArrayList<WeakReference<IPaletteView>>()
	protected var listener: IExternalPaletteListener? = null

	protected var editedItem: PaletteItem? = null
	protected var selectedItem: PaletteItem? = null

	private val idMapper = IdMapper()

	// --- View Management ---

	override fun attachView(view: IPaletteView) {
		views.add(WeakReference(view))
	}

	override fun detachView(view: IPaletteView) {
		val iterator = views.iterator()
		while (iterator.hasNext()) {
			if (iterator.next().get() == view) {
				iterator.remove()
				break
			}
		}
	}

	protected fun collectActivePalettes(): List<IPaletteView> {
		val result = ArrayList<IPaletteView>()
		val iterator = views.iterator()
		while (iterator.hasNext()) {
			val palette = iterator.next().get()
			if (palette != null) {
				result.add(palette)
			} else {
				iterator.remove()
			}
		}
		return result
	}

	// --- Listener Management ---

	override fun setPaletteListener(listener: IExternalPaletteListener?) {
		this.listener = listener
	}

	// --- Data Access ---

	override fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem> {
		return repository.getPaletteItems(paletteId, sortMode)
	}

	override fun getSelectedPaletteItem(): PaletteItem? = selectedItem

	override fun isPaletteItemSelected(item: PaletteItem): Boolean {
		return item.id == selectedItem?.id
	}

	// --- Selection Logic ---

	override fun selectPaletteItem(item: PaletteItem?) {
		selectPaletteItemInternal(item, silently = false)
	}

	override fun selectPaletteItemSilently(item: PaletteItem?) {
		selectPaletteItemInternal(item, silently = true)
	}

	private fun selectPaletteItemInternal(item: PaletteItem?, silently: Boolean = false) {
		val oldSelected = selectedItem
		selectedItem = item

		if (!silently) {
			notifyPaletteItemSelected(item)
		}
		if (oldSelected != null && item != null) {
			notifyUpdatePaletteSelection(oldSelected, item)
		} else {
			notifyUpdatePaletteColors(item)
		}
	}

	override fun scrollToPaletteItem(item: PaletteItem?, smoothScroll: Boolean) {
		notifyUpdatePaletteScrollPosition(item, smoothScroll)
	}

	override fun renewLastUsedTime() {
		selectedItem?.let { renewLastUsedTime(it) }
	}

	protected fun renewLastUsedTime(item: PaletteItem) {
		repository.markPaletteItemAsUsed(paletteId, item.id)
	}

	// --- IPaletteInteractionListener (Common) ---

	override fun onPaletteItemClick(item: PaletteItem, markAsUsed: Boolean) {
		if (markAsUsed) {
			renewLastUsedTime(item)
		}
		selectPaletteItem(item)
	}

	override fun onPaletteItemLongClick(anchorView: View, item: PaletteItem) {
		showItemPopUpMenu(anchorView, item)
	}

	protected abstract fun showItemPopUpMenu(anchorView: View, item: PaletteItem)

	// --- Notification Helpers ---

	protected open fun notifyPaletteItemSelected(item: PaletteItem?) {
		if (item != null) {
			listener?.onPaletteItemSelected(item)
		}
	}

	protected fun notifyUpdatePaletteColors(targetItem: PaletteItem?) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteItems(targetItem)
		}
	}

	protected fun notifyUpdatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteSelection(oldItem, newItem)
		}
	}

	protected fun notifyUpdatePaletteScrollPosition(targetItem: PaletteItem?, smoothScroll: Boolean) {
		for (palette in collectActivePalettes()) {
			palette.askScrollToPaletteItemPosition(targetItem, smoothScroll)
		}
	}

	// --- Base UI components ---

	fun isNightMode() = collectActivePalettes()[0].isNightMode()

	fun getFragmentActivity() = collectActivePalettes()[0].getActivity()

	// --- UI Helpers ---

	override fun getControlsAccentColor(nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(app, nightMode)
	}

	override fun isAccentColorCanBeChanged(): Boolean = false

	override fun getStableId(itemId: String): Long {
		return idMapper.getSafeId(itemId)
	}

	protected fun getContentIcon(@DrawableRes id: Int): Drawable? {
		return app.uiUtilities.getThemedIcon(id)
	}
}