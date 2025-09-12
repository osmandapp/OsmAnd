package net.osmand.plus.views.layers;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.MapHudLayout;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;
	private final MapTransparencyHelper mapTransparencyHelper;

	private View mapHudContainer;
	private MapHudLayout mapHudLayout;
	private List<MapButton> mapButtons = new ArrayList<>();
	private List<MapButton> customMapButtons = new ArrayList<>();

	private MapRouteInfoMenu mapRouteInfoMenu;
	private long touchEvent;
	private boolean touchPrevFolowingMode;
	private final Set<String> themeInfoProviderTags = new HashSet<>();
	private WidgetsVisibilityHelper visibilityHelper;

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public MapControlsLayer(@NonNull Context context) {
		super(context);
		app = getApplication();
		settings = app.getSettings();
		mapView = app.getOsmandMap().getMapView();

		mapTransparencyHelper = new MapTransparencyHelper(this);
	}

	@NonNull
	public MapTransparencyHelper getMapTransparencyHelper() {
		return mapTransparencyHelper;
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
			visibilityHelper = mapActivity.getWidgetsVisibilityHelper();
			initMapButtons();
			mapTransparencyHelper.initTransparencyBar();
			updateControls(null);
		} else {
			mapButtons = new ArrayList<>();
			customMapButtons = new ArrayList<>();
			mapTransparencyHelper.destroyTransparencyBar();
			mapRouteInfoMenu = null;
			mapHudLayout = null;
			mapHudContainer = null;
		}
	}

	public void setControlsClickable(boolean clickable) {
		for (MapButton mapButton : getAllMapButtons()) {
			mapButton.setClickable(clickable);
		}
	}


	@Nullable
	public MapHudLayout getMapHudLayout() {
		return mapHudLayout;
	}

	public boolean switchToRoutePlanningLayout() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (!routingHelper.isRoutePlanningMode() && routingHelper.isFollowingMode()) {
			routingHelper.setRoutePlanningMode(true);
			app.getMapViewTrackingUtilities().switchRoutePlanningMode();
			mapView.refreshMap();
			return true;
		}
		return false;
	}

	public void initMapButtons() {
		MapActivity activity = requireMapActivity();
		mapHudContainer = activity.findViewById(R.id.map_hud_container);
		mapHudLayout = mapHudContainer.findViewById(R.id.map_hud_layout);

		for (MapButton button : mapButtons) {
			mapHudLayout.removeMapButton(button);
		}

		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);

		addMapButton(createMapButton(inflater, R.layout.configure_map_button));
		addMapButton(createMapButton(inflater, R.layout.map_search_button));
		addMapButton(createMapButton(inflater, R.layout.map_compass_button));

		addMapButton(createMapButton(inflater, R.layout.map_zoom_out_button));
		addMapButton(createMapButton(inflater, R.layout.map_zoom_in_button));
		addMapButton(createMapButton(inflater, R.layout.my_location_button));

		addMapButton(createMapButton(inflater, R.layout.drawer_menu_button));
		addMapButton(createMapButton(inflater, R.layout.navigation_menu_button));

		MapButton button = createMapButton(inflater, R.layout.map_3d_button);
		button.setUseCustomPosition(true);
		addMapButton(button);

		setInvalidated(true);
	}

	@NonNull
	private MapButton createMapButton(@NonNull LayoutInflater inflater, @LayoutRes int layoutId) {
		MapButton button = (MapButton) inflater.inflate(layoutId, mapHudLayout, false);
		button.setMapActivity(requireMapActivity());
		return button;
	}

	private void addMapButton(@NonNull MapButton mapButton) {
		mapButtons.add(mapButton);
		mapHudLayout.addMapButton(mapButton);
	}

	public void addCustomMapButton(@NonNull MapButton mapButton, boolean alwaysVisible, boolean longClickable, boolean useCustomPosition, boolean useDefaultAppearance) {
		customMapButtons.add(mapButton);
		mapButton.setAlwaysVisible(alwaysVisible);
		mapButton.setLongClickable(longClickable);
		mapButton.setUseCustomPosition(useCustomPosition);
		mapButton.setUseDefaultAppearance(useDefaultAppearance);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapButton.setMapActivity(mapActivity);
		} else {
			mapButton.updatePositions();
		}
	}

	public void addCustomMapButton(@NonNull MapButton mapButton) {
		addCustomMapButton(mapButton, true, false, false, true);
	}

	public void addCustomizedDefaultMapButtons(@NonNull List<MapButton> mapButtons) {
		for (MapButton mapButton : mapButtons) {
			addCustomizedDefaultMapButton(mapButton);
		}
	}

	public void addCustomizedDefaultMapButton(@NonNull MapButton mapButton) {
		addCustomMapButton(mapButton, true, false, false, false);
	}

	@NonNull
	private List<MapButton> getAllMapButtons() {
		List<MapButton> buttons = new ArrayList<>(mapButtons);
		buttons.addAll(customMapButtons);
		return buttons;
	}

	public void removeCustomMapButtons(@NonNull List<MapButton> mapButtons) {
		customMapButtons = CollectionUtils.removeAllFromList(customMapButtons, mapButtons);
	}

	public void showMapControlsIfHidden() {
		MapActivity activity = getMapActivity();
		if (!isMapControlsVisible() && activity != null) {
			showMapControls();
		}
	}

	private void showMapControls() {
		updateMapControls(true);

		MapActivity activity = getMapActivity();
		if (activity != null) {
			AndroidUtils.showNavBar(activity);
		}
	}

	public void hideMapControls() {
		updateMapControls(false);
	}

	private void updateMapControls(boolean show) {
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			AndroidUiHelper.setVisibility(show ? VISIBLE : INVISIBLE, mapHudContainer);
		} else if (mapHudLayout != null) {
			animateMapControls(show);
		}
	}

	private void animateMapControls(boolean show) {
		View mapHudButtonsTop = mapHudLayout.findViewById(R.id.MapHudButtonsOverlayTop);
		View mapHudButtonsBottom = mapHudLayout.findViewById(R.id.MapHudButtonsOverlayBottom);

		float transTopInitial = show ? -mapHudButtonsTop.getHeight() : 0;
		float transBottomInitial = show ? mapHudButtonsBottom.getHeight() : 0;
		float alphaInitial = show ? 0f : 1f;

		float transTopFinal = show ? 0 : -mapHudButtonsTop.getHeight();
		float transBottomFinal = show ? 0 : mapHudButtonsBottom.getHeight();
		float alphaFinal = show ? 1f : 0f;

		AnimatorSet set = new AnimatorSet();
		set.setDuration(300).playTogether(
				ObjectAnimator.ofFloat(mapHudContainer, View.ALPHA, alphaInitial, alphaFinal),
				ObjectAnimator.ofFloat(mapHudButtonsTop, View.TRANSLATION_Y, transTopInitial, transTopFinal),
				ObjectAnimator.ofFloat(mapHudButtonsBottom, View.TRANSLATION_Y, transBottomInitial, transBottomFinal)
		);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
				if (show) {
					mapHudContainer.setVisibility(VISIBLE);
				}
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (!show) {
					mapHudContainer.setVisibility(INVISIBLE);
					mapHudButtonsTop.setTranslationY(transTopInitial);
					mapHudButtonsBottom.setTranslationY(transBottomInitial);
					mapHudContainer.setAlpha(alphaInitial);
				}
				mapHudLayout.updateButtons();

				MapActivity activity = getMapActivity();
				if (activity != null) {
					activity.updateStatusBarColor();
				}
			}
		});
		set.start();
	}

	public boolean isMapControlsVisible() {
		return mapHudContainer != null && mapHudContainer.getVisibility() == VISIBLE;
	}

	public void switchMapControlsVisibility(boolean switchNavBarVisibility) {
		MapActivity mapActivity = requireMapActivity();
		if (app.getRoutingHelper().isFollowingMode() || app.getRoutingHelper().isPauseNavigation()
				|| mapActivity.getFragmentsHelper().getMeasurementToolFragment() != null
				|| mapActivity.getFragmentsHelper().getPlanRouteFragment() != null
				|| mapActivity.getMapLayers().getDistanceRulerControlLayer().rulerModeOn()) {
			return;
		}
		if (isMapControlsVisible()) {
			hideMapControls();
			if (switchNavBarVisibility) {
				AndroidUtils.hideNavBar(mapActivity);
			}
		} else {
			showMapControls();
		}
		mapActivity.updateStatusBarColor();
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		destroyButtons();
	}

	private void destroyButtons() {
		mapButtons.clear();
		customMapButtons.clear();
	}

	public void refreshButtons() {
		for (MapButton button : getAllMapButtons()) {
			button.update();
		}
		if(mapHudLayout != null) {
			mapHudLayout.updateButtons();
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		updateControls(nightMode);

		if (invalidated) {
			setInvalidated(false);
			app.runInUIThread(() -> {
				if (getMapActivity() != null) {
					refreshButtons();
				}
			});
		}
	}

	private void updateControls(@Nullable DrawSettings drawSettings) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean nightMode = isNightModeForMapControls(drawSettings);

		boolean isRoutePlanningMode = isInRoutePlanningMode();
		boolean isRouteFollowingMode = !isRoutePlanningMode && app.getRoutingHelper().isFollowingMode();
		if (!touchPrevFolowingMode && isRouteFollowingMode) {
			touchEvent = 0;
		}
		boolean isTimeToShowButtons = System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS;

		boolean shouldShowRouteCalculationControls = isRoutePlanningMode || ((app.accessibilityEnabled() || isTimeToShowButtons) && isRouteFollowingMode);
		boolean isRouteDialogOpened = mapRouteInfoMenu.isVisible() || (shouldShowRouteCalculationControls && mapRouteInfoMenu.needShowMenu());
		touchPrevFolowingMode = isRouteFollowingMode;
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		boolean androidAutoAttached = carNavigationSession != null && carNavigationSession.hasStarted();
		boolean showBottomMenuButtons = visibilityHelper.shouldShowBottomMenuButtons()
				&& (shouldShowRouteCalculationControls || !isRouteFollowingMode || androidAutoAttached);

		mapTransparencyHelper.updateTransparencySliderUi();

		mapRouteInfoMenu.setVisible(shouldShowRouteCalculationControls);

		for (MapButton mapButton : getAllMapButtons()) {
			mapButton.setNightMode(nightMode);
			mapButton.setRouteDialogOpened(isRouteDialogOpened);
			mapButton.setShowBottomButtons(showBottomMenuButtons);
			mapButton.update();
		}
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		touchEvent = System.currentTimeMillis();

		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode()) {
			mapView.refreshMap();
		}
		return false;
	}

	private boolean isInRoutePlanningMode() {
		RoutingHelper routingHelper = app.getRoutingHelper();
		return routingHelper.isRoutePlanningMode()
				|| ((routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated())
				&& !routingHelper.isFollowingMode());
	}

	public void addThemeInfoProviderTag(@NonNull String tag) {
		themeInfoProviderTags.add(tag);
	}

	public void removeThemeInfoProviderTag(@NonNull String tag) {
		themeInfoProviderTags.remove(tag);
	}

	private boolean isNightModeForMapControls(@Nullable DrawSettings drawSettings) {
		MapControlsThemeProvider provider = getThemeProvider();
		if (provider != null) {
			return provider.isNightModeForMapControls();
		}
		return drawSettings != null && drawSettings.isNightMode();
	}

	@Nullable
	private MapControlsThemeProvider getThemeProvider() {
		MapActivity mapActivity = requireMapActivity();
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		for (String tag : themeInfoProviderTags) {
			Fragment fragment = manager.findFragmentByTag(tag);
			if (fragment instanceof MapControlsThemeProvider) {
				return (MapControlsThemeProvider) fragment;
			}
		}
		return null;
	}

	public interface MapControlsThemeProvider {
		boolean isNightModeForMapControls();
	}
}
