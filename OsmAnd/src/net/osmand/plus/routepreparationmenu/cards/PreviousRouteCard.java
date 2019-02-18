package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

public class PreviousRouteCard extends BaseCard {

	public PreviousRouteCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.prev_route_card;
	}

	@Override
	protected void updateContent() {
		final TargetPointsHelper targetPointsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
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
			String descr = startPoint.getPointDescription(mapActivity).getSimpleName(mapActivity, false);
			if (!Algorithms.isEmpty(descr)) {
				if (startText.length() > 0) {
					startText.append(" — ");
				}
				startText.append(descr);
			}
		}
		startTitle.setText(startText.toString());
		TargetPoint destinationPoint = targetPointsHelper.getPointToNavigateBackup();
		destinationTitle.setText(destinationPoint != null ?
				destinationPoint.getPointDescription(mapActivity).getSimpleName(mapActivity, false) : "");
		View homeButton = view.findViewById(R.id.card_button);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				targetPointsHelper.restoreTargetPoints(true);
			}
		});
	}

	@Override
	protected void applyDayNightMode() {
		AndroidUtils.setTextSecondaryColor(app, (TextView) view.findViewById(R.id.start_title), nightMode);
		AndroidUtils.setTextPrimaryColor(app, (TextView) view.findViewById(R.id.destination_title), nightMode);
		Drawable img = app.getUIUtilities().getIcon(R.drawable.ic_action_previous_route, nightMode ? R.color.route_info_control_icon_color_dark : R.color.route_info_control_icon_color_light);
		((AppCompatImageView) view.findViewById(R.id.card_img)).setImageDrawable(img);
		AndroidUtils.setBackground(app, view.findViewById(R.id.card_content), nightMode, R.color.route_info_bg_light, R.color.route_info_bg_dark);
	}
}
