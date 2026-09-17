package net.osmand.plus.plugins.audionotes.library

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import net.osmand.PlatformUtil
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.library.MediaDialogs
import net.osmand.plus.gallery.library.MediaLibraryEntry
import net.osmand.plus.gallery.library.MediaShareHelper
import net.osmand.plus.gallery.library.ShowOnMapNavigator
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.plus.mapcontextmenu.other.SelectFavouriteBottomSheet
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.AlertDialogExtra
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.domain.MediaItem

object MediaItemMenu {
	private val log = PlatformUtil.getLog(MediaItemMenu::class.java)

	@JvmStatic
	fun show(activity: FragmentActivity, entry: MediaLibraryEntry, anchor: View, nightMode: Boolean,
		fromViewer: Boolean, orderedIds: List<String>) {
		val app = activity.application as OsmandApplication
		val items = buildList {
			if (!fromViewer) add(item(app, R.string.shared_string_view, MediaLibraryIcons.VIEW, nightMode) {
				GalleryPagerController.show(activity, GalleryKey.MediaLibrary, entry.id, orderedIds, autoPlay = true)
			})
			if (entry.lat != null && entry.lon != null) add(item(app, R.string.shared_string_show_on_map,
				R.drawable.ic_show_on_map_outlined, nightMode) { ShowOnMapNavigator.show(activity, entry) })
			add(item(app, R.string.shared_string_details, R.drawable.ic_action_info_outlined, nightMode, divider = true) {
				GalleryPagerController.getInstance(app, GalleryKey.MediaLibrary).openDetails(activity, entry.mediaItem, orderedIds)
			})
			if (entry.mediaItem is MediaItem.Internal) add(item(app, R.string.shared_string_rename,
				R.drawable.ic_action_edit_outlined, nightMode) { rename(activity, entry, nightMode) })
			add(item(app, R.string.attach_to_track, R.drawable.ic_action_track_add, nightMode, divider = true, enabled = false) {})
			add(item(app, R.string.attach_to_favorite, MediaLibraryIcons.ATTACH_TO_FAVORITE, nightMode) {
				SelectMediaFavoriteBottomSheet.create(entry.href, entry.title)
					.show(activity.supportFragmentManager, SelectMediaFavoriteBottomSheet.TAG)
			})
			add(item(app, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, nightMode, divider = true) {
				MediaShareHelper.share(activity, listOf(entry.mediaItem))
			})
			add(item(app, R.string.shared_string_delete, R.drawable.ic_action_delete_outlined, nightMode, divider = true, warning = true) {
				MediaDialogs.delete(activity, 1, nightMode) { delete(activity, listOf(entry)) {
					if (fromViewer) (activity.supportFragmentManager.findFragmentByTag(GalleryPhotoPagerFragment.TAG)
						as? GalleryPhotoPagerFragment)?.dismissAllowingStateLoss()
				} }
			})
		}
		popup(anchor, nightMode, items)
	}

	fun showSelection(activity: FragmentActivity, entries: List<MediaLibraryEntry>, anchor: View,
		nightMode: Boolean, onDone: () -> Unit) {
		val app = activity.application as OsmandApplication
		popup(anchor, nightMode, listOf(
			item(app, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, nightMode, enabled = entries.isNotEmpty()) {
				MediaShareHelper.share(activity, entries.map { it.mediaItem }); onDone()
			},
			item(app, R.string.shared_string_delete, R.drawable.ic_action_delete_outlined, nightMode, enabled = entries.isNotEmpty(), warning = true) {
				MediaDialogs.delete(activity, entries.size, nightMode) { delete(activity, entries, onDone) }
			}))
	}

	private fun item(app: OsmandApplication, title: Int, icon: Int, nightMode: Boolean,
		divider: Boolean = false, enabled: Boolean = true, warning: Boolean = false, action: () -> Unit): PopUpMenuItem {
		val color = if (warning) ColorUtilities.getWarningColor(app, nightMode) else ColorUtilities.getDefaultIconColor(app, nightMode)
		return PopUpMenuItem.Builder(app).setTitleId(title).setIcon(app.uiUtilities.getPaintedIcon(icon, color))
			.showTopDivider(divider).setEnabled(enabled).apply { if (warning) setTitleColor(color) }
			.setOnClickListener { action() }.create()
	}

	private fun popup(anchor: View, nightMode: Boolean, items: List<PopUpMenuItem>) {
		PopUpMenu.show(PopUpMenuDisplayData().apply {
			anchorView = anchor; this.nightMode = nightMode; menuItems = items
			widthMode = PopUpMenuWidthMode.STANDARD; showCompound = false
		})
	}

