package net.osmand.plus.parkingpoint;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.FontCache;

import java.util.Calendar;

/**
 * Created by Denis on
 * 26.01.2015.
 */
public class DashParkingFragment extends DashLocationFragment {
	private static final String TAG = "DASH_PARKING_FRAGMENT";
	private static final int TITLE_ID = R.string.osmand_parking_plugin_name;
	ParkingPositionPlugin plugin;

	private static final DashFragmentData.ShouldShowFunction SHOULD_SHOW_FUNCTION =
			new DashboardOnMap.DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			DashParkingFragment.TAG, DashParkingFragment.class,
			SHOULD_SHOW_FUNCTION, 50, null);

	@Override
	public View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_parking_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		Button remove = (Button) view.findViewById(R.id.remove_tag);
		remove.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog dialog = plugin.showDeleteDialog(getActivity());
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						updateParkingPosition();
					}
				});
			}
		});
		remove.setTypeface(typeface);

		view.findViewById(R.id.parking_header).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LatLon point = plugin.getParkingPosition();
				getMyApplication().getSettings().setMapLocationToShow(point.getLatitude(), point.getLongitude(),
						15, new PointDescription(PointDescription.POINT_TYPE_PARKING_MARKER, getString(R.string.osmand_parking_position_name)), false,
						point); //$NON-NLS-1$
				MapActivity.launchMapActivityMoveToTop(getActivity());
			}
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);
		updateParkingPosition();
	}

	private void updateParkingPosition() {
		View mainView = getView();
		if (mainView == null) {
			return;
		}
		if (plugin == null || plugin.getParkingPosition() == null) {
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		LatLon loc = getDefaultLocation();
		LatLon position = plugin.getParkingPosition();
		boolean limited = plugin.getParkingType();
		String descr;

		TextView timeLeft = (TextView) mainView.findViewById(R.id.time_left);
		if (limited) {
			descr = getString(R.string.parking_place_limited) + " " + plugin.getFormattedTime( plugin.getParkingTime());
			long endtime = plugin.getParkingTime();
			long currTime = Calendar.getInstance().getTimeInMillis();
			long timeDiff = endtime - currTime;
			String time = getFormattedTime(timeDiff) + " ";
			TextView leftLbl = (TextView) mainView.findViewById(R.id.left_lbl);
			timeLeft.setText(time);
			if (timeDiff < 0) {
				timeLeft.setText(time);
				leftLbl.setTextColor(getResources().getColor(R.color.parking_outdated_color));
				leftLbl.setText(getString(R.string.osmand_parking_overdue));
			} else {
				timeLeft.setText(time);
				leftLbl.setTextColor(Color.WHITE);
				leftLbl.setText(getString(R.string.osmand_parking_time_left));
			}
			timeLeft.setVisibility(View.VISIBLE);
		} else {
			descr = getString(R.string.parking_place);
			timeLeft.setText("");
			timeLeft.setVisibility(View.GONE);
		}
		((TextView) mainView.findViewById(R.id.name)).setText(descr);
		ImageView direction = (ImageView) mainView.findViewById(R.id.direction_icon);
		if (loc != null) {
			DashLocationView dv = new DashLocationView(direction, (TextView) mainView.findViewById(R.id.distance), position);
			dv.paint = false;
			dv.arrowResId = R.drawable.ic_action_start_navigation; 
			distances.add(dv);
		}


	}

	String getFormattedTime(long timeInMillis) {
		if (timeInMillis < 0) {
			timeInMillis *= -1;
		}
		StringBuilder timeStringBuilder = new StringBuilder();
		int hours = (int) timeInMillis / (1000 * 60 * 60);
		int minMills = (int) timeInMillis % (1000 * 60 * 60);
		int minutes = minMills / (1000 * 60);
		if (hours > 0) {
			timeStringBuilder.append(hours);
			timeStringBuilder.append(" ");
			timeStringBuilder.append(getResources().getString(R.string.osmand_parking_hour));
		}

		timeStringBuilder.append(" ");
		timeStringBuilder.append(minutes);
		timeStringBuilder.append(" ");
		timeStringBuilder.append(getResources().getString(R.string.osmand_parking_minute));


		return timeStringBuilder.toString();
	}
}
