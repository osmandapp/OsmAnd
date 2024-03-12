package net.osmand.plus.views.layers;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

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
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.SideWidgetsPanel;
import net.osmand.plus.views.controls.VerticalWidgetPanel;
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

public class MapInfoLayer extends OsmandMapLayer implements ICoveredScreenRectProvider {

	private final RouteLayer routeLayer;
	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapWidgetRegistry widgetRegistry;
	private final MapDisplayPositionManager mapDisplayPositionManager;

	private SideWidgetsPanel leftWidgetsPanel;
	private SideWidgetsPanel rightWidgetsPanel;
	private VerticalWidgetPanel topWidgetsPanel;
	private VerticalWidgetPanel bottomWidgetsPanel;

	private View mapRulerLayout;
	private AlarmWidget alarmWidget;
	private SpeedometerWidget speedometerWidget;
	private List<RulerWidget> rulerWidgets;
	private List<SideWidgetsPanel> sideWidgetsPanels;

	private AndroidAutoMapPlaceholderView androidAutoMapPlaceholderView;

	private DrawSettings drawSettings;
	private int themeId = -1;

	private TopToolbarView topToolbarView;

	private final BoundsChangeListener topPanelBoundsChangeListener;
	private final BoundsChangeListener bottomPanelBoundsChangeListener;

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
			topWidgetsPanel = mapActivity.findViewById(R.id.top_widgets_panel);
			leftWidgetsPanel = mapActivity.findViewById(R.id.map_left_widgets_panel);
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			bottomWidgetsPanel = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			mapRulerLayout = mapActivity.findViewById(R.id.map_ruler_layout);
			androidAutoMapPlaceholderView = mapActivity.findViewById(R.id.AndroidAutoPlaceholder);

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
			}
			mapDisplayPositionManager.unregisterCoveredScreenRectProvider(this);
			mapDisplayPositionManager.updateMapDisplayPosition(true);

			resetCashedTheme();
			widgetRegistry.clearWidgets();

			topWidgetsPanel = null;
			bottomWidgetsPanel = null;
			leftWidgetsPanel = null;
			rightWidgetsPanel = null;
			mapRulerLayout = null;
			androidAutoMapPlaceholderView = null;

			drawSettings = null;
			alarmWidget = null;
			speedometerWidget = null;
			rulerWidgets = null;
			sideWidgetsPanels = null;
			topToolbarView = null;
		}
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

		topToolbarView = new TopToolbarView(mapActivity);
		updateTopToolbar(false);

		alarmWidget = new AlarmWidget(app, mapActivity);
		alarmWidget.setVisibility(false);

		View speedometerView = mapActivity.findViewById(R.id.speedometer_widget);
		speedometerWidget = new SpeedometerWidget(app, mapActivity, speedometerView);
		speedometerWidget.setVisibility(false);

		setupRulerWidget(mapRulerLayout);
		widgetRegistry.registerAllControls(mapActivity);
	}

	public void recreateControls() {
		if (getMapActivity() != null) {
			resetCashedTheme();
			ApplicationMode appMode = settings.getApplicationMode();
			widgetRegistry.updateWidgetsInfo(appMode, drawSettings);
			topWidgetsPanel.update(drawSettings);
			bottomWidgetsPanel.update(drawSettings);
			leftWidgetsPanel.update(drawSettings);
			rightWidgetsPanel.update(drawSettings);
		}
	}

	public void recreateTopWidgetsPanel() {
		ApplicationMode appMode = settings.getApplicationMode();
		widgetRegistry.updateWidgetsInfo(appMode, drawSettings);

		if (topWidgetsPanel != null) {
			topWidgetsPanel.update(drawSettings);
		}
		if (bottomWidgetsPanel != null) {
			bottomWidgetsPanel.update(drawSettings);
		}
	}

	public void updateRow(MapWidget widget) {
		if(getMapActivity() != null || !getMapActivity().isActivityDestroyed()) {
			topWidgetsPanel.updateRow(widget);
			bottomWidgetsPanel.updateRow(widget);
		}
	}

	@Nullable
	public RulerWidget setupRulerWidget(@NonNull View mapRulerView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RulerWidget widget = new RulerWidget(app, mapRulerView);
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

			topWidgetsPanel.updateColors(sideWidgetsState);
			bottomWidgetsPanel.updateColors(sideWidgetsState);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, sideWidgetsState.textColor, sideWidgetsState.textShadowColor, (int) (2 * view.getDensity()));
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.updateColors(sideWidgetsState);
			}
			androidAutoMapPlaceholderView.updateNightMode(nightMode);
		}
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
			ts.textColor = nightMode ? ContextCompat.getColor(getContext(), R.color.widgettext_night) :
					ContextCompat.getColor(getContext(), R.color.widgettext_day);
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
			ts.widgetBackgroundId = verticalWidget ? R.color.widget_background_color_dark : R.drawable.bs_side_widget_night;
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
			topToolbarView.updateInfo();
			alarmWidget.updateInfo(drawSettings, false);
			speedometerWidget.updateInfo(drawSettings);

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateInfo(tileBox);
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.update(drawSettings);
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