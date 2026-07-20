package net.osmand.plus.views.layers;


import static android.view.View.VISIBLE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.views.AndroidAutoMapPlaceholderView;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapDisplayPositionManager.BoundsChangeListener;
import net.osmand.plus.helpers.MapDisplayPositionManager.ICoveredScreenRectProvider;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.InsetTargetBuilder;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.ObservableFrameLayout;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.controls.SideWidgetsPanel;
import net.osmand.plus.views.controls.VerticalWidgetPanel;
import net.osmand.plus.views.controls.VerticalWidgetPanel.VerticalPanelVisibilityListener;
import net.osmand.plus.views.controls.WidgetsContainer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.CenterWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.TopToolbarView;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.configure.appearance.PanelAppearanceSettingsManager;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;

public class MapInfoLayer extends OsmandMapLayer implements ICoveredScreenRectProvider {

	private final RouteLayer routeLayer;
	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final PanelAppearanceSettingsManager appearanceSettingsManager;
	private final MapWidgetRegistry widgetRegistry;
	private final MapDisplayPositionManager mapDisplayPositionManager;

	private MapHudLayout mapHudLayout;
	private SideWidgetsPanel leftWidgetsPanel;
	private SideWidgetsPanel rightWidgetsPanel;
	private VerticalWidgetPanel topWidgetsPanel;
	private VerticalWidgetPanel bottomWidgetsPanel;

	private RulerWidget rulerWidget;
	private AlarmWidget alarmWidget;
	private SpeedometerWidget speedometerWidget;
	private List<RulerWidget> rulerWidgets;
	private List<SideWidgetsPanel> sideWidgetsPanels;
	private List<WidgetsContainer> additionalWidgets;

	private AndroidAutoMapPlaceholderView androidAutoMapPlaceholderView;
	private ObservableFrameLayout bottomFragmentContainer;

	private DrawSettings drawSettings;
	private boolean colorsInitialized;
	private boolean systemBarsInitialized;
	private long appliedAppearanceRevision;
	private long appliedCommittedAppearanceRevision;
	@Nullable
	private ScreenLayoutMode appliedLayoutMode;
	private boolean appliedNightMode;
	private boolean appliedFollowingMode;

	private TopToolbarView topToolbarView;

	private final BoundsChangeListener topPanelBoundsChangeListener;
	private final BoundsChangeListener bottomPanelBoundsChangeListener;
	private VerticalPanelVisibilityListener topWidgetsVisibilityListener;
	private VerticalPanelVisibilityListener bottomWidgetsVisibilityListener;
	private boolean topWidgetsVisibleForSystemBars;
	private boolean bottomWidgetsVisibleForSystemBars;
	private WindowInsetsCompat lastWindowInsets;

	private boolean isContentVisible = false;
	private boolean appearanceRefreshScheduled;
	private final Runnable appearanceRefreshRunnable = () -> {
		appearanceRefreshScheduled = false;
		refreshWidgetAppearance();
	};
	private final PanelAppearanceSettingsManager.Listener appearanceSettingsListener;

