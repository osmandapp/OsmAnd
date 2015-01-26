package net.osmand.plus.dashboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.parkingpoint.ParkingPositionPlugin;
import net.osmand.util.MapUtils;

/**
 * Created by Denis on
 * 26.01.2015.
 */
public class DashParkingFragment extends DashLocationFragment {
	ParkingPositionPlugin plugin;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		plugin = OsmandPlugin.getEnabledPlugin(ParkingPositionPlugin.class);



		if (getMyApplication().getSettings().getLastKnownMapLocation() != null) {
			loc = getMyApplication().getSettings().getLastKnownMapLocation();
		} else {
			loc = new LatLon(0f, 0f);
		}

		updateParkingPosition();
	}

	private void updateParkingPosition() {
		View mainView = getView();
		if (plugin == null || plugin.getParkingPosition() == null){
			mainView.setVisibility(View.GONE);
			return;
		} else {
			mainView.setVisibility(View.VISIBLE);
		}

		LatLon position = plugin.getParkingPosition();

		int dist = (int) (MapUtils.getDistance(position.getLatitude(), position.getLongitude(),
				loc.getLatitude(), loc.getLongitude()));
		String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication());
		((TextView) mainView.findViewById(R.id.distance)).setText(distance);
		//TODO add parking time
		String parking_name = plugin.getParkingType() ?
				getString(R.string.parking_place) : getString(R.string.parking_place);
		((TextView) mainView.findViewById(R.id.name)).setText(parking_name);
		ImageView direction = (ImageView) mainView.findViewById(R.id.direction_icon);
		if (loc != null){
			direction.setVisibility(View.VISIBLE);
			updateArrow(position, direction, 10, R.drawable.ic_destination_arrow);
		}
	}

	@Override
	public boolean updateCompassValue(float value) {
		if (plugin == null){
			return true;
		}
		if (super.updateCompassValue(value)){
			updateParkingPosition();
		}
		return true;
	}

	@Override
	public void updateLocation(Location location) {
		super.updateLocation(location);

		if (plugin == null){
			return;
		}
		updateParkingPosition();
	}
}
