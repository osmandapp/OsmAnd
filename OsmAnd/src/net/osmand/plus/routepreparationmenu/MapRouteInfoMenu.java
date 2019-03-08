package net.osmand.plus.routepreparationmenu;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.AutoTransition;
import android.support.transition.Scene;
import android.support.transition.Transition;
import android.support.transition.TransitionListenerAdapter;
import android.support.transition.TransitionManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.mapmarkers.MapMarkerSelectionFragment;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.ShowAlongTheRouteItem;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.HistoryCard;
import net.osmand.plus.routepreparationmenu.cards.HomeWorkCard;
import net.osmand.plus.routepreparationmenu.cards.PreviousRouteCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.cards.SimpleRouteCard;
import net.osmand.plus.routepreparationmenu.cards.TracksCard;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchResult;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;

public class MapRouteInfoMenu implements IRouteInformationListener, CardListener {

	private static final int BUTTON_ANIMATION_DELAY = 2000;
	public static final int DEFAULT_MENU_STATE = 0;

	public static class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	public static int directionInfo = -1;
	public static boolean chooseRoutesVisible = false;
	private boolean routeCalculationInProgress;

	private boolean selectFromMapTouch;
	private PointType selectFromMapPointType;
	private int selectFromMapMenuState = MenuState.HEADER_ONLY;

	private boolean showMenu = false;
	private int showMenuState = DEFAULT_MENU_STATE;

	@Nullable
	private MapActivity mapActivity;
	@Nullable
	private OsmandApplication app;
	@Nullable
	private Handler animationsHandler;

	private boolean nightMode;
	private boolean switched;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;
	private List<LatLon> intermediateRequestsLatLon = new ArrayList<>();
	private OnDismissListener onDismissListener;
	private List<BaseCard> menuCards = new ArrayList<>();

	private OnMarkerSelectListener onMarkerSelectListener;
	private StateChangedListener<Void> onStateChangedListener;
	@Nullable
	private View mainView;

	private int currentMenuState;
	private boolean portraitMode;

	private boolean swapButtonCollapsing;
	private boolean swapButtonCollapsed;
	private boolean editButtonCollapsing;
	private boolean editButtonCollapsed;
	private boolean addButtonCollapsing;
	private boolean addButtonCollapsed;

	private interface OnButtonCollapsedListener {
		void onButtonCollapsed(boolean success);
	}

	public interface OnMarkerSelectListener {
		void onSelect(int index, PointType pointType);
	}

	public enum PointType {
		START,
		TARGET,
		INTERMEDIATE,
		HOME,
		WORK
	}

	public MapRouteInfoMenu() {
		currentMenuState = getInitialMenuState();
		onMarkerSelectListener = new OnMarkerSelectListener() {
			@Override
			public void onSelect(int index, PointType pointType) {
				selectMapMarker(index, pointType);
			}
		};
		onStateChangedListener = new StateChangedListener<Void>() {
			@Override
			public void stateChanged(Void change) {
				updateMenu();
			}
		};
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.mainView = null;
		this.animationsHandler = null;
		if (mapActivity != null) {
			app = mapActivity.getMyApplication();
			portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
			animationsHandler = new Handler();
			mapActivity.getMyApplication().getRoutingHelper().addListener(this);
		}
	}

