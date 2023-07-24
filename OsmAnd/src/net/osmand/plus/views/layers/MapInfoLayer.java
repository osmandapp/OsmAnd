package net.osmand.plus.views.layers;


import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.AndroidAutoMapPlaceholderView;
import net.osmand.plus.charts.TrackChartPoints;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.controls.SideWidgetsPanel;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.TopToolbarView;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.AlarmWidget;
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class MapInfoLayer extends OsmandMapLayer {

	private final RouteLayer routeLayer;
	private final OsmandSettings settings;
	private final MapWidgetRegistry widgetRegistry;

	private ViewGroup topWidgetsContainer;
	private SideWidgetsPanel leftWidgetsPanel;
	private SideWidgetsPanel rightWidgetsPanel;
	private ViewGroup bottomWidgetsContainer;

	private View mapRulerLayout;
	private AlarmWidget alarmControl;
	private List<RulerWidget> rulerWidgets;
	private List<SideWidgetsPanel> sideWidgetsPanels;

	private AndroidAutoMapPlaceholderView androidAutoMapPlaceholderView;

	private DrawSettings drawSettings;
	private int themeId = -1;

	private TopToolbarView topToolbarView;

	public MapInfoLayer(@NonNull Context context, @NonNull RouteLayer layer) {
		super(context);
		this.routeLayer = layer;

		OsmandApplication app = getApplication();
		settings = app.getSettings();
		MapLayers mapLayers = app.getOsmandMap().getMapLayers();
		widgetRegistry = mapLayers.getMapWidgetRegistry();
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			topWidgetsContainer = mapActivity.findViewById(R.id.top_widgets_panel);
			leftWidgetsPanel = mapActivity.findViewById(R.id.map_left_widgets_panel);
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			bottomWidgetsContainer = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			mapRulerLayout = mapActivity.findViewById(R.id.map_ruler_layout);
			androidAutoMapPlaceholderView = mapActivity.findViewById(R.id.AndroidAutoPlaceholder);

			registerAllControls(mapActivity);
			recreateControls();
		} else {
			resetCashedTheme();
			widgetRegistry.clearWidgets();

			topWidgetsContainer = null;
			leftWidgetsPanel = null;
			rightWidgetsPanel = null;
			mapRulerLayout = null;
			androidAutoMapPlaceholderView = null;

			drawSettings = null;
			alarmControl = null;
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

		alarmControl = new AlarmWidget(mapActivity.getMyApplication(), mapActivity);
		alarmControl.setVisibility(false);

		setupRulerWidget(mapRulerLayout);
		widgetRegistry.registerAllControls(mapActivity);
	}

	public void recreateControls() {
		if (getMapActivity() != null) {
			resetCashedTheme();
			ApplicationMode appMode = settings.getApplicationMode();
			widgetRegistry.updateWidgetsInfo(appMode, drawSettings);
			recreateWidgetsPanel(topWidgetsContainer, WidgetsPanel.TOP, appMode);
			recreateWidgetsPanel(bottomWidgetsContainer, WidgetsPanel.BOTTOM, appMode);
			leftWidgetsPanel.update(drawSettings);
			rightWidgetsPanel.update(drawSettings);
		}
	}

	public void recreateTopWidgetsPanel() {
		ApplicationMode appMode = settings.getApplicationMode();
		widgetRegistry.updateWidgetsInfo(appMode, drawSettings);
		recreateWidgetsPanel(topWidgetsContainer, WidgetsPanel.TOP, appMode);
	}

	private void recreateWidgetsPanel(@Nullable ViewGroup container, @NonNull WidgetsPanel panel, @NonNull ApplicationMode appMode) {
		if (container != null) {
			container.removeAllViews();
			widgetRegistry.populateControlsContainer(container, appMode, panel);
			container.requestLayout();
		}
	}

	public RulerWidget setupRulerWidget(@NonNull View mapRulerView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RulerWidget rulerWidget = new RulerWidget(mapActivity.getMyApplication(), mapRulerView);
			rulerWidget.setVisibility(false);

			TextState ts = calculateTextState();
			boolean nightMode = drawSettings != null && drawSettings.isNightMode();
			rulerWidget.updateTextSize(nightMode, ts.textColor, ts.textShadowColor, (int) (2 * view.getDensity()));

			rulerWidgets = Algorithms.addToList(rulerWidgets, rulerWidget);

			return rulerWidget;
		} else {
			return null;
		}
	}

	public void removeRulerWidgets(@NonNull List<RulerWidget> rulers) {
		if (rulerWidgets != null) {
			rulerWidgets = Algorithms.removeAllFromList(rulerWidgets, rulers);
		}
	}

	public void addSideWidgetsPanel(@NonNull SideWidgetsPanel panel) {
		if (sideWidgetsPanels != null) {
			sideWidgetsPanels = Algorithms.addToList(sideWidgetsPanels, panel);
			panel.updateColors(calculateTextState());
		}
	}

	public void removeSideWidgetsPanel(@NonNull SideWidgetsPanel panel) {
		if (sideWidgetsPanels != null) {
			sideWidgetsPanels = Algorithms.removeFromList(sideWidgetsPanels, panel);
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
			TextState state = calculateTextState();
			for (MapWidgetInfo widgetInfo : widgetRegistry.getAllWidgets()) {
				widgetInfo.widget.updateColors(state);
			}
			updateTopToolbar(nightMode);
			leftWidgetsPanel.updateColors(state);
			rightWidgetsPanel.updateColors(state);

			topWidgetsContainer.invalidate();
			bottomWidgetsContainer.invalidate();

			for (RulerWidget rulerWidget : rulerWidgets) {
				rulerWidget.updateTextSize(nightMode, state.textColor, state.textShadowColor, (int) (2 * view.getDensity()));
			}
			for (SideWidgetsPanel panel : sideWidgetsPanels) {
				panel.updateColors(state);
			}
			androidAutoMapPlaceholderView.updateNightMode(nightMode);
		}
	}

	private void updateTopToolbar(boolean nightMode) {
		topToolbarView.updateColors(nightMode);
	}

	@NonNull
	private TextState calculateTextState() {
		boolean transparent = view.getSettings().TRANSPARENT_MAP_THEME.get();
		boolean nightMode = drawSettings != null && drawSettings.isNightMode();
		boolean following = routeLayer.getHelper().isFollowingMode();
		TextState ts = new TextState();
		ts.textBold = following;
		ts.night = nightMode;
		ts.textColor = nightMode ? ContextCompat.getColor(getContext(), R.color.widgettext_night) :
				ContextCompat.getColor(getContext(), R.color.widgettext_day);
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
			ts.widgetBackgroundId = R.drawable.bs_side_widget_night;
			ts.boxFree = R.drawable.btn_round_night;
			ts.widgetDividerColorId = R.color.divider_color_dark;
			ts.panelBorderColorId = R.color.icon_color_secondary_dark;
		} else {
			ts.boxTop = R.drawable.btn_flat;
			ts.widgetBackgroundId = R.drawable.bg_side_widget_day;
			ts.boxFree = R.drawable.btn_round;
			ts.widgetDividerColorId = R.color.divider_color_light;
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
			alarmControl.updateInfo(drawSettings, false);

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
}