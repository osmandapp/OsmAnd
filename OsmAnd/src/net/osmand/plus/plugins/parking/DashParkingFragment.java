package net.osmand.plus.plugins.parking;


import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.dashboard.tools.DashFragmentData.DefaultShouldShow;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.FontCache;

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
			new DefaultShouldShow() {
				@Override
				public int getTitleId() {
					return TITLE_ID;
				}
			};
	static final DashFragmentData FRAGMENT_DATA = new DashFragmentData(
			TAG, DashParkingFragment.class,
			SHOULD_SHOW_FUNCTION, 50, null);

	@Override
	public View initView(@Nullable ViewGroup container, @Nullable Bundle savedState) {
		View view = inflate(R.layout.dash_parking_fragment, container, false);
		Button remove = view.findViewById(R.id.remove_tag);
		remove.setOnClickListener(v -> {
			if (plugin != null) {
				AlertDialog dialog = plugin.showDeleteDialog(getActivity());
				dialog.setOnDismissListener(d -> updateParkingPosition());
			}
		});
		remove.setTypeface(FontCache.getMediumFont());

		view.findViewById(R.id.parking_header).setOnClickListener(v -> {
			if (plugin == null || plugin.getParkingPosition() == null) {
				return;
			}
			LatLon parkingPosition = plugin.getParkingPosition();
			double lat = parkingPosition.getLatitude();
			double lon = parkingPosition.getLongitude();
			PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_PARKING_MARKER,
					getString(R.string.osmand_parking_position_name));

			settings.setMapLocationToShow(lat, lon,
					15, pointDescription, false, parkingPosition);
			MapActivity.launchMapActivityMoveToTop(requireActivity());
		});
		return view;
	}

	@Override
	public void onOpenDash() {
		plugin = PluginsHelper.getActivePlugin(ParkingPositionPlugin.class);
		updateParkingPosition();
	}

	private void updateParkingPosition() {
		View mainView = getView();
		if (mainView == null) return;

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

		TextView timeLeft = mainView.findViewById(R.id.time_left);
		if (limited) {
			descr = getString(R.string.parking_place_limited) + " " + plugin.getFormattedTime(plugin.getParkingTime());
			long endtime = plugin.getParkingTime();
			long currTime = Calendar.getInstance().getTimeInMillis();
			long timeDiff = endtime - currTime;
			String time = getFormattedTime(timeDiff) + " ";
			TextView leftLbl = mainView.findViewById(R.id.left_lbl);
			timeLeft.setText(time);
			if (timeDiff < 0) {
				leftLbl.setTextColor(getColor(R.color.parking_outdated_color));
				leftLbl.setText(getString(R.string.osmand_parking_overdue));
			} else {
				leftLbl.setTextColor(Color.WHITE);
				leftLbl.setText(getString(R.string.osmand_parking_time_left));
			}
			timeLeft.setVisibility(View.VISIBLE);
		} else {
			descr = getString(R.string.osmand_parking_position_name);
			timeLeft.setText("");
			timeLeft.setVisibility(View.GONE);
		}
		((TextView) mainView.findViewById(R.id.name)).setText(descr);
		ImageView direction = mainView.findViewById(R.id.direction_icon);
		if (loc != null) {
			DashLocationView dv = new DashLocationView(direction, mainView.findViewById(R.id.distance), position);
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
			timeStringBuilder.append(getString(R.string.osmand_parking_hour));
		}

		timeStringBuilder.append(" ");
		timeStringBuilder.append(minutes);
		timeStringBuilder.append(" ");
		timeStringBuilder.append(getString(R.string.shared_string_minute_lowercase));

		return timeStringBuilder.toString();
	}
}