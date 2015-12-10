package net.osmand.plus.mapcontextmenu.other;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.ValueHolder;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.FavoritesListFragment.FavouritesAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.activities.search.SearchAddressActivity;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
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
	private boolean selectFromMapTouch;
	private boolean selectFromMapForTarget;

	private boolean showMenu = false;
	private MapActivity mapActivity;
	private MapControlsLayer mapControlsLayer;
	public static final String TARGET_SELECT = "TARGET_SELECT";

	public MapRouteInfoMenu(MapActivity mapActivity, MapControlsLayer mapControlsLayer) {
		this.mapActivity = mapActivity;
		this.mapControlsLayer = mapControlsLayer;
		contextMenu = mapActivity.getContextMenu();
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
		routingHelper.addListener(this);
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
			contextMenu.showMinimized(latlon, null, null);
			show();
			return true;
		}
		return false;
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

	public void updateInfo(final View main) {
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
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
			TextView textView = (TextView) main.findViewById(R.id.InfoTextView);
			ImageView iconView = (ImageView) main.findViewById(R.id.InfoIcon);
			main.findViewById(R.id.Prev).setVisibility(View.GONE);
			main.findViewById(R.id.Next).setVisibility(View.GONE);
			textView.setText(R.string.route_is_too_long);
			textView.setVisibility(View.VISIBLE);
			iconView.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_warning));
		} else {
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
		}
	}

	private void updateApplicationModes(final View parentView) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final Set<ApplicationMode> selected = new HashSet<>();
		selected.add(am);
		ViewGroup vg = (ViewGroup) parentView.findViewById(R.id.app_modes);
		vg.removeAllViews();
		AppModeDialog.prepareAppModeView(mapActivity, selected, false,
				vg, true, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selected.size() > 0) {
							ApplicationMode next = selected.iterator().next();
							settings.APPLICATION_MODE.set(next);
							mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
						}
					}
				});
	}

	private void updateViaView(final View parentView) {
		String via = generateViaDescription();
		if (via.length() == 0) {
			parentView.findViewById(R.id.ViaLayout).setVisibility(View.GONE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.GONE);
		} else {
			parentView.findViewById(R.id.ViaLayout).setVisibility(View.VISIBLE);
			parentView.findViewById(R.id.viaLayoutDivider).setVisibility(View.VISIBLE);
			((TextView) parentView.findViewById(R.id.ViaView)).setText(via);
		}

		ImageView viaIcon = (ImageView) parentView.findViewById(R.id.viaIcon);
		if (isLight()) {
			viaIcon.setImageDrawable(getIconOrig(R.drawable.widget_intermediate_day));
		} else {
			viaIcon.setImageDrawable(getIconOrig(R.drawable.widget_intermediate_night));
		}
	}

	private void updateToSpinner(final View parentView) {
		final Spinner toSpinner = setupToSpinner(parentView);
		toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 1) {
					selectFavorite(parentView, true);
				} else if (position == 2) {
					selectOnScreen(true);
				} else if (position == 3) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(TARGET_SELECT, true);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		ImageView toIcon = (ImageView) parentView.findViewById(R.id.toIcon);
		if (isLight()) {
			toIcon.setImageDrawable(getIconOrig(R.drawable.widget_target_day));
		} else {
			toIcon.setImageDrawable(getIconOrig(R.drawable.widget_target_night));
		}

		ImageView toDropDownIcon = (ImageView) parentView.findViewById(R.id.toDropDownIcon);
		toDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_arrow_drop_down));
		toDropDownIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toSpinner.performClick();
			}
		});
	}

	@SuppressWarnings("deprecation")
	private void updateFromSpinner(final View parentView) {
		final TargetPointsHelper targets = getTargets();
		final Spinner fromSpinner = setupFromSpinner(parentView);
		fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0) {
					if (targets.getPointToStart() != null) {
						targets.clearStartPoint(true);
					}
				} else if (position == 1) {
					selectFavorite(parentView, false);
				} else if (position == 2) {
					selectOnScreen(false);
				} else if (position == 3) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(TARGET_SELECT, false);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		ImageView fromIcon = (ImageView) parentView.findViewById(R.id.fromIcon);
		ApplicationMode appMode = mapActivity.getMyApplication().getSettings().getApplicationMode();
		fromIcon.setImageDrawable(mapActivity.getResources().getDrawable(appMode.getResourceLocationDay()));

		ImageView fromDropDownIcon = (ImageView) parentView.findViewById(R.id.fromDropDownIcon);
		fromDropDownIcon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_arrow_drop_down));
		fromDropDownIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fromSpinner.performClick();
			}
		});
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
		hide();
		show();
	}

	protected void selectFavorite(final View parentView, final boolean target) {
		final FavouritesAdapter favouritesAdapter = new FavouritesAdapter(mapActivity, mapActivity.getMyApplication()
				.getFavorites().getFavouritePoints(), false);
		Dialog[] dlgHolder = new Dialog[1];
		OnItemClickListener click = getOnClickListener(target, favouritesAdapter, dlgHolder);
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

	private boolean isLight() {
		return mapActivity.getMyApplication().getSettings().isLightContent();
	}

	private Drawable getIconOrig(int iconId) {
		IconsCache iconsCache = mapActivity.getMyApplication().getIconsCache();
		return iconsCache.getIcon(iconId, 0, 0f);
	}

	private OnItemClickListener getOnClickListener(final boolean target, final FavouritesAdapter favouritesAdapter,
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
				//Next 2 lines ensure Dialog is shown in the right correct position after a selection been made
				hide();
				show();
			}
		};
	}

	public static int getDirectionInfo() {
		return directionInfo;
	}

	public boolean isVisible() {
		return findMenuFragment() != null;
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
		mainView.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
		final OsmandApplication ctx = mapActivity.getMyApplication();
		ImageView prev = (ImageView) mainView.findViewById(R.id.Prev);
		prev.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_prev));
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
							mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(),
									mapView.getZoom(), true);
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
		next.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_next));
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1) {
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
					contextMenu.showMinimized(new LatLon(l.getLatitude(), l.getLongitude()), null, info);
					mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
				}
				mapView.refreshMap();
				updateInfo(mainView);
			}

		});
		View info = mainView.findViewById(R.id.Info);
		info.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapView.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapView.getContext().startActivity(intent);
			}
		});

		TextView textView = (TextView) mainView.findViewById(R.id.InfoTextView);
		ImageView iconView = (ImageView) mainView.findViewById(R.id.InfoIcon);
		if (directionInfo >= 0) {
			iconView.setVisibility(View.GONE);
		} else {
			iconView.setImageDrawable(ctx.getIconsCache().getContentIcon(R.drawable.ic_action_info_dark));
			iconView.setVisibility(View.VISIBLE);
		}
		if (directionInfo >= 0 && routingHelper.getRouteDirections() != null
				&& directionInfo < routingHelper.getRouteDirections().size()) {
			RouteDirectionInfo ri = routingHelper.getRouteDirections().get(directionInfo);
			textView.setText((directionInfo + 1) + ". " + ri.getDescriptionRoutePart() + " " + OsmAndFormatter.getFormattedDistance(ri.distance, ctx));
		} else {
			textView.setText(ctx.getRoutingHelper().getGeneralRouteInformation().replace(",", ",\n"));
		}
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
			via.append(getRoutePointDescription(points.get(i).point, points.get(i).getOnlyName()));
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
		ArrayList<String> fromActions = new ArrayList<>();
		fromActions.add(mapActivity.getString(R.string.route_descr_current_location));
		fromActions.add(mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis));
		fromActions.add(mapActivity.getString(R.string.shared_string_select_on_map));
		fromActions.add(mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis));

		TargetPoint start = getTargets().getPointToStart();
		if (start != null) {
			String oname = start.getOnlyName().length() > 0 ? start.getOnlyName()
					: (mapActivity.getString(R.string.route_descr_map_location) + " " + getRoutePointDescription(start.getLatitude(), start.getLongitude()));
			fromActions.add(oname);
		}
		final Spinner fromSpinner = ((Spinner) view.findViewById(R.id.FromSpinner));
		ArrayAdapter<String> fromAdapter = new ArrayAdapter<>(view.getContext(),
				android.R.layout.simple_spinner_item,
				fromActions
		);
		fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(fromAdapter);
		if (start != null) {
			fromSpinner.setSelection(fromActions.size() - 1);
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
		ArrayList<String> toActions = new ArrayList<>();
		if (targets.getPointToNavigate() != null) {
			toActions.add(mapActivity.getString(R.string.route_descr_destination) + " "
					+ getRoutePointDescription(targets.getPointToNavigate().point,
					targets.getPointToNavigate().getOnlyName()));
		} else {
			toSpinner.setPromptId(R.string.route_descr_select_destination);
			toActions.add(mapActivity.getString(R.string.route_descr_select_destination));
		}
		toActions.add(mapActivity.getString(R.string.shared_string_favorite) + mapActivity.getString(R.string.shared_string_ellipsis));
		toActions.add(mapActivity.getString(R.string.shared_string_select_on_map));
		toActions.add(mapActivity.getString(R.string.shared_string_address) + mapActivity.getString(R.string.shared_string_ellipsis));

		ArrayAdapter<String> toAdapter = new ArrayAdapter<>(view.getContext(),
				android.R.layout.simple_spinner_item,
				toActions
		);
		toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		toSpinner.setAdapter(toAdapter);
		return toSpinner;
	}

	private TargetPointsHelper getTargets() {
		return mapActivity.getMyApplication().getTargetPointsHelper();
	}

	@Override
	public void routeWasCancelled() {
		directionInfo = -1;
		// do not hide fragment (needed for use case entering Planning mode without destination)
	}


	public void show() {
		MapRouteInfoMenuFragment.showInstance(mapActivity);
	}

	public void hide() {
		WeakReference<MapRouteInfoMenuFragment> fragmentRef = findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().dismiss();
		}
	}

	public void setShowMenu() {
		showMenu = true;
	}
}