	private fun rename(activity: FragmentActivity, entry: MediaLibraryEntry, nightMode: Boolean) {
		val app = activity.application as OsmandApplication
		val data = AlertDialogData(activity, nightMode)
			.setTitle(R.string.shared_string_rename)
			.setControlsColor(ColorUtilities.getActiveColor(app, nightMode))
			.setNegativeButton(R.string.shared_string_cancel, null)
		data.setPositiveButton(R.string.shared_string_apply) { _, _ ->
			val name = (data.getExtra(AlertDialogExtra.EDIT_TEXT) as? EditText)?.text?.toString()?.trim().orEmpty()
			if (name.isEmpty()) {
				app.showShortToastMessage(R.string.empty_name)
				return@setPositiveButton
			}
			AttachedMediaDataHelper(app).renameMedia(entry.recording, entry.href, name,
				entry.attachments.groupBy({ it.target }, { it.link })) { newHref ->
				app.runInUIThread {
					if (newHref == null) {
						app.showShortToastMessage(R.string.rename_failed)
					} else {
						onRenamed(activity, entry, LinkMediaFactory.getInternalPath(newHref))
					}
				}
				true
			}
		}
		val currentName = entry.recording?.getDescriptionName(entry.recording.fileName) ?: entry.title.substringBeforeLast('.')
		CustomAlert.showInput(data, activity, currentName, app.getString(R.string.shared_string_name))
	}

	private fun onRenamed(activity: FragmentActivity, entry: MediaLibraryEntry, newId: String?) {
		val app = activity.application as OsmandApplication
		app.galleryHelper.mediaLibraryRepository.refresh {
			if (newId != null && !activity.isDestroyed && !activity.supportFragmentManager.isStateSaved) {
				val pager = GalleryPagerController.getExistingInstance(app)
				if (pager?.key == GalleryKey.MediaLibrary) {
					pager.orderedIds = pager.orderedIds?.map { if (it == entry.id) newId else it }
					(activity.supportFragmentManager.findFragmentByTag(GalleryPhotoPagerFragment.TAG)
						as? GalleryPhotoPagerFragment)?.refreshMediaItems(newId)
				}
			}
		}
	}

	private fun delete(activity: FragmentActivity, entries: List<MediaLibraryEntry>, onDone: () -> Unit) {
		val app = activity.application as OsmandApplication
		val storage = MediaStorageHelper(app)
		val location = MediaStorageLocation.fromSettings(app)
		app.taskManager.runInBackground(object : OsmAndTaskRunnable<Void, Void, List<MediaLibraryEntry>>() {
			override fun doInBackground(vararg params: Void?): List<MediaLibraryEntry> = entries.filter { entry ->
				runCatching {
					val source = storage.resolveMediaSource(location, entry.href, true)
						?: error("Media source unavailable")
					source.delete()
				}.onFailure { log.warn("Unable to delete media", it) }.isSuccess
			}

			override fun onPostExecute(deleted: List<MediaLibraryEntry>) {
				deleted.forEach { it.recording?.let { recording ->
					PluginsHelper.getPlugin(AudioVideoNotesPlugin::class.java)?.deleteRecording(recording, true)
				} }
				val linksByTarget = deleted.flatMap { it.attachments }.groupBy({ it.target }, { it.link })
				AttachedMediaDataHelper(app).removeMediaLinks(linksByTarget, false) { saved ->
					app.runInUIThread {
						if (!saved || deleted.size != entries.size) app.showShortToastMessage(R.string.media_delete_failed)
						app.galleryHelper.mediaLibraryRepository.refresh()
						onDone()
					}
					true
				}
			}
		})
	}
}

class SelectMediaFavoriteBottomSheet : SelectFavouriteBottomSheet() {
	override fun onFavouriteSelected(favourite: FavouritePoint) {
		val href = arguments?.getString(HREF_KEY) ?: return
		if (favourite.links.orEmpty().none { it.href == href }) {
			AttachedMediaDataHelper(app).addMediaLinks(favourite,
				listOf(Link(href).apply { text = arguments?.getString(TITLE_KEY) }), null)
		}
		app.galleryHelper.mediaLibraryRepository.refresh()
		dismiss()
	}

	companion object {
		const val TAG = "SelectMediaFavoriteBottomSheet"
		private const val HREF_KEY = "href"
		private const val TITLE_KEY = "title"

		fun create(href: String, title: String) = SelectMediaFavoriteBottomSheet().apply {
			arguments = Bundle().apply { putString(HREF_KEY, href); putString(TITLE_KEY, title) }
		}
	}
}
