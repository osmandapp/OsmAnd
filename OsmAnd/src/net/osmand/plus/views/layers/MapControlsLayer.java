package net.osmand.plus.views.layers;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.BACK_TO_LOC_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_IN_HUD_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.ZOOM_OUT_HUD_ID;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.maphudbuttons.CompassButton;
import net.osmand.plus.views.controls.maphudbuttons.ConfigureMapButton;
import net.osmand.plus.views.controls.maphudbuttons.DrawerMenuButton;
import net.osmand.plus.views.controls.maphudbuttons.Map3DButton;
import net.osmand.plus.views.controls.maphudbuttons.MapButton;
import net.osmand.plus.views.controls.maphudbuttons.MyLocationButton;
import net.osmand.plus.views.controls.maphudbuttons.NavigationMenuButton;
import net.osmand.plus.views.controls.maphudbuttons.QuickSearchButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomInButton;
import net.osmand.plus.views.controls.maphudbuttons.ZoomOutButton;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MapControlsLayer extends OsmandMapLayer {

	private static final int TIMEOUT_TO_SHOW_BUTTONS = 7000;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapView;
	private final MapActionsHelper mapActionsHelper;
	private final MapTransparencyHelper mapTransparencyHelper;

	private List<MapButton> mapButtons = new ArrayList<>();
	private Map3DButton map3DButton;
	private CompassButton compassButton;

	private MapRouteInfoMenu mapRouteInfoMenu;
	private long touchEvent;
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
		mapActionsHelper = new MapActionsHelper(this);
		mapTransparencyHelper = new MapTransparencyHelper(this);
	}

	@NonNull
	public MapActionsHelper getMapActionsHelper() {
		return mapActionsHelper;
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
			initTopControls();
			initFabButtons(mapActivity);
			mapTransparencyHelper.initTransparencyBar();
			initZooms();
			initDashboardRelatedControls();
			updateControls(mapView.getCurrentRotatedTileBox(), null);
		} else {
			mapButtons = new ArrayList<>();
			mapTransparencyHelper.destroyTransparencyBar();
			mapRouteInfoMenu = null;
			compassButton = null;
			if (map3DButton != null) {
				map3DButton.onDestroyButton();
			}
			map3DButton = null;
		}
	}

	public View moveCompassButton(@NonNull ViewGroup destLayout, @NonNull ViewGroup.LayoutParams params) {
		return compassButton.moveToSpecialPosition(destLayout, params);
	}

	public void moveMap3DButton(@NonNull ViewGroup destLayout, @NonNull ViewGroup.LayoutParams params) {
		if (map3DButton != null) {
			map3DButton.moveToSpecialPosition(destLayout, params);
		}
	}

	public void restoreCompassButton() {
		compassButton.moveToDefaultPosition();
	}

	public void restoreMap3DButton() {
		if (map3DButton != null) {
			map3DButton.restoreSavedPosition();
		}
	}

	private void initTopControls() {
		MapActivity mapActivity = requireMapActivity();

		mapButtons.add(new ConfigureMapButton(mapActivity));
		mapButtons.add(new QuickSearchButton(mapActivity));

		compassButton = new CompassButton(mapActivity);
		mapButtons.add(compassButton);
	}

	private void initFabButtons(@NonNull MapActivity mapActivity) {
		ImageView buttonView = mapActivity.findViewById(R.id.map_3d_button);
		if (map3DButton != null) {
			map3DButton.onDestroyButton();
		}
		map3DButton = new Map3DButton(mapActivity, buttonView);
		mapButtons.add(map3DButton);
	}

	public void setControlsClickable(boolean clickable) {
		for (MapButton mapButton : mapButtons) {
			mapButton.getView().setClickable(clickable);
		}
	}

	public void initDashboardRelatedControls() {
		MapActivity mapActivity = requireMapActivity();
		ImageView backToLocation = mapActivity.findViewById(R.id.map_my_location_button);

		mapButtons.add(new DrawerMenuButton(mapActivity));
		mapButtons.add(new NavigationMenuButton(mapActivity));
		mapButtons.add(new MyLocationButton(mapActivity, backToLocation, BACK_TO_LOC_HUD_ID, true));
	}

	protected void resetTouchEvent() {
		touchEvent = 0;
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

	private void initZooms() {
		MapActivity mapActivity = requireMapActivity();

		ImageView zoomInButton = mapActivity.findViewById(R.id.map_zoom_in_button);
		ImageView zoomOutButton = mapActivity.findViewById(R.id.map_zoom_out_button);

		addMapButton(new ZoomInButton(mapActivity, zoomInButton, ZOOM_IN_HUD_ID));
		addMapButton(new ZoomOutButton(mapActivity, zoomOutButton, ZOOM_OUT_HUD_ID));
	}

	public void addMapButton(@NonNull MapButton mapButton) {
		mapButtons.add(mapButton);
	}

	public void removeMapButtons(@NonNull List<String> buttonIds) {
		List<MapButton> mapButtons = new ArrayList<>(this.mapButtons);
		for (Iterator<MapButton> iterator = mapButtons.iterator(); iterator.hasNext(); ) {
			MapButton mapButton = iterator.next();
			if (buttonIds.contains(mapButton.getId())) {
				iterator.remove();
			}
		}
		this.mapButtons = mapButtons;
	}

	public void showMapControlsIfHidden() {
		if (!isMapControlsVisible()) {
			showMapControls();
		}
	}

	private void showMapControls() {
		MapActivity mapActivity = requireMapActivity();
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.VISIBLE);
		} else {
			animateMapControls(true);
		}
		AndroidUtils.showNavBar(mapActivity);
	}

	public void hideMapControls() {
		MapActivity mapActivity = requireMapActivity();
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			mapActivity.findViewById(R.id.MapHudButtonsOverlay).setVisibility(View.INVISIBLE);
		} else {
			animateMapControls(false);
		}
	}

	private void animateMapControls(boolean show) {
		MapActivity mapActivity = requireMapActivity();
		View mapHudButtonsOverlay = mapActivity.findViewById(R.id.MapHudButtonsOverlay);
		View mapHudButtonsTop = mapActivity.findViewById(R.id.MapHudButtonsOverlayTop);
		View mapHudButtonsBottom = mapActivity.findViewById(R.id.MapHudButtonsOverlayBottom);
		View mapHudButtonsQuickActions = mapActivity.findViewById(R.id.MapHudButtonsOverlayQuickActions);

		float transTopInitial = show ? -mapHudButtonsTop.getHeight() : 0;
		float transBottomInitial = show ? mapHudButtonsBottom.getHeight() : 0;
		float alphaInitial = show ? 0f : 1f;

		float transTopFinal = show ? 0 : -mapHudButtonsTop.getHeight();
		float transBottomFinal = show ? 0 : mapHudButtonsBottom.getHeight();
		float alphaFinal = show ? 1f : 0f;

		AnimatorSet set = new AnimatorSet();
		set.setDuration(300).playTogether(
				ObjectAnimator.ofFloat(mapHudButtonsTop, View.TRANSLATION_Y, transTopInitial, transTopFinal),
				ObjectAnimator.ofFloat(mapHudButtonsBottom, View.TRANSLATION_Y, transBottomInitial, transBottomFinal),
				ObjectAnimator.ofFloat(mapHudButtonsQuickActions, View.ALPHA, alphaInitial, alphaFinal)
		);
		set.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				super.onAnimationStart(animation);
				if (show) {
					mapHudButtonsOverlay.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				if (!show) {
					mapHudButtonsOverlay.setVisibility(View.INVISIBLE);
					mapHudButtonsTop.setTranslationY(transTopInitial);
					mapHudButtonsBottom.setTranslationY(transBottomInitial);
					mapHudButtonsQuickActions.setAlpha(alphaInitial);
				}
				mapActivity.updateStatusBarColor();
			}
		});
		set.start();
	}

	public boolean isMapControlsVisible() {
		MapActivity mapActivity = requireMapActivity();
		return mapActivity.findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
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
		for (MapButton button : mapButtons) {
			button.onDestroyButton();
		}
		mapButtons.clear();
	}

	public void refreshButtons() {
		for (MapButton button : mapButtons) {
			button.refresh();
		}
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		updateControls(tileBox, nightMode);
	}

	private void updateControls(@NonNull RotatedTileBox tileBox, DrawSettings drawSettings) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		boolean nightMode = isNightModeForMapControls(drawSettings);

		boolean isRoutePlanningMode = isInRoutePlanningMode();
		boolean isRouteFollowingMode = !isRoutePlanningMode && app.getRoutingHelper().isFollowingMode();
		boolean isTimeToShowButtons = System.currentTimeMillis() - touchEvent < TIMEOUT_TO_SHOW_BUTTONS;
		boolean shouldShowRouteCalculationControls = isRoutePlanningMode || ((app.accessibilityEnabled() || isTimeToShowButtons) && isRouteFollowingMode);
		boolean isRouteDialogOpened = mapRouteInfoMenu.isVisible() || (shouldShowRouteCalculationControls && mapRouteInfoMenu.needShowMenu());

		NavigationSession carNavigationSession = app.getCarNavigationSession();
		boolean androidAutoAttached = carNavigationSession != null && carNavigationSession.hasStarted();
		boolean showBottomMenuButtons = visibilityHelper.shouldShowBottomMenuButtons()
				&& (shouldShowRouteCalculationControls || !isRouteFollowingMode || androidAutoAttached);

		mapTransparencyHelper.updateTransparencySliderUi();

		mapRouteInfoMenu.setVisible(shouldShowRouteCalculationControls);

		for (MapButton mapButton : mapButtons) {
			mapButton.update(nightMode, isRouteDialogOpened, showBottomMenuButtons);
		}
	}

	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return mapRouteInfoMenu != null && mapRouteInfoMenu.onSingleTap(point, tileBox);
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
