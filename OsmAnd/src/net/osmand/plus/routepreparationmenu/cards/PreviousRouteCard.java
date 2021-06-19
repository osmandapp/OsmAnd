package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.TextView;

import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
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
		final TargetPointsHelper targetPointsHelper = app.getTargetPointsHelper();
		TextView startTitle = (TextView) view.findViewById(R.id.start_title);
		TextView destinationTitle = (TextView) view.findViewById(R.id.destination_title);

		TargetPoint startPoint = targetPointsHelper.getPointToStartBackup();
		boolean myLocation = false;
		if (startPoint == null) {
			myLocation = true;
			startPoint = targetPointsHelper.getMyLocationToStart();
		}
		StringBuilder startText = new StringBuilder(myLocation ? mapActivity.getText(R.string.my_location) : "");
		if (startPoint != null) {
			String descr = getPointName(startPoint);
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
		destinationName = getPointName(destinationPoint);
		destinationTitle.setText(destinationName);
		View button = view.findViewById(R.id.card_button);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(PreviousRouteCard.this, 0);
				}
			}
		});
	}

	private String getPointName(TargetPoint targetPoint) {
		String name = "";
		if (targetPoint != null) {
			PointDescription description = targetPoint.getOriginalPointDescription();
			if (description != null && !Algorithms.isEmpty(description.getName()) &&
					!description.getName().equals(mapActivity.getString(R.string.no_address_found))) {
				name = description.getName();
			} else {
				name = PointDescription.getLocationName(mapActivity,
						targetPoint.point.getLatitude(), targetPoint.point.getLongitude(), true).replace('\n', ' ');
			}
		}
		return name;
	}
}
