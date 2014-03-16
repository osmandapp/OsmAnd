package net.osmand.plus.views.controls;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.ShowRouteInfoActivity;
import net.osmand.plus.activities.actions.NavigateAction;
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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class MapRouteInfoControl extends MapControls implements IRouteInformationListener {
	private Button infoButton;
	public static int directionInfo = -1;
	public static boolean controlVisible = false;
	
	private final ContextMenuLayer contextMenu;
	private final RoutingHelper routingHelper;
	private OsmandMapTileView mapView;
	private ImageButton next;
	private ImageButton prev;
	private ImageButton info;
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
		infoButton = addButton(parent, R.string.info_button, R.drawable.map_btn_signpost);
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
        LinearLayout lmain = new LinearLayout(mapActivity);
		lmain.setOrientation(LinearLayout.VERTICAL);
		boolean addButtons = routingHelper.isRouteCalculated();
		if(addButtons) {
			LinearLayout buttons = createButtonsLayout(scaleCoefficient);
			lmain.addView(buttons);
		}
		View lv = new NavigateAction(mapActivity).createDialogView();
		lv.setId(R.id.MainLayout);
		lmain.addView(lv);
		if(addButtons) {
			attachListeners(lmain);
		}
        builder.setView(lmain);
        
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
	
	private LinearLayout createButtonsLayout(float density) {
		LinearLayout ll = new LinearLayout(mapActivity);
		ll.setGravity(Gravity.CENTER);
		ll.setOrientation(LinearLayout.HORIZONTAL);
		ll.setPadding((int) (density * 10), (int) (density * 5), (int) (density * 10), (int) (density * 5));
		prev = new ImageButton(mapActivity);
		prev.setContentDescription(mapActivity.getString(R.string.previous_button));
		prev.setImageResource(R.drawable.ax_1_navigation_previous_item_light);
		prev.setAdjustViewBounds(true);
		prev.setBackgroundResource(R.drawable.map_btn_plain);
		ll.addView(prev);
		info = new ImageButton(mapActivity);
		info.setContentDescription(mapActivity.getString(R.string.info_button));
		info.setBackgroundResource(R.drawable.map_btn_plain);
		info.setAdjustViewBounds(true);
		info.setImageResource(R.drawable.ax_2_action_about_light);
		ll.addView(info);
		next = new ImageButton(mapActivity);
		next.setContentDescription(mapActivity.getString(R.string.next_button));
		next.setBackgroundResource(R.drawable.map_btn_plain);
		next.setAdjustViewBounds(true);
		next.setImageResource(R.drawable.ax_1_navigation_next_item_light);
		ll.addView(next);
		return ll;
	}

	
	private void attachListeners(final View mainView) {
		final OsmandApplication ctx = mapActivity.getMyApplication();
		prev.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				if(routingHelper.getRouteDirections() != null && directionInfo > 0){
					directionInfo--;
					if(routingHelper.getRouteDirections().size() > directionInfo){
						mainView.findViewById(R.id.MainLayout).setVisibility(View.GONE);
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
					mainView.findViewById(R.id.MainLayout).setVisibility(View.GONE);
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

	public boolean isVisibleButtons(){
		return routingHelper.isRouteCalculated() && !routingHelper.isFollowingMode();
	}
	

	@Override
	public void newRouteIsCalculated(boolean newRoute) {
		directionInfo = -1;
		mapView.refreshMap();
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
