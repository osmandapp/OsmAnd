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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
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
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
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
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.TopToolbarView;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.SpeedometerWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MapInfoLayer extends OsmandMapLayer implements ICoveredScreenRectProvider {

	private final RouteLayer routeLayer;
	private final OsmandApplication app;
	private final OsmandSettings settings;
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
	private int themeId = -1;

	private TopToolbarView topToolbarView;

	private final BoundsChangeListener topPanelBoundsChangeListener;
	private final BoundsChangeListener bottomPanelBoundsChangeListener;
	private VerticalPanelVisibilityListener bottomWidgetsVisibilityListener;
	private WindowInsetsCompat lastWindowInsets;

	private boolean isContentVisible = false;

	public MapInfoLayer(@NonNull Context context, @NonNull RouteLayer layer) {
		super(context);
		this.routeLayer = layer;

		app = getApplication();
		settings = app.getSettings();
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

			registerInsetListeners();

			LayoutInflater inflater = mapActivity.getLayoutInflater();
			rulerWidget = (RulerWidget) inflater.inflate(R.layout.map_ruler, mapHudLayout, false);
			mapHudLayout.addWidget(rulerWidget);

			registerAllControls(mapActivity);
			recreateControls();

			mapDisplayPositionManager.registerCoveredScreenRectProvider(this);
			topWidgetsPanel.addOnLayoutChangeListener(topPanelBoundsChangeListener);
			bottomWidgetsPanel.addOnLayoutChangeListener(bottomPanelBoundsChangeListener);
			mapDisplayPositionManager.updateMapDisplayPosition(true);
		} else {
			if (topWidgetsPanel != null) {
				topWidgetsPanel.removeOnLayoutChangeListener(topPanelBoundsChangeListener);
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

			resetCashedTheme();
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
		}
	}

	private void onBottomWidgetPanelChanged(boolean isVisible) {
		if (bottomFragmentContainer != null) {
			boolean bottomFragmentVisible = bottomFragmentContainer.getChildCount() > 0;
			if (bottomFragmentVisible) {
				isVisible = true;
			}
		}
		updateLayerInsets(isVisible, false);
	}

	private void updateLayerInsets(boolean isVisible, boolean forceUpdate) {
		if (!forceUpdate && (!InsetsUtils.isEdgeToEdgeSupported() || isContentVisible == isVisible)) {
			return;
		}
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || lastWindowInsets == null) {
			return;
		}
		InsetTargetsCollection targetsCollection = new InsetTargetsCollection();
		InsetTargetBuilder mapHudLayoutBuilder = InsetTarget.createCustomBuilder(mapHudLayout).applyPadding(true);

		if (isVisible) {
			targetsCollection.add(InsetTarget.createCustomBuilder(bottomWidgetsPanel)
					.portraitSides(InsetSide.BOTTOM)
					.applyPadding(true).build());

			mapHudLayoutBuilder.portraitSides(InsetSide.TOP, InsetSide.RESET)
					.landscapeSides(InsetSide.TOP, InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT);
		} else {
			mapHudLayoutBuilder.portraitSides(InsetSide.TOP, InsetSide.BOTTOM, InsetSide.RESET)
					.landscapeSides(InsetSide.TOP, InsetSide.BOTTOM, InsetSide.LEFT, InsetSide.RIGHT);
		}
		targetsCollection.add(mapHudLayoutBuilder);
		InsetsUtils.processInsets(mapActivity.findViewById(R.id.map_hud_container), targetsCollection, lastWindowInsets);
		isContentVisible = isVisible;
	}

	@Override
	public void setWindowInsets(@NonNull WindowInsetsCompat windowInsets) {
		super.setWindowInsets(windowInsets);
		this.lastWindowInsets = windowInsets;
		Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars());
		if (leftWidgetsPanel != null) {
			leftWidgetsPanel.setInsets(insets);
		}
		if (rightWidgetsPanel != null) {
			rightWidgetsPanel.setInsets(insets);
		}
		if (bottomWidgetsPanel != null && mapHudLayout != null) {
			updateLayerInsets(bottomWidgetsPanel.isAnyRowVisible(), true);
			mapHudLayout.setWindowInsets(windowInsets);
		}
	}

	private void registerInsetListeners() {
		if (!InsetsUtils.isEdgeToEdgeSupported()) {
			return;
		}
		bottomFragmentContainer.setOnChildChanged(hasChild -> {
			if (bottomFragmentContainer != null) {
				updateLayerInsets(hasChild, true);
			}
			return Unit.INSTANCE;
		});
		bottomWidgetsVisibilityListener = this::onBottomWidgetPanelChanged;
		bottomWidgetsPanel.addVisibilityListener(bottomWidgetsVisibilityListener);
	}

	@Nullable
	public TopToolbarView getTopToolbarView() {
		return topToolbarView;
	}

	private void resetCashedTheme() {
		themeId = -1;
	}

	public void removeSideWidget(TextInfoWidget widget) {
		widgetRegistry.removeSideWidgetInternal(widget);
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
		return topToolbarView != null && topToolbarView.getTopController() != null;
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
		widgetRegistry.reorderWidgets();
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
		speedometerWidget = new SpeedometerWidget(app, mapActivity, speedometerView);
		speedometerWidget.setVisibility(false);

		setupRulerWidget(rulerWidget);
		widgetRegistry.registerAllControls(mapActivity);
	}

	public void recreateControls() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			resetCashedTheme();
			ApplicationMode appMode = settings.getApplicationMode();
			clearCustomContainers(mapActivity);
			widgetRegistry.updateWidgetsInfo(appMode, drawSettings);
			topWidgetsPanel.update(drawSettings);
			bottomWidgetsPanel.update(drawSettings);
			leftWidgetsPanel.update(drawSettings);
			rightWidgetsPanel.update(drawSettings);
		}
	}

	public void updateVerticalPanels() {
		ApplicationMode appMode = settings.getApplicationMode();
		widgetRegistry.updateWidgetsInfo(appMode, drawSettings);

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
			widget.setVisibility(false);

			TextState state = calculateTextState(false);
			boolean nightMode = drawSettings != null && drawSettings.isNightMode();
			widget.updateTextSize(nightMode, state.textColor, state.textShadowColor, (int) (2 * view.getDensity()));

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
			panel.updateColors(calculateTextState(false));
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
			container.updateColors(calculateTextState(false));
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

	public static class TextState {
		public boolean textBold;
		public boolean night;
		public int textColor;
		public int textShadowColor;
		public int secondaryTextColor;
		public int boxTop;
		public int widgetBackgroundId;
		public int boxFree;
		public int textShadowRadius;
		public int widgetDividerColorId;
		public int panelBorderColorId;
	}

	public void updateColorShadowsOfText() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		int calcThemeId = (transparent ? 4 : 0) | (nightMode ? 2 : 0) | (following ? 1 : 0);
		if (themeId != calcThemeId) {
			themeId = calcThemeId;
			TextState verticalWidgetsState = calculateTextState(true);
			TextState sideWidgetsState = calculateTextState(false);
			for (MapWidgetInfo widgetInfo : widgetRegistry.getSideWidgets()) {
				widgetInfo.widget.updateColors(sideWidgetsState);
			}
			for (MapWidgetInfo widgetInfo : widgetRegistry.getVerticalWidgets()) {
				widgetInfo.widget.updateColors(verticalWidgetsState);
			}
			updateTopToolbar(nightMode);
			leftWidgetsPanel.updateColors(sideWidgetsState);
			rightWidgetsPanel.updateColors(sideWidgetsState);

			topWidgetsPanel.updateColors(verticalWidgetsState);
			bottomWidgetsPanel.updateColors(verticalWidgetsState);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, sideWidgetsState.textColor, sideWidgetsState.textShadowColor, (int) (2 * view.getDensity()));
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.updateColors(sideWidgetsState);
			}
			for (WidgetsContainer container : additionalWidgets) {
				container.updateColors(verticalWidgetsState);
			}
			androidAutoMapPlaceholderView.updateNightMode(nightMode);
		}
	}

	public void updateTopToolbar() {
		updateTopToolbar(topToolbarView.isNightMode());
	}

	private void updateTopToolbar(boolean nightMode) {
		topToolbarView.updateColors(nightMode);
	}

	@NonNull
	private TextState calculateTextState(boolean verticalWidget) {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.night = nightMode;
		if (verticalWidget) {
			ts.textColor = ColorUtilities.getPrimaryTextColor(getContext(), nightMode);
		} else {
			int textColorId = nightMode ? R.color.widgettext_night : R.color.widgettext_day;
			ts.textColor = ColorUtilities.getColor(getContext(), textColorId);
		}
		ts.secondaryTextColor = ColorUtilities.getSecondaryTextColor(getContext(), nightMode);

		// Night shadowColor always use widgettext_shadow_night, same as widget background color for non-transparent
		ts.textShadowColor = nightMode ? ContextCompat.getColor(getContext(), R.color.widgettext_shadow_night) :
				ContextCompat.getColor(getContext(), R.color.widgettext_shadow_day);
		if (!transparent && !nightMode) {
			ts.textShadowRadius = 0;
		} else {
			ts.textShadowRadius = (int) (4 * view.getDensity());
		}
		if (transparent) {
			ts.boxTop = R.drawable.btn_flat_transparent;
			ts.widgetBackgroundId = R.drawable.bg_side_widget_transparent;
			ts.boxFree = R.drawable.btn_round_transparent;
			ts.widgetDividerColorId = R.color.widget_divider_transparent;
			ts.panelBorderColorId = R.color.widget_panel_border_transparent;
		} else if (nightMode) {
			ts.boxTop = R.drawable.btn_flat_night;
			ts.widgetBackgroundId = verticalWidget ? R.drawable.bs_vertical_widget_night : R.drawable.bs_side_widget_night;
			ts.boxFree = R.drawable.btn_round_night;
			ts.widgetDividerColorId = R.color.divider_color_dark;
			ts.panelBorderColorId = R.color.icon_color_secondary_dark;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.widgetBackgroundId = R.drawable.bg_side_widget_day;
			ts.boxFree = R.drawable.btn_round;
			ts.widgetDividerColorId = verticalWidget ? R.color.widget_background_color_light : R.color.divider_color_light;
			ts.panelBorderColorId = R.color.stroked_buttons_and_links_outline_light;
		}
		return ts;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		this.drawSettings = drawSettings;
		if (getMapActivity() != null) {
			updateColorShadowsOfText();
			widgetRegistry.updateWidgetsInfo(settings.getApplicationMode(), drawSettings);
			leftWidgetsPanel.update(drawSettings);
			rightWidgetsPanel.update(drawSettings);
			topWidgetsPanel.update(drawSettings);
			bottomWidgetsPanel.update(drawSettings);
			topToolbarView.updateInfo();
			alarmWidget.updateInfo(drawSettings, false);
			speedometerWidget.updateInfo(drawSettings);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateInfo(tileBox);
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.update(drawSettings);
			}
			for (WidgetsContainer container : additionalWidgets) {
				container.update(drawSettings);
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