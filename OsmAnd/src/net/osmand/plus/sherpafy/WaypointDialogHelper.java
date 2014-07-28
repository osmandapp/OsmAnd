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
import net.osmand.plus.api.render.Paint;
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

	public static boolean OVERLAP_LAYOUT = true;

	public WaypointDialogHelper(OsmandApplication app) {
		this.mapActivity = app.mapActivity;
		this.app = app;
		this.mainLayout = (FrameLayout) ((FrameLayout) mapActivity.getLayout()).getChildAt(0);
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
					if (checkIfDialogExists() && OVERLAP_LAYOUT) {
						shiftButtons();
					}
				}
			}
		});

		if (reachedView != null && mainLayout != null) {
			mainLayout.addView(reachedView, params);
			waitBeforeLayoutIsResized(reachedView);
		}
	}

	private boolean checkIfDialogExists() {
		if (mainLayout == null){
			return true;
		}

		if (mainLayout.findViewById(R.id.package_delivered_layout) != null) {
			return false;
		}
		return true;
	}

	private void shiftButtons() {
		if (mainLayout == null || mapActivity == null) {
			return;
		}

		MapControlsLayer mapControls = mapActivity.getMapLayers().getMapControlsLayer();
		if (mapControls != null){
			mapControls.shiftControl();
		}
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
				if (height > 0 && OVERLAP_LAYOUT) {
					shiftButtons();
				}
			}
		}.execute(reachedView);
	}
}
