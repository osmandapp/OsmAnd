package net.osmand.plus.routepreparationmenu;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
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
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.mapcontextmenu.other.FavouritesBottomSheetMenuFragment;
import net.osmand.plus.mapmarkers.MapMarkerSelectionFragment;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.routepreparationmenu.routeCards.BaseRouteCard;
import net.osmand.plus.routepreparationmenu.routeCards.PublicTransportCard;
import net.osmand.plus.routepreparationmenu.routeCards.SimpleRouteCard;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.router.GeneralRouter;
import net.osmand.router.TransportRoutePlanner;

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

public class MapRouteInfoMenu implements IRouteInformationListener {

	public static class MenuState {
		public static final int HEADER_ONLY = 1;
		public static final int HALF_SCREEN = 2;
		public static final int FULL_SCREEN = 4;
	}

	public static int directionInfo = -1;
	private static boolean visible;
	public static boolean controlVisible = false;
	public static boolean chooseRoutesVisible = false;
	public static final String TARGET_SELECT = "TARGET_SELECT";

	private final RoutingHelper routingHelper;
	private final TransportRoutingHelper transportHelper;
	private final RoutingOptionsHelper routingOptionsHelper;
	private GeocodingLookupService geocodingLookupService;

	private boolean selectFromMapTouch;
	private boolean selectFromMapForTarget;
	private boolean selectFromMapForIntermediate;

	private boolean showMenu = false;
	private MapActivity mapActivity;
	private OsmandApplication app;
	private MapControlsLayer mapControlsLayer;
	private boolean nightMode;
	private boolean switched;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;
	private List<LatLon> intermediateRequestsLatLon = new ArrayList<>();
	private OnDismissListener onDismissListener;
	private List<BaseRouteCard> routeCards = new ArrayList<BaseRouteCard>();

	private OnMarkerSelectListener onMarkerSelectListener;
	private StateChangedListener<Void> onStateChangedListener;
	private View mainView;

	private int currentMenuState;
	private boolean portraitMode;

	private static final long SPINNER_MY_LOCATION_ID = 1;
	public static final long SPINNER_FAV_ID = 2;
	public static final long SPINNER_MAP_ID = 3;
	public static final long SPINNER_ADDRESS_ID = 4;
	private static final long SPINNER_START_ID = 5;
	private static final long SPINNER_FINISH_ID = 6;
	private static final long SPINNER_HINT_ID = 100;
	public static final long SPINNER_MAP_MARKER_1_ID = 301;
	public static final long SPINNER_MAP_MARKER_2_ID = 302;
	private static final long SPINNER_MAP_MARKER_3_ID = 303;
	public static final long SPINNER_MAP_MARKER_MORE_ID = 350;

	public interface OnMarkerSelectListener {
		void onSelect(int index, boolean target, boolean intermediate);
	}

	public MapRouteInfoMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.mapControlsLayer = mapControlsLayer;
		this.routingHelper = mapActivity.getRoutingHelper();
		this.transportHelper = routingHelper.getTransportRoutingHelper();
		this.routingOptionsHelper = app.getRoutingOptionsHelper();

		routingHelper.addListener(this);
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		currentMenuState = getInitialMenuState();

