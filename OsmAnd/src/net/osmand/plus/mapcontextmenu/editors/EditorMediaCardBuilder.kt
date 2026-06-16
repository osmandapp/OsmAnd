package net.osmand.plus.mapcontextmenu.editors

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.attached.AttachedMediaRowController
import net.osmand.plus.gallery.contract.IGalleryRowView
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.palette.view.PaletteElements
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

/**
 * Hosts an attached-media row inside the Add/Edit Favorite editor.
 *
 * Reuses the gallery [AttachedMediaRowController] / [GalleryGridAdapter] / media delegate stack and
 * the [IGalleryRowView] contract, plus the colour-palette card chrome (vertical divider, circular
 * "+" add button from [PaletteElements], and a "View all" row). Items are laid out as a single
 * horizontal row of compact 48dp tiles (the controller is created in compact mode). When there is
 * no media a descriptive empty-state text is shown next to the always-visible "+" button.
 */
class EditorMediaCardBuilder(
	override val mapActivity: MapActivity,
	private val nightMode: Boolean,
	private val controller: AttachedMediaRowController
) : IGalleryRowView {

	private val app: OsmandApplication = mapActivity.app

	val cardView: View = UiUtilities.inflate(mapActivity, nightMode, R.layout.card_editor_media)

	private val adapter: GalleryGridAdapter = controller.createAdapter(mapActivity, nightMode)
	private val mediaList: RecyclerView = cardView.findViewById(R.id.media_list)
	private val emptyDescription: View = cardView.findViewById(R.id.media_empty_description)
	private val viewAllButton: View = cardView.findViewById(R.id.button_view_all)
	private val viewAllDivider: View = cardView.findViewById(R.id.media_view_all_divider)

	init {
		controller.attach(this)
		setupRecyclerView()
		setupAddButton()
		setupViewAllButton()
		render()
	}

	private fun setupRecyclerView() {
		mediaList.layoutManager =
			LinearLayoutManager(mapActivity, LinearLayoutManager.HORIZONTAL, false)
		mediaList.addItemDecoration(HorizontalSpaceDecorator(AndroidUtils.dpToPx(app, 6f)))
		mediaList.adapter = adapter
	}

	private fun setupAddButton() {
		val container = cardView.findViewById<ViewGroup>(R.id.add_button_container)
		container.addView(PaletteElements(mapActivity, nightMode).createAddButtonView(container))
		container.setOnClickListener { controller.showAddMenu(it) }
	}

	private fun setupViewAllButton() {
		UiUtilities.setupListItemBackground(
			mapActivity, viewAllButton, ColorUtilities.getActiveColor(app, nightMode)
		)
		viewAllButton.setOnClickListener { controller.showAllMedia(it) }
	}

	fun load() {
		controller.onRowBuilt(false)
	}

	fun detach() {
		controller.detach()
	}

	override fun render() {
		adapter.setItems(ArrayList(controller.getGalleryItems()))
		val hasMedia = controller.hasMedia()
		AndroidUiHelper.updateVisibility(mediaList, hasMedia)
		AndroidUiHelper.updateVisibility(emptyDescription, !hasMedia)
		AndroidUiHelper.updateVisibility(viewAllButton, hasMedia)
		AndroidUiHelper.updateVisibility(viewAllDivider, hasMedia)
	}

	override fun onLoadingImage(loading: Boolean) {
		adapter.onLoadingImages(loading)
	}

	override fun isNightMode(): Boolean = nightMode

	private class HorizontalSpaceDecorator(
		private val spacePx: Int
	) : RecyclerView.ItemDecoration() {
		override fun getItemOffsets(
			outRect: Rect,
			view: View,
			parent: RecyclerView,
			state: RecyclerView.State
		) {
			val position = parent.getChildAdapterPosition(view)
			outRect.left = if (position == 0) spacePx else 0
			outRect.right = spacePx
		}
	}
}
