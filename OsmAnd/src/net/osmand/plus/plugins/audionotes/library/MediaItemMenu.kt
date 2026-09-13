package net.osmand.plus.plugins.audionotes.library

import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.gallery.attached.helpers.AttachedMediaDataHelper
import net.osmand.plus.gallery.controller.GalleryPagerController
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.plus.gallery.ui.GalleryPhotoPagerFragment
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin
import net.osmand.plus.plugins.audionotes.library.data.MediaLibraryEntry
import net.osmand.plus.settings.mediastorage.MediaStorageHelper
import net.osmand.plus.settings.mediastorage.MediaStorageLocation
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.LinkMediaFactory
import net.osmand.shared.media.MediaProvider
import java.io.File
import java.util.concurrent.Executors

object MediaItemMenu {
	private val executor = Executors.newSingleThreadExecutor()
	private val log = PlatformUtil.getLog(MediaItemMenu::class.java)

	@JvmStatic
	fun show(activity: FragmentActivity, entry: MediaLibraryEntry, anchor: View, nightMode: Boolean,
		includeView: Boolean, orderedIds: List<String>) {
		val app = activity.application as OsmandApplication
		val pager = GalleryPagerController.getInstance(app, GalleryKey.MediaLibrary).apply { this.orderedIds = orderedIds }
		val items = buildList {
			if (includeView) add(item(app, R.string.shared_string_view, MediaLibraryIcons.VIEW, nightMode) {
				GalleryPagerController.show(activity, GalleryKey.MediaLibrary, entry.id, orderedIds)
			})
			if (entry.lat != null && entry.lon != null) add(item(app, R.string.shared_string_show_on_map,
				R.drawable.ic_show_on_map_outlined, nightMode) { ShowOnMapNavigator.show(activity, entry) })
			add(item(app, R.string.shared_string_details, R.drawable.ic_action_info_outlined, nightMode, divider = true) {
				pager.openDetails(activity, entry.mediaItem)
			})
			if (entry.mediaItem is MediaItem.Internal) add(item(app, R.string.shared_string_rename,
				R.drawable.ic_action_edit_outlined, nightMode) { rename(activity, entry, nightMode) })
			add(item(app, R.string.attach_to_track, R.drawable.ic_action_track_add, nightMode, divider = true, enabled = false) {})
			add(item(app, R.string.attach_to_favorite, MediaLibraryIcons.ATTACH_TO_FAVORITE, nightMode) {
				SelectMediaFavoriteBottomSheet.create(entry.href, entry.title)
					.show(activity.supportFragmentManager, "select_media_favorite")
			})
			add(item(app, R.string.shared_string_share, R.drawable.ic_action_gshare_dark, nightMode, divider = true) {
				MediaShareHelper.share(activity, listOf(entry.mediaItem))
			})
			add(item(app, R.string.shared_string_delete, R.drawable.ic_action_delete_outlined, nightMode, divider = true, warning = true) {
				MediaDialogs.delete(activity, 1, nightMode) { delete(activity, listOf(entry)) {
					if (!includeView) activity.supportFragmentManager.popBackStack()
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
		val context = UiUtilities.getThemedContext(activity, nightMode, R.style.OsmandMaterialLightTheme, R.style.OsmandMaterialDarkTheme)
		val content = android.view.LayoutInflater.from(context).inflate(R.layout.note_edit_dialog, null)
		val input = content.findViewById<EditText>(R.id.name)
		input.setSingleLine(true)
		input.setText(entry.recording?.getDescriptionName(entry.recording.fileName) ?: entry.title.substringBeforeLast('.'))
		val dialog = MaterialAlertDialogBuilder(context).setTitle(R.string.shared_string_rename).setView(content)
			.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(R.string.shared_string_apply, null).create()
		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				val name = input.text.toString().trim()
				if (name.isEmpty()) { input.error = app.getString(R.string.empty_name); return@setOnClickListener }
				dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
				AttachedMediaDataHelper(app).renameMedia(entry.recording, entry.href, name,
					entry.attachments.groupBy({ it.target }, { it.link })) { success ->
					app.runInUIThread {
						dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
						if (success) {
							dialog.dismiss()
							val oldFile = MediaProvider.resolveInternalMediaFile(app.getAppPath().absolutePath,
								(entry.mediaItem as MediaItem.Internal).relativePath)
							val newFile = entry.recording?.file ?: File(oldFile.parentFile,
								name + oldFile.extension.takeIf { it.isNotEmpty() }?.let { ".$it" }.orEmpty())
							val newId = LinkMediaFactory.getInternalPath(MediaStorageHelper(app).createMediaFileHref(newFile))
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
						} else input.error = app.getString(R.string.rename_failed)
					}
					true
				}
			}
		}
		dialog.show()
		input.requestFocus()
	}

	private fun delete(activity: FragmentActivity, entries: List<MediaLibraryEntry>, onDone: () -> Unit) {
		val app = activity.application as OsmandApplication
		val helper = AttachedMediaDataHelper(app)
		val storage = MediaStorageHelper(app)
		val location = MediaStorageLocation.fromSettings(app)
		executor.execute {
			val deleted = entries.filter { entry ->
				runCatching {
					val source = storage.resolveMediaSource(location, entry.href, true)
						?: error("Media source unavailable")
					source.delete()
				}.onFailure { log.warn("Unable to delete media", it) }.isSuccess
			}
			app.runInUIThread {
				deleted.forEach { it.recording?.let { recording ->
					PluginsHelper.getPlugin(AudioVideoNotesPlugin::class.java)?.deleteRecording(recording, true)
				} }
				val targets = deleted.flatMap { it.attachments }.groupBy({ it.target }, { it.link }).entries.toList()
				fun removeLinks(index: Int, success: Boolean) {
					if (index == targets.size) {
						if (!success || deleted.size != entries.size) app.showShortToastMessage(R.string.media_delete_failed)
						app.galleryHelper.mediaLibraryRepository.refresh()
						onDone()
						return
					}
					val target = targets[index]
					helper.removeMediaLinks(target.key, target.value, true) { saved ->
						app.runInUIThread { removeLinks(index + 1, success && saved) }; true
					}
				}
				removeLinks(0, true)
			}
		}
	}
}
