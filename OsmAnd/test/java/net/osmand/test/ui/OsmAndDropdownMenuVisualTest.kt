package net.osmand.test.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.help.HelpActivity
import net.osmand.test.common.AndroidTest
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode
import net.osmand.plus.widgets.popup.showComposeDropdownMenu
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Ignore

@LargeTest
@RunWith(AndroidJUnit4::class)
class OsmAndDropdownMenuVisualTest : AndroidTest() {

	@Rule
	@JvmField
	val activityRule = ActivityTestRule(HelpActivity::class.java, true, false)

	private fun getThemedIcon(ctx: Context, @DrawableRes resId: Int, isNight: Boolean): Drawable? {
		val app = ctx.applicationContext as? OsmandApplication ?: return ContextCompat.getDrawable(ctx, resId)
		val actualResId = when (resId) {
			R.drawable.ic_action_delete_dark -> R.drawable.ic_action_delete_outlined
			R.drawable.ic_action_edit_dark -> R.drawable.ic_action_edit_outlined
			else -> resId
		}
		return app.uiUtilities.getIcon(actualResId, !isNight)
	}

	data class RealScreenMenuScenario(
		val id: Int,
		val screenName: String,
		val sourceLocation: String,
		val alignRight: Boolean = true,
		val topMarginDp: Float = 90f,
		val builder: (ctx: Context, anchor: View, nightMode: Boolean) -> PopUpMenuDisplayData
	)

