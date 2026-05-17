package net.osmand.plus.mapcontextmenu.gallery

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.gallery.controller.GalleryController
import net.osmand.plus.gallery.model.GalleryAction
import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.model.GalleryItem.NoInternet
import net.osmand.plus.gallery.ui.GalleryGridAdapter
import net.osmand.plus.gallery.ui.GalleryGridConfig
import net.osmand.plus.gallery.ui.GalleryGridFragment
import net.osmand.plus.gallery.ui.GalleryGridItemDecorator
import net.osmand.plus.gallery.ui.GalleryListener
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.mapcontextmenu.MapContextMenu
import net.osmand.plus.mapcontextmenu.MenuBuilder
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.shared.media.domain.MediaItem
import net.osmand.util.Algorithms

class GalleryRowBuilder(
	val menuBuilder: MenuBuilder
) {

	private val app: OsmandApplication = menuBuilder.application
	private val mapActivity: MapActivity = menuBuilder.mapActivity

	private val galleryItems = mutableListOf<GalleryItem>()

	lateinit var galleryView: View
		private set

	private lateinit var galleryGridAdapter: GalleryGridAdapter

	fun setItems(vararg items: GalleryItem) {
		setItems(items.asList())
	}

	fun setItems(items: Collection<GalleryItem>) {
		galleryItems.clear()
		galleryItems.addAll(items)

		if (!menuBuilder.isHidden) {
			val list = ArrayList(items)
			galleryGridAdapter.setItems(list)

			val mapContextMenu: MapContextMenu? = menuBuilder.mapContextMenu
			if (itemsCount() > 0 && mapContextMenu != null) {
				mapContextMenu.updateLayout()
			}
		}
		updateShowAll()
	}

	private fun updateShowAll() {
		val viewAllButton = galleryView.findViewById<View>(R.id.view_all)
		AndroidUiHelper.updateVisibility(viewAllButton, shouldShowViewAll())
	}

	fun onLoadingImage(loading: Boolean) {
		galleryGridAdapter.onLoadingImages(loading)
	}

	fun build(
		controller: GalleryController,
		config: GalleryGridConfig,
		nightMode: Boolean
	) {
		galleryView = UiUtilities.inflate(mapActivity, nightMode, R.layout.gallery_card)
		val recyclerView = galleryView.findViewById<RecyclerView>(R.id.recycler_view)

		val items = mutableListOf<GalleryItem>()
		val listener = getGalleryListener(controller)
		galleryGridAdapter = GalleryGridAdapter(mapActivity, listener, controller, null, config, nightMode)

		if (!app.settings.isInternetConnectionAvailable) {
			items.add(NoInternet)
		} else {
			items.addAll(galleryItems)
		}
		galleryGridAdapter.setItems(items)

		recyclerView.layoutManager = getGridLayoutManager()
		val galleryGridItemDecorator = GalleryGridItemDecorator(app)
		recyclerView.addItemDecoration(galleryGridItemDecorator)
		recyclerView.adapter = galleryGridAdapter

		setupViewAllButton(config)
	}

	private fun getGridLayoutManager(): GridLayoutManager {
		val gridLayoutManager = GridLayoutManager(
			app,
			2,
			GridLayoutManager.HORIZONTAL,
			false
		)
		gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
			override fun getSpanSize(position: Int): Int {
				return if (galleryGridAdapter.isRegularMediaItemOnPosition(position)) 1 else 2
			}
		}
		return gridLayoutManager
	}

	private fun setupViewAllButton(config: GalleryGridConfig) {
		val actionButton = galleryView.findViewById<DialogButton>(R.id.view_all)
		actionButton.setTitleId(config.showAllButtonTitleResId)
		actionButton.setOnClickListener { onShowAllButtonClicked(config) }
		updateShowAll()
	}

	private fun getGalleryListener(controller: GalleryController): GalleryListener {
		return object : GalleryListener {
			override fun onMediaItemClicked(mediaItem: MediaItem) {
				if (!PluginsHelper.handleGalleryMediaItemClick(mapActivity, mediaItem)) {
					val position = controller.getPhotoItemIndexById(mediaItem.id)
					GalleryPhotoPagerFragment.showInstance(mapActivity, position)
				}
			}

			override fun onReloadMediaItems() {
				if (!app.settings.isInternetConnectionAvailable) {
					app.showShortToastMessage(R.string.shared_string_no_internet_connection)
				} else {
					menuBuilder.startLoadingImages()
				}
			}
		}
	}

	private fun onShowAllButtonClicked(config: GalleryGridConfig) {
		val action: GalleryAction? = config.showAllButtonAction
		if (action != null) {
			PluginsHelper.handleGalleryAction(action)
		} else {
			GalleryGridFragment.showInstance(mapActivity)
		}
	}

	private fun shouldShowViewAll(): Boolean {
		if (Algorithms.isEmpty(galleryItems)) {
			return false
		}
		for (item in galleryItems) {
			if (item is GalleryItem.Media) {
				return true
			}
		}
		return false
	}

	private fun itemsCount(): Int {
		return galleryItems.size
	}
}