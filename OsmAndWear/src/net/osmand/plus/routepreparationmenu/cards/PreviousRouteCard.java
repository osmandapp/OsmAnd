package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class PreviousRouteCard extends MapBaseCard {

	public PreviousRouteCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.prev_route_card;
	}

	@Override
	protected void updateContent() {
		TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TextView startTitle = view.findViewById(R.id.start_title);
		TextView destinationTitle = view.findViewById(R.id.destination_title);

		TargetPoint startPoint = targetPointsHelper.getPointToStartBackup();
		boolean myLocation = false;
		if (startPoint == null) {
			myLocation = true;
			startPoint = targetPointsHelper.getMyLocationToStart();
		}
		StringBuilder startText = new StringBuilder(myLocation ? mapActivity.getText(R.string.my_location) : "");
		if (startPoint != null) {
			String descr = getPointName(app, startPoint);
			if (!Algorithms.isEmpty(descr)) {
				if (startText.length() > 0) {
					startText.append(" — ");
				}
				startText.append(descr);
			}
		}
		startTitle.setText(startText.toString());
		TargetPoint destinationPoint = targetPointsHelper.getPointToNavigateBackup();
		String destinationName = "";
		destinationName = getPointName(app, destinationPoint);
		destinationTitle.setText(destinationName);
		View button = view.findViewById(R.id.card_button);
		button.setOnClickListener(v -> notifyButtonPressed(0));
	}

	public static String getPointName(@NonNull OsmandApplication app, @Nullable TargetPoint point) {
		String name = "";
		if (point != null) {
			PointDescription description = point.getOriginalPointDescription();
			if (description != null && !Algorithms.isEmpty(description.getName()) &&
					!description.getName().equals(app.getString(R.string.no_address_found))) {
				name = description.getName();
			} else {
				name = PointDescription.getLocationName(app, point.point.getLatitude(), point.point.getLongitude(), true)
						.replace('\n', ' ');
			}
		}
		return name;
	}
}
