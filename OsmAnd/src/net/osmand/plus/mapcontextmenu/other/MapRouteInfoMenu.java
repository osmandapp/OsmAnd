package net.osmand.plus.mapcontextmenu.other;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ValueHolder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.GeocodingLookupService.AddressLookupRequest;
import net.osmand.plus.IconsCache;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.FavoritesListFragment.FavouritesAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoDialogFragment;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapmarkers.MapMarkerSelectionFragment;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapRouteInfoMenu implements IRouteInformationListener {
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	private final MapContextMenu contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private GeocodingLookupService geocodingLookupService;
	private boolean selectFromMapTouch;
	private boolean selectFromMapForTarget;

	private boolean showMenu = false;
	private static boolean visible;
	private MapActivity mapActivity;
	private MapControlsLayer mapControlsLayer;
	public static final String TARGET_SELECT = "TARGET_SELECT";
	private boolean nightMode;
	private boolean switched;

	private AddressLookupRequest startPointRequest;
	private AddressLookupRequest targetPointRequest;
	private List<LatLon> intermediateRequestsLatLon = new ArrayList<>();
	private OnDismissListener onDismissListener;

	private OnMarkerSelectListener onMarkerSelectListener;

	private static final long SPINNER_MY_LOCATION_ID = 1;
	private static final long SPINNER_FAV_ID = 2;
	private static final long SPINNER_MAP_ID = 3;
	private static final long SPINNER_ADDRESS_ID = 4;
	private static final long SPINNER_START_ID = 5;
	private static final long SPINNER_FINISH_ID = 6;
	private static final long SPINNER_HINT_ID = 100;
	private static final long SPINNER_MAP_MARKER_1_ID = 301;
	private static final long SPINNER_MAP_MARKER_2_ID = 302;
	private static final long SPINNER_MAP_MARKER_3_ID = 303;
	private static final long SPINNER_MAP_MARKER_MORE_ID = 350;

	public interface OnMarkerSelectListener {
		void onSelect(int index, boolean target);
	}

	public MapRouteInfoMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.mapControlsLayer = mapControlsLayer;
		contextMenu = mapActivity.getContextMenu();
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
		routingHelper.addListener(this);
		geocodingLookupService = mapActivity.getMyApplication().getGeocodingLookupService();
		onMarkerSelectListener = new OnMarkerSelectListener() {
			@Override
			public void onSelect(int index, boolean target) {
				selectMapMarker(index, target);
			}
		};
	}

	public OnDismissListener getOnDismissListener() {
		return onDismissListener;
	}

	public void setOnDismissListener(OnDismissListener onDismissListener) {
		this.onDismissListener = onDismissListener;
	}

	public void cancelSelectionFromMap() {
		selectFromMapTouch = false;
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (selectFromMapTouch) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			selectFromMapTouch = false;
			if (selectFromMapForTarget) {
				getTargets().navigateToPoint(latlon, true, -1);
			} else {
				getTargets().setStartPoint(latlon, true, null);
			}
			show();
			return true;
		}
		return false;
	}

	public OnMarkerSelectListener getOnMarkerSelectListener() {
		return onMarkerSelectListener;
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


	public void showHideMenu() {
		intermediateRequestsLatLon.clear();
		if (isVisible()) {
			hide();
		} else {
			show();
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

	public void updateInfo(final View main) {
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		updateViaView(main);
		updateFromSpinner(main);
		updateToSpinner(main);
		updateApplicationModes(main);
		mapControlsLayer.updateRouteButtons(main, true);
		boolean addButtons = routingHelper.isRouteCalculated();
		if (addButtons) {
			updateRouteButtons(main);
		} else {
			updateRouteCalcProgress(main);
		}
	}

	private void updateRouteCalcProgress(final View main) {
		TargetPointsHelper targets = getTargets();
		if (targets.hasTooLongDistanceToNavigate()) {
			main.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
			TextView textView = (TextView) main.findViewById(R.id.InfoTextView);
			ImageView iconView = (ImageView) main.findViewById(R.id.InfoIcon);
			main.findViewById(R.id.Prev).setVisibility(View.GONE);
			main.findViewById(R.id.Next).setVisibility(View.GONE);
			main.findViewById(R.id.InfoIcon).setVisibility(View.GONE);
			main.findViewById(R.id.DurationIcon).setVisibility(View.GONE);
			main.findViewById(R.id.InfoDistance).setVisibility(View.GONE);
			main.findViewById(R.id.InfoDuration).setVisibility(View.GONE);
			textView.setText(R.string.route_is_too_long_v2);
			textView.setVisibility(View.VISIBLE);
			iconView.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_warning, isLight()));
		} else {
			main.findViewById(R.id.dividerToDropDown).setVisibility(View.GONE);
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
		}
	}

	private void updateApplicationModes(final View parentView) {
		//final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		//ApplicationMode am = settings.APPLICATION_MODE.get();
		final ApplicationMode am = routingHelper.getAppMode();
		final Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = (ViewGroup) parentView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		AppModeDialog.prepareAppModeView(mapActivity, selected, false,
				vg, true, false,true, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selected.size() > 0) {
							ApplicationMode next = selected.iterator().next();
							OsmandPreference<ApplicationMode> appMode
									= mapActivity.getMyApplication().getSettings().APPLICATION_MODE;
							if (routingHelper.isFollowingMode() && appMode.get() == am) {
								appMode.set(next);
								//updateMenu();
							}
							routingHelper.setAppMode(next);
							mapActivity.getMyApplication().initVoiceCommandPlayer(mapActivity, next, true, null, false, false);
							routingHelper.recalculateRouteDueToSettingsChange();
						}
					}
				});
	}

	private void updateViaView(final View parentView) {
		String via = generateViaDescription();
		View viaLayout = parentView.findViewById(R.id.ViaLayout);
		View viaLayoutDivider = parentView.findViewById(R.id.viaLayoutDivider);
		ImageView swapDirectionView = (ImageView) parentView.findViewById(R.id.swap_direction_image_view);
		if (via.length() == 0) {
			viaLayout.setVisibility(View.GONE);
			viaLayoutDivider.setVisibility(View.GONE);
			swapDirectionView.setVisibility(View.VISIBLE);
		} else {
			swapDirectionView.setVisibility(View.GONE);
			viaLayout.setVisibility(View.VISIBLE);
			viaLayoutDivider.setVisibility(View.VISIBLE);
			((TextView) parentView.findViewById(R.id.ViaView)).setText(via);
		}

		viaLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargets().checkPointToNavigateShort()) {
					mapActivity.getMapActions().openIntermediatePointsDialog();
				}
			}
		});

		ImageView viaIcon = (ImageView) parentView.findViewById(R.id.viaIcon);
		viaIcon.setImageDrawable(getIconOrig(R.drawable.list_intermediate));

		swapDirectionView.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_change_navigation_points,
				isLight() ? R.color.route_info_control_icon_color_light : R.color.route_info_control_icon_color_dark));
		swapDirectionView.setOnClickListener(new View.OnClickListener() {
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

					updateFromIcon();
					updateToIcon(parentView);
				}
			}
		});
	}

	private void updateToSpinner(final View parentView) {
		final Spinner toSpinner = setupToSpinner(parentView);
		toSpinner.setClickable(false);
		final View toLayout = parentView.findViewById(R.id.ToLayout);
		toSpinner.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				event.offsetLocation(AndroidUtils.dpToPx(mapActivity, 48f), 0);
				toLayout.onTouchEvent(event);
				return true;
			}
		});
		toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, final long id) {
				parentView.post(new Runnable() {
					@Override
					public void run() {
						if (id == SPINNER_FAV_ID) {
							selectFavorite(parentView, true);
						} else if (id == SPINNER_MAP_ID) {
							selectOnScreen(true);
						} else if (id == SPINNER_ADDRESS_ID) {
							mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.DESTINATION_SELECTION, false);
							setupToSpinner(parentView);
						} else if (id == SPINNER_MAP_MARKER_MORE_ID) {
							selectMapMarker(-1, true);
							setupToSpinner(parentView);
						} else if (id == SPINNER_MAP_MARKER_1_ID) {
							selectMapMarker(0, true);
						} else if (id == SPINNER_MAP_MARKER_2_ID) {
							selectMapMarker(1, true);
						} else if (id == SPINNER_MAP_MARKER_3_ID) {
							selectMapMarker(2, true);
						}
					}
				});
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		toLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toSpinner.performClick();
			}
		});

		updateToIcon(parentView);

		ImageView toDropDownIcon = (ImageView) parentView.findViewById(R.id.toDropDownIcon);
		toDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_arrow_drop_down, isLight()));
	}

	private void updateToIcon(View parentView) {
		ImageView toIcon = (ImageView) parentView.findViewById(R.id.toIcon);
		toIcon.setImageDrawable(getIconOrig(R.drawable.list_destination));
	}

	private void updateFromSpinner(final View parentView) {
		final TargetPointsHelper targets = getTargets();
		final Spinner fromSpinner = setupFromSpinner(parentView);
		fromSpinner.setClickable(false);
		final View fromLayout = parentView.findViewById(R.id.FromLayout);
		fromSpinner.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				event.offsetLocation(AndroidUtils.dpToPx(mapActivity, 48f), 0);
				fromLayout.onTouchEvent(event);
				return true;
			}
		});
		fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, final long id) {
				parentView.post(new Runnable() {
					@Override
					public void run() {
						if (id == SPINNER_MY_LOCATION_ID) {
							if (targets.getPointToStart() != null) {
								targets.clearStartPoint(true);
								mapActivity.getMyApplication().getSettings().backupPointToStart();
							}
							updateFromIcon(parentView);
						} else if (id == SPINNER_FAV_ID) {
							selectFavorite(parentView, false);
						} else if (id == SPINNER_MAP_ID) {
							selectOnScreen(false);
						} else if (id == SPINNER_ADDRESS_ID) {
							mapActivity.showQuickSearch(MapActivity.ShowQuickSearchMode.START_POINT_SELECTION, false);
							setupFromSpinner(parentView);
						} else if (id == SPINNER_MAP_MARKER_MORE_ID) {
							selectMapMarker(-1, false);
							setupFromSpinner(parentView);
						} else if (id == SPINNER_MAP_MARKER_1_ID) {
							selectMapMarker(0, false);
						} else if (id == SPINNER_MAP_MARKER_2_ID) {
							selectMapMarker(1, false);
						} else if (id == SPINNER_MAP_MARKER_3_ID) {
							selectMapMarker(2, false);
						}
					}
				});
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		fromLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fromSpinner.performClick();
			}
		});

		updateFromIcon(parentView);

		ImageView fromDropDownIcon = (ImageView) parentView.findViewById(R.id.fromDropDownIcon);
		fromDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getIcon(R.drawable.ic_action_arrow_drop_down, isLight()));
	}

	public void updateFromIcon(View parentView) {
		((ImageView) parentView.findViewById(R.id.fromIcon)).setImageDrawable(ContextCompat.getDrawable(mapActivity,
				getTargets().getPointToStart() == null ? R.drawable.ic_action_location_color : R.drawable.list_startpoint));
	}

	protected void selectOnScreen(boolean target) {
		selectFromMapTouch = true;
		selectFromMapForTarget = target;
		hide();
	}

	public void selectAddress(String name, LatLon l, final boolean target) {
		PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
		if (target) {
			getTargets().navigateToPoint(l, true, -1, pd);
		} else {
			getTargets().setStartPoint(l, true, pd);
		}
		updateMenu();
	}

	protected void selectFavorite(final View parentView, final boolean target) {
		final FavouritesAdapter favouritesAdapter = new FavouritesAdapter(mapActivity, mapActivity.getMyApplication()
				.getFavorites().getVisibleFavouritePoints(), false);
		Dialog[] dlgHolder = new Dialog[1];
		OnItemClickListener click = getOnFavoriteClickListener(target, favouritesAdapter, dlgHolder);
		OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				if (target) {
					setupToSpinner(parentView);
				} else {
					setupFromSpinner(parentView);
				}
			}
		};
		favouritesAdapter.updateLocation(mapActivity.getMapLocation());
		FavoriteDialogs.showFavoritesDialog(mapActivity, favouritesAdapter, click, dismissListener, dlgHolder, true);
	}

	private void selectMapMarker(final int index, final boolean target) {
		if (index != -1) {
			MapMarker m = mapActivity.getMyApplication().getMapMarkersHelper().getMapMarkers().get(index);
			LatLon point = new LatLon(m.getLatitude(), m.getLongitude());
			if (target) {
				getTargets().navigateToPoint(point, true, -1, m.getPointDescription(mapActivity));
			} else {
				getTargets().setStartPoint(point, true, m.getPointDescription(mapActivity));
			}
			updateFromIcon();

		} else {

			MapMarkerSelectionFragment selectionFragment = MapMarkerSelectionFragment.newInstance(target);
			selectionFragment.show(mapActivity.getSupportFragmentManager(), MapMarkerSelectionFragment.TAG);
		}
	}

	private boolean isLight() {
		return !nightMode;
	}

	private Drawable getIconOrig(int iconId) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, 0);
	}

	private OnItemClickListener getOnFavoriteClickListener(final boolean target, final FavouritesAdapter favouritesAdapter,
														   final Dialog[] dlg) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FavouritePoint fp = favouritesAdapter.getItem(position);
				LatLon point = new LatLon(fp.getLatitude(), fp.getLongitude());
				if (target) {
					getTargets().navigateToPoint(point, true, -1, fp.getPointDescription());
				} else {
					getTargets().setStartPoint(point, true, fp.getPointDescription());
				}
				if (dlg != null && dlg.length > 0 && dlg[0] != null) {
					dlg[0].dismiss();
				}
				updateFromIcon();
			}
		};
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

	private void updateRouteButtons(final View mainView) {
		mainView.findViewById(R.id.dividerToDropDown).setVisibility(View.VISIBLE);
		mainView.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
		final OsmandApplication ctx = mapActivity.getMyApplication();
		ImageView prev = (ImageView) mainView.findViewById(R.id.Prev);
		prev.setImageDrawable(ctx.getIconsCache().getIcon(R.drawable.ic_prev, isLight()));
		if (directionInfo >= 0) {
			prev.setVisibility(View.VISIBLE);
			prev.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (directionInfo >= 0) {
						directionInfo--;
					}
					if (routingHelper.getRouteDirections() != null && directionInfo >= 0) {
						if (routingHelper.getRouteDirections().size() > directionInfo) {
							RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
							net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
							contextMenu.showMinimized(new LatLon(l.getLatitude(), l.getLongitude()), null, info);
							showLocationOnMap(mapActivity, l.getLatitude(), l.getLongitude());
						}
					}
					mapView.refreshMap();
					updateInfo(mainView);
				}

			});
		} else {
			prev.setVisibility(View.GONE);
		}
		ImageView next = (ImageView) mainView.findViewById(R.id.Next);
		next.setVisibility(View.VISIBLE);
		next.setImageDrawable(ctx.getIconsCache().getIcon(R.drawable.ic_next, isLight()));
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1) {
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
					contextMenu.showMinimized(new LatLon(l.getLatitude(), l.getLongitude()), null, info);
					showLocationOnMap(mapActivity, l.getLatitude(), l.getLongitude());
				}
				mapView.refreshMap();
				updateInfo(mainView);
			}

		});
		View info = mainView.findViewById(R.id.Info);
		info.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowRouteInfoDialogFragment.showDialog(mapActivity.getSupportFragmentManager());
			}
		});

		TextView textView = (TextView) mainView.findViewById(R.id.InfoTextView);
		ImageView infoIcon = (ImageView) mainView.findViewById(R.id.InfoIcon);
		ImageView durationIcon = (ImageView) mainView.findViewById(R.id.DurationIcon);
		View infoDistanceView = mainView.findViewById(R.id.InfoDistance);
		View infoDurationView = mainView.findViewById(R.id.InfoDuration);
		if (directionInfo >= 0) {
			infoIcon.setVisibility(View.GONE);
			durationIcon.setVisibility(View.GONE);
			infoDistanceView.setVisibility(View.GONE);
			infoDurationView.setVisibility(View.GONE);
			textView.setVisibility(View.VISIBLE);
		} else {
			infoIcon.setImageDrawable(ctx.getIconsCache().getIcon(R.drawable.ic_action_route_distance, R.color.route_info_unchecked_mode_icon_color));
			infoIcon.setVisibility(View.VISIBLE);
			durationIcon.setImageDrawable(ctx.getIconsCache().getIcon(R.drawable.ic_action_time_span, R.color.route_info_unchecked_mode_icon_color));
			durationIcon.setVisibility(View.VISIBLE);
			infoDistanceView.setVisibility(View.VISIBLE);
			infoDurationView.setVisibility(View.VISIBLE);
			textView.setVisibility(View.GONE);
		}
		if (directionInfo >= 0 && routingHelper.getRouteDirections() != null
				&& directionInfo < routingHelper.getRouteDirections().size()) {
			RouteDirectionInfo ri = routingHelper.getRouteDirections().get(directionInfo);
			if (!ri.getDescriptionRoutePart().endsWith(OsmAndFormatter.getFormattedDistance(ri.distance, ctx))) {
				textView.setText((directionInfo + 1) + ". " + ri.getDescriptionRoutePart() + " " + OsmAndFormatter.getFormattedDistance(ri.distance, ctx));
			} else {
				textView.setText((directionInfo + 1) + ". " + ri.getDescriptionRoutePart());
			}
		} else {
			TextView distanceText = (TextView) mainView.findViewById(R.id.DistanceText);
			TextView durationText = (TextView) mainView.findViewById(R.id.DurationText);
			distanceText.setText(OsmAndFormatter.getFormattedDistance(ctx.getRoutingHelper().getLeftDistance(), ctx));
			durationText.setText(OsmAndFormatter.getFormattedDuration(ctx.getRoutingHelper().getLeftTime(), ctx));
		}
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

	private Spinner setupFromSpinner(View view) {
		List<RouteSpinnerRow> fromActions = new ArrayList<>();
		fromActions.add(new RouteSpinnerRow(SPINNER_MY_LOCATION_ID, R.drawable.ic_action_get_my_location,
				mapActivity.getString(R.string.shared_string_my_location)));
		fromActions.add(new RouteSpinnerRow(SPINNER_FAV_ID, R.drawable.ic_action_fav_dark,
				mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis)));
		fromActions.add(new RouteSpinnerRow(SPINNER_MAP_ID, R.drawable.ic_action_marker_dark,
				mapActivity.getString(R.string.shared_string_select_on_map)));
		fromActions.add(new RouteSpinnerRow(SPINNER_ADDRESS_ID, R.drawable.ic_action_home_dark,
				mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis)));

		TargetPoint start = getTargets().getPointToStart();
		int startPos = -1;
		if (start != null) {
			String oname = start.getOnlyName().length() > 0 ? start.getOnlyName()
					: (mapActivity.getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));
			startPos = fromActions.size();
			fromActions.add(new RouteSpinnerRow(SPINNER_START_ID, R.drawable.ic_action_get_my_location, oname));

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

		addMarkersToSpinner(fromActions);

		final Spinner fromSpinner = ((Spinner) view.findViewById(R.id.FromSpinner));
		RouteSpinnerArrayAdapter fromAdapter = new RouteSpinnerArrayAdapter(view.getContext());
		for (RouteSpinnerRow row : fromActions) {
			fromAdapter.add(row);
		}
		fromSpinner.setAdapter(fromAdapter);
		if (start != null) {
			fromSpinner.setSelection(startPos);
		} else {
			if (mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() == null) {
				fromSpinner.setPromptId(R.string.search_poi_location);
			}
			//fromSpinner.setSelection(0);
		}
		return fromSpinner;
	}

	private Spinner setupToSpinner(View view) {
		final Spinner toSpinner = ((Spinner) view.findViewById(R.id.ToSpinner));
		final TargetPointsHelper targets = getTargets();
		List<RouteSpinnerRow> toActions = new ArrayList<>();

		TargetPoint finish = getTargets().getPointToNavigate();
		if (finish != null) {
			toActions.add(new RouteSpinnerRow(SPINNER_FINISH_ID, R.drawable.ic_action_get_my_location,
					getRoutePointDescription(targets.getPointToNavigate().point,
							targets.getPointToNavigate().getOnlyName())));

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
			toSpinner.setPromptId(R.string.route_descr_select_destination);
			toActions.add(new RouteSpinnerRow(SPINNER_HINT_ID, R.drawable.ic_action_get_my_location,
					mapActivity.getString(R.string.route_descr_select_destination)));
		}
		toActions.add(new RouteSpinnerRow(SPINNER_FAV_ID, R.drawable.ic_action_fav_dark,
				mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis)));
		toActions.add(new RouteSpinnerRow(SPINNER_MAP_ID, R.drawable.ic_action_marker_dark,
				mapActivity.getString(R.string.shared_string_select_on_map)));
		toActions.add(new RouteSpinnerRow(SPINNER_ADDRESS_ID, R.drawable.ic_action_home_dark,
				mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis)));

		addMarkersToSpinner(toActions);

		RouteSpinnerArrayAdapter toAdapter = new RouteSpinnerArrayAdapter(view.getContext());
		for (RouteSpinnerRow row : toActions) {
			toAdapter.add(row);
		}
		toSpinner.setAdapter(toAdapter);
		return toSpinner;
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
		return mapActivity.getMyApplication().getTargetPointsHelper();
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
	}

	public void show() {
		if (!visible) {
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

	private class RouteSpinnerArrayAdapter extends ArrayAdapter<RouteSpinnerRow> {

		public RouteSpinnerArrayAdapter(Context context) {
			super(context, android.R.layout.simple_spinner_item);
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView label = (TextView) super.getView(position, convertView, parent);
			RouteSpinnerRow row = getItem(position);
			label.setText(row.text);
			label.setTextColor(!isLight() ?
					ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_dark) : ContextCompat.getColorStateList(mapActivity, android.R.color.primary_text_light));
			return label;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			long id = getItemId(position);
			TextView label = (TextView) super.getDropDownView(position, convertView, parent);

			RouteSpinnerRow row = getItem(position);
			label.setText(row.text);
			if (id != SPINNER_HINT_ID) {
				Drawable icon = null;
				if (row.icon != null) {
					icon = row.icon;
				} else if (row.iconId > 0) {
					icon = mapActivity.getMyApplication().getIconsCache().getThemedIcon(row.iconId);
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
}