	public MapInfoLayer(@NonNull Context context, @NonNull RouteLayer layer) {
		super(context);
		this.routeLayer = layer;

		app = getApplication();
		settings = app.getSettings();
		appearanceSettingsManager = app.getPanelAppearanceSettingsManager();
		appearanceSettingsListener = origin -> scheduleWidgetAppearanceRefresh();
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		widgetRegistry = mapLayers.getMapWidgetRegistry();
		mapDisplayPositionManager = app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
		topPanelBoundsChangeListener = new BoundsChangeListener(mapDisplayPositionManager, true);
		bottomPanelBoundsChangeListener = new BoundsChangeListener(mapDisplayPositionManager, true);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			mapHudLayout = mapActivity.findViewById(R.id.map_hud_layout);
			topWidgetsPanel = mapActivity.findViewById(R.id.top_widgets_panel);
			leftWidgetsPanel = mapActivity.findViewById(R.id.map_left_widgets_panel);
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			bottomWidgetsPanel = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			androidAutoMapPlaceholderView = mapActivity.findViewById(R.id.AndroidAutoPlaceholder);
			bottomFragmentContainer = mapActivity.findViewById(R.id.bottomFragmentContainer);

			leftWidgetsPanel.setScreenSize(mapActivity);
			rightWidgetsPanel.setScreenSize(mapActivity);

			registerWidgetPanelListeners();

			LayoutInflater inflater = mapActivity.getLayoutInflater();
			rulerWidget = (RulerWidget) inflater.inflate(R.layout.map_ruler, mapHudLayout, false);
			mapHudLayout.addWidget(rulerWidget);

			registerAllControls(mapActivity);
			recreateControls();

			mapDisplayPositionManager.registerCoveredScreenRectProvider(this);
			topWidgetsPanel.addOnLayoutChangeListener(topPanelBoundsChangeListener);
			bottomWidgetsPanel.addOnLayoutChangeListener(bottomPanelBoundsChangeListener);
			mapDisplayPositionManager.updateMapDisplayPosition(true);
			appearanceSettingsManager.addListener(appearanceSettingsListener);
		} else {
			appearanceSettingsManager.removeListener(appearanceSettingsListener);
			if (mapHudLayout != null) {
				mapHudLayout.removeCallbacks(appearanceRefreshRunnable);
			}
			appearanceRefreshScheduled = false;
			if (topWidgetsPanel != null) {
				topWidgetsPanel.removeOnLayoutChangeListener(topPanelBoundsChangeListener);
				topWidgetsPanel.removeVisibilityListener(topWidgetsVisibilityListener);
			}
			if (bottomWidgetsPanel != null) {
				bottomWidgetsPanel.removeOnLayoutChangeListener(bottomPanelBoundsChangeListener);
				bottomWidgetsPanel.removeVisibilityListener(bottomWidgetsVisibilityListener);
			}
			if (mapHudLayout != null) {
				mapHudLayout.removeWidget(rulerWidget);
			}

			if (bottomFragmentContainer != null) {
				bottomFragmentContainer.setOnChildChanged(null);
			}

			mapDisplayPositionManager.unregisterCoveredScreenRectProvider(this);
			mapDisplayPositionManager.updateMapDisplayPosition(true);

			resetCachedTheme();
			widgetRegistry.clearWidgets();

			mapHudLayout = null;
			topWidgetsPanel = null;
			bottomWidgetsPanel = null;
			leftWidgetsPanel = null;
			rightWidgetsPanel = null;
			rulerWidget = null;
			androidAutoMapPlaceholderView = null;
			bottomFragmentContainer = null;

			drawSettings = null;
			alarmWidget = null;
			speedometerWidget = null;
			rulerWidgets = null;
			sideWidgetsPanels = null;
			additionalWidgets = null;
			topToolbarView = null;
			lastWindowInsets = null;
			topWidgetsVisibilityListener = null;
			bottomWidgetsVisibilityListener = null;
			topWidgetsVisibleForSystemBars = false;
			bottomWidgetsVisibleForSystemBars = false;
		}
	}

	private void onTopWidgetPanelChanged(boolean isVisible) {
		if (topWidgetsVisibleForSystemBars != isVisible) {
			topWidgetsVisibleForSystemBars = isVisible;
			updateSystemBarsForPanelVisibilityChange();
		}
	}

	private void onBottomWidgetPanelChanged(boolean isVisible) {
		if (bottomWidgetsVisibleForSystemBars != isVisible) {
			bottomWidgetsVisibleForSystemBars = isVisible;
			updateSystemBarsForPanelVisibilityChange();
		}
		if (bottomFragmentContainer != null) {
			boolean bottomFragmentVisible = bottomFragmentContainer.getChildCount() > 0;
			if (bottomFragmentVisible) {
				isVisible = true;
			}
		}
		updateLayerInsets(isVisible, false);
	}

	private void updateSystemBarsForPanelVisibilityChange() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.updateStatusBarColor();
		}
	}

	private void updateLayerInsets(boolean isVisible, boolean forceUpdate) {
		if (!forceUpdate && (!InsetsUtils.isEdgeToEdgeSupported() || isContentVisible == isVisible)) {
			return;
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || lastWindowInsets == null) {
			return;
		}
		InsetTargetsCollection collection = new InsetTargetsCollection();
		InsetTargetBuilder builder = InsetTarget.createCustomBuilder(mapHudLayout).preferMargin(true);

		InsetSide[] sides = {InsetSide.TOP, InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT};
		builder.portraitSides(sides).landscapeSides(sides);

		collection.add(builder);
		InsetsUtils.processInsets(mapActivity.findViewById(R.id.map_hud_container), collection, lastWindowInsets);
		isContentVisible = isVisible;
	}

	@Override
	public void setWindowInsets(@NonNull WindowInsetsCompat windowInsets) {
		super.setWindowInsets(windowInsets);
		this.lastWindowInsets = windowInsets;
		Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
		if (leftWidgetsPanel != null) {
			leftWidgetsPanel.setInsets(insets);
		}
		if (rightWidgetsPanel != null) {
			rightWidgetsPanel.setInsets(insets);
		}
		if (bottomWidgetsPanel != null && mapHudLayout != null) {
			updateLayerInsets(bottomWidgetsPanel.isAnyRowVisible(), true);
		}
	}

	private void registerWidgetPanelListeners() {
		topWidgetsVisibilityListener = this::onTopWidgetPanelChanged;
		bottomWidgetsVisibilityListener = this::onBottomWidgetPanelChanged;
		topWidgetsPanel.addVisibilityListener(topWidgetsVisibilityListener);
		bottomWidgetsPanel.addVisibilityListener(bottomWidgetsVisibilityListener);
		if (!InsetsUtils.isEdgeToEdgeSupported()) {
			return;
		}
		bottomFragmentContainer.setOnChildChanged(hasChild -> {
			if (bottomFragmentContainer != null) {
				updateLayerInsets(hasChild, true);
			}
			return Unit.INSTANCE;
		});
	}

	@Nullable
	public TopToolbarView getTopToolbarView() {
		return topToolbarView;
	}

	private void resetCachedTheme() {
		colorsInitialized = false;
		systemBarsInitialized = false;
	}

	public void removeWidget(@NonNull MapWidget widget) {
		widgetRegistry.removeWidget(widget);
	}

	public void addTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.addController(controller);
		}
	}

	public void removeTopToolbarController(TopToolbarController controller) {
		if (topToolbarView != null) {
			topToolbarView.removeController(controller);
		}
	}

	public boolean hasTopToolbar() {
		return getTopToolbarController() != null;
	}

	public TopToolbarController getTopToolbarController() {
		return topToolbarView == null ? null : topToolbarView.getTopController();
	}

	@Nullable
	public TopToolbarController getTopToolbarController(TopToolbarControllerType type) {
		return topToolbarView == null ? null : topToolbarView.getController(type);
	}

	public boolean isTopToolbarViewVisible() {
		return topToolbarView != null && topToolbarView.isTopToolbarViewVisible();
	}

	public boolean isMapControlsVisible() {
		return mapHudLayout != null && mapHudLayout.getVisibility() == VISIBLE;
	}

	public void updateSideWidgets() {
		if (leftWidgetsPanel != null) {
			leftWidgetsPanel.update(drawSettings);
		}
		if (rightWidgetsPanel != null) {
			rightWidgetsPanel.update(drawSettings);
		}
		if (!Algorithms.isEmpty(sideWidgetsPanels)) {
			for (SideWidgetsPanel sideWidgetsPanel : sideWidgetsPanels) {
				sideWidgetsPanel.update(drawSettings);
			}
		}
		if (!Algorithms.isEmpty(additionalWidgets)) {
			for (WidgetsContainer widgetsContainer : additionalWidgets) {
				widgetsContainer.update(drawSettings);
			}
		}
	}

	public void recreateAllControls(@NonNull MapActivity mapActivity) {
		widgetRegistry.clearWidgets();
		registerAllControls(mapActivity);
		widgetRegistry.reorderWidgets(ScreenLayoutMode.getDefault(mapActivity));
		recreateControls();
	}

	private void registerAllControls(@NonNull MapActivity mapActivity) {
		rulerWidgets = new ArrayList<>();
		sideWidgetsPanels = new ArrayList<>();
		additionalWidgets = new ArrayList<>();

		if (topToolbarView == null) {
			topToolbarView = mapActivity.findViewById(R.id.widget_top_bar);
			topToolbarView.setMapActivity(mapActivity);
		}
		updateTopToolbar(false);

		alarmWidget = new AlarmWidget(app, mapActivity);
		alarmWidget.setVisibility(false);

		View speedometerView = mapActivity.findViewById(R.id.speedometer_widget);
		speedometerWidget = new SpeedometerWidget(app, mapActivity, speedometerView, ThemeUsageContext.MAP);
		speedometerWidget.setVisibility(false);

		setupRulerWidget(rulerWidget);
		widgetRegistry.registerAllControls(mapActivity);
	}

	public void recreateControls() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			resetCachedTheme();
			clearCustomContainers(mapActivity);
			refreshWidgetsAndAppearance();
		}
	}

	public void refreshWidgetAppearance() {
		if (getMapActivity() == null || leftWidgetsPanel == null || rightWidgetsPanel == null
				|| topWidgetsPanel == null || bottomWidgetsPanel == null) {
			return;
		}
		if (appearanceRefreshScheduled && mapHudLayout != null) {
			mapHudLayout.removeCallbacks(appearanceRefreshRunnable);
			appearanceRefreshScheduled = false;
		}
		updateColorShadowsOfText();
	}

	public void refreshWidgetPanels() {
		if (getMapActivity() == null) {
			return;
		}
		refreshWidgetAppearance();
		updateMainWidgetPanels();
	}

	private void refreshWidgetsAndAppearance() {
		refreshWidgetAppearance();
		updateWidgetsInfo(drawSettings);
		updateMainWidgetPanels();
		if (sideWidgetsPanels != null) {
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.update(drawSettings);
			}
		}
		if (additionalWidgets != null) {
			for (WidgetsContainer container : additionalWidgets) {
				container.update(drawSettings);
			}
		}
	}

	private void updateMainWidgetPanels() {
		updatePanelVisibilityPolicy();
		leftWidgetsPanel.update(drawSettings);
		rightWidgetsPanel.update(drawSettings);
		topWidgetsPanel.update(drawSettings);
		bottomWidgetsPanel.update(drawSettings);
	}

	private void updatePanelVisibilityPolicy() {
		MapActivity activity = getMapActivity();
		WidgetsPanel previewPanel = activity != null
				? activity.getWidgetsVisibilityHelper().getAppearancePreviewPanel()
				: null;
		if (leftWidgetsPanel != null) {
			leftWidgetsPanel.setVisibilityAllowed(previewPanel == null || previewPanel == WidgetsPanel.LEFT);
		}
		if (rightWidgetsPanel != null) {
			rightWidgetsPanel.setVisibilityAllowed(previewPanel == null || previewPanel == WidgetsPanel.RIGHT);
		}
		if (topWidgetsPanel != null) {
			topWidgetsPanel.setVisibilityAllowed(previewPanel == null || previewPanel == WidgetsPanel.TOP);
		}
		if (bottomWidgetsPanel != null) {
			bottomWidgetsPanel.setVisibilityAllowed(previewPanel == null || previewPanel == WidgetsPanel.BOTTOM);
		}
	}

	private void scheduleWidgetAppearanceRefresh() {
		app.runInUIThread(() -> {
			MapHudLayout hudLayout = mapHudLayout;
			if (hudLayout != null && !appearanceRefreshScheduled) {
				appearanceRefreshScheduled = true;
				if (!hudLayout.post(appearanceRefreshRunnable)) {
					appearanceRefreshScheduled = false;
				}
			}
		});
	}

	public void updateVerticalPanels() {
		updateWidgetsInfo(drawSettings);
		updatePanelVisibilityPolicy();

		if (topWidgetsPanel != null) {
			topWidgetsPanel.update(drawSettings);
		}
		if (bottomWidgetsPanel != null) {
			bottomWidgetsPanel.update(drawSettings);
		}
	}

	private void clearCustomContainers(@NonNull MapActivity activity) {
		ViewGroup container = activity.findViewById(R.id.lanes_widget_special_position);
		if (container != null) {
			container.removeAllViews();
		}
	}

	public void updateRow(@NonNull MapWidget widget) {
		if (AndroidUtils.isActivityNotDestroyed(getMapActivity())) {
			topWidgetsPanel.updateRow(widget);
			bottomWidgetsPanel.updateRow(widget);
		}
	}

	@Nullable
	public RulerWidget setupRulerWidget(@NonNull RulerWidget widget) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ResolvedPanelAppearance appearance = calculateRulerAppearance();
			boolean nightMode = resolveMapNightMode();
			widget.updateTextSize(nightMode, appearance.getPrimaryTextColor(),
					appearance.getTextShadowColor(), (int) (2 * view.getDensity()));

			rulerWidgets = CollectionUtils.addToList(rulerWidgets, widget);

			return widget;
		} else {
			return null;
		}
	}

	public void removeRulerWidgets(@NonNull List<RulerWidget> rulers) {
		if (rulerWidgets != null) {
			rulerWidgets = CollectionUtils.removeAllFromList(rulerWidgets, rulers);
		}
	}

	public void addSideWidgetsPanel(@NonNull SideWidgetsPanel panel) {
		if (sideWidgetsPanels != null) {
			sideWidgetsPanels = CollectionUtils.addToList(sideWidgetsPanels, panel);
			panel.applyPanelAppearance(calculatePanelAppearance(WidgetsPanel.RIGHT));
		}
	}

	public void removeSideWidgetsPanel(@NonNull SideWidgetsPanel panel) {
		if (sideWidgetsPanels != null) {
			sideWidgetsPanels = CollectionUtils.removeFromList(sideWidgetsPanels, panel);
		}
	}

	public void addAdditionalWidgetsContainer(@NonNull WidgetsContainer container) {
		if (additionalWidgets != null) {
			additionalWidgets = CollectionUtils.addToList(additionalWidgets, container);
			container.applyPanelAppearance(calculatePanelAppearance(WidgetsPanel.TOP));
		}
	}

	public void removeAdditionalWidgetsContainer(@NonNull WidgetsContainer container) {
		if (additionalWidgets != null) {
			additionalWidgets = CollectionUtils.removeFromList(additionalWidgets, container);
		}
	}

	public void setTrackChartPoints(TrackChartPoints trackChartPoints) {
		routeLayer.setTrackChartPoints(trackChartPoints);
	}

	public void updateColorShadowsOfText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(mapActivity);
		boolean nightMode = resolveMapNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		long appearanceRevision = appearanceSettingsManager.getRevision();
		long committedAppearanceRevision = appearanceSettingsManager.getCommittedRevision();
		boolean contextChanged = appliedLayoutMode != layoutMode
				|| appliedNightMode != nightMode
				|| appliedFollowingMode != following;
		boolean updateAppearance = !colorsInitialized
				|| appliedAppearanceRevision != appearanceRevision
				|| contextChanged;
		boolean updateSystemBars = !systemBarsInitialized
				|| appliedCommittedAppearanceRevision != committedAppearanceRevision
				|| contextChanged;
		if (updateAppearance) {
			colorsInitialized = true;
			appliedAppearanceRevision = appearanceRevision;
			appliedLayoutMode = layoutMode;
			appliedNightMode = nightMode;
			appliedFollowingMode = following;
			ResolvedPanelAppearance leftAppearance = calculatePanelAppearance(WidgetsPanel.LEFT, layoutMode, nightMode, following);
			ResolvedPanelAppearance rightAppearance = calculatePanelAppearance(WidgetsPanel.RIGHT, layoutMode, nightMode, following);
			ResolvedPanelAppearance topAppearance = calculatePanelAppearance(WidgetsPanel.TOP, layoutMode, nightMode, following);
			ResolvedPanelAppearance bottomAppearance = calculatePanelAppearance(WidgetsPanel.BOTTOM, layoutMode, nightMode, following);
			for (MapWidgetInfo widgetInfo : widgetRegistry.getSideWidgets()) {
				boolean right = widgetInfo.getWidgetPanel() == WidgetsPanel.RIGHT;
				widgetInfo.widget.applyPanelAppearance(right ? rightAppearance : leftAppearance);
			}
			for (MapWidgetInfo widgetInfo : widgetRegistry.getVerticalWidgets()) {
				boolean bottom = widgetInfo.getWidgetPanel() == WidgetsPanel.BOTTOM;
				widgetInfo.widget.applyPanelAppearance(bottom ? bottomAppearance : topAppearance);
			}
			updateTopToolbar(nightMode);
			leftWidgetsPanel.applyPanelAppearance(leftAppearance);
			rightWidgetsPanel.applyPanelAppearance(rightAppearance);

			topWidgetsPanel.applyPanelAppearance(topAppearance);
			bottomWidgetsPanel.applyPanelAppearance(bottomAppearance);

			ResolvedPanelAppearance rulerAppearance = calculateRulerAppearance(layoutMode, nightMode, following);
			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, rulerAppearance.getPrimaryTextColor(),
						rulerAppearance.getTextShadowColor(), (int) (2 * view.getDensity()));
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.applyPanelAppearance(rightAppearance);
			}
			for (WidgetsContainer container : additionalWidgets) {
				container.applyPanelAppearance(topAppearance);
			}
			androidAutoMapPlaceholderView.updateNightMode(nightMode);
		}
		if (updateSystemBars) {
			systemBarsInitialized = true;
			appliedCommittedAppearanceRevision = committedAppearanceRevision;
			mapActivity.updateStatusBarColor();
		}
	}

	private boolean resolveMapNightMode() {
		return drawSettings != null
				? drawSettings.isNightMode()
				: app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
	}

	public void updateTopToolbar() {
		updateTopToolbar(topToolbarView.isNightMode());
	}

	private void updateTopToolbar(boolean nightMode) {
		topToolbarView.updateColors(nightMode);
	}

	@NonNull
	private ResolvedPanelAppearance calculatePanelAppearance(@NonNull WidgetsPanel panel) {
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(requireMapActivity());
		boolean nightMode = resolveMapNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		return calculatePanelAppearance(panel, layoutMode, nightMode, following);
	}

	@NonNull
	private ResolvedPanelAppearance calculatePanelAppearance(@NonNull WidgetsPanel panel,
	                                                        @Nullable ScreenLayoutMode layoutMode,
	                                                        boolean nightMode, boolean following) {
		return appearanceSettingsManager.resolve(panel, layoutMode, nightMode, following,
				view.getDensity(), true);
	}

	@NonNull
	private ResolvedPanelAppearance calculateRulerAppearance() {
		ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(requireMapActivity());
		boolean nightMode = resolveMapNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		return calculateRulerAppearance(layoutMode, nightMode, following);
	}

	@NonNull
	private ResolvedPanelAppearance calculateRulerAppearance(@Nullable ScreenLayoutMode layoutMode,
	                                                        boolean nightMode, boolean following) {
		return appearanceSettingsManager.resolve(WidgetsPanel.LEFT, layoutMode, nightMode, following,
				view.getDensity(), false);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		if (getMapActivity() != null) {
			updateColorShadowsOfText();
			updateWidgetsInfo(drawSettings);

			updateMainWidgetPanels();
			topToolbarView.updateInfo();
			alarmWidget.updateInfo(drawSettings, false);
			speedometerWidget.updateInfo(drawSettings);

			for (RulerWidget widget : rulerWidgets) {
				widget.updateInfo(tileBox);
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.update(drawSettings);
			}
			for (WidgetsContainer container : additionalWidgets) {
				container.update(drawSettings);
			}
		}
	}

	private void updateWidgetsInfo(@Nullable DrawSettings drawSettings) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			ApplicationMode appMode = settings.getApplicationMode();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);
			List<String> widgetsVisibility = MapWidgetInfo.getWidgetsVisibility(app, appMode, layoutMode);

			for (MapWidgetInfo widgetInfo : widgetRegistry.getWidgets(activity, appMode, layoutMode)) {
				boolean enabled = widgetInfo.isEnabledForAppMode(appMode, widgetsVisibility);
				boolean forceUpdate = (widgetInfo instanceof CenterWidgetInfo) && widgetInfo.widget.isAttached();
				if (enabled || forceUpdate) {
					widgetInfo.widget.updateInfo(drawSettings);
				}
			}
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@NonNull
	@Override
	public List<Rect> getCoveredScreenRects() {
		List<Rect> rects = new ArrayList<>();
		rects.add(AndroidUtils.getViewBoundOnScreen(topWidgetsPanel));
		rects.add(AndroidUtils.getViewBoundOnScreen(bottomWidgetsPanel));
		return rects;
	}
}