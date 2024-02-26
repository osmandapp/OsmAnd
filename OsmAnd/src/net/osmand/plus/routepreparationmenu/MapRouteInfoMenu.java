package net.osmand.plus.routepreparationmenu;


import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_APP_MODES_OPTIONS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.NAVIGATION_OPTIONS_MENU_ID;

import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.transition.AutoTransition;
import androidx.transition.Scene;
import androidx.transition.Transition;
import androidx.transition.TransitionListenerAdapter;
import androidx.transition.TransitionManager;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.SpecialPointType;
import net.osmand.data.ValueHolder;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.map.WorldRegion;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenuFragment;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkerSelectionFragment;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.myplaces.favorites.FavoritesListener;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.ConfigureAppModesBottomSheetDialogFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidPTTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.RouteMenuAppModes;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.ShowAlongTheRouteItem;
import net.osmand.plus.routepreparationmenu.cards.AttachTrackToRoadsBannerCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.routepreparationmenu.cards.HistoryCard;
import net.osmand.plus.routepreparationmenu.cards.HomeWorkCard;
import net.osmand.plus.routepreparationmenu.cards.LongDistanceWarningCard;
import net.osmand.plus.routepreparationmenu.cards.MapMarkersCard;
import net.osmand.plus.routepreparationmenu.cards.NauticalBridgeHeightWarningCard;
import net.osmand.plus.routepreparationmenu.cards.PedestrianRouteCard;
import net.osmand.plus.routepreparationmenu.cards.PreviousRouteCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportBetaWarningCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportNotFoundSettingsWarningCard;
import net.osmand.plus.routepreparationmenu.cards.PublicTransportNotFoundWarningCard;
import net.osmand.plus.routepreparationmenu.cards.SimpleRouteCard;
import net.osmand.plus.routepreparationmenu.cards.SuggestionsMapsDownloadWarningCard;
import net.osmand.plus.routepreparationmenu.cards.TrackEditCard;
import net.osmand.plus.routepreparationmenu.cards.TracksCard;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.settings.fragments.RouteLineAppearanceFragment;
import net.osmand.plus.settings.fragments.voice.VoiceLanguageBottomSheetFragment;
import net.osmand.plus.track.SelectTrackTabsFragment;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet;
import net.osmand.plus.track.fragments.TrackSelectSegmentBottomSheet.OnSegmentSelectedListener;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.TextViewExProgress;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.TransportRouteResult;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class MapRouteInfoMenu implements IRouteInformationListener, CardListener, FavoritesListener {

	private static final Log LOG = PlatformUtil.getLog(MapRouteInfoMenu.class);

	private static final int BUTTON_ANIMATION_DELAY = 2000;
	public static final int DEFAULT_MENU_STATE = 0;
	private static final int MAX_PEDESTRIAN_ROUTE_DURATION = 30 * 60;

	public static int directionInfo = -1;
	public static boolean chooseRoutesVisible;
	public static boolean waypointsVisible;
	public static boolean followTrackVisible;

	private final Stack<MapRouteMenuStateHolder> menuBackStack = new Stack<>();

	private boolean routeCalculationInProgress;

	private boolean selectFromMapTouch;
	private PointType selectFromMapPointType;
	private int selectFromMapMenuState = MenuState.HEADER_ONLY;
	private boolean selectFromMapWaypoints;
	private boolean selectFromTracks;
	private boolean customizingRouteLine;

	private boolean showMenu;
	private int showMenuState = DEFAULT_MENU_STATE;

	@Nullable
	private MapActivity mapActivity;
	@Nullable
	private OsmandApplication app;
	@Nullable
	private Handler animationsHandler;

	private boolean nightMode;
	private boolean switched;
	private boolean routeSelected;
	private boolean currentMuteState;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;
	private final List<LatLon> intermediateRequestsLatLon = new ArrayList<>();
	private OnDismissListener onDismissListener;
	private List<BaseCard> menuCards = new ArrayList<>();

	private final OnMarkerSelectListener onMarkerSelectListener;
	private final StateChangedListener<Void> onStateChangedListener;
	private final StateChangedListener<Boolean> voiceMuteChangeListener;
	@Nullable
	private View mainView;

	private boolean portraitMode;

	private boolean swapButtonCollapsing;
	private boolean swapButtonCollapsed;
	private boolean editButtonCollapsing;
	private boolean editButtonCollapsed;
	private boolean addButtonCollapsing;
	private boolean addButtonCollapsed;

	private List<WorldRegion> suggestedMaps;
	private boolean suggestedMapsOnlineSearch;

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
		WORK,
		PARKING
	}

	public MapRouteInfoMenu() {
		onMarkerSelectListener = new OnMarkerSelectListener() {
			@Override
			public void onSelect(int index, PointType pointType) {
				selectMapMarker(index, pointType);
			}
		};
		onStateChangedListener = new StateChangedListener<Void>() {
			@Override
			public void stateChanged(Void change) {
				if (app != null) {
					app.runInUIThread(() -> updateMenu());
				}
			}
		};
		voiceMuteChangeListener = new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				if (app != null) {
					app.runInUIThread(() -> updateWhenMuteChanged());
				}
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

	public int getInitialMenuState() {
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
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (selectFromMapTouch) {
				selectFromMapTouch = false;

				Pair<LatLon, PointDescription> pair = getObjectLocation(mapActivity.getMapView(), point, tileBox);
				LatLon selectedPoint;
				PointDescription name = null;
				if (pair != null) {
					selectedPoint = pair.first;
					name = pair.second;
				} else {
					MapRendererView mapRenderer = mapActivity.getMapView().getMapRenderer();
					selectedPoint = NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tileBox, point);
				}
				choosePointTypeAction(selectedPoint, selectFromMapPointType, name, null);
				if (selectFromMapWaypoints) {
					WaypointsFragment.showInstance(mapActivity.getSupportFragmentManager(), true);
				} else {
					show(selectFromMapMenuState);
				}
				return true;
			}
		}
		return false;
	}

	@Nullable
	private Pair<LatLon, PointDescription> getObjectLocation(OsmandMapTileView mapView, PointF point, RotatedTileBox tileBox) {
		for (OsmandMapLayer layer : mapView.getLayers()) {
			if (layer instanceof IContextMenuProvider) {
				List<Object> objects = new ArrayList<>();
				IContextMenuProvider provider = (IContextMenuProvider) layer;
				provider.collectObjectsFromPoint(point, tileBox, objects, true, true);
				for (Object o : objects) {
					LatLon latLon = provider.getObjectLocation(o);
					PointDescription name = null;
					if (o instanceof FavouritePoint) {
						name = ((FavouritePoint) o).getPointDescription(mapView.getApplication());
					}
					return new Pair<>(latLon, name);
				}
			}
		}
		return null;
	}

	private void choosePointTypeAction(LatLon latLon, PointType pointType, PointDescription pd, String address) {
		OsmandApplication app = getApp();
		FavouritesHelper favorites = app.getFavoritesHelper();
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		switch (pointType) {
			case START:
				targetPointsHelper.setStartPoint(latLon, true, pd);
				break;
			case TARGET:
				targetPointsHelper.navigateToPoint(latLon, true, -1, pd);
				break;
			case INTERMEDIATE:
				targetPointsHelper.navigateToPoint(latLon, true, targetPointsHelper.getIntermediatePoints().size(), pd);
				break;
			case HOME:
				favorites.setSpecialPoint(latLon, SpecialPointType.HOME, address);
				break;
			case WORK:
				favorites.setSpecialPoint(latLon, SpecialPointType.WORK, address);
				break;
		}
	}

	public OnMarkerSelectListener getOnMarkerSelectListener() {
		return onMarkerSelectListener;
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

	public boolean isRouteSelected() {
		return routeSelected;
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
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			return fragmentRef.get().getCurrentMenuState();
		}
		return getInitialMenuState();
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

	public void showHideMenu() {
		intermediateRequestsLatLon.clear();
		if (isVisible()) {
			hide();
		} else {
			show();
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

	public void routeCalculationStarted() {
		setRouteCalculationInProgress(true);
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		MapRouteInfoMenuFragment fragment = fragmentRef != null ? fragmentRef.get() : null;
		if (fragmentRef != null && fragment.isVisible()) {
			fragment.updateRouteCalculationProgress(0);
			fragment.updateInfo();
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
		}
	}

	public void routeCalculationFinished() {
		OsmandApplication app = getApp();
		if (app != null) {
			RouteCalculationResult route = app.getRoutingHelper().getRoute();
			boolean routeCalculating = app.getRoutingHelper().isRouteBeingCalculated() || app.getTransportRoutingHelper().isRouteBeingCalculated();

			WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
			MapRouteInfoMenuFragment fragment = fragmentRef != null ? fragmentRef.get() : null;

			boolean calculationStatusChanged = setRouteCalculationInProgress(routeCalculating);
			if (fragmentRef != null && fragment.isVisible()) {
				if (routeCalculating && route.isCalculated() && route.isInitialCalculation()) {
					openMenuAfterCalculation(fragment, app);
				}
				if (calculationStatusChanged) {
					fragment.updateInfo();
					if (!routeCalculationInProgress) {
						fragment.hideRouteCalculationProgressBar();
						openMenuAfterCalculation(fragment, app);
					}
				}
			}
		}
	}

	private void openMenuAfterCalculation(MapRouteInfoMenuFragment fragment, OsmandApplication app) {
		if (!app.getSettings().OPEN_ONLY_HEADER_STATE_ROUTE_CALCULATED.getModeValue(app.getRoutingHelper().getAppMode())
				|| app.getRoutingHelper().getRoute().hasMissingMaps()) {
			fragment.openMenuHalfScreen();
		} else {
			fragment.openMenuHeaderOnly();
		}
	}

	public void openMenuFullScreen() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().openMenuFullScreen();
		}
	}

	public void updateSuggestedMissingMaps(@Nullable List<WorldRegion> missingMaps, boolean onlineSearch) {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		MapRouteInfoMenuFragment fragment = fragmentRef != null ? fragmentRef.get() : null;
		if (fragmentRef != null && fragment.isVisible()) {
			boolean updated = !Algorithms.objectEquals(missingMaps, suggestedMaps) || suggestedMapsOnlineSearch != onlineSearch;
			if (updated) {
				suggestedMaps = missingMaps;
				suggestedMapsOnlineSearch = onlineSearch;
				fragment.updateInfo();
			}
		}
	}

	public List<WorldRegion> getSuggestedMaps() {
		return suggestedMaps;
	}

	public boolean isSuggestedMapsOnlineSearch() {
		return suggestedMapsOnlineSearch;
	}

	public void clearSuggestedMissingMaps() {
		suggestedMaps = null;
		suggestedMapsOnlineSearch = false;
	}

	public void openMenuHeaderOnly() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().openMenuHeaderOnly();
		}
	}

	private void updateWhenMuteChanged() {
		if (app != null) {
			ApplicationMode mode = app.getRoutingHelper().getAppMode();
			boolean changedState = app.getSettings().VOICE_MUTE.getModeValue(mode);
			if (changedState != currentMuteState) {
				currentMuteState = changedState;
				updateMenu();
			}
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
		updateViaView();
		updateFinishPointView();

		updateApplicationModes();
		updateApplicationModesOptions();
		updateCards();
		updateOptionsButtons();
	}

	private void applyCardsState(@NonNull List<BaseCard> newCards, @NonNull List<BaseCard> prevCards) {
		for (BaseCard newCard : newCards) {
			for (BaseCard prevCard : prevCards) {
				if (newCard.getClass() == prevCard.getClass()) {
					newCard.applyState(prevCard);
					break;
				}
			}
		}
	}

	private void updateCards() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}

		OsmandApplication app = mapActivity.getMyApplication();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		RoutingHelper routingHelper = app.getRoutingHelper();

		boolean hasPrecalculatedMissingMaps = hasPrecalculatedMissingMaps();
		boolean hasCalculatedMissingMaps = hasCalculatedMissingMaps(app);

		List<BaseCard> menuCards = new ArrayList<>();

		boolean bottomShadowVisible = true;
		if (isBasicRouteCalculated()) {
			GPXFile gpx = GpxUiHelper.makeGpxFromRoute(routingHelper.getRoute(), app);
			SimpleRouteCard simpleRouteCard = new SimpleRouteCard(mapActivity, gpx);
			simpleRouteCard.setListener(this);
			menuCards.add(simpleRouteCard);
			bottomShadowVisible = false;
		} else if (isTransportRouteCalculated()) {
			TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
			List<TransportRouteResult> routes = transportRoutingHelper.getRoutes();
			if (routes != null && routes.size() > 0) {
				TransportRouteResult route = routes.get(0);
				int walkTimeReal = transportRoutingHelper.getWalkingTime(route.getSegments());
				int walkTimePT = (int) route.getWalkTime();
				int walkTime = walkTimeReal > 0 ? walkTimeReal : walkTimePT;
				int travelTime = (int) route.getTravelTime() + walkTime;
				LatLon startLocation = transportRoutingHelper.getStartLocation();
				LatLon endLocation = transportRoutingHelper.getEndLocation();
				int approxPedestrianTime = (int) MapUtils.getDistance(startLocation, endLocation);
				boolean showPedestrianCard = approxPedestrianTime < travelTime + 60 && approxPedestrianTime < MAX_PEDESTRIAN_ROUTE_DURATION;
				boolean hasTopCard = false;
				if (routes.size() > 1 && routeSelected) {
					int currentRoute = app.getTransportRoutingHelper().getCurrentRoute();
					if (currentRoute >= 0 && currentRoute < routes.size()) {
						route = routes.get(currentRoute);
						PublicTransportCard card = new PublicTransportCard(mapActivity, startLocation, endLocation, route, currentRoute);
						card.setRouteInfoVisible(false);
						card.setRouteButtonsVisible(false);
						card.setShowBottomShadow(false);
						card.setShowTopShadow(false);
						card.setListener(this);
						menuCards.add(card);
						hasTopCard = true;
					}
				}
				for (int i = 0; i < routes.size(); i++) {
					route = routes.get(i);
					PublicTransportCard card = new PublicTransportCard(mapActivity, startLocation, endLocation, route, i);
					card.setShowButtonCustomTitle(mapActivity.getString(R.string.shared_string_show_on_map));
					card.setShowBottomShadow(i == routes.size() - 1 && !showPedestrianCard);
					card.setShowTopShadow(i != 0 || hasTopCard);
					card.setListener(this);
					menuCards.add(card);
				}
				if (showPedestrianCard) {
					PedestrianRouteCard pedestrianRouteCard = new PedestrianRouteCard(mapActivity, approxPedestrianTime);
					pedestrianRouteCard.setListener(this);
					menuCards.add(pedestrianRouteCard);
				}
				bottomShadowVisible = routes.size() == 0;
			} else {
				RouteMenuAppModes mode = app.getRoutingOptionsHelper().getRouteMenuAppMode(routingHelper.getAppMode());
				if (mode != null) {
					boolean avoidPTTypesCustomized = false;
					for (LocalRoutingParameter parameter : mode.parameters) {
						if (parameter instanceof AvoidPTTypesRoutingParameter) {
							avoidPTTypesCustomized = true;
							break;
						}
					}
					if (avoidPTTypesCustomized) {
						PublicTransportNotFoundSettingsWarningCard warningCard = new PublicTransportNotFoundSettingsWarningCard(mapActivity);
						warningCard.setListener(this);
						menuCards.add(warningCard);
					} else {
						PublicTransportNotFoundWarningCard warningCard = new PublicTransportNotFoundWarningCard(mapActivity);
						warningCard.setListener(this);
						menuCards.add(warningCard);
					}
				}
			}
		} else if (routeCalculationInProgress) {
			if (app.getRoutingHelper().isPublicTransportMode()) {
				menuCards.add(new PublicTransportBetaWarningCard(mapActivity));
			} else if (app.getRoutingHelper().isBoatMode()) {
				menuCards.add(new NauticalBridgeHeightWarningCard(mapActivity));
			} else if (hasPrecalculatedMissingMaps || suggestedMapsOnlineSearch) {
				menuCards.add(new SuggestionsMapsDownloadWarningCard(mapActivity));
			} else if (app.getTargetPointsHelper().hasTooLongDistanceToNavigate() && !hasCalculatedMissingMaps) {
				menuCards.add(new LongDistanceWarningCard(mapActivity));
			}
		} else {
			if (hasCalculatedMissingMaps) {
				menuCards.add(new SuggestionsMapsDownloadWarningCard(mapActivity));
			} else {
				// Home/work card
				HomeWorkCard homeWorkCard = new HomeWorkCard(mapActivity);
				menuCards.add(homeWorkCard);

				// Previous route card
				boolean historyEnabled = app.getSettings().NAVIGATION_HISTORY.get();
				if (historyEnabled) {
					TargetPoint startBackup = targetPointsHelper.getPointToStartBackup();
					if (startBackup == null) {
						startBackup = targetPointsHelper.getMyLocationToStart();
					}
					TargetPoint destinationBackup = targetPointsHelper.getPointToNavigateBackup();
					if (startBackup != null && destinationBackup != null) {
						PreviousRouteCard previousRouteCard = new PreviousRouteCard(mapActivity);
						previousRouteCard.setListener(this);
						menuCards.add(previousRouteCard);
					}
				}

				// Gpx card
				List<SelectedGpxFile> selectedGPXFiles = app.getSelectedGpxHelper().getSelectedGPXFiles();
				List<GPXFile> gpxFiles = new ArrayList<>();
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

				// Map markers card
				List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
				if (mapMarkers.size() > 0) {
					MapMarkersCard mapMarkersCard = new MapMarkersCard(mapActivity, mapMarkers);
					mapMarkersCard.setListener(this);
					menuCards.add(mapMarkersCard);
				}

				// History card
				if (historyEnabled) {
					SearchHistoryHelper historyHelper = SearchHistoryHelper.getInstance(app);
					List<SearchResult> results = historyHelper.getHistoryResults(HistorySource.NAVIGATION, true, false);
					if (!Algorithms.isEmpty(results)) {
						HistoryCard historyCard = new HistoryCard(mapActivity, results);
						historyCard.setListener(this);
						menuCards.add(historyCard);
					}
				}
			}
		}
		applyCardsState(menuCards, this.menuCards);
		this.menuCards = menuCards;
		setBottomShadowVisible(bottomShadowVisible);
		setupCards();
	}

	private boolean hasPrecalculatedMissingMaps() {
		return !Algorithms.isEmpty(suggestedMaps);
	}

	private boolean hasCalculatedMissingMaps(@NonNull OsmandApplication app) {
		return !Algorithms.isEmpty(app.getRoutingHelper().getRoute().getMissingMaps());
	}

	private void setupCards() {
		View mainView = getMainView();
		if (mainView != null) {
			LinearLayout cardsContainer = mainView.findViewById(R.id.route_menu_cards_container);
			build(cardsContainer);
		}
	}

	@Override
	public void onCardLayoutNeeded(@NonNull BaseCard card) {
		updateLayout();
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (card instanceof SimpleRouteCard) {
				hide();
				ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(), 0, MenuState.FULL_SCREEN);
			} else if (card instanceof PublicTransportCard) {
				hide();
				ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(),
						((PublicTransportCard) card).getRouteId(), MenuState.FULL_SCREEN);
			}
		}
	}

	@Override
	public void onCardButtonPressed(@NonNull BaseCard card, int buttonIndex) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (card instanceof PreviousRouteCard) {
				ApplicationMode lastAppMode = app.getSettings().LAST_ROUTE_APPLICATION_MODE.get();
				ApplicationMode currentAppMode = app.getRoutingHelper().getAppMode();
				if (lastAppMode == ApplicationMode.DEFAULT) {
					lastAppMode = currentAppMode;
				}
				updateApplicationMode(currentAppMode, lastAppMode);
				updateFinishPointView();
				updateOptionsButtons();

				app.getTargetPointsHelper().restoreTargetPoints(true);
			} else if (card instanceof PublicTransportCard) {
				if (buttonIndex == PublicTransportCard.DETAILS_BUTTON_INDEX) {
					hide();
					ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(),
							((PublicTransportCard) card).getRouteId(), MenuState.FULL_SCREEN);
				} else if (buttonIndex == PublicTransportCard.SHOW_BUTTON_INDEX) {
					showRouteOnMap(mapActivity, ((PublicTransportCard) card).getRouteId());
				}
			} else if (card instanceof PublicTransportNotFoundWarningCard) {
				updateApplicationMode(null, ApplicationMode.PEDESTRIAN);
			} else if (card instanceof PublicTransportNotFoundSettingsWarningCard) {
				AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
				avoidRoadsFragment.setHideImpassableRoads(true);
				avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
			} else if (card instanceof PedestrianRouteCard) {
				updateApplicationMode(null, ApplicationMode.PEDESTRIAN);
			} else if (card instanceof AttachTrackToRoadsBannerCard) {
				if (MeasurementToolFragment.showSnapToRoadsDialog(mapActivity, true)) {
					hide();
				}
			}
		}
	}

	public void selectTrack(@NonNull GPXFile gpxFile, boolean showSelectionDialog) {
		selectTrack(gpxFile, showSelectionDialog, null);
	}

	public void selectTrack(@NonNull GPXFile gpxFile, boolean showSelectionDialog, @Nullable OnSegmentSelectedListener onSegmentSelectedListener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			List<WptPt> points = gpxFile.getRoutePoints();
			if (!points.isEmpty()) {
				ApplicationMode mode = ApplicationMode.valueOfStringKey(points.get(0).getProfileType(), null);
				if (mode != null) {
					app.getRoutingHelper().setAppMode(mode);
					app.initVoiceCommandPlayer(mapActivity, mode, null,
							true, false, false, true);
				}
			}
			if (showSelectionDialog && TrackSelectSegmentBottomSheet.shouldShowForGpxFile(gpxFile)) {
				FragmentManager manager = mapActivity.getSupportFragmentManager();
				if (onSegmentSelectedListener == null) {
					onSegmentSelectedListener = (MapRouteInfoMenuFragment) manager.findFragmentByTag(MapRouteInfoMenuFragment.TAG);
				}
				if (onSegmentSelectedListener == null) {
					onSegmentSelectedListener = (FollowTrackFragment) manager.findFragmentByTag(FollowTrackFragment.TAG);
				}
				TrackSelectSegmentBottomSheet.showInstance(manager, gpxFile, onSegmentSelectedListener);
			} else {
				mapActivity.getMapActions().setGPXRouteParams(gpxFile);
				app.getTargetPointsHelper().updateRouteAndRefresh(true);
			}
		}
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

	public boolean hasTransportRoutes() {
		OsmandApplication app = getApp();
		if (app != null) {
			List<TransportRouteResult> routes = app.getTransportRoutingHelper().getRoutes();
			return routes != null && routes.size() > 0;
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
			AppCompatImageView foldButtonView = mainView.findViewById(R.id.fold_button);
			foldButtonView.setImageResource(getCurrentMenuState() == MenuState.HEADER_ONLY ?
					R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down);
			foldButtonView.setOnClickListener(v -> expandCollapse());

			OsmAndAppCustomization customization = getApp().getAppCustomization();
			View options = mainView.findViewById(R.id.app_modes_options);
			options.setOnClickListener(v -> showProfileBottomSheetDialog());
			AndroidUiHelper.updateVisibility(options, customization.isFeatureEnabled(NAVIGATION_APP_MODES_OPTIONS_ID));
		}
	}

	private void expandCollapse() {
		if (getCurrentMenuState() == MenuState.HEADER_ONLY) {
			openMenuFullScreen();
		} else {
			openMenuHeaderOnly();
		}
		updateApplicationModesOptions();
	}

	private void showProfileBottomSheetDialog() {
		FragmentActivity activity = getMapActivity();
		if (activity != null) {
			FragmentManager manager = activity.getSupportFragmentManager();
			ConfigureAppModesBottomSheetDialogFragment.showInstance(manager, true, this::updateApplicationModes);
		}
	}

	private void updateApplicationMode(ApplicationMode mode, ApplicationMode next) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			RoutingHelper routingHelper = app.getRoutingHelper();
			routingHelper.setAppMode(next);
			if (app.getSettings().getApplicationMode() != ApplicationMode.DEFAULT) {
				app.getSettings().setApplicationMode(next, false);
			}
			app.initVoiceCommandPlayer(mapActivity, next, null, true,
					false, true, true);
			routingHelper.onSettingsChanged(true);
		}
	}

	private void updateApplicationModes() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		int layoutDirection = AndroidUtils.getLayoutDirection(app);
		ApplicationMode am = app.getRoutingHelper().getAppMode();
		Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = mainView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		OnClickListener listener = v -> {
			MapActivity mapActivity1 = getMapActivity();
			if (mapActivity1 != null) {
				if (selected.size() > 0) {
					ApplicationMode next = selected.iterator().next();
					updateApplicationMode(am, next);
				}
				updateFinishPointView();
				updateOptionsButtons();
			}
		};
		List<ApplicationMode> values = new ArrayList<>(ApplicationMode.values(app));
		values.remove(ApplicationMode.DEFAULT);

		if (values.size() > 0 && !values.contains(am)) {
			ApplicationMode next = values.iterator().next();
			updateApplicationMode(am, next);
		}

		View ll = mapActivity.getLayoutInflater().inflate(R.layout.mode_toggles, vg);
		ll.setBackgroundColor(ColorUtilities.getCardAndListBackgroundColor(mapActivity, nightMode));

		HorizontalScrollView scrollView = ll.findViewById(R.id.app_modes_scroll_container);
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		int contentPadding = mapActivity.getResources().getDimensionPixelSize(R.dimen.content_padding);
		int contentPaddingHalf = mapActivity.getResources().getDimensionPixelSize(R.dimen.content_padding_half);
		int startTogglePadding = layoutDirection == View.LAYOUT_DIRECTION_LTR ? contentPaddingHalf : contentPadding;
		int endTogglePadding = layoutDirection == View.LAYOUT_DIRECTION_LTR ? contentPadding : contentPaddingHalf;
		View[] buttons = new View[values.size()];
		int k = 0;
		Iterator<ApplicationMode> iterator = values.iterator();
		boolean firstMode = true;
		while (iterator.hasNext()) {
			ApplicationMode mode = iterator.next();
			View toggle = AppModeDialog.createToggle(mapActivity.getLayoutInflater(), app, R.layout.mode_view_route_preparation,
					ll.findViewById(R.id.app_modes_content), mode, true);

			toggle.setAccessibilityDelegate(new View.AccessibilityDelegate() {
				public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
					super.onInitializeAccessibilityNodeInfo(host, info);
					info.setContentDescription(mode.toHumanString());
					info.setEnabled(host.isEnabled());
				}
			});

			if (toggle.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
				ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) toggle.getLayoutParams();
				if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
					if (firstMode) {
						firstMode = false;
						p.setMargins(p.leftMargin, p.topMargin, p.rightMargin + startTogglePadding, p.bottomMargin);
					}
					if (!iterator.hasNext()) {
						p.setMargins(p.leftMargin + endTogglePadding, p.topMargin, p.rightMargin, p.bottomMargin);
					}
				} else { // LTR
					if (firstMode) {
						firstMode = false;
						p.setMargins(p.leftMargin + startTogglePadding, p.topMargin, p.rightMargin, p.bottomMargin);
					}
					if (!iterator.hasNext()) {
						p.setMargins(p.leftMargin, p.topMargin, p.rightMargin + endTogglePadding, p.bottomMargin);
					}
				}
			}

			buttons[k++] = toggle;
		}
		for (int i = 0; i < buttons.length; i++) {
			AppModeDialog.updateButtonStateForRoute(app, values, selected, listener, buttons, i, true, true, nightMode);
		}

		ApplicationMode activeMode = app.getRoutingHelper().getAppMode();

		int idx = values.indexOf(activeMode);

		OnGlobalLayoutListener globalListener = new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				LinearLayout container = ll.findViewById(R.id.app_modes_content);
				int s = container.getChildAt(idx) != null ? container.getChildAt(idx).getRight() + endTogglePadding : 0;
				scrollView.scrollTo(Math.max(s - scrollView.getWidth(), 0), 0);
				ll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		};
		ll.getViewTreeObserver().addOnGlobalLayoutListener(globalListener);
	}

	private void updateOptionsButtons() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		ApplicationMode applicationMode = routingHelper.getAppMode();
		RouteMenuAppModes mode = app.getRoutingOptionsHelper().getRouteMenuAppMode(applicationMode);
		boolean isLayoutRTL = AndroidUtils.isLayoutRtl(app);

		updateControlButtons(mapActivity, mainView);
		LinearLayout optionsButton = mainView.findViewById(R.id.map_options_route_button);

		OsmAndAppCustomization customization = app.getAppCustomization();
		AndroidUiHelper.updateVisibility(optionsButton, customization.isFeatureEnabled(NAVIGATION_OPTIONS_MENU_ID));

		TextView optionsTitle = mainView.findViewById(R.id.map_options_route_button_title);
		ImageView optionsIcon = mainView.findViewById(R.id.map_options_route_button_icon);

		setupButtonIcon(optionsIcon, R.drawable.ic_action_settings);
		optionsButton.setOnClickListener(v -> clickRouteParams());
		AndroidUtils.setBackground(app, optionsButton, nightMode,
				isLayoutRTL ? R.drawable.route_info_trans_gradient_left_light : R.drawable.route_info_trans_gradient_light,
				isLayoutRTL ? R.drawable.route_info_trans_gradient_left_dark : R.drawable.route_info_trans_gradient_dark);

		HorizontalScrollView scrollView = mainView.findViewById(R.id.route_options_scroll_container);
		scrollView.setVerticalScrollBarEnabled(false);
		scrollView.setHorizontalScrollBarEnabled(false);

		LinearLayout optionsContainer = mainView.findViewById(R.id.route_options_container);
		optionsContainer.removeAllViews();
		if (mode == null) {
			return;
		}
		createRoutingParametersButtons(mapActivity, mode, optionsContainer);
		int endPadding = mapActivity.getResources().getDimensionPixelSize(R.dimen.action_bar_image_side_margin);
		if (mode.parameters.size() > 2) {
			optionsTitle.setVisibility(View.GONE);
		} else {
			optionsTitle.setVisibility(View.VISIBLE);
			endPadding += AndroidUtils.getTextWidth(app.getResources().getDimensionPixelSize(R.dimen.text_button_text_size), app.getString(R.string.shared_string_options));
		}
		if (AndroidUtils.isLayoutRtl(app)) {
			optionsContainer.setPadding(endPadding, optionsContainer.getPaddingTop(), optionsContainer.getPaddingRight(), optionsContainer.getPaddingBottom());
		} else { // LTR
			optionsContainer.setPadding(optionsContainer.getPaddingLeft(), optionsContainer.getPaddingTop(), endPadding, optionsContainer.getPaddingBottom());
		}
	}

	private void updateControlButtons(MapActivity mapActivity, View mainView) {
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper helper = app.getRoutingHelper();
		TargetPointsHelper targetHelper = app.getTargetPointsHelper();

		View startButton = mainView.findViewById(R.id.start_button);
		TextViewExProgress startButtonText = mainView.findViewById(R.id.start_button_descr);
		ProgressBar progressBar = mainView.findViewById(R.id.progress_bar_button);
		boolean publicTransportMode = helper.isPublicTransportMode();
		boolean routeCalculated = isRouteCalculated();
		boolean currentLocationNotFound = OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)
				&& targetHelper.getPointToStart() == null && targetHelper.getPointToNavigate() != null;
		boolean hasCalculatedMissingMaps = hasCalculatedMissingMaps(app);

		int iconId = publicTransportMode ? R.drawable.ic_map : R.drawable.ic_action_start_navigation;
		int color1;
		int color2;
		if (publicTransportMode) {
			if (routeCalculated && hasTransportRoutes()) {
				color1 = ColorUtilities.getActiveColorId(nightMode);
				AndroidUtils.setBackground(app, startButton, ColorUtilities.getCardAndListBackgroundColorId(nightMode));
			} else {
				color1 = R.color.icon_color_default_light;
				AndroidUtils.setBackground(app, startButton, nightMode, R.color.activity_background_color_light, R.color.activity_background_color_dark);
			}
			color2 = color1;
		} else {
			color1 = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
			if (routeCalculated || currentLocationNotFound && !helper.isRouteBeingCalculated() && !hasCalculatedMissingMaps) {
				AndroidUtils.setBackgroundColor(app, startButton, ColorUtilities.getActiveColorId(nightMode));
				color2 = color1;
			} else {
				AndroidUtils.setBackgroundColor(app, startButton, ColorUtilities.getActivityBgColorId(nightMode));
				color2 = R.color.icon_color_default_light;
			}
		}
		setupRouteCalculationButtonProgressBar(progressBar, startButtonText, color1, color2);

		startButtonText.setCompoundDrawablesWithIntrinsicBounds(app.getUIUtilities().getIcon(iconId, color2), null, null, null);
		if (publicTransportMode) {
			startButtonText.setText(R.string.shared_string_show_on_map);
		} else if (helper.isFollowingMode() || helper.isPauseNavigation()) {
			startButtonText.setText(R.string.shared_string_resume);
		} else {
			startButtonText.setText(R.string.shared_string_control_start);
		}

		if (hasCalculatedMissingMaps) {
			startButton.setClickable(false);
			startButton.setEnabled(false);
		} else {
			startButton.setEnabled(true);
			startButton.setClickable(true);
		}

		startButton.setOnClickListener(v -> clickRouteGo());
		startButton.setFocusable(true);
		startButton.requestFocus();
		View cancelButton = mainView.findViewById(R.id.cancel_button);
		TextView cancelButtonText = mainView.findViewById(R.id.cancel_button_descr);
		if (helper.isRouteCalculated() || helper.isRouteBeingCalculated() || isTransportRouteCalculated()) {
			cancelButtonText.setText(R.string.stop_navigation_service);
		} else {
			cancelButtonText.setText(R.string.shared_string_cancel);
		}
		AndroidUtils.setBackground(app, cancelButton, ColorUtilities.getCardAndListBackgroundColorId(nightMode));
		cancelButton.setOnClickListener(v -> clickRouteCancel());
	}

	private void setupRouteCalculationButtonProgressBar(@NonNull ProgressBar pb, @NonNull TextViewExProgress textProgress, @ColorRes int progressTextColor, @ColorRes int bgTextColor) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			int progressColor = ContextCompat.getColor(mapActivity, ColorUtilities.getActiveColorId(nightMode));
			pb.setProgressDrawable(AndroidUtils.createProgressDrawable(ContextCompat.getColor(mapActivity, R.color.color_transparent), ContextCompat.getColor(mapActivity, progressTextColor)));
			textProgress.paint.setColor(progressColor);
			textProgress.setTextColor(ContextCompat.getColor(mapActivity, bgTextColor));
		}
	}

	private void createRoutingParametersButtons(MapActivity mapActivity, RouteMenuAppModes mode, LinearLayout optionsContainer) {
		if (mapActivity == null || optionsContainer == null) {
			return;
		}
		for (LocalRoutingParameter parameter : mode.parameters) {
			if (parameter instanceof MuteSoundRoutingParameter) {
				createMuteSoundRoutingParameterButton(mapActivity, (MuteSoundRoutingParameter) parameter, mode, optionsContainer);
			} else if (parameter instanceof ShowAlongTheRouteItem) {
				createShowAlongTheRouteItems(mapActivity, optionsContainer);
			} else if (parameter instanceof AvoidRoadsRoutingParameter || parameter instanceof AvoidPTTypesRoutingParameter) {
				createAvoidRoadsRoutingParameterButton(mapActivity, parameter, mode, optionsContainer);
			} else if (parameter instanceof LocalRoutingParameterGroup) {
				createLocalRoutingParameterGroupButton(mapActivity, parameter, optionsContainer);
			} else {
				createSimpleRoutingParameterButton(mapActivity, parameter, optionsContainer);
			}
		}
	}

	private void createMuteSoundRoutingParameterButton(MapActivity mapActivity,
	                                                   MuteSoundRoutingParameter parameter,
	                                                   RouteMenuAppModes mode,
	                                                   LinearLayout optionsContainer) {
		ApplicationMode appMode = mapActivity.getRoutingHelper().getAppMode();
		int colorActive = ContextCompat.getColor(mapActivity, ColorUtilities.getActiveColorId(nightMode));
		int colorDisabled = ContextCompat.getColor(mapActivity, R.color.icon_color_default_light);
		String text = null;
		boolean active = !mapActivity.getRoutingHelper().getVoiceRouter().isMuteForMode(appMode);
		if (mode.parameters.size() <= 2) {
			text = mapActivity.getString(active ? R.string.shared_string_on : R.string.shared_string_off);
		}
		View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), v -> {
			OsmandApplication app = getApp();
			if (app != null) {
				if (app.getSettings().isVoiceProviderNotSelected(appMode)) {
					VoiceLanguageBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager(),
							null, appMode, true);
				} else {
					app.getRoutingOptionsHelper().switchSound();
				}
				boolean active1 = !app.getRoutingHelper().getVoiceRouter().isMuteForMode(appMode);
				String text1 = app.getString(active1 ? R.string.shared_string_on : R.string.shared_string_off);

				Drawable itemDrawable = app.getUIUtilities().getIcon(active1 ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
				Drawable activeItemDrawable = app.getUIUtilities().getIcon(active1 ? parameter.getActiveIconId() : parameter.getDisabledIconId(), ColorUtilities.getActiveColorId(nightMode));

				if (Build.VERSION.SDK_INT >= 21) {
					itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
				}
				((ImageView) v.findViewById(R.id.route_option_image_view)).setImageDrawable(active1 ? activeItemDrawable : itemDrawable);
				((TextView) v.findViewById(R.id.route_option_title)).setText(text1);
				((TextView) v.findViewById(R.id.route_option_title)).setTextColor(active1 ? colorActive : colorDisabled);
			}
		});
		if (item != null) {
			optionsContainer.addView(item, getContainerButtonLayoutParams(mapActivity, true));
		}
	}

	private void createShowAlongTheRouteItems(MapActivity mapActivity, LinearLayout optionsContainer) {
		OsmandApplication app = mapActivity.getMyApplication();
		ApplicationMode applicationMode = app.getRoutingHelper().getAppMode();
		Set<PoiUIFilter> poiFilters = app.getPoiFilters().getSelectedPoiFilters();
		boolean traffic = app.getSettings().SHOW_TRAFFIC_WARNINGS.getModeValue(applicationMode);
		boolean fav = app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(applicationMode);
		if (!poiFilters.isEmpty()) {
			createPoiFiltersItems(mapActivity, poiFilters, optionsContainer);
		}
		if (traffic && app.getSettings().SHOW_ROUTING_ALARMS.get()) {
			createWaypointItem(mapActivity, optionsContainer, WaypointHelper.ALARMS);
		}
		if (fav) {
			createWaypointItem(mapActivity, optionsContainer, WaypointHelper.FAVORITES);
		}
	}

	private void createPoiFiltersItems(MapActivity mapActivity, Set<PoiUIFilter> poiFilters, LinearLayout optionsContainer) {
		LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
		if (item != null) {
			item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
			Iterator<PoiUIFilter> it = poiFilters.iterator();
			while (it.hasNext()) {
				PoiUIFilter poiUIFilter = it.next();
				View container = createToolbarSubOptionView(true, poiUIFilter.getName(), R.drawable.ic_action_remove_dark, !it.hasNext(), v -> {
					MapActivity mapActivity1 = getMapActivity();
					if (mapActivity1 != null) {
						mapActivity1.getMyApplication().getPoiFilters()
								.removeSelectedPoiFilter(poiUIFilter);
						mapActivity1.refreshMap();
						updateOptionsButtons();
					}
				});
				if (container != null) {
					item.addView(container, getContainerButtonLayoutParams(mapActivity, false));
				}
			}
			optionsContainer.addView(item, getContainerButtonLayoutParams(mapActivity, true));
		}
	}

	private void createWaypointItem(MapActivity mapActivity, LinearLayout optionsContainer, int waypointType) {
		LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
		if (item != null) {
			item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
			String title = "";
			if (waypointType == WaypointHelper.ALARMS) {
				title = mapActivity.getString(R.string.way_alarms);
			} else if (waypointType == WaypointHelper.FAVORITES) {
				title = mapActivity.getString(R.string.favourites);
			}
			View container = createToolbarSubOptionView(true, title, R.drawable.ic_action_remove_dark, true, v -> {
				OsmandApplication app = getApp();
				if (app != null) {
					app.getWaypointHelper().enableWaypointType(waypointType, false);
					updateOptionsButtons();
				}
			});
			if (container != null) {
				AndroidUtils.setBackground(app, container, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
				item.addView(container, getContainerButtonLayoutParams(mapActivity, false));
				optionsContainer.addView(item, getContainerButtonLayoutParams(mapActivity, true));
			}
		}
	}

	private void createAvoidRoadsRoutingParameterButton(MapActivity mapActivity, LocalRoutingParameter parameter, RouteMenuAppModes mode, LinearLayout optionsContainer) {
		OsmandApplication app = mapActivity.getMyApplication();
		LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
		if (item != null) {
			item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
			Map<LatLon, AvoidRoadInfo> impassableRoads = new HashMap<>();
			if (parameter instanceof AvoidRoadsRoutingParameter) {
				impassableRoads = app.getAvoidSpecificRoads().getImpassableRoads();
			}

			List<RoutingParameter> avoidedParameters = getAvoidedParameters(app);
			createImpassableRoadsItems(mapActivity, impassableRoads, parameter, mode, item);
			createAvoidParametersItems(mapActivity, avoidedParameters, parameter, mode, item);
			if (avoidedParameters.size() > 0 || impassableRoads.size() > 0) {
				optionsContainer.addView(item, getContainerButtonLayoutParams(mapActivity, true));
			}
		}
	}

	private List<RoutingParameter> getAvoidedParameters(OsmandApplication app) {
		ApplicationMode applicationMode = app.getRoutingHelper().getAppMode();
		List<RoutingParameter> avoidParameters = app.getRoutingOptionsHelper().getAvoidRoutingPrefsForAppMode(applicationMode);
		List<RoutingParameter> avoidedParameters = new ArrayList<>();
		for (int i = 0; i < avoidParameters.size(); i++) {
			RoutingParameter p = avoidParameters.get(i);
			CommonPreference<Boolean> preference = app.getSettings().getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());
			if (preference != null && preference.getModeValue(app.getRoutingHelper().getAppMode())) {
				avoidedParameters.add(p);
			}
		}
		return avoidedParameters;
	}

	private void createImpassableRoadsItems(MapActivity mapActivity, Map<LatLon, AvoidRoadInfo> impassableRoads,
	                                        LocalRoutingParameter parameter, RouteMenuAppModes mode, LinearLayout item) {
		Iterator<AvoidRoadInfo> it = impassableRoads.values().iterator();
		while (it.hasNext()) {
			AvoidRoadInfo avoidRoadInfo = it.next();
			View container = createToolbarSubOptionView(false, avoidRoadInfo.name, R.drawable.ic_action_remove_dark, !it.hasNext(), v -> {
				MapActivity mapActivity1 = getMapActivity();
				if (mapActivity1 != null) {
					OsmandApplication app = mapActivity1.getMyApplication();
					app.getAvoidSpecificRoads().removeImpassableRoad(avoidRoadInfo);
					app.getRoutingHelper().onSettingsChanged(true);
					if (app.getAvoidSpecificRoads().getImpassableRoads().isEmpty() && getAvoidedParameters(app).isEmpty()) {
						mode.parameters.remove(parameter);
					}
					mapActivity1.refreshMap();
					if (mode.parameters.size() > 2) {
						item.removeView(v);
					} else {
						updateOptionsButtons();
					}
				}
			});
			if (container != null) {
				item.addView(container, getContainerButtonLayoutParams(mapActivity, false));
			}
		}
	}

	private void createAvoidParametersItems(MapActivity mapActivity, List<RoutingParameter> avoidedParameters,
	                                        LocalRoutingParameter parameter, RouteMenuAppModes mode,
	                                        LinearLayout item) {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		for (int i = 0; i < avoidedParameters.size(); i++) {
			RoutingParameter routingParameter = avoidedParameters.get(i);
			View container = createToolbarSubOptionView(false, AndroidUtils.getRoutingStringPropertyName(
							app, routingParameter.getId(), routingParameter.getName()), R.drawable.ic_action_remove_dark,
					i == avoidedParameters.size() - 1, v -> {
						OsmandApplication app = getApp();
						if (app == null) {
							return;
						}
						CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(
								routingParameter.getId(), routingParameter.getDefaultBoolean());
						preference.setModeValue(app.getRoutingHelper().getAppMode(), false);
						avoidedParameters.remove(routingParameter);
						app.getRoutingHelper().onSettingsChanged(true);
						if (app.getAvoidSpecificRoads().getImpassableRoads().isEmpty() && avoidedParameters.isEmpty()) {
							mode.parameters.remove(parameter);
						}
						if (mode.parameters.size() > 2) {
							item.removeView(v);
						} else {
							updateOptionsButtons();
						}
					});
			if (container != null) {
				item.addView(container, getContainerButtonLayoutParams(mapActivity, false));
			}
		}
	}

	private void createLocalRoutingParameterGroupButton(MapActivity mapActivity, LocalRoutingParameter parameter, LinearLayout optionsContainer) {
		OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) parameter;
		String text = null;
		LocalRoutingParameter selected = group.getSelected(settings);
		if (selected != null) {
			text = group.getText(mapActivity);
		}
		View item = createToolbarOptionView(false, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), v -> {
			MapActivity mapActv = getMapActivity();
			if (mapActv != null) {
				mapActv.getMyApplication().getRoutingOptionsHelper().showLocalRoutingParameterGroupDialog(group, mapActv, this::updateOptionsButtons);
			}
		});
		if (item != null) {
			optionsContainer.addView(item, getContainerButtonLayoutParams(mapActivity, true));
		}
	}

	private void createSimpleRoutingParameterButton(MapActivity mapActivity, LocalRoutingParameter parameter, LinearLayout optionsContainer) {
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		int colorActive = ContextCompat.getColor(app, ColorUtilities.getActiveColorId(nightMode));
		int colorDisabled = ContextCompat.getColor(app, R.color.icon_color_default_light);
		int margin = AndroidUtils.dpToPx(app, 3);
		OsmandSettings settings = app.getSettings();
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
			View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), v -> {
				OsmandApplication app1 = getApp();
				if (parameter.routingParameter != null && app1 != null) {
					boolean selected = !parameter.isSelected(settings);
					app1.getRoutingOptionsHelper().applyRoutingParameter(parameter, selected);

					Drawable itemDrawable = app1.getUIUtilities().getIcon(selected ? parameter.getActiveIconId() : parameter.getDisabledIconId(), nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
					Drawable activeItemDrawable = app1.getUIUtilities().getIcon(selected ? parameter.getActiveIconId() : parameter.getDisabledIconId(), ColorUtilities.getActiveColorId(nightMode));

					if (Build.VERSION.SDK_INT >= 21) {
						itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
					}
					((ImageView) v.findViewById(R.id.route_option_image_view)).setImageDrawable(selected ? activeItemDrawable : itemDrawable);
					((TextView) v.findViewById(R.id.route_option_title)).setTextColor(selected ? colorActive : colorDisabled);
				}
			});
			if (item != null) {
				LinearLayout.LayoutParams layoutParams = getContainerButtonLayoutParams(mapActivity, false);
				AndroidUtils.setMargins(layoutParams, margin, 0, margin, 0);
				optionsContainer.addView(item, layoutParams);
			}
		}
	}

	private LinearLayout.LayoutParams getContainerButtonLayoutParams(Context context, boolean containerParams) {
		if (containerParams) {
			int margin = AndroidUtils.dpToPx(context, 3);
			LinearLayout.LayoutParams containerBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
			AndroidUtils.setMargins(containerBtnLp, margin, 0, margin, 0);
			return containerBtnLp;
		} else {
			return new LinearLayout.LayoutParams(AndroidUtils.dpToPx(context, 100), ViewGroup.LayoutParams.MATCH_PARENT);
		}
	}

	@Nullable
	private LinearLayout createToolbarOptionView(boolean active, String title, @DrawableRes int activeIconId,
	                                             @DrawableRes int disabledIconId, OnClickListener listener) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return null;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		LinearLayout item = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.route_option_btn, null);
		TextView textView = item.findViewById(R.id.route_option_title);
		ImageView imageView = item.findViewById(R.id.route_option_image_view);
		int colorActive = ContextCompat.getColor(app, ColorUtilities.getActiveColorId(nightMode));
		int colorDisabled = ContextCompat.getColor(app, R.color.icon_color_default_light);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, item.findViewById(R.id.route_option_container), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			AndroidUtils.setBackground(app, item, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		} else {
			AndroidUtils.setBackground(app, item, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}

		Drawable itemDrawable = null;
		Drawable activeItemDrawable = null;
		if (activeIconId != -1 && disabledIconId != -1) {
			itemDrawable = app.getUIUtilities().getIcon(active ? activeIconId : disabledIconId, nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
			activeItemDrawable = app.getUIUtilities().getIcon(active ? activeIconId : disabledIconId, ColorUtilities.getActiveColorId(nightMode));
			if (Build.VERSION.SDK_INT >= 21) {
				itemDrawable = AndroidUtils.createPressedStateListDrawable(itemDrawable, activeItemDrawable);
			}
		}
		if (title == null) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setVisibility(View.VISIBLE);
			textView.setTextColor(active ? colorActive : colorDisabled);
			textView.setText(title);
		}
		if (activeItemDrawable != null && itemDrawable != null) {
			imageView.setImageDrawable(active ? activeItemDrawable : itemDrawable);
		} else {
			imageView.setVisibility(View.GONE);
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
		View container = mapActivity.getLayoutInflater().inflate(R.layout.route_options_container, null);
		TextView routeOptionTV = container.findViewById(R.id.route_removable_option_title);
		ImageView routeOptionImageView = container.findViewById(R.id.removable_option_icon);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.btn_pressed_trans_light, R.drawable.btn_pressed_trans_dark);
		}
		AndroidUtils.setBackground(app, container.findViewById(R.id.options_divider_end), ColorUtilities.getDividerColorId(nightMode));
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
		routeOptionTV.setTextColor(ContextCompat.getColor(app, R.color.icon_color_default_light));
		routeOptionImageView.setImageDrawable(app.getUIUtilities().getIcon(iconId, ColorUtilities.getActiveColorId(nightMode)));
		container.setOnClickListener(listener);

		return container;
	}

	private void clickRouteGo() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (app.getRoutingHelper().isPublicTransportMode()) {
				if (isTransportRouteCalculated() && hasTransportRoutes()) {
					showRouteOnMap(mapActivity, app.getTransportRoutingHelper().getCurrentRoute());
				}
			} else {
				if (mapActivity.getPointToNavigate() != null) {
					hide();
				}
				mapActivity.getMapLayers().getMapActionsHelper().startNavigation();
			}
		}
	}

	private void showRouteOnMap(@NonNull MapActivity mapActivity, int routeIndex) {
		if (mapActivity.getPointToNavigate() != null) {
			hide();
		}
		ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(), routeIndex, MenuState.HEADER_ONLY);
	}

	private void clickRouteCancel() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapLayers().getMapActionsHelper().stopNavigation();
			resetRouteCalculation();
		}
	}

	public void resetRouteCalculation() {
		setRouteCalculationInProgress(false);
		restoreCollapsedButtons();
	}

	private void clickRouteParams() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			RouteOptionsBottomSheet.showInstance(mapActivity);
		}
	}

	private void updateViaView() {
		View mainView = getMainView();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();
		View viaLayout = mainView.findViewById(R.id.ViaLayout);

		String viaDescription = generateViaDescription();
		GPXRouteParamsBuilder routeParams = app.getRoutingHelper().getCurrentGPXRoute();
		if (routeParams == null && Algorithms.isEmpty(viaDescription)) {
			AndroidUiHelper.updateVisibility(viaLayout, false);
			return;
		} else {
			AndroidUiHelper.updateVisibility(viaLayout, true);
		}

		if (routeParams != null) {
			viaLayout.setOnClickListener(v -> {
				MapActivity mapActivity1 = getMapActivity();
				if (mapActivity1 != null) {
					GPXRouteParamsBuilder routeParams1 = mapActivity1.getRoutingHelper().getCurrentGPXRoute();
					if (routeParams1 != null) {
						hide();
						chooseAndShowFollowTrack();
					}
				}
			});
		} else {
			viaLayout.setClickable(false);
			viaLayout.setOnClickListener(null);
		}
		setupViaText(mainView);

		FrameLayout viaButton = mainView.findViewById(R.id.via_button);
		int viaContentDescId = routeParams != null ? R.string.shared_string_add : R.string.shared_string_edit;
		viaButton.setContentDescription(app.getString(viaContentDescId));
		AndroidUiHelper.updateVisibility(viaButton, routeParams == null || isFinishPointFromTrack());

		viaButton.setOnClickListener(v -> {
			MapActivity mapActivity12 = getMapActivity();
			if (mapActivity12 != null) {
				GPXRouteParamsBuilder routeParams12 = mapActivity12.getRoutingHelper().getCurrentGPXRoute();
				if (routeParams12 != null) {
					AddPointBottomSheetDialog.showInstance(mapActivity12, PointType.TARGET);
				} else if (mapActivity12.getMyApplication().getTargetPointsHelper().checkPointToNavigateShort()) {
					hide();
					WaypointsFragment.showInstance(mapActivity12.getSupportFragmentManager(), true);
				}
			}
		});

		LinearLayout viaButtonContainer = mainView.findViewById(R.id.via_button_container);
		setupButtonBackground(viaButton, viaButtonContainer);

		ImageView viaButtonImageView = mainView.findViewById(R.id.via_button_image_view);

		int iconId = routeParams != null ? R.drawable.ic_action_plus : R.drawable.ic_action_edit_dark;
		setupButtonIcon(viaButtonImageView, iconId);

		View textView = mainView.findViewById(R.id.via_button_description);
		if (!editButtonCollapsing && !editButtonCollapsed &&
				viaButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
			editButtonCollapsing = true;
			collapseButtonAnimated(R.id.via_button, R.id.via_button_description, success -> {
				editButtonCollapsing = false;
				editButtonCollapsed = success;
			});
		} else if (editButtonCollapsed) {
			textView.setVisibility(View.GONE);
		}
		updateViaIcon(mainView);
	}

	private void setupViaText(View view) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			TextView title = view.findViewById(R.id.ViaView);
			TextView description = view.findViewById(R.id.ViaSubView);
			TextView buttonDescription = view.findViewById(R.id.via_button_description);

			String via = generateViaDescription();
			GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
			if (paramsBuilder != null) {
				GPXFile gpxFile = paramsBuilder.getFile();
				String fileName = null;
				if (!Algorithms.isEmpty(gpxFile.path)) {
					fileName = new File(gpxFile.path).getName();
				} else if (!Algorithms.isEmpty(gpxFile.tracks)) {
					fileName = gpxFile.tracks.get(0).name;
				}
				if (Algorithms.isEmpty(fileName)) {
					fileName = app.getString(R.string.shared_string_gpx_track);
				}
				fileName = TrackEditCard.getGpxTitleWithSelectedItem(app, paramsBuilder, fileName);
				title.setText(GpxUiHelper.getGpxTitle(fileName));
				description.setText(R.string.follow_track);
				buttonDescription.setText(R.string.shared_string_add);
			} else {
				title.setText(via);
				buttonDescription.setText(R.string.shared_string_edit);
				description.setText(app.getString(R.string.intermediate_destinations) + " (" +
						app.getTargetPointsHelper().getIntermediatePoints().size() + ")");
			}
		}
	}

	private void updateViaIcon(View parentView) {
		ImageView viaIcon = parentView.findViewById(R.id.viaIcon);

		OsmandApplication app = (OsmandApplication) parentView.getContext().getApplicationContext();
		GPXRouteParamsBuilder routeParams = app.getRoutingHelper().getCurrentGPXRoute();
		if (routeParams != null) {
			viaIcon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_polygom_dark));
		} else {
			viaIcon.setImageDrawable(getIconOrig(R.drawable.list_intermediate));
		}
	}

	private void updateFinishPointView() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		OsmandApplication app = mapActivity.getMyApplication();

		View toLayout = mainView.findViewById(R.id.ToLayout);
		View viaLayout = mainView.findViewById(R.id.ViaLayout);
		View toLayoutDivider = mainView.findViewById(R.id.toLayoutDivider);

		if (isFinishPointFromTrack()) {
			AndroidUiHelper.updateVisibility(toLayout, false);
			AndroidUiHelper.updateVisibility(toLayoutDivider, false);
			return;
		} else {
			boolean dividerVisible = viaLayout.getVisibility() == View.VISIBLE;
			AndroidUiHelper.updateVisibility(toLayout, true);
			AndroidUiHelper.updateVisibility(toLayoutDivider, dividerVisible);
		}

		toLayout.setOnClickListener(v -> {
			MapActivity mapActivity1 = getMapActivity();
			if (mapActivity1 != null) {
				AddPointBottomSheetDialog.showInstance(mapActivity1, PointType.TARGET);
			}
		});
		setupToText(mainView);

		FrameLayout toButton = mainView.findViewById(R.id.to_button);
		if (app.getRoutingHelper().isPublicTransportMode()) {
			toButton.setVisibility(View.GONE);
		} else {
			toButton.setVisibility(View.VISIBLE);

			LinearLayout toButtonContainer = mainView.findViewById(R.id.to_button_container);
			setupButtonBackground(toButton, toButtonContainer);

			ImageView toButtonImageView = mainView.findViewById(R.id.to_button_image_view);
			setupButtonIcon(toButtonImageView, R.drawable.ic_action_plus);

			toButton.setOnClickListener(view -> {
				MapActivity mapActivity12 = getMapActivity();
				if (mapActivity12 != null) {
					PointType pointType;
					if (mapActivity12.getPointToNavigate() == null
							|| mapActivity12.getRoutingHelper().getCurrentGPXRoute() != null) {
						pointType = PointType.TARGET;
					} else {
						pointType = PointType.INTERMEDIATE;
					}
					AddPointBottomSheetDialog.showInstance(mapActivity12, pointType);
				}
			});

			View textView = mainView.findViewById(R.id.to_button_description);
			if (!addButtonCollapsing && !addButtonCollapsed &&
					toButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
				addButtonCollapsing = true;
				collapseButtonAnimated(R.id.to_button, R.id.to_button_description, success -> {
					addButtonCollapsing = false;
					addButtonCollapsed = success;
				});
			} else if (addButtonCollapsed) {
				textView.setVisibility(View.GONE);
			}
		}
		updateToIcon(mainView);
	}

	private void updateToIcon(View parentView) {
		ImageView toIcon = parentView.findViewById(R.id.toIcon);
		toIcon.setImageDrawable(getIconOrig(R.drawable.list_destination));
	}

	private boolean isFinishPointFromTrack() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			GPXRouteParamsBuilder routeParams = app.getRoutingHelper().getCurrentGPXRoute();
			if (routeParams != null) {
				TargetPoint target = app.getTargetPointsHelper().getPointToNavigate();
				if (target != null) {
					List<Location> points = routeParams.getPoints(app);
					if (!Algorithms.isEmpty(points)) {
						Location loc = points.get(points.size() - 1);
						LatLon latLon = new LatLon(loc.getLatitude(), loc.getLongitude());
						LatLon targetLatLon = new LatLon(target.getLatitude(), target.getLongitude());
						return latLon.equals(targetLatLon);
					}
				}
			}
		}
		return false;
	}

	private void updateStartPointView() {
		MapActivity mapActivity = getMapActivity();
		View mainView = getMainView();
		if (mapActivity == null || mainView == null) {
			return;
		}
		setupFromText(mainView);
		View fromLayout = mainView.findViewById(R.id.FromLayout);
		fromLayout.setOnClickListener(v -> {
			MapActivity mapActv = getMapActivity();
			if (mapActv != null) {
				AddPointBottomSheetDialog.showInstance(mapActv, PointType.START);
			}
		});

		FrameLayout fromButton = mainView.findViewById(R.id.from_button);
		boolean isFollowTrack = mapActivity.getMyApplication().getRoutingHelper().getCurrentGPXRoute() != null;

		if (isFollowTrack) {
			fromButton.setVisibility(View.GONE);
		} else {
			fromButton.setVisibility(View.VISIBLE);
		}
		LinearLayout fromButtonContainer = mainView.findViewById(R.id.from_button_container);
		setupButtonBackground(fromButton, fromButtonContainer);

		ImageView swapDirectionView = mainView.findViewById(R.id.from_button_image_view);
		setupButtonIcon(swapDirectionView, R.drawable.ic_action_change_navigation_points);

		fromButton.setOnClickListener(view -> {
			MapActivity mapActv = getMapActivity();
			if (mapActv != null) {
				OsmandApplication app = mapActv.getMyApplication();
				TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
				TargetPoint startPoint = targetPointsHelper.getPointToStart();
				TargetPoint endPoint = targetPointsHelper.getPointToNavigate();
				Location loc = app.getLocationProvider().getLastKnownLocation();
				if (loc == null && startPoint == null && endPoint == null) {
					app.showShortToastMessage(R.string.add_start_and_end_points);
				} else if (endPoint == null) {
					app.showShortToastMessage(R.string.mark_final_location_first);
				} else {
					GPXRouteParamsBuilder gpxParams = app.getRoutingHelper().getCurrentGPXRoute();
					if (gpxParams != null) {
						boolean reverse = !gpxParams.isReverse();
						LocalRoutingParameter parameter = new OtherLocalRoutingParameter(R.string.gpx_option_reverse_route, app.getString(R.string.gpx_option_reverse_route), reverse);
						app.getRoutingOptionsHelper().applyRoutingParameter(parameter, reverse);
					} else {
						if (startPoint == null && loc != null) {
							startPoint = TargetPoint.createStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()),
									new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION, mapActv.getString(R.string.shared_string_my_location)));
						}
						if (startPoint != null) {
							int intermediateSize = targetPointsHelper.getIntermediatePoints().size();
							if (intermediateSize > 1) {
								WaypointDialogHelper.reverseAllPoints(app, mapActv, mapActv.getDashboard().getWaypointDialogHelper());
							} else {
								WaypointDialogHelper.switchStartAndFinish(mapActv.getMyApplication(),
										mapActv, mapActv.getDashboard().getWaypointDialogHelper(), true);
							}
						} else {
							app.showShortToastMessage(R.string.route_add_start_point);
						}
					}
				}
			}
		});

		updateFromIcon(mainView);

		View textView = mainView.findViewById(R.id.from_button_description);
		if (!swapButtonCollapsing && !swapButtonCollapsed &&
				fromButton.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE) {
			swapButtonCollapsing = true;
			collapseButtonAnimated(R.id.from_button, R.id.from_button_description, success -> {
				swapButtonCollapsing = false;
				swapButtonCollapsed = success;
			});
		} else if (swapButtonCollapsed) {
			textView.setVisibility(View.GONE);
		}
	}

	public void updateFromIcon(View parentView) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			Location loc = app.getLocationProvider().getLastKnownLocation();
			int locationIconResByStatus = OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity) && loc != null
					? R.drawable.ic_action_location_color : R.drawable.ic_action_location_color_lost;

			((ImageView) parentView.findViewById(R.id.fromIcon)).setImageDrawable(AppCompatResources.getDrawable(mapActivity,
					mapActivity.getMyApplication().getTargetPointsHelper().getPointToStart() == null
							? locationIconResByStatus : R.drawable.list_startpoint));
		}
	}

	private void collapseButtonAnimated(int containerRes, int viewRes, OnButtonCollapsedListener listener) {
		runButtonAnimation(() -> {
			boolean started = false;
			View mainView = getMainView();
			if (isVisible() && mainView != null) {
				ViewGroup container = mainView.findViewById(containerRes);
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

	private void setupButtonIcon(ImageView imageView, @DrawableRes int iconId) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Drawable normal = UiUtilities.createTintedDrawable(mapActivity, iconId, nightMode
					? ContextCompat.getColor(mapActivity, R.color.icon_color_default_dark)
					: ContextCompat.getColor(mapActivity, R.color.icon_color_default_light));
			if (Build.VERSION.SDK_INT >= 21) {
				Drawable active = UiUtilities.createTintedDrawable(mapActivity, iconId, ColorUtilities.getActiveColor(mapActivity, nightMode));
				normal = AndroidUtils.createPressedStateListDrawable(normal, active);
			}
			imageView.setImageDrawable(normal);
		}
	}

	private void setupButtonBackground(View button, View buttonContainer) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, button, nightMode, R.drawable.btn_rounded_light, R.drawable.btn_rounded_dark);
			AndroidUtils.setBackground(app, buttonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, buttonContainer, nightMode, R.drawable.btn_trans_rounded_light, R.drawable.btn_trans_rounded_dark);
		}
	}

	public void selectOnScreen(PointType pointType) {
		selectOnScreen(pointType, getCurrentMenuState(), false);
	}

	public void selectOnScreen(PointType pointType, boolean waypointsMenu) {
		selectOnScreen(pointType, getCurrentMenuState(), waypointsMenu);
	}

	public void selectOnScreen(PointType pointType, int menuState, boolean waypointsMenu) {
		selectFromMapTouch = true;
		selectFromMapPointType = pointType;
		selectFromMapMenuState = menuState;
		selectFromMapWaypoints = waypointsMenu;
		hide();
	}

	public void selectAddress(@Nullable String name, @NonNull LatLon latLon, PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
			choosePointTypeAction(latLon, pointType, pd, name);
			updateMenu();
		}
	}

	public void chooseAndShowFollowTrack() {
		selectFromTracks = true;
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			SelectTrackTabsFragment.GpxFileSelectionListener gpxFileSelectionListener = gpxFile -> {
				if (TrackSelectSegmentBottomSheet.shouldShowForGpxFile(gpxFile)) {
					mapActivity.getMapRouteInfoMenu().selectTrack(gpxFile, true, getOnSegmentSelectedListener());
				} else {
					mapActivity.getMapRouteInfoMenu().selectTrack(gpxFile, false);
					FollowTrackFragment trackOptionsFragment = new FollowTrackFragment();
					FollowTrackFragment.showInstance(mapActivity.getSupportFragmentManager(), trackOptionsFragment);
				}
			};
			SelectTrackTabsFragment.showInstance(mapActivity.getSupportFragmentManager(), gpxFileSelectionListener);
		}
	}

	private OnSegmentSelectedListener getOnSegmentSelectedListener() {
		return new OnSegmentSelectedListener() {
			@Override
			public void onSegmentSelect(@NonNull GPXFile gpxFile, int selectedSegment) {
				if (app == null) {
					return;
				}
				selectTrack(gpxFile, false);
				onGpxSelected(app, gpxFile, app.getSettings().GPX_SEGMENT_INDEX, selectedSegment);
			}

			@Override
			public void onRouteSelected(@NonNull GPXFile gpxFile, int selectedRoute) {
				if (app == null) {
					return;
				}
				onGpxSelected(app, gpxFile, app.getSettings().GPX_ROUTE_INDEX, selectedRoute);
			}
		};
	}

	private void onGpxSelected(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile, @NonNull OsmandPreference<Integer> gpxRouteSegmentPreference, int selectedIndex) {
		gpxRouteSegmentPreference.set(selectedIndex);
		selectTrack(gpxFile, false);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedRoute(selectedIndex);
			app.getRoutingHelper().onSettingsChanged(true);
		}
		if (mapActivity != null) {
			FollowTrackFragment trackOptionsFragment = new FollowTrackFragment();
			FollowTrackFragment.showInstance(mapActivity.getSupportFragmentManager(), trackOptionsFragment);
		}
		updateCards();
	}

	public void cancelSelectionFromTracks() {
		selectFromTracks = false;
	}

	public void customizeRouteLine() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			customizingRouteLine = true;
			ApplicationMode routingAppMode = mapActivity.getMyApplication().getRoutingHelper().getAppMode();
			RouteLineAppearanceFragment.showInstance(mapActivity, routingAppMode);
		}
	}

	public void finishRouteLineCustomization() {
		customizingRouteLine = false;
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

	public void selectMapMarker(int index, PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapMarker m = null;
			List<MapMarker> mapMarkers = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers();
			if (index != -1 && mapMarkers.size() > index) {
				m = mapMarkers.get(index);
			}
			selectMapMarker(m, pointType);
		}
	}

	public void selectMapMarker(@Nullable MapMarker marker, @NonNull PointType pointType) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (marker != null) {
				LatLon latLon = new LatLon(marker.getLatitude(), marker.getLongitude());
				PointDescription pd = marker.getPointDescription(mapActivity);
				choosePointTypeAction(latLon, pointType, pd, null);
				updateMenu();
			} else {
				MapMarkerSelectionFragment selectionFragment = MapMarkerSelectionFragment.newInstance(pointType);
				selectionFragment.show(mapActivity.getSupportFragmentManager(), MapMarkerSelectionFragment.TAG);
			}
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

	public boolean shouldShowTopControls() {
		return shouldShowTopControls(isVisible());
	}

	public boolean shouldShowTopControls(boolean menuVisible) {
		return !menuVisible || !portraitMode || getCurrentMenuState() == MenuState.HEADER_ONLY;
	}

	public boolean isVisible() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			MapRouteInfoMenuFragment f = fragmentRef.get();
			if (f != null) {
				return f.isVisible() && !f.isDismissing();
			}
		}
		return false;
	}

	public static boolean isRelatedFragmentVisible(@Nullable OsmandMapTileView mapView) {
		if (MapRouteInfoMenu.chooseRoutesVisible
				|| MapRouteInfoMenu.waypointsVisible
				|| MapRouteInfoMenu.followTrackVisible) {
			return true;
		}
		MapActivity activity = mapView != null ? mapView.getMapActivity() : null;
		MapRouteInfoMenu menu = activity != null ? activity.getMapRouteInfoMenu() : null;
		return menu != null && menu.isVisible();
	}

	@Nullable
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

	@Nullable
	public WeakReference<ChooseRouteFragment> findChooseRouteFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(ChooseRouteFragment.TAG);
			if (fragment instanceof ChooseRouteFragment && !((ChooseRouteFragment) fragment).isPaused()) {
				return new WeakReference<>((ChooseRouteFragment) fragment);
			}
		}
		return null;
	}

	@Nullable
	public WeakReference<FollowTrackFragment> findFollowTrackFragment() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(FollowTrackFragment.TAG);
			if (fragment instanceof FollowTrackFragment && !((FollowTrackFragment) fragment).isPaused()) {
				return new WeakReference<>((FollowTrackFragment) fragment);
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
			if (f != null) {
				if (!f.isPortrait()) {
					tileBoxWidthPx = tb.getPixWidth() - f.getWidth();
				} else {
					tileBoxHeightPx = tb.getPixHeight() - f.getHeight();
				}
			}
		}
		mapActivity.getMapView().fitLocationToMap(latitude, longitude, mapActivity.getMapView().getZoom(),
				tileBoxWidthPx, tileBoxHeightPx, AndroidUtils.dpToPx(mapActivity, 40f), true);
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		directionInfo = -1;
		routeSelected = false;
		updateMenu();
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
					AddressLookupRequest lookupRequest = new AddressLookupRequest(p.point, address -> updateMenu(), null);
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
			return PointDescription.getLocationNamePlain(mapActivity, lat, lon);
		}
		return "";
	}

	public String getRoutePointDescription(LatLon l, String d) {
		if (d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		MapActivity mapActivity = getMapActivity();
		if (l != null && mapActivity != null) {
			return getRoutePointDescription(l.getLatitude(), l.getLongitude());
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

				LatLon latLon = start.point;
				PointDescription pointDescription = start.getOriginalPointDescription();
				boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
				cancelStartPointAddressRequest();
				if (needAddress) {
					startPointRequest = new AddressLookupRequest(latLon, address -> {
						startPointRequest = null;
						updateMenu();
					}, null);
					mapActivity.getMyApplication().getGeocodingLookupService().lookupAddress(startPointRequest);
				}
			}

			TextView fromText = view.findViewById(R.id.fromText);
			if (start != null) {
				fromText.setText(name);
			} else {
				if (OsmAndLocationProvider.isLocationPermissionAvailable(mapActivity)) {
					fromText.setText(R.string.shared_string_my_location);
				} else {
					fromText.setText(R.string.route_descr_select_start_point);
				}
			}
		}
	}

	private void setupToText(View view) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			OsmandApplication app = mapActivity.getMyApplication();
			TextView toText = view.findViewById(R.id.toText);
			TargetPointsHelper targets = app.getTargetPointsHelper();
			TargetPoint finish = targets.getPointToNavigate();
			if (finish != null) {
				toText.setText(getRoutePointDescription(finish.point, finish.getOnlyName()));

				PointDescription pointDescription = finish.getOriginalPointDescription();
				boolean needAddress = pointDescription != null && pointDescription.isSearchingAddress(mapActivity);
				cancelTargetPointAddressRequest();
				if (needAddress) {
					targetPointRequest = new AddressLookupRequest(finish.point, address -> {
						targetPointRequest = null;
						updateMenu();
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

	public void onResume() {
		OsmandApplication app = getApp();
		if (app != null) {
			OsmandSettings settings = app.getSettings();
			RoutingHelper routingHelper = app.getRoutingHelper();
			ApplicationMode mode = routingHelper.getAppMode();
			currentMuteState = settings.VOICE_MUTE.getModeValue(mode);

			routingHelper.addListener(this);
			app.getFavoritesHelper().addListener(this);
			settings.VOICE_MUTE.addListener(voiceMuteChangeListener);
			app.getTargetPointsHelper().addListener(onStateChangedListener);
		}
	}

	public void onPause() {
		OsmandApplication app = getApp();
		if (app != null) {
			app.getRoutingHelper().removeListener(this);
			app.getFavoritesHelper().removeListener(this);
			app.getTargetPointsHelper().removeListener(onStateChangedListener);
			app.getSettings().VOICE_MUTE.removeListener(voiceMuteChangeListener);
		}
		menuCards = new ArrayList<>();
	}

	public void onDismiss(Fragment fragment, int currentMenuState, Bundle arguments, boolean backPressed) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			if (fragment instanceof MapRouteInfoMenuFragment) {
				cancelButtonsAnimations();
				mapActivity.getMapPositionManager().setMapPositionShiftedX(false);
				mapActivity.refreshMap();
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
				if (switched) {
					mapActivity.getMapLayers().getMapActionsHelper().switchToRouteFollowingLayout();
				}
				if (mapActivity.getPointToNavigate() == null && !selectFromMapTouch && !selectFromTracks
						&& !customizingRouteLine) {
					mapActivity.getMapActions().stopNavigationWithoutConfirm();
				}
				mapActivity.updateStatusBarColor();
				RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
				menuBackStack.clear();
				if (routingHelper.isRoutePlanningMode() || routingHelper.isFollowingMode()) {
					menuBackStack.push(new MapRouteMenuStateHolder(MapRouteMenuType.ROUTE_INFO, currentMenuState, fragment.getArguments()));
				}
				if (onDismissListener != null) {
					onDismissListener.onDismiss(null);
				}
			} else if (fragment instanceof ChooseRouteFragment) {
				routeSelected = true;
				MapRouteMenuStateHolder holder = new MapRouteMenuStateHolder(MapRouteMenuType.ROUTE_DETAILS, currentMenuState, fragment.getArguments());
				if (!menuBackStack.empty() && menuBackStack.peek().type == holder.type) {
					menuBackStack.pop();
				}
				if (backPressed) {
					holder.onDismiss(onBackPressed());
				} else {
					menuBackStack.push(holder);
					holder.onDismiss(null);
				}
			} else if (fragment instanceof TrackDetailsMenuFragment) {
				if (backPressed) {
					onBackPressed();
				}
			}
		}
	}

	public void show() {
		show(getInitialMenuState());
	}

	private void show(int menuState) {
		MapRouteMenuStateHolder holder = !menuBackStack.empty() ? menuBackStack.pop() : null;
		if (holder != null) {
			holder.showMenu();
		} else {
			showInternal(menuState);
		}
	}

	public MapRouteMenuStateHolder onBackPressed() {
		MapRouteMenuStateHolder holder = !menuBackStack.empty() ? menuBackStack.pop() : null;
		if (holder != null) {
			holder.showMenu();
		}
		return holder;
	}

	private void showInternal(int menuState) {
		if (menuState == DEFAULT_MENU_STATE) {
			menuState = getInitialMenuState();
		}
		MapActivity mapActivity = getMapActivity();
		if (!isVisible() && mapActivity != null) {
			int initialMenuState = menuState;
			switched = mapActivity.getMapLayers().getMapControlsLayer().switchToRoutePlanningLayout();
			boolean refreshMap = !switched;
			boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
			if (!portrait) {
				initialMenuState = MenuState.FULL_SCREEN;
				mapActivity.getMapPositionManager().setMapPositionShiftedX(true);
				refreshMap = true;
			}

			if (refreshMap) {
				mapActivity.refreshMap();
			}

			MapRouteInfoMenuFragment.showInstance(mapActivity, initialMenuState);
		}
	}

	public void hide() {
		cancelButtonsAnimations();
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		}
	}

	public boolean needShowMenu() {
		return showMenu;
	}

	public void setShowMenu(int menuState) {
		showMenu = true;
		showMenuState = menuState;
	}

	@DrawableRes
	public int getRoutePlanningBtnImage() {
		return menuBackStack.empty() ? 0 : menuBackStack.peek().getButtonImage();
	}

	@Override
	public void onFavoritesLoaded() {
	}

	@Override
	public void onFavoriteDataUpdated(@NonNull FavouritePoint point) {
		updateMenu();
	}

	@Override
	public void onFavoritePropertiesUpdated() {
	}

	@NonNull
	public QuadRect getRouteRect(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		RoutingHelper routingHelper = app.getRoutingHelper();
		QuadRect rect = new QuadRect(0, 0, 0, 0);
		if (isTransportRouteCalculated()) {
			TransportRoutingHelper transportRoutingHelper = app.getTransportRoutingHelper();
			TransportRouteResult result = transportRoutingHelper.getCurrentRouteResult();
			if (result != null) {
				QuadRect transportRouteRect = transportRoutingHelper.getTransportRouteRect(result);
				if (transportRouteRect != null) {
					rect = transportRouteRect;
				}
			}
		} else if (routingHelper.isRouteCalculated()) {
			RouteCalculationResult result = routingHelper.getRoute();
			QuadRect routeRect = RoutingHelperUtils.getRouteRect(app, result);
			if (routeRect != null) {
				rect = routeRect;
			}
		}
		return rect;
	}

	public void onUpdatedIndexesList() {
		for (BaseCard card : menuCards) {
			if (card instanceof DownloadEvents) {
				((DownloadEvents) card).onUpdatedIndexesList();
			}
		}
	}

	public void downloadInProgress() {
		for (BaseCard card : menuCards) {
			if (card instanceof DownloadEvents) {
				((DownloadEvents) card).downloadInProgress();
			}
		}
	}

	public void downloadHasFinished() {
		for (BaseCard card : menuCards) {
			if (card instanceof DownloadEvents) {
				((DownloadEvents) card).downloadHasFinished();
			}
		}
	}

	public enum MapRouteMenuType {
		ROUTE_INFO,
		ROUTE_DETAILS
	}

	private class MapRouteMenuStateHolder {

		private final MapRouteMenuType type;
		private final int menuState;
		private final Bundle arguments;

		MapRouteMenuStateHolder(MapRouteMenuType type, int menuState, Bundle arguments) {
			this.type = type;
			this.menuState = menuState;
			this.arguments = arguments;
		}

		@DrawableRes
		int getButtonImage() {
			OsmandApplication app = getApp();
			switch (type) {
				case ROUTE_INFO:
					return 0;
				case ROUTE_DETAILS:
					return app != null ? app.getRoutingHelper().getAppMode().getIconRes() : R.drawable.ic_action_gdirections_dark;
				default:
					return 0;
			}
		}

		void showMenu() {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				switch (type) {
					case ROUTE_INFO:
						showInternal(menuState);
						break;
					case ROUTE_DETAILS:
						ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager(), arguments);
						break;
				}
			}
		}

		void onDismiss(@Nullable MapRouteMenuStateHolder openingMenuStateHolder) {
			boolean openingRouteInfo = openingMenuStateHolder != null && openingMenuStateHolder.type == MapRouteMenuType.ROUTE_INFO;
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				if (!openingRouteInfo) {
					switch (type) {
						case ROUTE_INFO:
							break;
						case ROUTE_DETAILS:
							if (!portraitMode) {
								mapActivity.getMapPositionManager().setMapPositionShiftedX(false);
							}
							break;
						default:
							break;
					}
				}
			}
		}
	}
}