	@Nullable
	public OsmandApplication getApp() {
		return app;
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	@Nullable
	public View getMainView() {
		return mainView;
	}

	@Nullable
	public Handler getAnimationsHandler() {
		return animationsHandler;
	}

	private int getInitialMenuState() {
		return MenuState.FULL_SCREEN;
	}

	public OnDismissListener getOnDismissListener() {
		return onDismissListener;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	public boolean isSelectFromMapTouch() {
		return selectFromMapTouch;
	}

	public void cancelSelectionFromMap() {
		selectFromMapTouch = false;
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		OsmandApplication app = getApp();
		if (app != null) {
			if (selectFromMapTouch) {
				LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
				selectFromMapTouch = false;
				TargetPointsHelper targets = app.getTargetPointsHelper();
				switch (selectFromMapPointType) {
					case START:
						targets.setStartPoint(latlon, true, null);
						break;
					case TARGET:
						targets.navigateToPoint(latlon, true, -1);
						break;
					case INTERMEDIATE:
						targets.navigateToPoint(latlon, true, targets.getIntermediatePoints().size());
						break;
					case HOME:
						targets.setHomePoint(latlon, null);
						break;
					case WORK:
						targets.setWorkPoint(latlon, null);
						break;
				}
				show(selectFromMapMenuState);
				return true;
			}
		}
		return false;
	}

	public OnMarkerSelectListener getOnMarkerSelectListener() {
		return onMarkerSelectListener;
	}

	public void addTargetPointListener() {
		OsmandApplication app = getApp();
		if (app != null) {
			app.getTargetPointsHelper().addListener(onStateChangedListener);
		}
	}

	private void removeTargetPointListener() {
		OsmandApplication app = getApp();
		if (app != null) {
			app.getTargetPointsHelper().removeListener(onStateChangedListener);
		}
	}

	private void cancelStartPointAddressRequest() {
		OsmandApplication app = getApp();
		if (startPointRequest != null && app != null) {
			app.getGeocodingLookupService().cancel(startPointRequest);
			startPointRequest = null;
		}
	}

	private void cancelTargetPointAddressRequest() {
		OsmandApplication app = getApp();
		if (targetPointRequest != null && app != null) {
			app.getGeocodingLookupService().cancel(targetPointRequest);
			targetPointRequest = null;
		}
	}

	private void runButtonAnimation(Runnable animation) {
		Handler animationsHandler = getAnimationsHandler();
		if (animationsHandler != null) {
			animationsHandler.postDelayed(animation, BUTTON_ANIMATION_DELAY);
		}
	}

	private void cancelAnimations() {
		Handler animationsHandler = getAnimationsHandler();
		if (animationsHandler != null) {
			animationsHandler.removeCallbacksAndMessages(null);
		}
	}

	public void setVisible(boolean visible) {
		if (visible) {
			if (showMenu) {
				show(showMenuState);
				showMenu = false;
				showMenuState = DEFAULT_MENU_STATE;
			}
		} else {
			hide();
		}
	}

	public int getCurrentMenuState() {
		return currentMenuState;
	}

	public int getSupportedMenuStates() {
		if (!portraitMode) {
			return MenuState.FULL_SCREEN;
		} else {
			return getSupportedMenuStatesPortrait();
		}
	}

	protected int getInitialMenuStatePortrait() {
		return MenuState.HEADER_ONLY;
	}

	protected int getSupportedMenuStatesPortrait() {
		return MenuState.HEADER_ONLY | MenuState.HALF_SCREEN | MenuState.FULL_SCREEN;
	}

	public boolean slideUp() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v << 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public boolean slideDown() {
		int v = currentMenuState;
		for (int i = 0; i < 2; i++) {
			v = v >> 1;
			if ((v & getSupportedMenuStates()) != 0) {
				currentMenuState = v;
				return true;
			}
		}
		return false;
	}

	public void showHideMenu(int menuState) {
		intermediateRequestsLatLon.clear();
		if (isVisible()) {
			hide();
		} else {
			show(menuState);
		}
	}

	private boolean setRouteCalculationInProgress(boolean routeCalculationInProgress) {
		if (this.routeCalculationInProgress != routeCalculationInProgress) {
			this.routeCalculationInProgress = routeCalculationInProgress;
			return true;
		} else {
			return false;
		}
	}

	public void updateRouteCalculationProgress(int progress) {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		MapRouteInfoMenuFragment fragment = fragmentRef != null ? fragmentRef.get() : null;
		if (fragmentRef != null && fragment.isVisible()) {
			if (setRouteCalculationInProgress(true)) {
				fragment.updateInfo();
			}
			fragment.updateRouteCalculationProgress(progress);
			fragment.updateControlButtons();
		}
	}

	public void routeCalculationFinished() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		MapRouteInfoMenuFragment fragment = fragmentRef != null ? fragmentRef.get() : null;
		if (fragmentRef != null && fragment.isVisible()) {
			setRouteCalculationInProgress(false);
			fragment.hideRouteCalculationProgressBar();
			fragment.updateControlButtons();
			fragment.updateInfo();
			fragment.openMenuHalfScreen();
		}
	}

	public void openMenuFullScreen() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().openMenuFullScreen();
		}
	}

	public void openMenuHeaderOnly() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().openMenuHeaderOnly();
		}
	}

	public void updateMenu() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateInfo();
	}

	public void updateLayout() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateLayout();
	}

	public void updateFromIcon() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateFromIcon();
	}

	public void setBottomShadowVisible(boolean visible) {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().setBottomShadowVisible(visible);
	}

	public void build(LinearLayout rootView) {
		rootView.removeAllViews();
		for (BaseCard card : menuCards) {
			rootView.addView(card.build(rootView.getContext()));
		}
	}

	public void updateInfo(@NonNull View main) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		mainView = main;
		OsmandApplication app = mapActivity.getMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		updateStartPointView();
		updateWaypointsView();
		updateFinishPointView();

		updateApplicationModes();
		updateApplicationModesOptions();
		updateOptionsButtons();

		menuCards.clear();

		boolean bottomShadowVisible = true;
		if (isBasicRouteCalculated()) {
			GPXFile gpx = GpxUiHelper.makeGpxFromRoute(app.getRoutingHelper().getRoute(), app);
			if (gpx != null) {
				menuCards.add(new SimpleRouteCard(mapActivity, gpx));
			}
			bottomShadowVisible = gpx == null;
		} else if (isTransportRouteCalculated()) {
			List<TransportRouteResult> routes = app.getTransportRoutingHelper().getRoutes();
			for (int i = 0; i < routes.size(); i++) {
				PublicTransportCard card = new PublicTransportCard(mapActivity, routes.get(i), i);
				card.setShowBottomShadow(i == routes.size() - 1);
				card.setShowTopShadow(i != 0);
				menuCards.add(card);
			}
			bottomShadowVisible = routes.size() == 0;
		} else if (!routeCalculationInProgress) {
			HomeWorkCard homeWorkCard = new HomeWorkCard(mapActivity);
			menuCards.add(homeWorkCard);

			TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
			TargetPoint startBackup = targetPointsHelper.getPointToStartBackup();
			if (startBackup == null) {
				startBackup = targetPointsHelper.getMyLocationToStart();
			}
			TargetPoint destinationBackup = targetPointsHelper.getPointToNavigateBackup();
			if (startBackup != null && destinationBackup != null) {
				PreviousRouteCard previousRouteCard = new PreviousRouteCard(mapActivity);
				menuCards.add(previousRouteCard);
			}

			List<SelectedGpxFile> selectedGPXFiles =
					app.getSelectedGpxHelper().getSelectedGPXFiles();
			final List<GPXFile> gpxFiles = new ArrayList<>();
			for (SelectedGpxFile gs : selectedGPXFiles) {
				if (!gs.isShowCurrentTrack()) {
					if (gs.getGpxFile().hasRtePt() || gs.getGpxFile().hasTrkPt()) {
						gpxFiles.add(gs.getGpxFile());
					}
				}
			}
			if (gpxFiles.size() > 0) {
				TracksCard tracksCard = new TracksCard(mapActivity, gpxFiles);
				tracksCard.setListener(this);
				menuCards.add(tracksCard);
			}

			SearchResultCollection res = null;
			try {
				res = app.getSearchUICore().getCore().shallowSearch(QuickSearchHelper.SearchHistoryAPI.class, "", null);
			} catch (IOException e) {
				// ignore
			}
			if (res != null) {
				List<SearchResult> results = res.getCurrentSearchResults();
				if (results.size() > 0) {
					HistoryCard historyCard = new HistoryCard(mapActivity, results);
					historyCard.setListener(this);
					menuCards.add(historyCard);
				}
			}
		}
		setBottomShadowVisible(bottomShadowVisible);
		setupCards();
	}

	private void setupCards() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = (LinearLayout) mainView.findViewById(R.id.route_menu_cards_container);
			build(cardsContainer);
		}
	}

	@Override
	public void onCardLayoutNeeded() {
		updateLayout();
	}

	public boolean isRouteCalculated() {
		return isBasicRouteCalculated() || isTransportRouteCalculated();
	}

	public boolean isTransportRouteCalculated() {
		OsmandApplication app = getApp();
		if (app != null) {
			return app.getRoutingHelper().isPublicTransportMode() && app.getTransportRoutingHelper().getRoutes() != null;
		}
		return false;
	}

	public boolean isBasicRouteCalculated() {
		OsmandApplication app = getApp();
		if (app != null) {
			RoutingHelper routingHelper = app.getRoutingHelper();
			return routingHelper.getFinalLocation() != null && routingHelper.isRouteCalculated();
		}
		return false;
	}

	public void updateApplicationModesOptions() {
		View mainView = getMainView();
		if (mainView != null) {
			AppCompatImageView foldButtonView = (AppCompatImageView) mainView.findViewById(R.id.fold_button);
			foldButtonView.setImageResource(currentMenuState == MenuState.HEADER_ONLY ?
					R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down);
			foldButtonView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					expandCollapse();
				}
			});

			mainView.findViewById(R.id.app_modes_options).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					availableProfileDialog();
				}
			});
		}
	}

	private void expandCollapse() {
		if (currentMenuState == MenuState.HEADER_ONLY) {
			openMenuFullScreen();
		} else {
			openMenuHeaderOnly();
		}
		updateApplicationModesOptions();
	}

	private void availableProfileDialog() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			AlertDialog.Builder b = new AlertDialog.Builder(mapActivity);
			final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
			final List<ApplicationMode> modes = ApplicationMode.allPossibleValues();
			modes.remove(ApplicationMode.DEFAULT);
			final Set<ApplicationMode> selected = new LinkedHashSet<>(ApplicationMode.values(mapActivity.getMyApplication()));
			selected.remove(ApplicationMode.DEFAULT);
			View v = AppModeDialog.prepareAppModeView(mapActivity, modes, selected, null, false, true, false,
					new OnClickListener() {

						@Override
						public void onClick(View v) {
							StringBuilder vls = new StringBuilder(ApplicationMode.DEFAULT.getStringKey() + ",");
							for (ApplicationMode mode : modes) {
								if (selected.contains(mode)) {
									vls.append(mode.getStringKey()).append(",");
								}
							}
							settings.AVAILABLE_APP_MODES.set(vls.toString());
						}
					});
			b.setTitle(R.string.profile_settings);
			b.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateApplicationModes();
				}
			});
			b.setView(v);
			b.show();
		}
	}

	private void updateApplicationMode(ApplicationMode mode, ApplicationMode next) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			RoutingHelper routingHelper = app.getRoutingHelper();
			OsmandPreference<ApplicationMode> appMode = app.getSettings().APPLICATION_MODE;
			if (routingHelper.isFollowingMode() && appMode.get() == mode) {
				appMode.set(next);
			}
			routingHelper.setAppMode(next);
			app.initVoiceCommandPlayer(mapActivity, next, true, null, false, false);
			routingHelper.recalculateRouteDueToSettingsChange();
		}
	}

	private void updateApplicationModes() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		final ApplicationMode am = mapActivity.getMyApplication().getRoutingHelper().getAppMode();
		final Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = (ViewGroup) mainView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selected.size() > 0) {
					ApplicationMode next = selected.iterator().next();
					updateApplicationMode(am, next);
				}
				updateFinishPointView();
				updateOptionsButtons();
			}
		};
		final List<ApplicationMode> values = new ArrayList<ApplicationMode>(ApplicationMode.values(mapActivity.getMyApplication()));
		values.remove(ApplicationMode.DEFAULT);

		if (values.size() > 0 && !values.contains(am)) {
			ApplicationMode next = values.iterator().next();
			updateApplicationMode(am, next);
		}

		View ll = mapActivity.getLayoutInflater().inflate(R.layout.mode_toggles, vg);
		ll.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.route_info_bg_dark : R.color.route_info_bg_light));

		HorizontalScrollView scrollView = ll.findViewById(R.id.app_modes_scroll_container);
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		int leftTogglePadding = AndroidUtils.dpToPx(mapActivity, 8f);
		int rightTogglePadding = mapActivity.getResources().getDimensionPixelSize(R.dimen.content_padding);
		final View[] buttons = new View[values.size()];
		int k = 0;
		Iterator<ApplicationMode> iterator = values.iterator();
		boolean firstMode = true;
		while (iterator.hasNext()) {
			ApplicationMode mode = iterator.next();
			View toggle = AppModeDialog.createToggle(mapActivity.getLayoutInflater(), (OsmandApplication) mapActivity.getApplication(),
					R.layout.mode_view_route_preparation, (LinearLayout) ll.findViewById(R.id.app_modes_content), mode, true);

			if (firstMode && toggle.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				firstMode = false;
				ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) toggle.getLayoutParams();
				p.setMargins(p.leftMargin + leftTogglePadding, p.topMargin, p.rightMargin, p.bottomMargin);
			}
			if (!iterator.hasNext() && toggle.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) toggle.getLayoutParams();
				p.setMargins(p.leftMargin, p.topMargin, p.rightMargin + rightTogglePadding, p.bottomMargin);
			}

			buttons[k++] = toggle;
		}
		for (int i = 0; i < buttons.length; i++) {
			AppModeDialog.updateButtonStateForRoute((OsmandApplication) mapActivity.getApplication(), values, selected, listener, buttons, i, true, true, nightMode);
		}
	}

	private void updateOptionsButtons() {
		MapActivity mapActivity = getMapActivity();
		final View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		final boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		final OsmandSettings settings = app.getSettings();
		final int colorActive = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		final int colorDisabled = ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);
		final ApplicationMode applicationMode = routingHelper.getAppMode();
		final RoutingOptionsHelper.RouteMenuAppModes mode = app.getRoutingOptionsHelper().modes.get(applicationMode);
		int margin = AndroidUtils.dpToPx(app, 3);

		View startButton = mainView.findViewById(R.id.start_button);
		TextView startButtonText = (TextView) mainView.findViewById(R.id.start_button_descr);
		if (isRouteCalculated()) {
			AndroidUtils.setBackground(app, startButton, nightMode, R.color.active_buttons_and_links_light, R.color.active_buttons_and_links_dark);
			int color = nightMode ? R.color.main_font_dark : R.color.card_and_list_background_light;
			startButtonText.setTextColor(ContextCompat.getColor(app, color));
			((ImageView) mainView.findViewById(R.id.start_icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, color));
		} else {
			AndroidUtils.setBackground(app, startButton, nightMode, R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);
			int color = R.color.description_font_and_bottom_sheet_icons;
			startButtonText.setTextColor(ContextCompat.getColor(app, color));
			((ImageView) mainView.findViewById(R.id.start_icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, color));
		}
		if (routingHelper.isFollowingMode() || routingHelper.isPauseNavigation()) {
			startButtonText.setText(R.string.shared_string_continue);
		} else {
			startButtonText.setText(R.string.shared_string_control_start);
		}
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteGo();
			}
		});

		View cancelButton = mainView.findViewById(R.id.cancel_button);
		AndroidUtils.setBackground(app, cancelButton, nightMode, R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteCancel();
			}
		});

		LinearLayout optionsButton = (LinearLayout) mainView.findViewById(R.id.map_options_route_button);
		TextView optionsTitle = (TextView) mainView.findViewById(R.id.map_options_route_button_title);
		ImageView optionsIcon = (ImageView) mainView.findViewById(R.id.map_options_route_button_icon);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.map_action_settings, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = app.getUIUtilities().getIcon(R.drawable.map_action_settings, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
			drawable = AndroidUtils.createPressedStateListDrawable(drawable, active);
		}
		optionsIcon.setImageDrawable(drawable);
		optionsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteParams();
			}
		});
		AndroidUtils.setBackground(app, optionsButton, nightMode, R.drawable.route_info_trans_gradient_light, R.drawable.route_info_trans_gradient_dark);

		HorizontalScrollView scrollView = mainView.findViewById(R.id.route_options_scroll_container);
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		LinearLayout optionsContainer = (LinearLayout) mainView.findViewById(R.id.route_options_container);
		optionsContainer.removeAllViews();
		if (mode == null) {
			return;
		}

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
		LinearLayout.LayoutParams newLp = new LinearLayout.LayoutParams(AndroidUtils.dpToPx(app, 100), ViewGroup.LayoutParams.MATCH_PARENT);

		lp.setMargins(margin, 0, margin, 0);

		if (mode.parameters.size() > 2) {
			optionsTitle.setVisibility(View.GONE);
		} else {
			optionsTitle.setVisibility(View.VISIBLE);
		}

		for (final RoutingOptionsHelper.LocalRoutingParameter parameter : mode.parameters) {
			if (parameter instanceof MuteSoundRoutingParameter) {
				String text = null;
				final boolean active = !app.getRoutingHelper().getVoiceRouter().isMute();
				if (mode.parameters.size() <= 2) {
					text = app.getString(active ? R.string.shared_string_on : R.string.shared_string_off);
				}
				View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new OnClickListener() {
					@Override
					public void onClick(View v) {
						OsmandApplication app = getApp();
						if (app != null) {
							app.getRoutingOptionsHelper().switchSound();
							boolean active = !app.getRoutingHelper().getVoiceRouter().isMute();
							String text = app.getString(active ? R.string.shared_string_on : R.string.shared_string_off);

							Drawable itemDrawable = app.getUIUtilities().getIcon(active ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
							Drawable activeItemDrawable = app.getUIUtilities().getIcon(active ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

							if (Build.VERSION.SDK_INT >= 21) {
								itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
							}

							((ImageView) v.findViewById(R.id.route_option_image_view)).setImageDrawable(active ? activeItemDrawable : itemDrawable);
							((TextView) v.findViewById(R.id.route_option_title)).setText(text);
							((TextView) v.findViewById(R.id.route_option_title)).setTextColor(active ? colorActive : colorDisabled);
						}
					}
				});
				if (item != null) {
					optionsContainer.addView(item, lp);
				}
			} else if (parameter instanceof ShowAlongTheRouteItem) {
				final Set<PoiUIFilter> poiFilters = app.getPoiFilters().getSelectedPoiFilters();
				final boolean traffic = app.getSettings().SHOW_TRAFFIC_WARNINGS.getModeValue(applicationMode);
				final boolean fav = app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(applicationMode);
				if (!poiFilters.isEmpty()) {
					LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					if (item != null) {
						item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
						Iterator<PoiUIFilter> it = poiFilters.iterator();
						while (it.hasNext()) {
							final PoiUIFilter poiUIFilter = it.next();
							final View container = createToolbarSubOptionView(true, poiUIFilter.getName(), R.drawable.ic_action_remove_dark, !it.hasNext(), new OnClickListener() {
								@Override
								public void onClick(View v) {
									MapActivity mapActivity = getMapActivity();
									if (mapActivity != null) {
										mapActivity.getMyApplication().getPoiFilters().removeSelectedPoiFilter(poiUIFilter);
										mapActivity.getMapView().refreshMap();
										updateOptionsButtons();
									}
								}
							});
							if (container != null) {
								item.addView(container, newLp);
							}
						}
						optionsContainer.addView(item, lp);
					}
				}
				if (traffic) {
					LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					if (item != null) {
						item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
						final View container = createToolbarSubOptionView(true, app.getString(R.string.way_alarms), R.drawable.ic_action_remove_dark, true, new OnClickListener() {
							@Override
							public void onClick(View v) {
								OsmandApplication app = getApp();
								if (app != null) {
									app.getWaypointHelper().enableWaypointType(WaypointHelper.ALARMS, false);
									updateOptionsButtons();
								}
							}
						});
						if (container != null) {
							AndroidUtils.setBackground(app, container, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
							item.addView(container, newLp);
							optionsContainer.addView(item, lp);
						}
					}
				}
				if (fav) {
					LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					if (item != null) {
						item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
						final View container = createToolbarSubOptionView(true, app.getString(R.string.favourites), R.drawable.ic_action_remove_dark, true, new OnClickListener() {
							@Override
							public void onClick(View v) {
								OsmandApplication app = getApp();
								if (app != null) {
									app.getWaypointHelper().enableWaypointType(WaypointHelper.FAVORITES, false);
									updateOptionsButtons();
								}
							}
						});
						if (container != null) {
							AndroidUtils.setBackground(app, container, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
							item.addView(container, newLp);
							optionsContainer.addView(item, lp);
						}
					}
				}
			} else if (parameter instanceof AvoidRoadsTypesRoutingParameter) {
				final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
				if (item != null) {
					item.findViewById(R.id.route_option_container).setVisibility(View.GONE);

					List<RoutingParameter> avoidParameters = app.getRoutingOptionsHelper().getAvoidRoutingPrefsForAppMode(applicationMode);
					final List<RoutingParameter> avoidedParameters = new ArrayList<>();
					for (int i = 0; i < avoidParameters.size(); i++) {
						RoutingParameter p = avoidParameters.get(i);
						CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());
						if (preference != null && preference.get()) {
							avoidedParameters.add(p);
						}
					}
					if (avoidedParameters.isEmpty()) {
						continue;
					}
					for (int i = 0; i < avoidedParameters.size(); i++) {
						final RoutingParameter routingParameter = avoidedParameters.get(i);
						final View container = createToolbarSubOptionView(false, SettingsBaseActivity.getRoutingStringPropertyName(app, routingParameter.getId(), routingParameter.getName()), R.drawable.ic_action_remove_dark, i == avoidedParameters.size() - 1, new OnClickListener() {
							@Override
							public void onClick(View v) {
								CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
								preference.set(false);
								avoidedParameters.remove(routingParameter);
								if (avoidedParameters.isEmpty()) {
									mode.parameters.remove(parameter);
								}
								if (mode.parameters.size() > 2) {
									item.removeView(v);
								} else {
									updateOptionsButtons();
								}
							}
						});
						if (container != null) {
							item.addView(container, newLp);
						}
					}
					optionsContainer.addView(item, lp);
				}
			} else if (parameter instanceof AvoidRoadsRoutingParameter) {
				final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
				if (item != null) {
					item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
					AvoidSpecificRoads avoidSpecificRoads = app.getAvoidSpecificRoads();
					Map<LatLon, RouteDataObject> impassableRoads = avoidSpecificRoads.getImpassableRoads();
					if (impassableRoads.isEmpty()) {
						continue;
					}
					Iterator<RouteDataObject> it = impassableRoads.values().iterator();
					while (it.hasNext()) {
						final RouteDataObject routeDataObject = it.next();
						final View container = createToolbarSubOptionView(false, avoidSpecificRoads.getText(routeDataObject), R.drawable.ic_action_remove_dark, !it.hasNext(), new OnClickListener() {
							@Override
							public void onClick(View v) {
								MapActivity mapActivity = getMapActivity();
								if (mapActivity != null) {
									OsmandApplication app = mapActivity.getMyApplication();
									RoutingHelper routingHelper = app.getRoutingHelper();
									if (routeDataObject != null) {
										app.getAvoidSpecificRoads().removeImpassableRoad(routeDataObject);
									}
									if (routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated()) {
										routingHelper.recalculateRouteDueToSettingsChange();
									}
									if (app.getAvoidSpecificRoads().getImpassableRoads().isEmpty()) {
										mode.parameters.remove(parameter);
									}
									mapActivity.getMapView().refreshMap();
									if (mode.parameters.size() > 2) {
										item.removeView(v);
									} else {
										updateOptionsButtons();
									}
								}
							}
						});
						if (container != null) {
							item.addView(container, newLp);
						}
					}
					optionsContainer.addView(item, lp);
				}
			} else if (parameter instanceof LocalRoutingParameterGroup) {
				final LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) parameter;
				String text = null;
				RoutingOptionsHelper.LocalRoutingParameter selected = group.getSelected(settings);
				if (selected != null) {
					text = group.getText(mapActivity);
				}
				View item = createToolbarOptionView(false, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = getMapActivity();
						if (mapActivity != null) {
							mapActivity.getMyApplication().getRoutingOptionsHelper().showLocalRoutingParameterGroupDialog(group, mapActivity, new RoutingOptionsHelper.OnClickListener() {
								@Override
								public void onClick() {
									updateOptionsButtons();
								}
							});
						}
					}
				});
				if (item != null) {
					optionsContainer.addView(item, lp);
				}
			} else {
				String text;
				boolean active;
				if (parameter.routingParameter != null) {
					if (parameter.routingParameter.getId().equals(GeneralRouter.USE_SHORTEST_WAY)) {
						// if short route settings - it should be inverse of fast_route_mode
						active = !settings.FAST_ROUTE_MODE.getModeValue(routingHelper.getAppMode());
					} else {
						active = parameter.isSelected(settings);
					}
					text = parameter.getText(mapActivity);
					View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new OnClickListener() {
						@Override
						public void onClick(View v) {
							OsmandApplication app = getApp();
							if (parameter.routingParameter != null && app != null) {
								boolean selected = !parameter.isSelected(settings);
								app.getRoutingOptionsHelper().applyRoutingParameter(parameter, selected);

								Drawable itemDrawable = app.getUIUtilities().getIcon(selected ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
								Drawable activeItemDrawable = app.getUIUtilities().getIcon(selected ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

								if (Build.VERSION.SDK_INT >= 21) {
									itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
								}
								((ImageView) v.findViewById(R.id.route_option_image_view)).setImageDrawable(selected ? activeItemDrawable : itemDrawable);
								((TextView) v.findViewById(R.id.route_option_title)).setTextColor(selected ? colorActive : colorDisabled);
							}
						}
					});
					if (item != null) {
						LinearLayout.LayoutParams newLp2 = new LinearLayout.LayoutParams(AndroidUtils.dpToPx(app, 100), ViewGroup.LayoutParams.MATCH_PARENT);
						newLp2.setMargins(margin, 0, margin, 0);
						optionsContainer.addView(item, newLp2);
					}
				}
			}
		}
		int rightPadding = AndroidUtils.dpToPx(app, 70);
		if (optionsTitle.getVisibility() == View.VISIBLE) {
			rightPadding += AndroidUtils.getTextWidth(app.getResources().getDimensionPixelSize(R.dimen.text_button_text_size), app.getString(R.string.shared_string_options));
		}
		optionsContainer.setPadding(optionsContainer.getPaddingLeft(), optionsContainer.getPaddingTop(), rightPadding, optionsContainer.getPaddingBottom());
	}

	@Nullable
	private LinearLayout createToolbarOptionView(boolean active, String title, @DrawableRes int activeIconId,
												 @DrawableRes int disabledIconId, OnClickListener listener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		final LinearLayout item = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.route_option_btn, null);
		final TextView textView = (TextView) item.findViewById(R.id.route_option_title);
		final ImageView imageView = (ImageView) item.findViewById(R.id.route_option_image_view);
		final int colorActive = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		final int colorDisabled = ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, item.findViewById(R.id.route_option_container), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			AndroidUtils.setBackground(app, item, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		} else {
			AndroidUtils.setBackground(app, item, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}

		Drawable itemDrawable = null;
		Drawable activeItemDrawable = null;
		if (activeIconId != -1 && disabledIconId != -1) {
			itemDrawable = app.getUIUtilities().getIcon(active ? activeIconId : disabledIconId, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
			activeItemDrawable = app.getUIUtilities().getIcon(active ? activeIconId : disabledIconId, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
			if (Build.VERSION.SDK_INT >= 21) {
				itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
			}
		}
		if (title == null) {
			textView.setVisibility(View.GONE);
			if (activeItemDrawable != null && itemDrawable != null) {
				imageView.setImageDrawable(active ? activeItemDrawable : itemDrawable);
			} else {
				imageView.setVisibility(View.GONE);
			}
		} else {
			textView.setVisibility(View.VISIBLE);
			textView.setTextColor(active ? colorActive : colorDisabled);
			textView.setText(title);
			if (activeItemDrawable != null && itemDrawable != null) {
				imageView.setImageDrawable(active ? activeItemDrawable : itemDrawable);
			} else {
				imageView.setVisibility(View.GONE);
			}
		}
		item.setOnClickListener(listener);

		return item;
	}

	@Nullable
	private View createToolbarSubOptionView(boolean hideTextLine, String title, @DrawableRes int iconId,
											boolean lastItem, OnClickListener listener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		final View container = mapActivity.getLayoutInflater().inflate(R.layout.route_options_container, null);
		final TextView routeOptionTV = (TextView) container.findViewById(R.id.route_removable_option_title);
		final ImageView routeOptionImageView = (ImageView) container.findViewById(R.id.removable_option_icon);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.btn_pressed_trans_light, R.drawable.btn_pressed_trans_dark);
		}
		AndroidUtils.setBackground(app, container.findViewById(R.id.options_divider_end), nightMode, R.color.divider_light, R.color.divider_dark);
		AndroidUtils.setBackground(app, routeOptionImageView, nightMode, R.drawable.route_info_trans_gradient_light, R.drawable.route_info_trans_gradient_dark);

		if (lastItem) {
			container.findViewById(R.id.options_divider_end).setVisibility(View.GONE);
		} else {
			container.findViewById(R.id.options_divider_end).setVisibility(View.VISIBLE);
		}
		if (hideTextLine) {
			container.findViewById(R.id.title_divider).setVisibility(View.GONE);
		}
		routeOptionTV.setText(title);
		routeOptionImageView.setImageDrawable(app.getUIUtilities().getIcon(iconId, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));
		container.setOnClickListener(listener);

		return container;
	}

	private void clickRouteGo() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (mapActivity.getPointToNavigate() != null) {
				hide();
			}
			if (isTransportRouteCalculated()) {
				ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager());
			} else {
				mapActivity.getMapLayers().getMapControlsLayer().startNavigation();
			}
		}
	}

	private void clickRouteCancel() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapControlsLayer().stopNavigation();
			setRouteCalculationInProgress(false);
			restoreCollapsedButtons();
		}
	}

	private void clickRouteParams() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RouteOptionsBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
		}
	}

	private void updateWaypointsView() {
		MapActivity mapActivity = getMapActivity();
		final View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		String via = generateViaDescription();
		View viaLayout = mainView.findViewById(R.id.ViaLayout);
		View viaLayoutDivider = mainView.findViewById(R.id.viaLayoutDivider);
		if (via.length() == 0) {
			viaLayout.setVisibility(View.GONE);
			viaLayoutDivider.setVisibility(View.GONE);
		} else {
			viaLayout.setVisibility(View.VISIBLE);
			viaLayoutDivider.setVisibility(View.VISIBLE);
			((TextView) mainView.findViewById(R.id.ViaView)).setText(via);
			((TextView) mainView.findViewById(R.id.ViaSubView)).setText(mapActivity.getString(R.string.intermediate_destinations) + ": " +
					mapActivity.getMyApplication().getTargetPointsHelper().getIntermediatePoints().size());
		}
		FrameLayout viaButton = (FrameLayout) mainView.findViewById(R.id.via_button);

		viaButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null && mapActivity.getMyApplication().getTargetPointsHelper().checkPointToNavigateShort()) {
					WaypointsFragment.showInstance(mapActivity);
				}
			}
		});

		ImageView viaIcon = (ImageView) mainView.findViewById(R.id.viaIcon);
		viaIcon.setImageDrawable(getIconOrig(R.drawable.list_intermediate));
		LinearLayout viaButtonContainer = (LinearLayout) mainView.findViewById(R.id.via_button_container);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, viaButton, nightMode, R.drawable.btn_border_rounded_light, R.drawable.btn_border_rounded_dark);
			AndroidUtils.setBackground(app, viaButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, viaButtonContainer, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
		}
		ImageView viaButtonImageView = (ImageView) mainView.findViewById(R.id.via_button_image_view);

		Drawable normal = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_edit_dark, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_edit_dark, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

			normal = AndroidUtils.createPressedStateListDrawable(normal, active);
		}
		viaButtonImageView.setImageDrawable(normal);

		final View textView = mainView.findViewById(R.id.via_button_description);
		if (!editButtonCollapsing && !editButtonCollapsed &&
				viaButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
			editButtonCollapsing = true;
			collapseButtonAnimated(R.id.via_button, R.id.via_button_description, new OnButtonCollapsedListener() {
				@Override
				public void onButtonCollapsed(boolean success) {
					editButtonCollapsing = false;
					editButtonCollapsed = success;
				}
			});
		} else if (editButtonCollapsed) {
			textView.setVisibility(View.GONE);
		}
	}

	private void updateFinishPointView() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();

		setupToText(mainView);
		final View toLayout = mainView.findViewById(R.id.ToLayout);
		toLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openAddPointDialog(PointType.TARGET);
			}
		});

		final FrameLayout toButton = (FrameLayout) mainView.findViewById(R.id.to_button);
		if (app.getRoutingHelper().isPublicTransportMode()) {
			toButton.setVisibility(View.GONE);
		} else {
			toButton.setVisibility(View.VISIBLE);

			final LinearLayout toButtonContainer = (LinearLayout) mainView.findViewById(R.id.to_button_container);

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(app, toButton, nightMode, R.drawable.btn_border_rounded_light, R.drawable.btn_border_rounded_dark);
				AndroidUtils.setBackground(app, toButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
			} else {
				AndroidUtils.setBackground(app, toButtonContainer, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
			}
			ImageView toButtonImageView = (ImageView) mainView.findViewById(R.id.to_button_image_view);

			Drawable normal = app.getUIUtilities().getIcon(R.drawable.ic_action_plus, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
			if (Build.VERSION.SDK_INT >= 21) {
				Drawable active = app.getUIUtilities().getIcon(R.drawable.ic_action_plus, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

				normal = AndroidUtils.createPressedStateListDrawable(normal, active);
			}

			toButtonImageView.setImageDrawable(normal);
			toButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					OsmandApplication app = getApp();
					if (app != null) {
						openAddPointDialog(app.getTargetPointsHelper().getPointToNavigate() == null ? PointType.TARGET : PointType.INTERMEDIATE);
					}
				}
			});

			final View textView = mainView.findViewById(R.id.to_button_description);
			if (!addButtonCollapsing && !addButtonCollapsed &&
					toButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
				addButtonCollapsing = true;
				collapseButtonAnimated(R.id.to_button, R.id.to_button_description, new OnButtonCollapsedListener() {
					@Override
					public void onButtonCollapsed(boolean success) {
						addButtonCollapsing = false;
						addButtonCollapsed = success;
					}
				});
			} else if (addButtonCollapsed) {
				textView.setVisibility(View.GONE);
			}
		}
		updateToIcon(mainView);
	}

	private void updateToIcon(View parentView) {
		ImageView toIcon = (ImageView) parentView.findViewById(R.id.toIcon);
		toIcon.setImageDrawable(getIconOrig(R.drawable.list_destination));
	}

	private void updateStartPointView() {
		MapActivity mapActivity = getMapActivity();
		final View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();

		setupFromText(mainView);
		final View fromLayout = mainView.findViewById(R.id.FromLayout);
		fromLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openAddPointDialog(PointType.START);
			}
		});

		FrameLayout fromButton = (FrameLayout) mainView.findViewById(R.id.from_button);
		final LinearLayout fromButtonContainer = (LinearLayout) mainView.findViewById(R.id.from_button_container);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, fromButton, nightMode, R.drawable.btn_border_rounded_light, R.drawable.btn_border_rounded_dark);
			AndroidUtils.setBackground(app, fromButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, fromButtonContainer, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
		}

		ImageView swapDirectionView = (ImageView) mainView.findViewById(R.id.from_button_image_view);

		Drawable normal = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_change_navigation_points, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_change_navigation_points, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
			normal = AndroidUtils.createPressedStateListDrawable(normal, active);
		}

		swapDirectionView.setImageDrawable(normal);
		fromButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					OsmandApplication app = mapActivity.getMyApplication();
					TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
					TargetPoint startPoint = targetPointsHelper.getPointToStart();
					TargetPoint endPoint = targetPointsHelper.getPointToNavigate();
					if (endPoint == null) {
						app.showShortToastMessage(R.string.mark_final_location_first);
					} else {
						if (startPoint == null) {
							Location loc = app.getLocationProvider().getLastKnownLocation();
							if (loc != null) {
								startPoint = TargetPoint.createStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()),
										new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
												mapActivity.getString(R.string.shared_string_my_location)));
							}
						}
						if (startPoint != null) {
							targetPointsHelper.navigateToPoint(startPoint.point, false, -1, startPoint.getPointDescription(mapActivity));
							targetPointsHelper.setStartPoint(endPoint.point, false, endPoint.getPointDescription(mapActivity));
							targetPointsHelper.updateRouteAndRefresh(true);
						}
					}
				}
			}
		});

		updateFromIcon(mainView);

		final View textView = mainView.findViewById(R.id.from_button_description);
		if (!swapButtonCollapsing && !swapButtonCollapsed &&
				fromButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
			swapButtonCollapsing = true;
			collapseButtonAnimated(R.id.from_button, R.id.from_button_description, new OnButtonCollapsedListener() {
				@Override
				public void onButtonCollapsed(boolean success) {
					swapButtonCollapsing = false;
					swapButtonCollapsed = success;
				}
			});
		} else if (swapButtonCollapsed) {
			textView.setVisibility(View.GONE);
		}
	}

	public void updateFromIcon(View parentView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			((ImageView) parentView.findViewById(R.id.fromIcon)).setImageDrawable(ContextCompat.getDrawable(mapActivity,
					mapActivity.getMyApplication().getTargetPointsHelper().getPointToStart() == null ? R.drawable.ic_action_location_color : R.drawable.list_startpoint));
		}
	}

	private void collapseButtonAnimated(final int containerRes, final int viewRes, final OnButtonCollapsedListener listener) {
		runButtonAnimation(new Runnable() {
			@Override
			public void run() {
				boolean started = false;
				View mainView = getMainView();
				if (isVisible() && mainView != null) {
					ViewGroup container = (ViewGroup) mainView.findViewById(containerRes);
					View v = mainView.findViewById(viewRes);
					if (container != null && v != null && v.getVisibility() == View.VISIBLE) {
						AutoTransition transition = new AutoTransition();
						transition.setStartDelay(BUTTON_ANIMATION_DELAY);
						transition.addListener(new TransitionListenerAdapter() {
							@Override
							public void onTransitionEnd(@NonNull Transition transition) {
								if (listener != null) {
									listener.onButtonCollapsed(true);
								}
							}
						});
						TransitionManager.go(new Scene(container), transition);
						v.setVisibility(View.GONE);
						started = true;
					}
				}
				if (!started) {
					if (listener != null) {
						listener.onButtonCollapsed(false);
					}
				}
			}
		});
	}

	private void restoreCollapsedButtons() {
		swapButtonCollapsed = false;
		editButtonCollapsed = false;
		addButtonCollapsed = false;
	}

	private void cancelButtonsAnimations() {
		Handler animationsHandler = getAnimationsHandler();
		if (animationsHandler != null) {
			animationsHandler.removeCallbacksAndMessages(null);
		}
		swapButtonCollapsing = false;
		editButtonCollapsing = false;
		addButtonCollapsing = false;
	}

	public void selectOnScreen(PointType pointType) {
		selectFromMapTouch = true;
		selectFromMapPointType = pointType;
		selectFromMapMenuState = currentMenuState;
		hide();
	}

	public void selectAddress(String name, LatLon l, PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
			TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
			switch (pointType) {
				case START:
					targets.setStartPoint(l, true, pd);
					break;
				case TARGET:
					targets.navigateToPoint(l, true, -1, pd);
					break;
				case INTERMEDIATE:
					targets.navigateToPoint(l, true, targets.getIntermediatePoints().size(), pd);
					break;
				case HOME:
					targets.setHomePoint(l, pd);
					break;
				case WORK:
					targets.setWorkPoint(l, pd);
					break;
			}
			updateMenu();
		}
	}

	public void setupFields(PointType pointType) {
		View mainView = getMainView();
		if (mainView != null) {
			switch (pointType) {
				case START:
					setupFromText(mainView);
					break;
				case TARGET:
					setupToText(mainView);
					break;
				case INTERMEDIATE:
					break;
				case HOME:
				case WORK:
					setupCards();
					break;
			}
		}
	}

	public void selectMapMarker(final int index, final PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			List<MapMarker> mapMarkers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
			if (index != -1 && mapMarkers.size() > index) {
				MapMarker m = mapMarkers.get(index);
				LatLon point = new LatLon(m.getLatitude(), m.getLongitude());
				TargetPointsHelper targets = mapActivity.getMyApplication().getTargetPointsHelper();
				switch (pointType) {
					case START:
						targets.setStartPoint(point, true, m.getPointDescription(mapActivity));
						break;
					case TARGET:
						targets.navigateToPoint(point, true, -1, m.getPointDescription(mapActivity));
						break;
					case INTERMEDIATE:
						targets.navigateToPoint(point, true, targets.getIntermediatePoints().size(), m.getPointDescription(mapActivity));
						break;
					case HOME:
						targets.setHomePoint(point, m.getPointDescription(mapActivity));
						break;
					case WORK:
						targets.setWorkPoint(point, m.getPointDescription(mapActivity));
						break;
				}
				updateMenu();
			} else {
				MapMarkerSelectionFragment selectionFragment = MapMarkerSelectionFragment.newInstance(pointType);
				selectionFragment.show(mapActivity.getSupportFragmentManager(), MapMarkerSelectionFragment.TAG);
			}
		}
	}

	private void openAddPointDialog(PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Bundle args = new Bundle();
			args.putString(AddPointBottomSheetDialog.POINT_TYPE_KEY, pointType.name());
			AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
			fragment.setArguments(args);
			fragment.show(mapActivity.getSupportFragmentManager(), AddPointBottomSheetDialog.TAG);
		}
	}

	private boolean isLight() {
		return !nightMode;
	}

	@Nullable
	private Drawable getIconOrig(int iconId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();
			return iconsCache.getIcon(iconId, 0);
		} else {
			return null;
		}
	}

	public static int getDirectionInfo() {
		return directionInfo;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
	}

	public WeakReference<MapRouteInfoMenuFragment> findMenuFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapRouteInfoMenuFragment.TAG);
			if (fragment instanceof MapRouteInfoMenuFragment && !((MapRouteInfoMenuFragment) fragment).isPaused()) {
				return new WeakReference<>((MapRouteInfoMenuFragment) fragment);
			}
		}
		return null;
	}

	public static void showLocationOnMap(MapActivity mapActivity, double latitude, double longitude) {
		RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
		int tileBoxWidthPx = 0;
		int tileBoxHeightPx = 0;

		MapRouteInfoMenu routeInfoMenu = mapActivity.getMapRouteInfoMenu();
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = routeInfoMenu.findMenuFragment();
		if (fragmentRef != null) {
			MapRouteInfoMenuFragment f = fragmentRef.get();
			if (mapActivity.isLandscapeLayout()) {
				tileBoxWidthPx = tb.getPixWidth() - f.getWidth();
			} else {
				tileBoxHeightPx = tb.getPixHeight() - f.getHeight();
			}
		}
		mapActivity.getMapView().fitLocationToMap(latitude, longitude, mapActivity.getMapView().getZoom(),
				tileBoxWidthPx, tileBoxHeightPx, AndroidUtils.dpToPx(mapActivity, 40f), true);
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		directionInfo = -1;
		updateMenu();
		if (isVisible()) {
			showToast.value = false;
		}
	}

	public String generateViaDescription() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			TargetPointsHelper targets = app.getTargetPointsHelper();
			List<TargetPoint> points = targets.getIntermediatePointsNavigation();
			if (points.size() == 0) {
				return "";
			}
			StringBuilder via = new StringBuilder();
			for (int i = 0; i < points.size(); i++) {
				if (i > 0) {
					via.append(" ");
				}
				TargetPoint p = points.get(i);
				String description = p.getOnlyName();
				via.append(getRoutePointDescription(p.point, description));
				boolean needAddress = new PointDescription(PointDescription.POINT_TYPE_LOCATION, description).isSearchingAddress(mapActivity)
						&& !intermediateRequestsLatLon.contains(p.point);
				if (needAddress) {
					AddressLookupRequest lookupRequest = new AddressLookupRequest(p.point, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							updateMenu();
						}
					}, null);
					intermediateRequestsLatLon.add(p.point);
					app.getGeocodingLookupService().lookupAddress(lookupRequest);
				}
			}
			return via.toString();
		}
		return "";
	}

	public String getRoutePointDescription(double lat, double lon) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			return mapActivity.getString(R.string.route_descr_lat_lon, lat, lon);
		}
		return "";
	}

	public String getRoutePointDescription(LatLon l, String d) {
		if (d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		MapActivity mapActivity = getMapActivity();
		if (l != null && mapActivity != null) {
			return mapActivity.getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
		}
		return "";
	}

	private void setupFromText(View view) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			TargetPoint start = mapActivity.getMyApplication().getTargetPointsHelper().getPointToStart();
			String name = null;
			if (start != null) {
				name = start.getOnlyName().length() > 0 ? start.getOnlyName() :
						(mapActivity.getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));

				final LatLon latLon = start.point;
				final PointDescription pointDescription = start.getOriginalPointDescription();
				boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
				cancelStartPointAddressRequest();
				if (needAddress) {
					startPointRequest = new AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							startPointRequest = null;
							updateMenu();
						}
					}, null);
					mapActivity.getMyApplication().getGeocodingLookupService().lookupAddress(startPointRequest);
				}
			}

			final TextView fromText = ((TextView) view.findViewById(R.id.fromText));
			if (start != null) {
				fromText.setText(name);
			} else {
				fromText.setText(R.string.shared_string_my_location);
			}
		}
	}

	private void setupToText(View view) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			final TextView toText = ((TextView) view.findViewById(R.id.toText));
			final TargetPointsHelper targets = app.getTargetPointsHelper();
			TargetPoint finish = targets.getPointToNavigate();
			if (finish != null) {
				toText.setText(getRoutePointDescription(targets.getPointToNavigate().point,
						targets.getPointToNavigate().getOnlyName()));

				final LatLon latLon = finish.point;
				final PointDescription pointDescription = finish.getOriginalPointDescription();
				boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
				cancelTargetPointAddressRequest();
				if (needAddress) {
					targetPointRequest = new AddressLookupRequest(latLon, new GeocodingLookupService.OnAddressLookupResult() {
						@Override
						public void geocodingDone(String address) {
							targetPointRequest = null;
							updateMenu();
						}
					}, null);
					app.getGeocodingLookupService().lookupAddress(targetPointRequest);
				}

			} else {
				toText.setText(R.string.route_descr_select_destination);
			}
		}
	}

	@Override
	public void routeWasCancelled() {
		directionInfo = -1;
		// do not hide fragment (needed for use case entering Planning mode without destination)
	}

	@Override
	public void routeWasFinished() {
	}

	public void onDismiss() {
		cancelButtonsAnimations();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapView().setMapPositionX(0);
			mapActivity.getMapView().refreshMap();
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
			if (switched) {
				mapActivity.getMapLayers().getMapControlsLayer().switchToRouteFollowingLayout();
			}
			if (mapActivity.getPointToNavigate() == null && !selectFromMapTouch) {
				mapActivity.getMapActions().stopNavigationWithoutConfirm();
			}
		}
		if (onDismissListener != null) {
			onDismissListener.onDismiss(null);
		}
		removeTargetPointListener();
	}

	public void show() {
		show(getInitialMenuState());
	}

	public void show(int menuState) {
		if (menuState == DEFAULT_MENU_STATE) {
			menuState = getInitialMenuState();
		}
		MapActivity mapActivity = getMapActivity();
		if (!isVisible() && mapActivity != null) {
			currentMenuState = menuState;
			switched = mapActivity.getMapLayers().getMapControlsLayer().switchToRoutePlanningLayout();
			boolean refreshMap = !switched;
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				mapActivity.getMapView().setMapPositionX(1);
				refreshMap = true;
			}

			if (refreshMap) {
				mapActivity.refreshMap();
			}

			MapRouteInfoMenuFragment.showInstance(mapActivity);

			if (!AndroidUiHelper.isXLargeDevice(mapActivity)) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			}
			if (!portrait) {
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
			}
		}
	}

	public void hide() {
		cancelButtonsAnimations();
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		}
		OsmandApplication app = getApp();
		if (app != null) {
			app.getRoutingHelper().removeListener(this);
		}
		removeTargetPointListener();
	}

	public void setShowMenu(int menuState) {
		showMenu = true;
		showMenuState = menuState;
	}

	public enum PermanentAppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY, AvoidRoadsRoutingParameter.KEY),
		BICYCLE(MuteSoundRoutingParameter.KEY, DRIVING_STYLE, GeneralRouter.USE_HEIGHT_OBSTACLES),
		PEDESTRIAN(MuteSoundRoutingParameter.KEY, GeneralRouter.USE_HEIGHT_OBSTACLES);

		List<String> routingParameters;

		PermanentAppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}