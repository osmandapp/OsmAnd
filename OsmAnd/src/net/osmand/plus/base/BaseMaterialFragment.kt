package net.osmand.plus.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.dialog.IOsmAndFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.utils.UiUtilities

open class BaseMaterialFragment : Fragment(), IOsmAndFragment, ISupportInsets {

	protected lateinit var osmandApp: OsmandApplication
	protected lateinit var osmandSettings: OsmandSettings
	protected lateinit var currentAppMode: ApplicationMode
	protected lateinit var uiUtilities: UiUtilities
	protected var nightMode: Boolean = false

	private var lastRootInsets: WindowInsetsCompat? = null

	@StyleRes
	protected open fun getMaterialThemeRes(nightMode: Boolean): Int =
		if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme

	protected open fun isUsedOnMap(): Boolean = false

	override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
		/* the map activity handles uiMode changes itself, so the view is inflated again without
		 * onCreate() - the night mode has to follow, or the dialogs and the icons of the screen
		 * keep the theme the screen was opened with */
		if (::osmandApp.isInitialized) {
			updateNightMode()
		}
		val inflater = super.onGetLayoutInflater(savedInstanceState)
		val themedContext = getMaterialThemedContext(inflater.context, savedInstanceState)
		return inflater.cloneInContext(themedContext)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		osmandApp = requireActivity().application as OsmandApplication
		osmandSettings = osmandApp.settings
		uiUtilities = osmandApp.uiUtilities
		currentAppMode = restoreAppMode(osmandApp, null, savedInstanceState, arguments)
		updateNightMode()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		saveAppModeToBundle(currentAppMode, outState)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		InsetsUtils.processInsets(this, view, null)
	}

	override fun onResume() {
		super.onResume()
		updateStatusBar()
	}

	override fun onDetach() {
		super.onDetach()
		updateStatusBar()
	}

	/**
	 * Colour of the status bar while the fragment is shown above the map, or -1 to leave the
	 * status bar to the map - the map activity asks the visible fragment through
	 * [UiUtilities.updateSystemBarColors].
	 */
	@ColorRes
	open fun getStatusBarColorId(): Int = -1

	/** true for light status bar icons, false for dark ones. */
	open fun getContentStatusBarNightMode(): Boolean = nightMode

	private fun updateStatusBar() {
		if (getStatusBarColorId() != -1) {
			(activity as? MapActivity)?.updateStatusBarColor()
		}
	}

	protected fun updateNightMode() {
		nightMode = resolveNightMode()
	}

	private fun getMaterialThemedContext(
		baseContext: Context,
		savedInstanceState: Bundle?
	): Context {
		val app = baseContext.applicationContext as? OsmandApplication ?: return baseContext
		val appMode = restoreAppMode(app, null, savedInstanceState, arguments)
		val isNightMode =
			app.daynightHelper.isNightMode(appMode, ThemeUsageContext.valueOf(isUsedOnMap()))
		return ContextThemeWrapper(baseContext, getMaterialThemeRes(isNightMode))
	}

	override fun getApp(): OsmandApplication = osmandApp

	override fun getThemedInflater(): LayoutInflater = layoutInflater

	override fun getThemeUsageContext(): ThemeUsageContext =
		ThemeUsageContext.valueOf(isUsedOnMap())

	override fun getIconsCache(): UiUtilities = uiUtilities

	override fun getAppMode(): ApplicationMode = currentAppMode

	override fun setAppMode(appMode: ApplicationMode) {
		currentAppMode = appMode
	}

	override fun onApplyInsets(insets: WindowInsetsCompat) = Unit

	override fun getLastRootInsets(): WindowInsetsCompat? = lastRootInsets

	override fun setLastRootInsets(rootInsets: WindowInsetsCompat) {
		lastRootInsets = rootInsets
	}
}