	/**
	 * Run command to pull screenshots to a git-ignored directory:
	 *
	 * mkdir -p build/screenshots_popups
	 * adb pull /sdcard/Download/osmand_popups/. build/screenshots_popups/
	 */
	@Ignore("Manual visual screenshot test only")
	@Test
	fun testCaptureAll56RealScreenDropdownMenus() {
		val activity = activityRule.launchActivity(null)
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		val targetContext = instrumentation.targetContext

		val publicDir = "/sdcard/Download/osmand_popups"
		instrumentation.uiAutomation.executeShellCommand("mkdir -p $publicDir").close()
		instrumentation.uiAutomation.executeShellCommand("chmod 777 $publicDir").close()
		instrumentation.uiAutomation.executeShellCommand("pm grant ${activity.packageName} android.permission.WRITE_EXTERNAL_STORAGE").close()
		instrumentation.uiAutomation.executeShellCommand("pm grant ${activity.packageName} android.permission.READ_EXTERNAL_STORAGE").close()

		val saveDir = File(publicDir).apply { mkdirs() }
		println(">>> [VisualTest] Target directory: ${saveDir.absolutePath}")

		val scenarios = getAllRealMenuScenarios()
		println(">>> [VisualTest] Loaded ${scenarios.size} real screen menu scenarios")

		var totalScreenshotsCaptured = 0
		val themes = listOf(false to "light", true to "dark")

		for (scenario in scenarios) {
			for ((isNight, themeName) in themes) {
				var anchorView: View? = null
				var overlayView: View? = null
				var popupWindow: android.widget.PopupWindow? = null

				val showLatch = CountDownLatch(1)
				activity.runOnUiThread {
					val app = activity.application as OsmandApplication
					app.settings.OSMAND_THEME.set(if (isNight) OsmandSettings.OSMAND_DARK_THEME else OsmandSettings.OSMAND_LIGHT_THEME)

					val contentLayout = activity.findViewById<ViewGroup>(android.R.id.content)
					val overlay = View(activity).apply {
						layoutParams = FrameLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.MATCH_PARENT
						)
						setBackgroundColor(if (isNight) 0xFF17181A.toInt() else 0xFFF0F0F0.toInt())
					}
					contentLayout.addView(overlay)
					overlayView = overlay

					val anchor = View(activity).apply {
						layoutParams = FrameLayout.LayoutParams(
							AndroidUtils.dpToPx(activity, 48f),
							AndroidUtils.dpToPx(activity, 48f)
						).apply {
							gravity = if (scenario.alignRight) Gravity.TOP or Gravity.END else Gravity.TOP or Gravity.START
							topMargin = AndroidUtils.dpToPx(activity, scenario.topMarginDp)
							if (scenario.alignRight) {
								rightMargin = AndroidUtils.dpToPx(activity, 16f)
							} else {
								leftMargin = AndroidUtils.dpToPx(activity, 16f)
							}
						}
					}
					contentLayout.addView(anchor)
					anchorView = anchor

					val displayData = scenario.builder(targetContext, anchor, isNight)
					displayData.nightMode = isNight
					displayData.anchorView = anchor

					popupWindow = showComposeDropdownMenu(displayData)
					showLatch.countDown()
				}

				showLatch.await(3, TimeUnit.SECONDS)
				instrumentation.waitForIdleSync()
				Thread.sleep(350)

				val fullScreenshot = instrumentation.uiAutomation.takeScreenshot()
				val filename = String.format("%02d_%s_%s.png", scenario.id, scenario.screenName, themeName)
				val croppedFile = File(saveDir, filename)

				var bitmapToSave = fullScreenshot
				popupWindow?.contentView?.let { contentView ->
					if (contentView.width > 0 && contentView.height > 0) {
						val location = IntArray(2)
						contentView.getLocationOnScreen(location)
						val marginPx = AndroidUtils.dpToPx(activity, 16f)
						val x = (location[0] - marginPx).coerceIn(0, fullScreenshot.width - 1)
						val y = (location[1] - marginPx).coerceIn(0, fullScreenshot.height - 1)
						val w = (contentView.width + marginPx * 2).coerceIn(1, fullScreenshot.width - x)
						val h = (contentView.height + marginPx * 2).coerceIn(1, fullScreenshot.height - y)
						try {
							bitmapToSave = Bitmap.createBitmap(fullScreenshot, x, y, w, h)
						} catch (e: Exception) {
							println(">>> [VisualTest] Crop error: ${e.message}")
						}
					}
				}

				try {
					FileOutputStream(croppedFile).use { out ->
						bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out)
					}
					totalScreenshotsCaptured++
					println(">>> [VisualTest] Captured [$totalScreenshotsCaptured/${scenarios.size * 2}] $filename (${croppedFile.length()} bytes)")
				} catch (e: Exception) {
					println(">>> [VisualTest] Error writing $filename: ${e.message}")
				}

				val dismissLatch = CountDownLatch(1)
				activity.runOnUiThread {
					popupWindow?.dismiss()
					val contentLayout = activity.findViewById<ViewGroup>(android.R.id.content)
					anchorView?.let { anchor ->
						contentLayout.removeView(anchor)
					}
					overlayView?.let { overlay ->
						contentLayout.removeView(overlay)
					}
					dismissLatch.countDown()
				}
				dismissLatch.await(2, TimeUnit.SECONDS)
				instrumentation.waitForIdleSync()
				Thread.sleep(80)
			}
		}

		instrumentation.uiAutomation.executeShellCommand("chmod -R 777 $publicDir").close()

		val expectedScreenshots = scenarios.size * 2
		println("=================================================================")
		println(">>> [VisualTest] ALL MENUS PROCESSED SUCCESSFULLY")
		println(">>> Call sites in OsmAnd codebase: ${scenarios.size}")
		println(">>> Total screenshots saved: $totalScreenshotsCaptured")
		println(">>> Expected (56 x 2 themes): $expectedScreenshots")
		println(">>> Ratio: ${totalScreenshotsCaptured.toDouble() / scenarios.size}x")
		println("=================================================================")

		assertEquals(expectedScreenshots, totalScreenshotsCaptured)
	}

	private fun getAllRealMenuScenarios(): List<RealScreenMenuScenario> {
		return listOf(
			// 1. ChangesFragment
			RealScreenMenuScenario(1, "ChangesFragment_options", "ChangesFragment.java:143") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.upload_local_versions).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.download_cloud_versions).create()
					)
				}
			},
			// 2. CloudTrashFragment
			RealScreenMenuScenario(2, "CloudTrashFragment_empty", "CloudTrashFragment.java:130") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx)
							.setTitleId(R.string.shared_string_empty_trash)
							.setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night))
							.create()
					)
				}
			},
			// 3. BaseMultiStateCardController
			RealScreenMenuScenario(3, "BaseMultiStateCard_states", "BaseMultiStateCardController.java:66") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_show).showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_hide).showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 4. GradientPaletteController
			RealScreenMenuScenario(4, "GradientPalette_delete", "GradientPaletteController.kt:165") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					widthMode = PopUpMenuWidthMode.STANDARD
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_remove).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).create()
					)
				}
			},
			// 5. SolidPaletteController
			RealScreenMenuScenario(5, "SolidPalette_delete", "SolidPaletteController.kt:158") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					widthMode = PopUpMenuWidthMode.STANDARD
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_remove).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).create()
					)
				}
			},
			// 6. CoordinatesGridController
			RealScreenMenuScenario(6, "CoordinatesGrid_format", "CoordinatesGridController.java:152") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("D.DDDDD°").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("D° M.MMM'").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("D° M' S.S\"").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("UTM").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("OLC").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 7. TracksTabsFragment (Options)
			RealScreenMenuScenario(7, "TracksTabs_options", "TracksTabsFragment.java:197", alignRight = true) { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Change appearance (3)").setIcon(getThemedIcon(ctx, R.drawable.ic_action_appearance, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_import).setIcon(getThemedIcon(ctx, R.drawable.ic_action_import, night)).create()
					)
				}
			},
			// 8. TracksTabsFragment (Folder options)
			RealScreenMenuScenario(8, "TracksTabs_folder_options", "TracksTabsFragment.java:456") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_rename).setIcon(getThemedIcon(ctx, R.drawable.ic_action_edit_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 9. GroupMenuProvider
			RealScreenMenuScenario(9, "GroupMenuProvider_download", "GroupMenuProvider.java:162") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Download all").setIcon(getThemedIcon(ctx, R.drawable.ic_action_device_download, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Reload").setIcon(getThemedIcon(ctx, R.drawable.ic_action_reset, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Delete all").setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 10. UpdatesIndexFragment
			RealScreenMenuScenario(10, "UpdatesIndex_options", "UpdatesIndexFragment.java:369") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					widthMode = PopUpMenuWidthMode.STANDARD
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_update).setIcon(getThemedIcon(ctx, R.drawable.ic_action_update, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.local_index_mi_backup).setIcon(getThemedIcon(ctx, R.drawable.ic_action_box_closed_arrow, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 11. AttachedMediaGridController
			RealScreenMenuScenario(11, "AttachedMediaGrid_item", "AttachedMediaGridController.kt:295") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_view).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 12. AttachedMediaUiHelper
			RealScreenMenuScenario(12, "AttachedMediaUiHelper_options", "AttachedMediaUiHelper.java:125") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Save to gallery").create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 13. GalleryPhotoPagerFragment
			RealScreenMenuScenario(13, "GalleryPhotoPager_options", "GalleryPhotoPagerFragment.java:449") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_details).setIcon(getThemedIcon(ctx, R.drawable.ic_action_layers, night)).showTopDivider(true).create()
					)
				}
			},
			// 14. GallerySortBarView
			RealScreenMenuScenario(14, "GallerySortBarView_sort", "GallerySortBarView.kt:115") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Date (newest first)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Date (oldest first)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Name").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 15. HelpMainFragment
			RealScreenMenuScenario(15, "HelpMainFragment_options", "HelpMainFragment.java:181") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.send_crash_log).setIcon(getThemedIcon(ctx, R.drawable.ic_action_bug_outlined_send, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.send_logcat_log).setIcon(getThemedIcon(ctx, R.drawable.ic_action_file_report_outlined_send, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.copy_build_version).setIcon(getThemedIcon(ctx, R.drawable.ic_action_osmand_logo, night)).showTopDivider(true).create()
					)
				}
			},
			// 16. EditKeyAssignmentController
			RealScreenMenuScenario(16, "EditKeyAssignment_action", "EditKeyAssignmentController.java:155") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Zoom in").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Zoom out").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Center to location").showTopDivider(true).create()
					)
				}
			},
			// 17. InputDevicesAdapter
			RealScreenMenuScenario(17, "InputDevices_device", "InputDevicesAdapter.java:144") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_rename).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 18. EditorIconScreenController
			RealScreenMenuScenario(18, "EditorIconScreen_category", "EditorIconScreenController.java:149") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Special").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Transport").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Food").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Tourism").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 19. DirectionIndicationDialogFragment
			RealScreenMenuScenario(19, "DirectionIndication_style", "DirectionIndicationDialogFragment.java:112") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Arrow").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Pointer").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Circle").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 20. SelectFileBottomSheet
			RealScreenMenuScenario(20, "SelectFile_format", "SelectFileBottomSheet.java:198") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("GPX").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("KML").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("GeoJSON").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 21. FavoriteMenu (Item options)
			RealScreenMenuScenario(21, "FavoriteMenu_item_options", "FavoriteMenu.java:196") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_show_on_map).setIcon(getThemedIcon(ctx, R.drawable.ic_action_layers, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_edit).setIcon(getThemedIcon(ctx, R.drawable.ic_action_edit_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 22. FavoriteMenu (Group options)
			RealScreenMenuScenario(22, "FavoriteMenu_group_options", "FavoriteMenu.java:323") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.change_appearance).setIcon(getThemedIcon(ctx, R.drawable.ic_action_appearance, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_rename).setIcon(getThemedIcon(ctx, R.drawable.ic_action_edit_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 23. FavoriteMenu (Sort)
			RealScreenMenuScenario(23, "FavoriteMenu_sort", "FavoriteMenu.java:453", alignRight = false) { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.sort_by_name).showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Date (newest)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Date (oldest)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.sort_by_distance).showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 24. FavoriteMenu (Batch)
			RealScreenMenuScenario(24, "FavoriteMenu_batch", "FavoriteMenu.java:494") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Move to group").create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.change_appearance).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 25. FavoriteMenu (Export options)
			RealScreenMenuScenario(25, "FavoriteMenu_export", "FavoriteMenu.java:636") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Export as GPX").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Export as CSV").create()
					)
				}
			},
			// 26. FavoriteMenu (Filter)
			RealScreenMenuScenario(26, "FavoriteMenu_filter", "FavoriteMenu.java:713") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_all).showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Personal").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Places to visit").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 27. TrackFoldersHelper (Track actions)
			RealScreenMenuScenario(27, "TrackFoldersHelper_track", "TrackFoldersHelper.java:201") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_edit).setIcon(getThemedIcon(ctx, R.drawable.ic_action_edit_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_appearance).setIcon(getThemedIcon(ctx, R.drawable.ic_action_appearance, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 28. TrackFoldersHelper (Folder actions)
			RealScreenMenuScenario(28, "TrackFoldersHelper_folder", "TrackFoldersHelper.java:270") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_rename).setIcon(getThemedIcon(ctx, R.drawable.ic_action_edit_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_export).setIcon(getThemedIcon(ctx, R.drawable.ic_action_export, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 29. TrackFoldersHelper (Multi selection)
			RealScreenMenuScenario(29, "TrackFoldersHelper_multi", "TrackFoldersHelper.java:369") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.change_activity).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.change_appearance).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 30. SmartFolderFragment
			RealScreenMenuScenario(30, "SmartFolder_options", "SmartFolderFragment.kt:112") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_edit).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 31. SplitSegmentDialogFragment
			RealScreenMenuScenario(31, "SplitSegment_method", "SplitSegmentDialogFragment.java:249") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("By distance").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("By time interval").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("By track points").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 32. StarMapSearchDialogFragment (Sort)
			RealScreenMenuScenario(32, "StarMapSearch_sort", "StarMapSearchDialogFragment.kt:1622", alignRight = false) { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Magnitude (brightest first)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Name (A to Z)").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Constellation").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Distance").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 33. StarMapSearchDialogFragment (Filter)
			RealScreenMenuScenario(33, "StarMapSearch_filter", "StarMapSearchDialogFragment.kt:1745", alignRight = true) { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					limitHeight = true
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Type").setTitleBold(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Show all").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Naked eye only").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(true).showTopDivider(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Category").setTitleBold(true).showTopDivider(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Solar system").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Stars").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Deep sky").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(false).create()
					)
				}
			},
			// 34. TerrainFragment
			RealScreenMenuScenario(34, "Terrain_color_ramp", "TerrainFragment.java:248") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Default relief").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Warm palette").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Cold palette").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Hypsometric").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 35. Buildings3DColorScreenController
			RealScreenMenuScenario(35, "Buildings3D_color", "Buildings3DColorScreenController.kt:85") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Single color").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Height based").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Type based").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 36. OfflineWeatherForecastCard
			RealScreenMenuScenario(36, "OfflineWeather_layer", "OfflineWeatherForecastCard.java:233") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Precipitation").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Wind speed").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Temperature").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Atmospheric pressure").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 37. WeatherContoursButton
			RealScreenMenuScenario(37, "WeatherContours_interval", "WeatherContoursButton.java:143") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("1 hPa").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("2 hPa").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("4 hPa").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("8 hPa").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 38. WeatherLayersButton
			RealScreenMenuScenario(38, "WeatherLayers_type", "WeatherLayersButton.java:157") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Wind gusts").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Cloud cover").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Rain / Snow").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 39. SelectNavProfileBottomSheet
			RealScreenMenuScenario(39, "SelectNavProfile_options", "SelectNavProfileBottomSheet.java:274") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.copy_from_other_profile).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.reset_to_default).showTopDivider(true).create()
					)
				}
			},
			// 40. QuickActionListFragment
			RealScreenMenuScenario(40, "QuickActionList_item", "QuickActionListFragment.java:355") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_edit).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_move_up).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_move_down).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 41. SliderButtonsCard
			RealScreenMenuScenario(41, "SliderButtonsCard_options", "SliderButtonsCard.java:131") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Small buttons").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Medium buttons").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Large buttons").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 42. ChooseRouteFragment
			RealScreenMenuScenario(42, "ChooseRoute_alternative", "ChooseRouteFragment.java:527") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Alternative 1 (Fastest)").setSupportingText("35 min • 28 km").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Alternative 2 (Shortest)").setSupportingText("42 min • 22 km").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Alternative 3 (No tolls)").setSupportingText("48 min • 31 km").showTopDivider(true).create()
					)
				}
			},
			// 43. FollowTrackFragment
			RealScreenMenuScenario(43, "FollowTrack_options", "FollowTrackFragment.java:535") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.sort_last_modified).setIcon(getThemedIcon(ctx, R.drawable.ic_action_time_start, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.sort_name_ascending).setIcon(getThemedIcon(ctx, R.drawable.ic_action_sort_by_name_ascending, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.sort_name_descending).setIcon(getThemedIcon(ctx, R.drawable.ic_action_sort_by_name_descending, night)).showTopDivider(true).create()
					)
				}
			},
			// 44. ChipsLayout
			RealScreenMenuScenario(44, "ChipsLayout_filter", "ChipsLayout.kt:388") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("< 1 km").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("< 5 km").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("< 10 km").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Any distance").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).showTopDivider(true).create()
					)
				}
			},
			// 45. RouteParametersFragment (Avoid roads)
			RealScreenMenuScenario(45, "RouteParameters_avoid", "RouteParametersFragment.java:474") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Avoid toll roads").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Avoid motorways").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Avoid unpaved roads").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Avoid ferries").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.CHECKBOX).setSelected(false).create()
					)
				}
			},
			// 46. RouteParametersFragment (Vehicle type)
			RealScreenMenuScenario(46, "RouteParameters_vehicle", "RouteParametersFragment.java:496") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Default car").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Electric vehicle").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Van / SUV").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 47. Track3DCard
			RealScreenMenuScenario(47, "Track3D_mode", "Track3DCard.java:180") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Altitude").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Speed").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Slope").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 48. WidgetsContextMenu
			RealScreenMenuScenario(48, "WidgetsContextMenu_actions", "WidgetsContextMenu.java:105") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_settings).setIcon(getThemedIcon(ctx, R.drawable.ic_action_settings_outlined, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_delete).setIcon(getThemedIcon(ctx, R.drawable.ic_action_delete_outlined, night)).showTopDivider(true).create()
					)
				}
			},
			// 49. PanelAppearanceFragment
			RealScreenMenuScenario(49, "PanelAppearance_options", "PanelAppearanceFragment.kt:359") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Top panel").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Bottom panel").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Left panel").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 50. WidgetsAppearanceFragment
			RealScreenMenuScenario(50, "WidgetsAppearance_style", "WidgetsAppearanceFragment.kt:293") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Small text size").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Medium text size").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Large text size").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 51. DefaultMapButtonFragment
			RealScreenMenuScenario(51, "DefaultMapButton_options", "DefaultMapButtonFragment.java:160") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Show always").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Show in navigation only").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Hide").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 52. DefaultMapButtonsFragment
			RealScreenMenuScenario(52, "DefaultMapButtons_list", "DefaultMapButtonsFragment.java:114") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_move_up).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_move_down).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.reset_to_default).showTopDivider(true).create()
					)
				}
			},
			// 53. ConfigureScreenFragment
			RealScreenMenuScenario(53, "ConfigureScreen_options", "ConfigureScreenFragment.java:200") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_help).setIcon(getThemedIcon(ctx, R.drawable.ic_action_help_online, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.reset_to_default).setIcon(getThemedIcon(ctx, R.drawable.ic_action_reset, night)).showTopDivider(true).create()
					)
				}
			},
			// 54. ConfigureWidgetsFragment
			RealScreenMenuScenario(54, "ConfigureWidgets_category", "ConfigureWidgetsFragment.java:313") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Navigation widgets").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Trip recording").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Speed and altitude").create(),
						PopUpMenuItem.Builder(ctx).setTitle("Special tools").showTopDivider(true).create()
					)
				}
			},
			// 55. WidgetInfoBaseFragment
			RealScreenMenuScenario(55, "WidgetInfoBase_units", "WidgetInfoBaseFragment.java:144") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Kilometers / meters").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(true).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Miles / feet").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Nautical miles").showCompoundBtn(0, PopUpMenuItem.CompoundButtonType.RADIO).setSelected(false).create()
					)
				}
			},
			// 56. WikipediaPoiMenu
			RealScreenMenuScenario(56, "WikipediaPoiMenu_actions", "WikipediaPoiMenu.java:250") { ctx, _, night ->
				PopUpMenuDisplayData().apply {
					menuItems = listOf(
						PopUpMenuItem.Builder(ctx).setTitle("Open full article").setIcon(getThemedIcon(ctx, R.drawable.ic_action_book_info, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitleId(R.string.shared_string_show_on_map).setIcon(getThemedIcon(ctx, R.drawable.ic_action_layers, night)).create(),
						PopUpMenuItem.Builder(ctx).setTitle("Download images for offline").setIcon(getThemedIcon(ctx, R.drawable.ic_action_device_download, night)).showTopDivider(true).create()
					)
				}
			}
		)
	}
}