		geocodingLookupService = mapActivity.getMyApplication().getGeocodingLookupService();
		onMarkerSelectListener = new OnMarkerSelectListener() {
			@Override
			public void onSelect(int index, boolean target, boolean intermediate) {
				selectMapMarker(index, target, intermediate);
			}
		};
		onStateChangedListener = new StateChangedListener<Void>() {
			@Override
			public void stateChanged(Void change) {
				updateMenu();
			}
		};
	}

	private int getInitialMenuState() {
		if (!portraitMode) {
			return MenuState.FULL_SCREEN;
		} else {
			return getInitialMenuStatePortrait();
		}
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
		if (selectFromMapTouch) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			selectFromMapTouch = false;
			if (selectFromMapForIntermediate) {
				getTargets().navigateToPoint(latlon, true, getTargets().getIntermediatePoints().size());
			} else if (selectFromMapForTarget) {
				getTargets().navigateToPoint(latlon, true, -1);
			} else {
				getTargets().setStartPoint(latlon, true, null);
			}
			show();
			if (selectFromMapForIntermediate && getTargets().checkPointToNavigateShort()) {
				WaypointsFragment.showInstance(mapActivity);
			}
			return true;
		}
		return false;
	}

	public OnMarkerSelectListener getOnMarkerSelectListener() {
		return onMarkerSelectListener;
	}

	public void addTargetPointListener() {
		app.getTargetPointsHelper().addListener(onStateChangedListener);
	}

	private void removeTargetPointListener() {
		app.getTargetPointsHelper().removeListener(onStateChangedListener);
	}

	private void cancelStartPointAddressRequest() {
		if (startPointRequest != null) {
			geocodingLookupService.cancel(startPointRequest);
			startPointRequest = null;
		}
	}

	private void cancelTargetPointAddressRequest() {
		if (targetPointRequest != null) {
			geocodingLookupService.cancel(targetPointRequest);
			targetPointRequest = null;
		}
	}

	public void setVisible(boolean visible) {
		if (visible) {
			if (showMenu) {
				show();
				showMenu = false;
			}
			controlVisible = true;
		} else {
			hide();
			controlVisible = false;
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

	public void showHideMenu() {
		intermediateRequestsLatLon.clear();
		if (isVisible()) {
			hide();
		} else {
			show();
		}
	}

	public void updateRouteCalculationProgress(int progress) {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().updateRouteCalculationProgress(progress);
			fragmentRef.get().updateControlButtons();
		}
	}

	public void routeCalculationFinished() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null && fragmentRef.get().isVisible()) {
			fragmentRef.get().hideRouteCalculationProgressBar();
			fragmentRef.get().updateControlButtons();
			fragmentRef.get().updateInfo();
			if (currentMenuState == MenuState.HEADER_ONLY) {
				fragmentRef.get().openMenuHalfScreen();
			}
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

	public void updateFromIcon() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null)
			fragmentRef.get().updateFromIcon();
	}

	public void build(LinearLayout rootView) {
		rootView.removeAllViews();
		for (BaseRouteCard card : routeCards) {
			rootView.addView(card.build(mapActivity));
		}
	}

	public void updateInfo(final View main) {
		mainView = main;
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();

		updateStartPointView();
		updateWaypointsView();
		updateFinishPointView();

		updateApplicationModes();
		updateApplicationModesOptions();
		updateOptionsButtons();

		routeCards.clear();

		if (isBasicRouteCalculated()) {
			GPXUtilities.GPXFile gpx = GPXUtilities.makeGpxFromRoute(routingHelper.getRoute());
			if (gpx != null) {
				routeCards.add(new SimpleRouteCard(mapActivity, gpx));
				LinearLayout cardsContainer = (LinearLayout) mainView.findViewById(R.id.route_menu_cards_container);
				build(cardsContainer);
			}
		} else if (isTransportRouteCalculated()) {
			List<TransportRoutePlanner.TransportRouteResult> routes = transportHelper.getRoutes();
			for (int i = 0; i < routes.size(); i++) {
				PublicTransportCard card = new PublicTransportCard(mapActivity, routes.get(i), i);
				card.setShowBottomShadow(i == routes.size() - 1);
				card.setShowTopShadow(i != 0);
				routeCards.add(card);
			}
			LinearLayout cardsContainer = (LinearLayout) mainView.findViewById(R.id.route_menu_cards_container);
			build(cardsContainer);
		} else {
			updateRouteCalcProgress(main);
		}
	}

	public boolean isRouteCalculated() {
		return isBasicRouteCalculated() || isTransportRouteCalculated();
	}

	public boolean isTransportRouteCalculated() {
		return routingHelper.isPublicTransportMode() && transportHelper.getRoutes() != null;
	}

	public boolean isBasicRouteCalculated() {
		return routingHelper.getFinalLocation() != null && routingHelper.isRouteCalculated();
	}

	public void updateApplicationModesOptions() {
		AppCompatImageView foldButtonView = (AppCompatImageView) mainView.findViewById(R.id.fold_button);
		foldButtonView.setImageResource(currentMenuState == MenuState.HEADER_ONLY ?
				R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down);
		foldButtonView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				expandCollapse();
			}
		});

		mainView.findViewById(R.id.app_modes_options).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				availableProfileDialog();
			}
		});
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
		AlertDialog.Builder b = new AlertDialog.Builder(mapActivity);
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final List<ApplicationMode> modes = ApplicationMode.allPossibleValues();
		modes.remove(ApplicationMode.DEFAULT);
		final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>(ApplicationMode.values(mapActivity.getMyApplication()));
		selected.remove(ApplicationMode.DEFAULT);
		View v = AppModeDialog.prepareAppModeView(mapActivity, modes, selected, null, false, true, false,
				new View.OnClickListener() {

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

	private void updateApplicationMode(ApplicationMode mode, ApplicationMode next) {
		OsmandPreference<ApplicationMode> appMode
				= mapActivity.getMyApplication().getSettings().APPLICATION_MODE;
		if (routingHelper.isFollowingMode() && appMode.get() == mode) {
			appMode.set(next);
		}
		routingHelper.setAppMode(next);
		mapActivity.getMyApplication().initVoiceCommandPlayer(mapActivity, next, true, null, false, false);
		routingHelper.recalculateRouteDueToSettingsChange();
	}

	private void updateRouteCalcProgress(final View main) {
		LinearLayout cardsContainer = (LinearLayout) main.findViewById(R.id.route_menu_cards_container);
		if (cardsContainer != null) {
			cardsContainer.removeAllViews();
		}
	}

	private void updateApplicationModes() {
		final ApplicationMode am = routingHelper.getAppMode();
		final Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = (ViewGroup) mainView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selected.size() > 0) {
					ApplicationMode next = selected.iterator().next();
					updateApplicationMode(am, next);
				}
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
			AppModeDialog.updateButtonState2((OsmandApplication) mapActivity.getApplication(), values, selected, listener, buttons, i, true, true, nightMode);
		}
	}

	private void updateOptionsButtons() {
		final boolean nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		final OsmandSettings settings = app.getSettings();
		final int colorActive = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		final int colorDisabled = ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);
		final ApplicationMode applicationMode = routingHelper.getAppMode();
		final RoutingOptionsHelper.RouteMenuAppModes mode = routingOptionsHelper.modes.get(applicationMode);
		int margin = AndroidUtils.dpToPx(app, 3);

		View startButton = mainView.findViewById(R.id.start_button);
		if (isRouteCalculated()) {
			AndroidUtils.setBackground(app, startButton, nightMode, R.color.active_buttons_and_links_light, R.color.active_buttons_and_links_dark);
			int color = nightMode ? R.color.main_font_dark : R.color.card_and_list_background_light;
			((TextView) mainView.findViewById(R.id.start_button_descr)).setTextColor(ContextCompat.getColor(app, color));
			((ImageView) mainView.findViewById(R.id.start_icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, color));
		} else {
			AndroidUtils.setBackground(app, startButton, nightMode, R.color.activity_background_light, R.color.route_info_cancel_button_color_dark);
			int color = R.color.description_font_and_bottom_sheet_icons;
			((TextView) mainView.findViewById(R.id.start_button_descr)).setTextColor(ContextCompat.getColor(app, color));
			((ImageView) mainView.findViewById(R.id.start_icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_start_navigation, color));
		}
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRouteGo();
			}
		});

		View cancelButton = mainView.findViewById(R.id.cancel_button);
		AndroidUtils.setBackground(app, cancelButton, nightMode, R.color.card_and_list_background_light, R.color.card_and_list_background_dark);
		cancelButton.setOnClickListener(new View.OnClickListener() {
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
		optionsButton.setOnClickListener(new View.OnClickListener() {
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
			if (parameter instanceof RoutingOptionsHelper.MuteSoundRoutingParameter) {
				String text = null;
				boolean active = !app.getRoutingHelper().getVoiceRouter().isMute();
				if (mode.parameters.size() <= 2) {
					text = app.getString(active ? R.string.shared_string_on : R.string.shared_string_off);
				}
				View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.switchSound();
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
				});
				optionsContainer.addView(item, lp);
			} else if (parameter instanceof RoutingOptionsHelper.ShowAlongTheRouteItem) {
				final Set<PoiUIFilter> poiFilters = app.getPoiFilters().getSelectedPoiFilters();
				final boolean traffic = app.getSettings().SHOW_TRAFFIC_WARNINGS.getModeValue(applicationMode);
				final boolean fav = app.getSettings().SHOW_NEARBY_FAVORITES.getModeValue(applicationMode);
				if (!poiFilters.isEmpty()) {
					final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
					Iterator<PoiUIFilter> it = poiFilters.iterator();
					while (it.hasNext()) {
						final PoiUIFilter poiUIFilter = it.next();
						final LinearLayout container = createToolbarSubOptionView(true, poiUIFilter.getName(), R.drawable.ic_action_remove_dark, !it.hasNext(), new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								app.getPoiFilters().removeSelectedPoiFilter(poiUIFilter);
								mapActivity.getMapView().refreshMap();
								updateOptionsButtons();
							}
						});
						item.addView(container, newLp);
					}
					optionsContainer.addView(item, lp);
				}
				if (traffic) {
					final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
					final LinearLayout container = createToolbarSubOptionView(true, app.getString(R.string.way_alarms), R.drawable.ic_action_remove_dark, true, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							app.getWaypointHelper().enableWaypointType(WaypointHelper.ALARMS, false);
							updateOptionsButtons();
						}
					});
					AndroidUtils.setBackground(app, container, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
					item.addView(container, newLp);
					optionsContainer.addView(item, lp);
				}
				if (fav) {
					final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
					item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
					final LinearLayout container = createToolbarSubOptionView(true, app.getString(R.string.favourites), R.drawable.ic_action_remove_dark, true, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							app.getWaypointHelper().enableWaypointType(WaypointHelper.FAVORITES, false);
							updateOptionsButtons();
						}
					});
					AndroidUtils.setBackground(app, container, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
					item.addView(container, newLp);
					optionsContainer.addView(item, lp);
				}
			} else if (parameter instanceof RoutingOptionsHelper.AvoidRoadsTypesRoutingParameter) {
				final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
				item.findViewById(R.id.route_option_container).setVisibility(View.GONE);

				List<GeneralRouter.RoutingParameter> avoidParameters = routingOptionsHelper.getAvoidRoutingPrefsForAppMode(applicationMode);
				final List<GeneralRouter.RoutingParameter> avoidedParameters = new ArrayList<GeneralRouter.RoutingParameter>();
				for (int i = 0; i < avoidParameters.size(); i++) {
					GeneralRouter.RoutingParameter p = avoidParameters.get(i);
					OsmandSettings.CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());
					if (preference != null && preference.get()) {
						avoidedParameters.add(p);
					}
				}
				if (avoidedParameters.isEmpty()) {
					continue;
				}
				for (int i = 0; i < avoidedParameters.size(); i++) {
					final GeneralRouter.RoutingParameter routingParameter = avoidedParameters.get(i);
					final LinearLayout container = createToolbarSubOptionView(false, SettingsBaseActivity.getRoutingStringPropertyName(app, routingParameter.getId(), routingParameter.getName()), R.drawable.ic_action_remove_dark, i == avoidedParameters.size() - 1, new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							OsmandSettings.CommonPreference<Boolean> preference = settings.getCustomRoutingBooleanProperty(routingParameter.getId(), routingParameter.getDefaultBoolean());
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
					item.addView(container, newLp);
				}
				optionsContainer.addView(item, lp);
			} else if (parameter instanceof RoutingOptionsHelper.AvoidRoadsRoutingParameter) {
				final LinearLayout item = createToolbarOptionView(false, null, -1, -1, null);
				item.findViewById(R.id.route_option_container).setVisibility(View.GONE);
				AvoidSpecificRoads avoidSpecificRoads = app.getAvoidSpecificRoads();
				Map<LatLon, RouteDataObject> impassableRoads = avoidSpecificRoads.getImpassableRoads();
				if (impassableRoads.isEmpty()) {
					continue;
				}
				Iterator<RouteDataObject> it = impassableRoads.values().iterator();
				while (it.hasNext()) {
					final RouteDataObject routeDataObject = it.next();
					final LinearLayout container = createToolbarSubOptionView(false, avoidSpecificRoads.getText(routeDataObject), R.drawable.ic_action_remove_dark, !it.hasNext(), new View.OnClickListener() {
						@Override
						public void onClick(View v) {
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
					});
					item.addView(container, newLp);
				}
				optionsContainer.addView(item, lp);
			} else if (parameter instanceof RoutingOptionsHelper.LocalRoutingParameterGroup) {
				final RoutingOptionsHelper.LocalRoutingParameterGroup group = (RoutingOptionsHelper.LocalRoutingParameterGroup) parameter;
				String text = null;
				RoutingOptionsHelper.LocalRoutingParameter selected = group.getSelected(settings);
				if (selected != null) {
					text = group.getText(mapActivity);
				}
				View item = createToolbarOptionView(false, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.showLocalRoutingParameterGroupDialog(group, mapActivity, new RoutingOptionsHelper.OnClickListener() {
							@Override
							public void onClick() {
								updateOptionsButtons();
							}
						});
					}
				});
				optionsContainer.addView(item, lp);
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
					View item = createToolbarOptionView(active, text, parameter.getActiveIconId(), parameter.getDisabledIconId(), new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (parameter.routingParameter != null) {
								boolean selected = !parameter.isSelected(settings);
								routingOptionsHelper.applyRoutingParameter(parameter, selected);

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
					LinearLayout.LayoutParams newLp2 = new LinearLayout.LayoutParams(AndroidUtils.dpToPx(app, 100), ViewGroup.LayoutParams.MATCH_PARENT);
					newLp2.setMargins(margin, 0, margin, 0);
					optionsContainer.addView(item, newLp2);
				}
			}
		}
		int rightPadding = AndroidUtils.dpToPx(app, 70);
		if (optionsTitle.getVisibility() == View.VISIBLE) {
			rightPadding += AndroidUtils.getTextWidth(app.getResources().getDimensionPixelSize(R.dimen.text_button_text_size), app.getString(R.string.shared_string_options));
		}
		optionsContainer.setPadding(optionsContainer.getPaddingLeft(), optionsContainer.getPaddingTop(), rightPadding, optionsContainer.getPaddingBottom());
	}

	private LinearLayout createToolbarOptionView(boolean active, String title, @DrawableRes int activeIconId, @DrawableRes int disabledIconId, View.OnClickListener listener) {
		final LinearLayout item = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.route_option_btn, null);
		final TextView textView = (TextView) item.findViewById(R.id.route_option_title);
		final ImageView imageView = (ImageView) item.findViewById(R.id.route_option_image_view);
		final int colorActive = ContextCompat.getColor(app, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
		final int colorDisabled = ContextCompat.getColor(app, R.color.description_font_and_bottom_sheet_icons);

		AndroidUtils.setBackground(app, item, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, item.findViewById(R.id.route_option_container), nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
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

	private LinearLayout createToolbarSubOptionView(boolean hideTextLine, String title, @DrawableRes int iconId, boolean lastItem, View.OnClickListener listener) {
		final LinearLayout container = (LinearLayout) mapActivity.getLayoutInflater().inflate(R.layout.route_options_container, null);
		final TextView routeOptionTV = (TextView) container.findViewById(R.id.route_removable_option_title);
		final ImageView routeOptionImageView = (ImageView) container.findViewById(R.id.removable_option_icon);

		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setForeground(app, container, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
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
		if (getTargets().getPointToNavigate() != null) {
			hide();
		}
		if (isTransportRouteCalculated()) {
			ChooseRouteFragment.showInstance(mapActivity.getSupportFragmentManager());
		} else {
			mapControlsLayer.startNavigation();
		}
	}

	private void clickRouteCancel() {
		mapControlsLayer.stopNavigation();
	}

	private void clickRouteParams() {
		RouteOptionsBottomSheet.showInstance(mapActivity.getSupportFragmentManager());
	}

	private void updateWaypointsView() {
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
			((TextView) mainView.findViewById(R.id.ViaSubView)).setText(app.getString(R.string.intermediate_destinations, getTargets().getIntermediatePoints().size()));
		}
		FrameLayout viaButton = (FrameLayout) mainView.findViewById(R.id.via_button);

		viaButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargets().checkPointToNavigateShort()) {
					WaypointsFragment.showInstance(mapActivity);
				}
			}
		});

		ImageView viaIcon = (ImageView) mainView.findViewById(R.id.viaIcon);
		viaIcon.setImageDrawable(getIconOrig(R.drawable.list_intermediate));
		LinearLayout viaButtonContainer = (LinearLayout) mainView.findViewById(R.id.via_button_container);

		AndroidUtils.setBackground(app, viaButton, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, viaButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, viaButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		}
		ImageView viaButtonImageView = (ImageView) mainView.findViewById(R.id.via_button_image_view);

		Drawable normal = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_edit_dark, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_edit_dark, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

			normal = AndroidUtils.createPressedStateListDrawable(normal, active);
		}
		viaButtonImageView.setImageDrawable(normal);
	}

	private void updateFinishPointView() {
		setupToText(mainView);
		final View toLayout = mainView.findViewById(R.id.ToLayout);
		toLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAddPointDialog(true, false);
			}
		});

		final FrameLayout toButton = (FrameLayout) mainView.findViewById(R.id.to_button);
		final LinearLayout toButtonContainer = (LinearLayout) mainView.findViewById(R.id.to_button_container);

		AndroidUtils.setBackground(app, toButton, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, toButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, toButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		}
		ImageView toButtonImageView = (ImageView) mainView.findViewById(R.id.to_button_image_view);

		Drawable normal = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_plus, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_plus, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);

			normal = AndroidUtils.createPressedStateListDrawable(normal, active);
		}

		toButtonImageView.setImageDrawable(normal);
		toButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openAddPointDialog(false, true);
			}
		});

		updateToIcon(mainView);
	}

	private void updateToIcon(View parentView) {
		ImageView toIcon = (ImageView) parentView.findViewById(R.id.toIcon);
		toIcon.setImageDrawable(getIconOrig(R.drawable.list_destination));
	}

	private void updateStartPointView() {
		setupFromText(mainView);
		final View fromLayout = mainView.findViewById(R.id.FromLayout);
		fromLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openAddPointDialog(false, false);
			}
		});

		FrameLayout fromButton = (FrameLayout) mainView.findViewById(R.id.from_button);
		final LinearLayout fromButtonContainer = (LinearLayout) mainView.findViewById(R.id.from_button_container);

		AndroidUtils.setBackground(app, fromButton, nightMode, R.drawable.btn_border_trans_rounded_light, R.drawable.btn_border_trans_rounded_dark);
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, fromButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		} else {
			AndroidUtils.setBackground(app, fromButtonContainer, nightMode, R.drawable.ripple_rounded_light, R.drawable.ripple_rounded_dark);
		}

		ImageView swapDirectionView = (ImageView) mainView.findViewById(R.id.from_button_image_view);

		Drawable normal = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_change_navigation_points, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable active = mapActivity.getMyApplication().getUIUtilities().getIcon(R.drawable.ic_action_change_navigation_points, nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light);
			normal = AndroidUtils.createPressedStateListDrawable(normal, active);
		}

		swapDirectionView.setImageDrawable(normal);
		fromButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				TargetPointsHelper targetPointsHelper = getTargets();
				TargetPoint startPoint = targetPointsHelper.getPointToStart();
				TargetPoint endPoint = targetPointsHelper.getPointToNavigate();

				if (startPoint == null) {
					Location loc = mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation();
					if (loc != null) {
						startPoint = TargetPoint.createStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()),
								new PointDescription(PointDescription.POINT_TYPE_MY_LOCATION,
										mapActivity.getString(R.string.shared_string_my_location)));
					}
				}

				if (startPoint != null && endPoint != null) {
					targetPointsHelper.navigateToPoint(startPoint.point, false, -1, startPoint.getPointDescription(mapActivity));
					targetPointsHelper.setStartPoint(endPoint.point, false, endPoint.getPointDescription(mapActivity));
					targetPointsHelper.updateRouteAndRefresh(true);
				}
			}
		});

		updateFromIcon(mainView);
	}

	public void updateFromIcon(View parentView) {
		((ImageView) parentView.findViewById(R.id.fromIcon)).setImageDrawable(ContextCompat.getDrawable(mapActivity,
				getTargets().getPointToStart() == null ? R.drawable.ic_action_location_color : R.drawable.list_startpoint));
	}

	public void selectOnScreen(boolean target, boolean intermediate) {
		selectFromMapTouch = true;
		selectFromMapForTarget = target;
		selectFromMapForIntermediate = intermediate;
		hide();
	}

	public void selectAddress(String name, LatLon l, final boolean target, final boolean intermediate) {
		PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
		if (intermediate) {
			getTargets().navigateToPoint(l, true, getTargets().getIntermediatePoints().size(), pd);
		} else if (target) {
			getTargets().navigateToPoint(l, true, -1, pd);
		} else {
			getTargets().setStartPoint(l, true, pd);
		}
		updateMenu();
	}

	public void selectFavorite(@Nullable final View parentView, final boolean target, final boolean intermediate) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		FavouritesBottomSheetMenuFragment fragment = new FavouritesBottomSheetMenuFragment();
		Bundle args = new Bundle();
		args.putBoolean(FavouritesBottomSheetMenuFragment.TARGET, target);
		args.putBoolean(FavouritesBottomSheetMenuFragment.INTERMEDIATE, intermediate);
		fragment.setArguments(args);
		fragment.show(fragmentManager, FavouritesBottomSheetMenuFragment.TAG);
	}

	public void setupSpinners(final boolean target, final boolean intermediate) {
		if (!intermediate && mainView != null) {
			if (target) {
				setupToText(mainView);
			} else {
				setupFromText(mainView);
			}
		}
	}

	public void selectMapMarker(final int index, final boolean target, final boolean intermediate) {
		if (index != -1) {
			MapMarker m = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers().get(index);
			LatLon point = new LatLon(m.getLatitude(), m.getLongitude());
			if (intermediate) {
				getTargets().navigateToPoint(point, true, getTargets().getIntermediatePoints().size(), m.getPointDescription(mapActivity));
			} else if (target) {
				getTargets().navigateToPoint(point, true, -1, m.getPointDescription(mapActivity));
			} else {
				getTargets().setStartPoint(point, true, m.getPointDescription(mapActivity));
			}
			updateFromIcon();
		} else {
			MapMarkerSelectionFragment selectionFragment = MapMarkerSelectionFragment.newInstance(target, intermediate);
			selectionFragment.show(mapActivity.getSupportFragmentManager(), MapMarkerSelectionFragment.TAG);
		}
	}

	private void openAddPointDialog(final boolean target, final boolean intermediate) {
		Bundle args = new Bundle();
		args.putBoolean(AddPointBottomSheetDialog.TARGET_KEY, target);
		args.putBoolean(AddPointBottomSheetDialog.INTERMEDIATE_KEY, intermediate);
		AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
		fragment.setArguments(args);
		fragment.setUsedOnMap(false);
		fragment.show(mapActivity.getSupportFragmentManager(), AddPointBottomSheetDialog.TAG);
	}

	private boolean isLight() {
		return !nightMode;
	}

	private Drawable getIconOrig(int iconId) {
		UiUtilities iconsCache = mapActivity.getMyApplication().getUIUtilities();
		return iconsCache.getIcon(iconId, 0);
	}

	public static int getDirectionInfo() {
		return directionInfo;
	}

	public static boolean isVisible() {
		return visible;
	}

	public WeakReference<MapRouteInfoMenuFragment> findMenuFragment() {
		Fragment fragment = mapActivity.getSupportFragmentManager().findFragmentByTag(MapRouteInfoMenuFragment.TAG);
		if (fragment != null && !fragment.isDetached()) {
			return new WeakReference<>((MapRouteInfoMenuFragment) fragment);
		} else {
			return null;
		}
	}

	public static boolean isControlVisible() {
		return controlVisible;
	}

	public static void showLocationOnMap(MapActivity mapActivity, double latitude, double longitude) {
		RotatedTileBox tb = mapActivity.getMapView().getCurrentRotatedTileBox().copy();
		int tileBoxWidthPx = 0;
		int tileBoxHeightPx = 0;

		MapRouteInfoMenu routeInfoMenu = mapActivity.getMapLayers().getMapControlsLayer().getMapRouteInfoMenu();
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
		TargetPointsHelper targets = getTargets();
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
				geocodingLookupService.lookupAddress(lookupRequest);
			}
		}
		return via.toString();
	}

	public String getRoutePointDescription(double lat, double lon) {
		return mapActivity.getString(R.string.route_descr_lat_lon, lat, lon);
	}

	public String getRoutePointDescription(LatLon l, String d) {
		if (d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		if (l != null) {
			return mapActivity.getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
		}
		return "";
	}

	private void setupFromText(View view) {
		TargetPoint start = getTargets().getPointToStart();
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
				geocodingLookupService.lookupAddress(startPointRequest);
			}
		}

		final TextView fromText = ((TextView) view.findViewById(R.id.fromText));
		if (start != null) {
			fromText.setText(name);
		} else {
			fromText.setText(R.string.shared_string_my_location);
		}
	}

	private void setupToText(View view) {
		final TextView toText = ((TextView) view.findViewById(R.id.toText));
		final TargetPointsHelper targets = getTargets();
		TargetPoint finish = getTargets().getPointToNavigate();
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
				geocodingLookupService.lookupAddress(targetPointRequest);
			}

		} else {
			toText.setText(R.string.route_descr_select_destination);
		}
	}

	public RoutePopupListArrayAdapter getIntermediatesPopupAdapter(Context ctx) {
		List<RouteSpinnerRow> viaActions = new ArrayList<>();

		viaActions.add(new RouteSpinnerRow(SPINNER_FAV_ID, R.drawable.ic_action_fav_dark,
				mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis)));
		viaActions.add(new RouteSpinnerRow(SPINNER_MAP_ID, R.drawable.ic_action_marker_dark,
				mapActivity.getString(R.string.shared_string_select_on_map)));
		viaActions.add(new RouteSpinnerRow(SPINNER_ADDRESS_ID, R.drawable.ic_action_home_dark,
				mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis)));

		addMarkersToSpinner(viaActions);

		RoutePopupListArrayAdapter viaAdapter = new RoutePopupListArrayAdapter(ctx);
		for (RouteSpinnerRow row : viaActions) {
			viaAdapter.add(row);
		}

		return viaAdapter;
	}

	private void addMarkersToSpinner(List<RouteSpinnerRow> actions) {
		MapMarkersHelper markersHelper = mapActivity.getMyApplication().getMapMarkersHelper();
		List<MapMarker> markers = markersHelper.getMapMarkers();
		if (markers.size() > 0) {
			MapMarker m = markers.get(0);
			actions.add(new RouteSpinnerRow(SPINNER_MAP_MARKER_1_ID,
					MapMarkerDialogHelper.getMapMarkerIcon(mapActivity.getMyApplication(), m.colorIndex),
					m.getName(mapActivity)));
		}
		if (markers.size() > 1) {
			MapMarker m = markers.get(1);
			actions.add(new RouteSpinnerRow(SPINNER_MAP_MARKER_2_ID,
					MapMarkerDialogHelper.getMapMarkerIcon(mapActivity.getMyApplication(), m.colorIndex),
					m.getName(mapActivity)));
		}
		/*
		if (markers.size() > 2) {
			MapMarker m = markers.get(2);
			actions.add(new RouteSpinnerRow(SPINNER_MAP_MARKER_3_ID,
					MapMarkerDialogHelper.getMapMarkerIcon(mapActivity.getMyApplication(), m.colorIndex),
					m.getOnlyName()));
		}
		*/
		if (markers.size() > 2) {
			actions.add(new RouteSpinnerRow(SPINNER_MAP_MARKER_MORE_ID, 0,
					mapActivity.getString(R.string.map_markers_other)));
		}
	}

	private TargetPointsHelper getTargets() {
		return app.getTargetPointsHelper();
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
		visible = false;
		mapActivity.getMapView().setMapPositionX(0);
		mapActivity.getMapView().refreshMap();
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
		AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
		if (switched) {
			mapControlsLayer.switchToRouteFollowingLayout();
		}
		if (getTargets().getPointToNavigate() == null && !selectFromMapTouch) {
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
		}
		if (onDismissListener != null) {
			onDismissListener.onDismiss(null);
		}
		removeTargetPointListener();
	}

	public void show() {
		if (!visible) {
			currentMenuState = getInitialMenuState();
			visible = true;
			switched = mapControlsLayer.switchToRoutePlanningLayout();
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
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		} else {
			visible = false;
		}
		routingHelper.removeListener(this);
	}

	public void setShowMenu() {
		showMenu = true;
	}

	private class RouteSpinnerRow {
		long id;
		int iconId;
		Drawable icon;
		String text;

		public RouteSpinnerRow(long id) {
			this.id = id;
		}

		public RouteSpinnerRow(long id, int iconId, String text) {
			this.id = id;
			this.iconId = iconId;
			this.text = text;
		}

		public RouteSpinnerRow(long id, Drawable icon, String text) {
			this.id = id;
			this.icon = icon;
			this.text = text;
		}
	}

	private class RouteBaseArrayAdapter extends ArrayAdapter<RouteSpinnerRow> {

		RouteBaseArrayAdapter(@NonNull Context context, int resource) {
			super(context, resource);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getItemId(int position) {
			RouteSpinnerRow row = getItem(position);
			return row.id;
		}

		@Override
		public boolean isEnabled(int position) {
			long id = getItemId(position);
			return id != SPINNER_HINT_ID;
		}

		View getRowItemView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getView(position, convertView, parent);
			RouteSpinnerRow row = getItem(position);
			label.setText(row.text);
			label.setTextColor(!isLight() ?
					ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_dark) : ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_light));
			return label;
		}

		View getListItemView(int position, View convertView, ViewGroup parent) {
			long id = getItemId(position);
			TextView label = (TextView) super.getDropDownView(position, convertView, parent);

			RouteSpinnerRow row = getItem(position);
			label.setText(row.text);
			if (id != SPINNER_HINT_ID) {
				Drawable icon = null;
				if (row.icon != null) {
					icon = row.icon;
				} else if (row.iconId > 0) {
					icon = mapActivity.getMyApplication().getUIUtilities().getThemedIcon(row.iconId);
				}
				label.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
				label.setCompoundDrawablePadding(AndroidUtils.dpToPx(mapActivity, 16f));
			} else {
				label.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
				label.setCompoundDrawablePadding(0);
			}

			if (id == SPINNER_MAP_MARKER_MORE_ID) {
				label.setTextColor(!mapActivity.getMyApplication().getSettings().isLightContent() ?
						mapActivity.getResources().getColor(R.color.color_dialog_buttons_dark) : mapActivity.getResources().getColor(R.color.color_dialog_buttons_light));
			} else {
				label.setTextColor(!mapActivity.getMyApplication().getSettings().isLightContent() ?
						ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_dark) : ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_light));
			}
			label.setPadding(AndroidUtils.dpToPx(mapActivity, 16f), 0, 0, 0);

			return label;
		}
	}

	private class RouteSpinnerArrayAdapter extends RouteBaseArrayAdapter {

		RouteSpinnerArrayAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			return getRowItemView(position, convertView, parent);
		}

		@Override
		public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
			return getListItemView(position, convertView, parent);
		}
	}

	private class RoutePopupListArrayAdapter extends RouteBaseArrayAdapter {

		RoutePopupListArrayAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_dropdown_item);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			return getListItemView(position, convertView, parent);
		}
	}

	public enum PermanentAppModeOptions {

		CAR(RoutingOptionsHelper.MuteSoundRoutingParameter.KEY, RoutingOptionsHelper.AvoidRoadsRoutingParameter.KEY),

		BICYCLE(RoutingOptionsHelper.MuteSoundRoutingParameter.KEY, DRIVING_STYLE, GeneralRouter.USE_HEIGHT_OBSTACLES),

		PEDESTRIAN(RoutingOptionsHelper.MuteSoundRoutingParameter.KEY, GeneralRouter.USE_HEIGHT_OBSTACLES);

		List<String> routingParameters;

		PermanentAppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}