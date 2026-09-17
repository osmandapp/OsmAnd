package net.osmand.plus.gallery.ui.viewer

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.osmand.data.PointDescription
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.gallery.data.Cancellable
import net.osmand.plus.gallery.data.GalleryMediaMetadata
import net.osmand.plus.gallery.data.MediaMetadataListener
import net.osmand.plus.gallery.library.MediaAttachment
import net.osmand.plus.gallery.library.MediaDialogs
import net.osmand.plus.gallery.library.MediaLibraryEntry
import net.osmand.plus.gallery.library.ShowOnMapNavigator
import net.osmand.plus.gallery.ui.GalleryItemAnimator
import net.osmand.plus.gallery.ui.GallerySectionCardDecoration
import net.osmand.plus.gallery.ui.motion.GalleryMotion
import net.osmand.plus.mapcontextmenu.other.ShareMenu
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import net.osmand.shared.media.domain.MediaItem

class ViewerSheetController(
	private val activity: FragmentActivity,
	list: RecyclerView,
	private val nightMode: Boolean
) : MediaDetailsAdapter.Actions {

	private val app = activity.application as OsmandApplication
	private val adapter = MediaDetailsAdapter(nightMode, this)
	private val builder = MediaDetailsContentBuilder(app)
	private val metadataRepository get() = app.galleryHelper.metadataRepository
	private val libraryRepository get() = app.galleryHelper.mediaLibraryRepository
	private val libraryListener: (List<MediaLibraryEntry>) -> Unit = { rebuild() }
	private val metadataListener = object : MediaMetadataListener {
		override fun onMetadataLoaded(item: MediaItem, metadata: GalleryMediaMetadata) = rebuild()
		override fun onBatchFinished() = rebuild()
	}
	private var item: MediaItem? = null
	private var metadataRequest: Cancellable? = null
	private var subscribed = false

	init {
		list.layoutManager = LinearLayoutManager(activity)
		list.adapter = adapter
		val cards = GallerySectionCardDecoration(app, nightMode)
		list.addItemDecoration(cards)
		list.itemAnimator = GalleryItemAnimator(list, adapter, cards, GalleryMotion.animationsEnabled(app))
	}

	fun setItem(item: MediaItem?) {
		this.item = item
		metadataRequest?.cancel()
		metadataRequest = null
		if (item != null) {
			if (metadataRepository.getCached(item) == null) {
				metadataRequest = metadataRepository.request(listOf(item), metadataListener)
			}
			if (item !is MediaItem.Remote && !subscribed) {
				subscribed = true
				libraryRepository.subscribe(libraryListener)
			}
		}
		rebuild()
	}

	fun release() {
		metadataRequest?.cancel()
		metadataRequest = null
		if (subscribed) {
			subscribed = false
			libraryRepository.unsubscribe(libraryListener)
		}
	}

	private fun rebuild() {
		val item = item
		if (item == null) {
			adapter.submit(emptyList())
			return
		}
		val entry = if (item is MediaItem.Remote) null else libraryRepository.getEntry(item.id)
		adapter.submit(builder.build(item, metadataRepository.getCached(item), entry))
	}

	override fun onRowAction(action: RowAction) {
		when (action) {
			is RowAction.ShowOnMap -> {
				val entry = item?.let { libraryRepository.getEntry(it.id) }
				if (entry != null) {
					ShowOnMapNavigator.show(activity, entry)
				} else {
					ShowOnMapNavigator.show(activity, action.lat, action.lon,
						PointDescription(PointDescription.POINT_TYPE_LOCATION, action.title), null)
				}
			}
			is RowAction.OpenUrl -> AndroidUtils.openUrl(activity, action.url, nightMode)
		}
	}

	override fun onCopy(text: String) {
		ShareMenu.copyToClipboardWithToast(app, text, false)
	}

	override fun onAttachmentMenu(anchor: View, attachment: MediaAttachment) {
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)
		val items = listOf(
			PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_detach)
				.setIcon(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_attachment_remove, iconColor))
				.setOnClickListener { detach(attachment) }
				.create(),
			PopUpMenuItem.Builder(app)
				.setTitleId(R.string.shared_string_show)
				.setIcon(app.uiUtilities.getPaintedIcon(R.drawable.ic_action_info_outlined, iconColor))
				.showTopDivider(true)
				.setOnClickListener { ShowOnMapNavigator.show(activity, attachment) }
				.create()
		)
		PopUpMenu.show(PopUpMenuDisplayData().apply {
			anchorView = anchor
			menuItems = items
			this.nightMode = this@ViewerSheetController.nightMode
			widthMode = PopUpMenuWidthMode.STANDARD
		})
	}

	private fun detach(attachment: MediaAttachment) {
		val target = attachment.target
		val link = target.links.orEmpty().firstOrNull { it === attachment.link } ?: return
		MediaDialogs.detach(activity, listOf(link), nightMode) {
			AttachedMediaDataHelper(app).removeMediaLinks(target, listOf(link), false) { success ->
				app.runInUIThread {
					if (!success) app.showShortToastMessage(R.string.media_detach_failed)
					libraryRepository.refresh()
				}
				true
			}
		}
	}
}
