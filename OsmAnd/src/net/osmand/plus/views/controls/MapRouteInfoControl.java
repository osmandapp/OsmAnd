package net.osmand.plus.views.controls;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ClientContext;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MapRouteInfoControl extends MapControls implements IRouteInformationListener {
	private Button infoButton;
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	
	private final ContextMenuLayer contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private View next;
	private View prev;
	private View info;
	
	
	public MapRouteInfoControl(ContextMenuLayer contextMenu, 
			MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		this.contextMenu = contextMenu;
		routingHelper = mapActivity.getRoutingHelper();
		mapView = mapActivity.getMapView();
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		infoButton = addButton(parent, R.string.info_button, R.drawable.map_btn_info);
		controlVisible = true;
		infoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
			}
		});
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
	
//	public RouteInfoLayer(RoutingHelper routingHelper, MapActivity activity, ContextMenuLayer contextMenu){
//		createLayout(activity, activity.getMapView().getDensity());
//		this.routingHelper = routingHelper;
//		this.contextMenu = contextMenu;
//		routingHelper.addListener(this);
//		attachListeners(activity.getMyApplication());
//		updateVisibility();
//
//		activity.accessibleContent.add(prev);
//		activity.accessibleContent.add(next);
//		activity.accessibleContent.add(info);
//	}
	
	private void createLayout(MapActivity activity, float density) {
		FrameLayout fl = (FrameLayout) activity.getMapView().getParent();
		LinearLayout ll = new LinearLayout(activity);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setPadding(0, 0, (int) (density * 15), (int) (density * 50));
		fl.addView(ll, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER));
		prev = new ImageButton(activity);
		prev.setContentDescription(activity.getString(R.string.previous_button));
		prev.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.ax_1_navigation_previous_item_light));
		ll.addView(prev);
		info = new ImageButton(activity);
		info.setContentDescription(activity.getString(R.string.info_button));
		info.setPadding((int) (density * 8), 0, 0, 0);
		info.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.ax_2_action_about_light));
		ll.addView(info);
		next = new ImageButton(activity);
		next.setContentDescription(activity.getString(R.string.next_button));
		next.setPadding((int) (density * 8), 0, 0, 0);
		next.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.ax_1_navigation_next_item_light));
		ll.addView(next);
	}

	
	private void attachListeners(final ClientContext ctx) {
		prev.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo > 0){
					directionInfo--;
					if(routingHelper.getRouteDirections().size() > directionInfo){
						RouteDirectionInfo info = routingHelper.getRouteDirections().get(directionInfo);
						net.osmand.Location l = routingHelper.getLocationFromRouteDirection(info);
						contextMenu.setLocation(new LatLon(l.getLatitude(), l.getLongitude()), info.getDescriptionRoute(ctx));
						mapView.getAnimatedDraggingThread().startMoving(l.getLatitude(), l.getLongitude(), mapView.getZoom(), true);
					}
				}
				mapView.refreshMap();
			}
			
		});
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
			}
			
		});
		info.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapView.getContext(), ShowRouteInfoActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				mapView.getContext().startActivity(intent);
			}
		});
	}

	public boolean isVisible(){
		return routingHelper.isRouteCalculated() && !routingHelper.isFollowingMode();
	}
	
	private void updateVisibility(){
		int vis = isVisible() ? View.VISIBLE : View.INVISIBLE; 
		prev.setVisibility(vis);
		next.setVisibility(vis);
		info.setVisibility(vis);
	}


	@Override
	public void newRouteIsCalculated(boolean newRoute) {
		directionInfo = -1;
		updateVisibility();
		mapView.refreshMap();
	}

	@Override
	public void routeWasCancelled() {
		directionInfo = -1;
		updateVisibility();
	}


}
