package net.osmand.plus.base

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.IOsmAndFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.InsetsUtils
import net.osmand.plus.utils.UiUtilities

open class BaseMaterialBottomSheetDialogFragment :
    BottomSheetDialogFragment(), IOsmAndFragment, ISupportInsets {

    protected lateinit var osmandApp: OsmandApplication
    protected lateinit var osmandSettings: OsmandSettings
    protected lateinit var currentAppMode: ApplicationMode
    protected lateinit var uiUtilities: UiUtilities
    protected var nightMode: Boolean = false

    private var lastRootInsets: WindowInsetsCompat? = null

    override fun getTheme(): Int =
        if (nightMode) R.style.OsmandMaterialDarkTheme else R.style.OsmandMaterialLightTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        osmandApp = requireActivity().application as OsmandApplication
        osmandSettings = osmandApp.getSettings()
        uiUtilities = osmandApp.uiUtilities
        currentAppMode = restoreAppMode(osmandApp, null, savedInstanceState, arguments)
        updateNightMode()
    }

    protected fun updateNightMode() {
        nightMode = resolveNightMode()
    }

    override fun onStart() {
        super.onStart()

        val dialog = getDialog()
        if (dialog != null && dialog.window != null && InsetsUtils.isEdgeToEdgeSupported()) {
            dialog.window!!.setNavigationBarContrastEnforced(false)
            InsetsUtils.processNavBarColor(this, dialog)

            if (Build.VERSION.SDK_INT >= 36) {
                //WindowCompat.enableEdgeToEdge(window);
            } else {
                WindowCompat.setDecorFitsSystemWindows(dialog.window!!, false)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dialog = getDialog()
        if (dialog != null && dialog.window != null && InsetsUtils.isEdgeToEdgeSupported()) {
            InsetsUtils.processInsets(this, dialog.window!!.decorView, view)
            dialog.window!!.setNavigationBarContrastEnforced(false)
        } else {
            InsetsUtils.processInsets(this, view, null)
        }
    }

    override fun getApp(): OsmandApplication {
        return osmandApp
    }

    override fun getThemedInflater(): LayoutInflater {
        return layoutInflater
    }

    override fun getThemeUsageContext(): ThemeUsageContext {
        return ThemeUsageContext.valueOf(isUsedOnMap())
    }

    protected open fun isUsedOnMap(): Boolean {
        return false
    }

    override fun getIconsCache(): UiUtilities {
        return uiUtilities
    }

    override fun getAppMode(): ApplicationMode {
        return currentAppMode
    }

    override fun setAppMode(appMode: ApplicationMode) {
        this.currentAppMode = appMode
    }

    override fun onApplyInsets(insets: WindowInsetsCompat) {
    }

    override fun getLastRootInsets(): WindowInsetsCompat? {
        return lastRootInsets
    }

    override fun setLastRootInsets(rootInsets: WindowInsetsCompat) {
        lastRootInsets = rootInsets;
    }
}