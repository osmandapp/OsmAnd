package net.osmand.plus.views.mapwidgets.configure.appearance

import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceState
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceResolver
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelBackground
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance
import java.util.EnumMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class PanelAppearanceSettingsManager(
	private val app: OsmandApplication,
	settings: OsmandSettings
) {

	enum class ChangeOrigin {
		COMMITTED,
		PREVIEW
	}

	fun interface Listener {
		fun onPanelAppearanceChanged(origin: ChangeOrigin)
	}

	private data class PreviewSession(
		val token: Long,
		val panel: WidgetsPanel,
		val appModeKey: String,
		val layoutMode: ScreenLayoutMode?,
		val state: PanelAppearanceState
	)

	private var settings = settings
	private val revisionCounter = AtomicLong()
	private val committedRevisionCounter = AtomicLong()
	private val previewTokenCounter = AtomicLong()
	private val previewSession = AtomicReference<PreviewSession?>()
	private val settingsByPanel = EnumMap<WidgetsPanel, PanelAppearanceSettings>(WidgetsPanel::class.java)
	private val listeners = CopyOnWriteArrayList<Listener>()
	private var customWidgetBackgroundColorAvailable =
		InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)

	private val preferenceListener = StateChangedListener<Any?> { notifyChanged(ChangeOrigin.COMMITTED) }
	private val appModeListener = StateChangedListener<ApplicationMode> { notifyChanged(ChangeOrigin.COMMITTED) }

	init {
		registerSettings()
	}

	private fun registerSettings() {
		for (panel in WidgetsPanel.values()) {
			val panelSettings = PanelAppearanceSettings(settings, panel)
			settingsByPanel[panel] = panelSettings
			panelSettings.addListener(preferenceListener)
		}
		settings.APPLICATION_MODE.addListener(appModeListener)
	}

	private fun unregisterSettings() {
		settingsByPanel.values.forEach { it.removeListener(preferenceListener) }
		settings.APPLICATION_MODE.removeListener(appModeListener)
	}

	@Synchronized
	fun updateSettings(settings: OsmandSettings) {
		if (this.settings === settings) {
			return
		}
		unregisterSettings()
		this.settings = settings
		settingsByPanel.clear()
		previewSession.set(null)
		registerSettings()
		customWidgetBackgroundColorAvailable =
			InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)
		notifyChanged(ChangeOrigin.COMMITTED)
	}

	@Synchronized
	fun refreshPurchaseState() {
		val available = InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)
		if (customWidgetBackgroundColorAvailable != available) {
			customWidgetBackgroundColorAvailable = available
			notifyChanged(ChangeOrigin.COMMITTED)
		}
	}

	operator fun get(panel: WidgetsPanel): PanelAppearanceSettings =
		requireNotNull(settingsByPanel[panel]) { "Appearance settings are not registered for $panel" }

	fun resolve(
		panel: WidgetsPanel,
		layoutMode: ScreenLayoutMode?,
		nightMode: Boolean,
		boldText: Boolean,
		density: Float,
		applyPanelOverrides: Boolean = true
	): ResolvedPanelAppearance = resolveForProfile(
		panel, settings.applicationMode, layoutMode, nightMode, boldText, density,
		applyPanelOverrides
	)

	fun resolveForProfile(
		panel: WidgetsPanel,
		appMode: ApplicationMode,
		layoutMode: ScreenLayoutMode?,
		nightMode: Boolean,
		boldText: Boolean,
		density: Float,
		applyPanelOverrides: Boolean = true
	): ResolvedPanelAppearance = resolveForProfile(
		panel, appMode, layoutMode, nightMode, boldText, density, applyPanelOverrides, true
	)

	fun resolveCommitted(
		panel: WidgetsPanel,
		layoutMode: ScreenLayoutMode?,
		nightMode: Boolean,
		boldText: Boolean,
		density: Float,
		applyPanelOverrides: Boolean = true
	): ResolvedPanelAppearance = resolveForProfile(
		panel, settings.applicationMode, layoutMode, nightMode, boldText, density,
		applyPanelOverrides, false
	)

	private fun resolveForProfile(
		panel: WidgetsPanel,
		appMode: ApplicationMode,
		layoutMode: ScreenLayoutMode?,
		nightMode: Boolean,
		boldText: Boolean,
		density: Float,
		applyPanelOverrides: Boolean,
		includePreview: Boolean
	): ResolvedPanelAppearance {
		var state = (if (includePreview) getPreviewState(panel, appMode, layoutMode) else null)
			?: get(panel).readState(appMode, layoutMode)
		if (state.backgroundMode == PanelBackgroundMode.CUSTOM
				&& !InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)) {
			state = state.copy(backgroundMode = PanelBackgroundMode.DEFAULT)
		}
		return PanelAppearanceResolver.resolve(
			app,
			state,
			panel,
			nightMode,
			boldText,
			density,
			applyPanelOverrides
		)
	}

	fun beginPreview(panel: WidgetsPanel, appMode: ApplicationMode,
	                 layoutMode: ScreenLayoutMode?, state: PanelAppearanceState): Long {
		val token = previewTokenCounter.incrementAndGet()
		previewSession.set(PreviewSession(token, panel, appMode.stringKey, layoutMode, state))
		notifyChanged(ChangeOrigin.PREVIEW)
		return token
	}

	fun resolveCommittedBackground(panel: WidgetsPanel, layoutMode: ScreenLayoutMode?,
	                               nightMode: Boolean): ResolvedPanelBackground {
		return resolveCommitted(
			panel = panel,
			layoutMode = layoutMode,
			nightMode = nightMode,
			boldText = false,
			density = app.resources.displayMetrics.density
		).background
	}

	fun updatePreview(token: Long, state: PanelAppearanceState): Boolean {
		while (true) {
			val current = previewSession.get() ?: return false
			if (current.token != token) {
				return false
			}
			if (current.state == state) {
				return true
			}
			if (previewSession.compareAndSet(current, current.copy(state = state))) {
				notifyChanged(ChangeOrigin.PREVIEW)
				return true
			}
		}
	}

	fun endPreview(token: Long): Boolean {
		while (true) {
			val current = previewSession.get() ?: return false
			if (current.token != token) {
				return false
			}
			if (previewSession.compareAndSet(current, null)) {
				notifyChanged(ChangeOrigin.PREVIEW)
				return true
			}
		}
	}

	private fun getPreviewState(panel: WidgetsPanel, appMode: ApplicationMode,
	                            layoutMode: ScreenLayoutMode?): PanelAppearanceState? {
		val preview = previewSession.get() ?: return null
		return preview.state.takeIf {
			preview.panel == panel
					&& preview.appModeKey == appMode.stringKey
					&& preview.layoutMode == layoutMode
		}
	}

	fun addListener(listener: Listener) {
		listeners.addIfAbsent(listener)
	}

	fun removeListener(listener: Listener) {
		listeners.remove(listener)
	}

	private fun notifyChanged(origin: ChangeOrigin) {
		revisionCounter.incrementAndGet()
		if (origin == ChangeOrigin.COMMITTED) {
			committedRevisionCounter.incrementAndGet()
		}
		listeners.forEach { it.onPanelAppearanceChanged(origin) }
	}

	fun getRevision(): Long = revisionCounter.get()

	fun getCommittedRevision(): Long = committedRevisionCounter.get()
}