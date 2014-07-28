package net.osmand.plus.sherpafy;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.*;

/**
 * Created by Denis on 25.07.2014.
 */
public class WaypointDialogHelper {
	private MapActivity mapActivity;
	private OsmandApplication app;
	private FrameLayout mainLayout;

	public WaypointDialogHelper(MapActivity activity, OsmandApplication app, FrameLayout layout) {
		this.mapActivity = activity;
		this.app = app;
		this.mainLayout = (FrameLayout) layout.getChildAt(0);
	}

	public void addDialogWithShift() {
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		final LayoutInflater vi = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View reachedView = vi.inflate(R.layout.waypoint_reached, null);

		Button btnY = (Button) reachedView.findViewById(R.id.info_yes);
		btnY.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//call your activity here
			}
		});

		Button btnN = (Button) reachedView.findViewById(R.id.info_no);
		btnN.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View child) {
				if (child == null || child.getParent() == null) {
					return;
				}

				View parent = (View) child.getParent().getParent().getParent();
				if (parent == null) {
					return;
				}

				if (mainLayout != null) {
					mainLayout.removeView(parent);
					if (checkIfDialogExists()) {
						shiftButtons(0, 120, 120);
					}
				}
			}
		});

		if (reachedView != null) {
			mainLayout.addView(reachedView, 1, params);
			waitBeforeLayoutIsResized(reachedView);
		}
	}

	private boolean checkIfDialogExists() {
		if (mainLayout.findViewById(R.id.package_delivered_layout) != null) {
			return false;
		}
		return true;
	}

	private void shiftButtons(int marginBottom, int marginRight, int marginLeft) {
		if (mainLayout == null) {
			return;
		}

		MapControlsLayer mapControls = mapActivity.getMapLayers().getMapControlsLayer();
		mapControls.shiftControl();

//		FrameLayout.LayoutParams btnp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		Button menu = (Button) mainLayout.findViewById(MapMenuControls.BACK_TO_MENU_BTN_ID);
//		if (menu == null){
//			menu = (Button) mainLayout.findViewById(SmallMapMenuControls.SMALL_MENU_CONTROL_ID);
//		}
//		if (menu != null) {
//			btnp.setMargins(0, 0, 0, marginBottom);
//			btnp.gravity = Gravity.BOTTOM;
//			menu.setLayoutParams(btnp);
//		}
//
//		Button navigate = (Button) mainLayout.findViewById(MapNavigateControl.NAVIGATE_BUTTON_ID);
//		if (navigate != null) {
//			btnp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			btnp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
//			btnp.setMargins(0, 0, 0, marginBottom);
//			//btnp.setMargins(0, 0, 120, marginBottom);
//			navigate.setLayoutParams(btnp);
//		}
//
//		Button stop = (Button) mainLayout.findViewById(MapCancelControl.CANCEL_BUTTON_ID);
//		if (stop != null) {
//			btnp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			btnp.gravity = Gravity.BOTTOM;
//			//TODO calculate left margin
//			btnp.setMargins(139, 0, 0, marginBottom);
//			stop.setLayoutParams(btnp);
//		}
//
//		ImageButton routePref = (ImageButton) mainLayout.findViewById(MapRoutePreferencesControl.ROUTE_PREFERENCES_BTN_ID);
//		if (routePref != null){
//			btnp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			btnp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
//			//TODO right left margin
//			btnp.setMargins(0, 0, 120, marginBottom);
//			routePref.setLayoutParams(btnp);
//		}
//
//		Button routeInfo = (Button) mainLayout.findViewById(MapRouteInfoControl.ROUTE_INFO_BUTTON_ID);
//		if (routeInfo != null) {
//			btnp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			btnp.gravity = Gravity.BOTTOM;
//			//TODO calculate left margin
//			btnp.setMargins(259, 0, 0, marginBottom);
//			routeInfo.setLayoutParams(btnp);
//		} else {
//			//if navigation doesnt exists - we need to push zoom buttons top
//			// otherwise they're placed at the right side
//			//image resource to know layout size
//			Button zoomIn = (Button) mainLayout.findViewById(MapZoomControls.ZOOM_IN_BTN_ID);
//			Drawable d = mapActivity.getApplicationContext().getResources().getDrawable(R.drawable.map_zoom_in);
//			if (zoomIn != null) {
//				btnp = new FrameLayout.LayoutParams(d.getMinimumWidth(), d.getMinimumHeight());
//				btnp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
//				btnp.setMargins(0, 0, marginRight, marginBottom);
//				zoomIn.setLayoutParams(btnp);
//			}
//
//			Button zoomOut = (Button) mainLayout.findViewById(MapZoomControls.ZOOM_OUT_BTN_ID);
//			if (zoomOut != null) {
//				d = mapActivity.getApplicationContext().getResources().getDrawable(R.drawable.map_zoom_out);
//				btnp = new FrameLayout.LayoutParams(d.getMinimumWidth(), d.getMinimumHeight());
//				btnp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
//				btnp.setMargins(0, 0, 0, marginBottom);
//				zoomOut.setLayoutParams(btnp);
//			}
//		}
	}


	private void waitBeforeLayoutIsResized(View reachedView) {
		//this async task is needed because layout height is not set
		// right after you add it so we need to w8 some time
		new AsyncTask<View, Void, Void>() {
			int height;

			@Override
			protected Void doInBackground(View... params) {
				for (int i = 0; i < 10; i++) {
					SystemClock.sleep(50);
					height = params[0].getHeight();
					if (params[0].getHeight() > 0) {
						break;
					}
				}
				return null;
			}

			protected void onPostExecute(Void result) {
				if (height > 0) {
					shiftButtons(height, 120, 120);
				}
			}
		}.execute(reachedView);
	}
}
