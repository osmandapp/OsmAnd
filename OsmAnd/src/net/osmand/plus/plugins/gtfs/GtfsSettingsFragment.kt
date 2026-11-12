package net.osmand.plus.plugins.gtfs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import net.osmand.plus.R
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.settings.fragments.BaseSettingsFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities
import net.osmand.router.gtfs.GtfsIndexSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/**
 * Feeds, index status and the walking limit of the GTFS plugin. Imports run in [GtfsFeedUpdate], owned by the
 * plugin, so leaving this screen does not stop one; the screen shows its progress when it is opened again.
 */
class GtfsSettingsFragment : BaseSettingsFragment() {

	private lateinit var plugin: GtfsTransitPlugin

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		plugin = PluginsHelper.requirePlugin(GtfsTransitPlugin::class.java)
	}

	override fun onResume() {
		super.onResume()
		plugin.updateListener = { if (isAdded) setupPreferences() }
		setupPreferences()
	}

	override fun onPause() {
		super.onPause()
		plugin.updateListener = null
	}

	override fun setupPreferences() {
		val screen = preferenceScreen ?: return
		screen.removeAll()
		val update = plugin.update
		val idle = update == null

		val info = add(screen, Preference(requireContext()))
		info.key = "gtfs_info"
		info.isSelectable = false
		info.setTitle(R.string.gtfs_plugin_name)
		info.summary = getString(R.string.gtfs_plugin_description)
		info.icon = getContentIcon(R.drawable.ic_action_info_dark)

		if (update != null) {
			val progress = add(screen, Preference(requireContext()))
			progress.isSelectable = false
			progress.title = update.stage
			progress.summary = if (update.percent >= 0) "${update.percent}%" else null
			val cancel = add(screen, Preference(requireContext()))
			cancel.key = KEY_CANCEL
			cancel.setTitle(R.string.gtfs_cancel_work)
			cancel.icon = getContentIcon(R.drawable.ic_action_remove_dark)
		} else {
			plugin.lastMessage?.let {
				val result = add(screen, Preference(requireContext()))
				result.isSelectable = false
				result.title = it
			}
		}

		val feedsCategory = PreferenceCategory(requireContext())
		feedsCategory.setTitle(R.string.gtfs_feeds)
		feedsCategory.layoutResource = R.layout.preference_category_with_descr
		screen.addPreference(feedsCategory)
		feedsCategory.isIconSpaceReserved = false
		val feeds = plugin.store.listFeeds()
		if (feeds.isEmpty()) {
			val none = add(feedsCategory, Preference(requireContext()))
			none.isSelectable = false
			none.setTitle(R.string.gtfs_no_feeds)
		}
		for (feed in feeds) {
			val pref = add(feedsCategory, Preference(requireContext()))
			pref.key = KEY_FEED_PREFIX + feed.name
			pref.title = feed.name
			pref.summary = AndroidUtils.formatSize(app, feed.sizeBytes)
			pref.icon = getContentIcon(R.drawable.ic_action_delete_dark)
			pref.isEnabled = idle
		}
		val add = add(feedsCategory, Preference(requireContext()))
		add.key = KEY_ADD
		add.setTitle(R.string.gtfs_add_feeds)
		add.summary = getString(R.string.gtfs_add_feeds_descr)
		add.icon = getContentIcon(R.drawable.ic_action_add)
		add.isEnabled = idle

		val indexCategory = PreferenceCategory(requireContext())
		indexCategory.setTitle(R.string.gtfs_index)
		indexCategory.layoutResource = R.layout.preference_category_with_descr
		screen.addPreference(indexCategory)
		indexCategory.isIconSpaceReserved = false
		val status = add(indexCategory, Preference(requireContext()))
		status.isSelectable = false
		status.setTitle(R.string.gtfs_index)
		status.summary = indexStatus()
		val rebuild = add(indexCategory, Preference(requireContext()))
		rebuild.key = KEY_REBUILD
		rebuild.setTitle(R.string.gtfs_rebuild_index)
		rebuild.summary = getString(R.string.gtfs_rebuild_index_descr)
		rebuild.icon = getContentIcon(R.drawable.ic_action_update)
		rebuild.isEnabled = idle && feeds.isNotEmpty()

		val walk = add(indexCategory, Preference(requireContext()))
		walk.key = KEY_MAX_WALK
		walk.setTitle(R.string.gtfs_max_walk)
		walk.summary = "${plugin.maxWalkMeters.get()} m — ${getString(R.string.gtfs_max_walk_descr)}"
		walk.icon = getContentIcon(R.drawable.ic_action_pedestrian_dark)

		val pref = add(indexCategory, Preference(requireContext()))
		pref.key = KEY_WALK_PREF
		pref.setTitle(R.string.gtfs_walk_pref)
		pref.summary = "${walkPrefLabel(plugin.walkWeightPercent.get())} — ${getString(R.string.gtfs_walk_pref_descr)}"
		pref.icon = getContentIcon(R.drawable.ic_action_pedestrian_dark)
	}

	private fun <T : Preference> add(group: androidx.preference.PreferenceGroup, pref: T): T {
		pref.layoutResource = R.layout.preference_with_descr
		pref.isPersistent = false
		pref.isIconSpaceReserved = false
		group.addPreference(pref)
		pref.onPreferenceClickListener = this
		return pref
	}

	private fun indexStatus(): String {
		val s = plugin.summary()
		if (s == null) {
			val error = plugin.indexError()
			return if (error != null) getString(R.string.gtfs_index_error, error) else getString(R.string.gtfs_index_none)
		}
		val dates = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
		val built = DateFormat.getMediumDateFormat(app).format(Date(s.builtMillis))
		val lines = ArrayList<String>()
		lines.add(
			getString(
				R.string.gtfs_index_summary, built, AndroidUtils.formatSize(app, s.sizeBytes),
				s.stopCount, s.routeCount, s.tripCount
			)
		)
		lines.add(validityLine(s, dates))
		return lines.joinToString("\n")
	}

	private fun validityLine(s: GtfsIndexSummary, dates: DateTimeFormatter): String {
		val from: LocalDate? = s.validFrom
		val to: LocalDate? = s.validTo
		return when (s.validity(System.currentTimeMillis())) {
			GtfsIndexSummary.Validity.NO_SERVICE -> getString(R.string.gtfs_schedule_none)
			GtfsIndexSummary.Validity.NOT_STARTED -> getString(R.string.gtfs_schedule_not_started, dates.format(from))
			GtfsIndexSummary.Validity.EXPIRED -> getString(R.string.gtfs_schedule_ended, dates.format(to))
			GtfsIndexSummary.Validity.ACTIVE -> getString(R.string.gtfs_schedule_dates, dates.format(from), dates.format(to))
		}
	}

	override fun onPreferenceClick(preference: Preference): Boolean {
		val key = preference.key ?: return false
		when {
			key == KEY_ADD -> chooseFiles()
			key == KEY_CANCEL -> plugin.cancelUpdate()
			key == KEY_REBUILD -> plugin.rebuildIndex()
			key == KEY_MAX_WALK -> chooseMaxWalk()
			key == KEY_WALK_PREF -> chooseWalkPref()
			key.startsWith(KEY_FEED_PREFIX) -> confirmRemove(key.removePrefix(KEY_FEED_PREFIX))
			else -> return false
		}
		return true
	}

	/** The open dialog, dismissed with the view so rotation does not leak its window. */
	private var openDialog: AlertDialog? = null

	override fun onDestroyView() {
		openDialog?.dismiss()
		openDialog = null
		super.onDestroyView()
	}

	private fun chooseFiles() {
		val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
			.addCategory(Intent.CATEGORY_OPENABLE)
			.setType("*/*")
			.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
			.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
		AndroidUtils.startActivityForResultIfSafe(this, intent, REQUEST_FEEDS)
	}

	@Deprecated("Deprecated in Java")
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode != REQUEST_FEEDS) {
			@Suppress("DEPRECATION")
			super.onActivityResult(requestCode, resultCode, data)
			return
		}
		if (resultCode != Activity.RESULT_OK || data == null) {
			return
		}
		val uris = ArrayList<android.net.Uri>()
		data.data?.let { uris.add(it) }
		val clip = data.clipData
		if (clip != null) {
			for (i in 0 until clip.itemCount) {
				clip.getItemAt(i).uri?.let { if (!uris.contains(it)) uris.add(it) }
			}
		}
		if (uris.isNotEmpty()) {
			plugin.importFeeds(uris)
			setupPreferences()
		}
	}

	private fun confirmRemove(name: String) {
		openDialog?.dismiss()
		openDialog = AlertDialog.Builder(UiUtilities.getThemedContext(requireActivity(), nightMode))
			.setMessage(getString(R.string.gtfs_remove_feed_question, name))
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_remove) { _, _ ->
				plugin.removeFeed(name)
				setupPreferences()
			}
			.show()
	}

	/** A transit plan already on screen was ranked with the old setting, so plan it again. */
	private fun replanShownRoute() {
		val transit = app.transportRoutingHelper
		if (app.routingHelper.isPublicTransportMode && transit.routes != null) {
			transit.recalculateRouteDueToSettingsChange()
		}
	}

	private fun walkPrefLabel(percent: Int) = when {
		percent <= 0 -> getString(R.string.gtfs_walk_pref_fastest)
		percent <= 100 -> getString(R.string.gtfs_walk_pref_balanced)
		else -> getString(R.string.gtfs_walk_pref_less)
	}

	private fun chooseWalkPref() {
		val values = intArrayOf(0, 100, 200)
		val labels = values.map { walkPrefLabel(it) }.toTypedArray()
		val current = values.indexOf(plugin.walkWeightPercent.get())
		openDialog?.dismiss()
		openDialog = AlertDialog.Builder(UiUtilities.getThemedContext(requireActivity(), nightMode))
			.setTitle(R.string.gtfs_walk_pref)
			.setSingleChoiceItems(labels, current) { dialog, which ->
				plugin.walkWeightPercent.set(values[which])
				replanShownRoute()
				dialog.dismiss()
				setupPreferences()
			}
			.show()
	}

	private fun chooseMaxWalk() {
		val values = intArrayOf(400, 600, 800, 1000, 1500, 2000)
		val labels = values.map { "$it m" }.toTypedArray()
		val current = values.indexOf(plugin.maxWalkMeters.get())
		openDialog?.dismiss()
		openDialog = AlertDialog.Builder(UiUtilities.getThemedContext(requireActivity(), nightMode))
			.setTitle(R.string.gtfs_max_walk)
			.setSingleChoiceItems(labels, current) { dialog, which ->
				plugin.maxWalkMeters.set(values[which])
				replanShownRoute()
				dialog.dismiss()
				setupPreferences()
			}
			.show()
	}

	private companion object {
		const val REQUEST_FEEDS = 6411
		const val KEY_ADD = "gtfs_add_feeds"
		const val KEY_CANCEL = "gtfs_cancel"
		const val KEY_REBUILD = "gtfs_rebuild"
		const val KEY_MAX_WALK = "gtfs_max_walk"
		const val KEY_WALK_PREF = "gtfs_walk_pref"
		const val KEY_FEED_PREFIX = "gtfs_feed:"
	}
}
