package net.osmand.plus.views.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

public class MapRouteInfoControl extends MapControls implements IRouteInformationListener {
	private Button infoButton;
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	
	private final ContextMenuLayer contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private Dialog dialog;
	
	
	public MapRouteInfoControl(ContextMenuLayer contextMenu, 
			MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		this.contextMenu = contextMenu;
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
		routingHelper.addListener(this);
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		infoButton = addButton(parent, R.string.route_info, R.drawable.map_btn_signpost);
		controlVisible = true;
		infoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(dialog != null) {
					dialog.hide();
					dialog = null;
					infoButton.setBackgroundResource(R.drawable.map_btn_signpost);
				} else {
					dialog = showDialog();
					dialog.show();
					infoButton.setBackgroundResource(R.drawable.map_btn_signpost_p);
					dialog.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dlg) {
							infoButton.setBackgroundResource(R.drawable.map_btn_signpost);
							dialog = null;
						}
					});
				}
			}
		});
	}
	
	private Dialog showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
		View lmain = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_info, null);
		boolean addButtons = routingHelper.isRouteCalculated();
		if(addButtons) {
			attachListeners(lmain);
			updateInfo(lmain);
		} else {
			lmain.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
			lmain.findViewById(R.id.SimulateRoute).setVisibility(View.GONE);
		}
        builder.setView(lmain);
//        builder.setTitle(R.string.route_info);
        
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        lp.y = (int) (infoButton.getBottom() - infoButton.getTop() + scaleCoefficient * 5); 
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setAttributes(lp);
        return dialog;
	}
	
	private void updateInfo(View view) {
		String via = generateViaDescription();
		if(via.length() == 0){
			((TextView) view.findViewById(R.id.ViaView)).setVisibility(View.GONE);
		} else {
			((TextView) view.findViewById(R.id.ViaView)).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.ViaView)).setText(via);
		}
		final Spinner fromSpinner = setupFromSpinner(null, null, view);
		final Spinner toSpinner = setupToSpinner(null, null ,view);
		
	}

	public static int getDirectionInfo() {
		return directionInfo;
	}

	public static boolean isControlVisible() {
		return controlVisible;
	}
	
	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, infoButton);
		controlVisible = false;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
	}
	
	public int getWidth() {
		if (width == 0) {
			Drawable buttonDrawable = mapActivity.getResources().getDrawable(R.drawable.map_btn_info);
			width = buttonDrawable.getMinimumWidth();
		}
		return width ;
	}
	
	
	private void attachListeners(final View mainView) {
		final OsmandApplication ctx = mapActivity.getMyApplication();
		final Button simulateRoute = (Button) mainView.findViewById(R.id.SimulateRoute);
		final OsmAndLocationProvider loc = ctx.getLocationProvider();
		simulateRoute.setText(loc.getLocationSimulation().isRouteAnimating() ? R.string.animate_route_off : R.string.animate_route);
		simulateRoute.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mainView.findViewById(R.id.RouteTargets).setVisibility(View.GONE);
				mainView.findViewById(R.id.RouteInfoControls).setVisibility(View.GONE);
				simulateRoute.setText(loc.getLocationSimulation().isRouteAnimating() ? R.string.animate_route : R.string.animate_route_off);
				loc.getLocationSimulation().startStopRouteAnimation(mapActivity);
				
			}
		});
		ImageButton prev = (ImageButton) mainView.findViewById(R.id.Prev);
		prev.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo > 0){
					directionInfo--;
					if(routingHelper.getRouteDirections().size() > directionInfo){
						mainView.findViewById(R.id.RouteTargets).setVisibility(View.GONE);
						simulateRoute.setVisibility(View.GONE);
						RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
						net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
						contextMenu.setLocation(new LatLon(l.getLatitude(), l.getLongitude()), info.getDescriptionRoute(ctx));
						mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
					}
				}
				mapView.refreshMap();
			}
			
		});
		ImageButton next = (ImageButton) mainView.findViewById(R.id.Next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo < routingHelper.getRouteDirections().size() - 1){
					mainView.findViewById(R.id.RouteTargets).setVisibility(View.GONE);
					simulateRoute.setVisibility(View.GONE);
					directionInfo++;
					RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
					net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
					contextMenu.setLocation(new LatLon(l.getLatitude(), l.getLongitude()), info.getDescriptionRoute(ctx));
					mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
				}
				mapView.refreshMap();
			}
			
		});
		ImageButton info = (ImageButton) mainView.findViewById(R.id.Info);
		info.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapView.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapView.getContext().startActivity(intent);
			}
		});
	}

	public boolean isVisibleButtons(){
		return routingHelper.isRouteCalculated() && !routingHelper.isFollowingMode();
	}
	

	@Override
	public void newRouteIsCalculated(boolean newRoute) {
		directionInfo = -1;
		mapView.refreshMap();
	}
	
	public String generateViaDescription() {
		TargetPointsHelper targets = getTargets();
		String via = "";
		List<String> names = targets.getIntermediatePointNames();
		List<LatLon> points = targets.getIntermediatePoints();
		if (names.size() == 0) {
			return via;
		}
		for (int i = 0; i < points.size() ; i++) {
			via += "\n - " + getRoutePointDescription(points.get(i), i >= names.size() ? "" :names.get(i));
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
    
    private Spinner setupFromSpinner(final Location start, String name, View view) {
		String currentLocation = mapActivity.getString(R.string.route_descr_current_location);
		ArrayList<String> fromActions = new ArrayList<String>();
		fromActions.add(currentLocation);
		if (start != null) {
			String oname = name != null ? name
					: getRoutePointDescription(start.getLatitude(), start.getLongitude());
			String mapLocation = mapActivity.getString(R.string.route_descr_map_location) + " " + oname;
			fromActions.add(mapLocation);
		}
		final Spinner fromSpinner = ((Spinner) view.findViewById(R.id.FromSpinner));
		ArrayAdapter<String> fromAdapter = new ArrayAdapter<String>(view.getContext(), 
				android.R.layout.simple_spinner_item, 
				fromActions
				);
		fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		fromSpinner.setAdapter(fromAdapter);
		if(start == null) {
			fromSpinner.setSelection(1);
		}
		return fromSpinner;
	}
    
    private Spinner setupToSpinner(final Location to, String name, View view) {
		final TargetPointsHelper targets = getTargets();
		ArrayList<String> toActions = new ArrayList<String>();
		if (targets.getPointToNavigate() != null) {
			toActions.add(mapActivity.getString(R.string.route_descr_destination) + " "
					+ getRoutePointDescription(targets.getPointToNavigate(), targets.getPointNavigateDescription()));
		}
		if(to != null) {
			String oname = name != null ? name : getRoutePointDescription(to.getLatitude(),to.getLongitude());
			String mapLocation = mapActivity.getString(R.string.route_descr_map_location) + " " + oname;
			toActions.add(mapLocation);
		}
		final Spinner toSpinner = ((Spinner) view.findViewById(R.id.ToSpinner));
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
		if(dialog != null) {
			dialog.hide();
			dialog = null;
		}
	}
	
}
