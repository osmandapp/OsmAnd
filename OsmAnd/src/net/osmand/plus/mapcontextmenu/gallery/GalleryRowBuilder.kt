package net.osmand.plus.mapcontextmenu.gallery

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.contract.IGalleryRowView
import net.osmand.plus.gallery.controller.GalleryRowController
import net.osmand.plus.gallery.model.GalleryActionButton
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.mapcontextmenu.MapContextMenu
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton

private const val SPAN_COUNT = 2

class GalleryRowBuilder(
	val menuBuilder: MenuBuilder,
	val controller: GalleryRowController
) : IGalleryRowView {

	override val mapActivity: MapActivity = menuBuilder.mapActivity
	private val app: OsmandApplication = menuBuilder.application
	private val nightMode = menuBuilder.isNightMode

	val galleryView: View = UiUtilities.inflate(mapActivity, nightMode, R.layout.gallery_card)

	private var actionButtons: List<GalleryActionButton>? = null

	private val adapter: GalleryGridAdapter = controller.createAdapter(mapActivity, nightMode)

	init {
		controller.attach(this)
		setupRecyclerView()
		render()
	}

	private fun setupRecyclerView() {
		val lookup = object : GridLayoutManager.SpanSizeLookup() {
			override fun getSpanSize(position: Int): Int {
				return if (adapter.isRegularMediaItemOnPosition(position)) 1 else 2
			}
		}

		val gridLayoutManager = GridLayoutManager(
			app, SPAN_COUNT, GridLayoutManager.HORIZONTAL, false
		).apply {
			spanSizeLookup = lookup
		}

		val recyclerView = galleryView.findViewById<RecyclerView>(R.id.recycler_view)
		recyclerView.layoutManager = gridLayoutManager

		val galleryGridItemDecorator = GalleryGridItemDecorator(app)
		recyclerView.addItemDecoration(galleryGridItemDecorator)
		recyclerView.adapter = adapter
	}

	override fun render() {
		if (!menuBuilder.isHidden) {
			val items = controller.getGalleryItems()
			val list = ArrayList(items)
			adapter.setItems(list)

			val mapContextMenu: MapContextMenu? = menuBuilder.mapContextMenu
			if (items.isNotEmpty() && mapContextMenu != null) {
				mapContextMenu.updateLayout()
			}
		}
		updateButtons()
	}

	private fun updateButtons() {
		val newButtons = controller.collectActionButtons()
		val cachedButtons = actionButtons
		if (cachedButtons == null || cachedButtons != newButtons) {
			actionButtons = newButtons

			val buttonIds = listOf(R.id.primary_action_button, R.id.secondary_action_button)
			for (i in buttonIds.indices) {
				val buttonView = galleryView.findViewById<DialogButton>(buttonIds[i])
				if (i < newButtons.size) {
					val button = newButtons[i]
					buttonView.setTitleId(button.titleId)
					buttonView.setOnClickListener { controller.handleGalleryAction(it, button.action) }
					buttonView.visibility = View.VISIBLE
				} else {
					buttonView.visibility = View.GONE
				}
			}
		}
	}

	override fun onLoadingImage(loading: Boolean) {
		adapter.onLoadingImages(loading)
	}

	override fun isNightMode(): Boolean {
		return nightMode
	}
}