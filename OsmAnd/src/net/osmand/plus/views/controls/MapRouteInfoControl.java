package net.osmand.plus.views.controls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.FavoritesListFragment.FavouritesAdapter;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.activities.search.SearchAddressActivity;
import net.osmand.plus.activities.search.SearchAddressFragment;
import net.osmand.plus.development.OsmandDevelopmentPlugin;
import net.osmand.plus.dialogs.FavoriteDialogs;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class MapRouteInfoControl implements IRouteInformationListener {
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	private final ContextMenuLayer contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private Dialog dialog;
	private boolean selectFromMapTouch; 
	private boolean selectFromMapForTarget;

	private boolean showDialog = false;
	private MapActivity mapActivity;

	public MapRouteInfoControl(ContextMenuLayer contextMenu,
			MapActivity mapActivity) {
		this.contextMenu = contextMenu;
		this.mapActivity = mapActivity;
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
		routingHelper.addListener(this);
	}
	
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if(selectFromMapTouch) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			selectFromMapTouch = false;
			if(selectFromMapForTarget) {
				getTargets().navigateToPoint(latlon, true, -1);
			} else {
				getTargets().setStartPoint(latlon, true, null);
			}
			contextMenu.setLocation(latlon, null);
			showDialog();
			return true;
		}
		return false;
	}
	
	public void setVisible(boolean visible) {
		if(visible) {
			if (showDialog){
				if (getTargets().getPointToNavigate() == null){
					showDialog();
				}
				showDialog = false;
			}
			controlVisible = true;
		} else {
			hideDialog();
			controlVisible = false;
		}
	}
	
	
	public void showHideDialog() {
		if(dialog != null) {
			hideDialog();
		} else {
			showDialog();
		}
	}
	
	private Dialog createDialog() {
		Dialog dialog = new Dialog(mapActivity);
		View lmain = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_info, null);
		updateInfo(lmain);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		//lp.copyFrom(dialog.getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.gravity = Gravity.BOTTOM;
		if(mapActivity.getMyApplication().getDaynightHelper().isNightMode()) {
			dialog.getContext().setTheme(R.style.Dialog_Fullscreen_Dark);
		} else {
			dialog.getContext().setTheme(R.style.Dialog_Fullscreen_Light);
		}
		
		dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(lmain, new LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.WRAP_CONTENT));
		dialog.setCanceledOnTouchOutside(true);
		dialog.getWindow().setAttributes(lp);
		return dialog;
	}
	
	public void updateDialog() {
		if(dialog != null) {
			updateInfo(dialog.findViewById(R.id.plan_route_info));
		}
	}
	
	private void updateInfo(final View main) {
		updateWptBtn(main);
		updateViaView(main);
		updateFromSpinner(main);
		updateToSpinner(main);
		updateApplicationModes(main);
		boolean addButtons = routingHelper.isRouteCalculated();
		if(addButtons) {
			updateRouteButtons(main);
		} else {
			updateRouteCalcProgress(main);
		}
	}

	private void updateRouteCalcProgress(final View main) {
		TargetPointsHelper targets = getTargets();
		if(targets.hasTooLongDistanceToNavigate()) {
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.VISIBLE);
			TextView textView = (TextView) main.findViewById(R.id.InfoTextView);
			ImageView iconView = (ImageView) main.findViewById(R.id.InfoIcon);
			main.findViewById(R.id.Prev).setVisibility(View.GONE);
			main.findViewById(R.id.Next).setVisibility(View.GONE);
			textView.setText(R.string.route_is_too_long);
			textView.setVisibility(View.VISIBLE);
			iconView.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_warning));
		} else{
			main.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
		}
	}

	private void updateWptBtn(final View parentView) {
		ImageView wptBtn = (ImageView) parentView.findViewById(R.id.waypoints);
		wptBtn.setImageDrawable(mapActivity.getMyApplication().getIconsCache()
				.getContentIcon(R.drawable.ic_action_flag_dark));
		wptBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getTargets().checkPointToNavigateShort()) {
					hideDialog();
					mapActivity.getMapActions().openIntermediatePointsDialog();
				}
			}

		});
	}

	private void updateApplicationModes(final View parentView) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final Set<ApplicationMode> selected = new HashSet<ApplicationMode>();
		selected.add(am);
		AppModeDialog.prepareAppModeView(mapActivity, selected, false,
				(ViewGroup) parentView.findViewById(R.id.app_modes), true, new View.OnClickListener() {
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
		if(via.length() == 0){
			parentView.findViewById(R.id.ViaLayout).setVisibility(View.GONE);
		} else {
			parentView.findViewById(R.id.ViaLayout).setVisibility(View.VISIBLE);
			((TextView) parentView.findViewById(R.id.ViaView)).setText(via);
		}
	}

	private void updateToSpinner(final View parentView) {
		final Spinner toSpinner = setupToSpinner(parentView);
		toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 1) {
					selectFavorite(parentView, true);
				} else if(position == 2) {
					selectOnScreen(parentView, true);
				} else if(position == 3) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY, (String) null);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void updateFromSpinner(final View parentView) {
		final TargetPointsHelper targets = getTargets();
		final Spinner fromSpinner = setupFromSpinner(parentView);
		fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0) {
					if(targets.getPointToStart() != null) {
						targets.clearStartPoint(true);
					}
				} else if(position == 1) {
					selectFavorite(parentView, false);
				} else if(position == 2) {
					selectOnScreen(parentView, false);
				} else if(position == 3) {
					Intent intent = new Intent(mapActivity, SearchAddressActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					intent.putExtra(SearchAddressFragment.SELECT_ADDRESS_POINT_INTENT_KEY, (String) null);
					mapActivity.startActivityForResult(intent, MapControlsLayer.REQUEST_ADDRESS_SELECT);
				}				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	protected void selectOnScreen(View parentView, boolean target) {
		selectFromMapTouch = true;
		selectFromMapForTarget =  target;
		hideDialog();
	}
	
	public void selectAddress(String name, LatLon l, final boolean target) {
		PointDescription pd = new PointDescription(PointDescription.POINT_TYPE_ADDRESS, name);
		if(target) {
			getTargets().navigateToPoint(l, true, -1, pd);
		} else {
			getTargets().setStartPoint(l, true, pd);
		}
		hideDialog();
		showDialog();
	}

	protected void selectFavorite(final View parentView, final boolean target) {
		final FavouritesAdapter favouritesAdapter = new FavouritesAdapter(mapActivity, mapActivity.getMyApplication()
				.getFavorites().getFavouritePoints());
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


	private OnItemClickListener getOnClickListener(final boolean target, final FavouritesAdapter favouritesAdapter,
			final Dialog[] dlg) {
		return new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FavouritePoint fp = favouritesAdapter.getItem(position);
				LatLon point = new LatLon(fp.getLatitude(), fp.getLongitude());
				if(target) {
					getTargets().navigateToPoint(point, true, -1, fp.getPointDescription());
				} else {
					getTargets().setStartPoint(point, true, fp.getPointDescription());
				}
				if(dlg != null && dlg.length > 0 && dlg[0] != null) {
					dlg[0].dismiss();
				}
				//Next 2 lines ensure Dialog is shown in the right correct position after a selection been made
				hideDialog();
				showDialog();
			}
		};
	}

	public static int getDirectionInfo() {
		return directionInfo;
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
							contextMenu.setLocation(new LatLon(l.getLatitude(), l.getLongitude()),
									info.getDescriptionRoute(ctx));
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
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1){
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
					contextMenu.setLocation(new LatLon(l.getLatitude(), l.getLongitude()), info.getDescriptionRoute(ctx));
					mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
				}
				mapView.refreshMap();
				updateInfo(mainView);
			}
			
		});
		View info = mainView.findViewById(R.id.Info);
		info.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapView.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapView.getContext().startActivity(intent);
			}
		});
		
		TextView textView = (TextView) mainView.findViewById(R.id.InfoTextView);
		ImageView iconView = (ImageView) mainView.findViewById(R.id.InfoIcon);
		if(directionInfo >= 0) {
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
			textView.setText(ctx.getRoutingHelper().getGeneralRouteInformation());
		}
	}

	private Button attachSimulateRoute(final View mainView, final OsmandApplication ctx) {
		final Button simulateRoute = null;//(Button) mainView.findViewById(R.id.SimulateRoute);
		final OsmAndLocationProvider loc = ctx.getLocationProvider();
		if(mapActivity.getRoutingHelper().isFollowingMode()) {
			simulateRoute.setVisibility(View.GONE);
		}
		if (OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) == null) {
			simulateRoute.setVisibility(View.GONE);
		}
		simulateRoute.setText(loc.getLocationSimulation().isRouteAnimating() ? R.string.animate_route_off : R.string.animate_route);
		simulateRoute.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mainView.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
				if(loc.getLocationSimulation().isRouteAnimating()) {
					loc.getLocationSimulation().startStopRouteAnimation(mapActivity);
					hideDialog();
				} else {
					simulateRoute.setText(R.string.animate_route_off);
					loc.getLocationSimulation().startStopRouteAnimation(mapActivity);
				}
				
			}
		});
		return simulateRoute;
	}


	@Override
	public void newRouteIsCalculated(boolean newRoute) {
		directionInfo = -1;
		updateDialog();
	}
	
	public String generateViaDescription() {
		TargetPointsHelper targets = getTargets();
		String via = "";
		List<TargetPoint> points = targets.getIntermediatePoints();
		if (points.size() == 0) {
			return via;
		}
		for (int i = 0; i < points.size() ; i++) {
			via += "\n - " + getRoutePointDescription(points.get(i).point, points.get(i).getOnlyName());
		}
		return mapActivity.getString(R.string.route_via) + via;
	}
	
	public String getRoutePointDescription(double lat, double lon) {
		return mapActivity.getString(R.string.route_descr_lat_lon, lat, lon);
	}
    
	public String getRoutePointDescription(LatLon l, String d) {
		if(d != null && d.length() > 0) {
			return d.replace(':', ' ');
		}
		if(l != null) {
			return mapActivity.getString(R.string.route_descr_lat_lon, l.getLatitude(), l.getLongitude());
		}
		return "";
	}
    
	private Spinner setupFromSpinner( View view) {
		ArrayList<String> fromActions = new ArrayList<String>();
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
		ArrayAdapter<String> fromAdapter = new ArrayAdapter<String>(view.getContext(), 
				android.R.layout.simple_spinner_item, 
				fromActions
				);
		fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(fromAdapter);
		if(start != null) {
			fromSpinner.setSelection(fromActions.size() - 1);
		} else {
			if(mapActivity.getMyApplication().getLocationProvider().getLastKnownLocation() == null) {
				fromSpinner.setPromptId(R.string.search_poi_location);
			}
			//fromSpinner.setSelection(0);
		}
		return fromSpinner;
	}
    
	private Spinner setupToSpinner(View view) {
		final Spinner toSpinner = ((Spinner) view.findViewById(R.id.ToSpinner));
		final TargetPointsHelper targets = getTargets();
		ArrayList<String> toActions = new ArrayList<String>();
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
		
		ArrayAdapter<String> toAdapter = new ArrayAdapter<String>(view.getContext(), 
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
		// do not hide dialog (needed for use case entering Planning mode without destination)
	}
	
	
	public void showDialog() {
		dialog = createDialog();
		dialog.show();
		dialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dlg) {
				dialog = null;
			}
		});
	}
	
	public void hideDialog() {
		if (dialog != null) {
			dialog.hide();
			dialog = null;
		}
	}

	public void setShowDialog() {
		showDialog = true;
	}
}
