package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;

public class HomeWorkCard extends BaseCard {

	public HomeWorkCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.home_work_card;
	}

	@Override
	protected void updateContent() {
		final TargetPointsHelper targetPointsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
		final FavouritesDbHelper favorites = getMyApplication().getFavorites();
		final FavouritePoint homePoint = favorites.getHomePoint();
		final FavouritePoint workPoint = favorites.getWorkPoint();

		TextView homeDescr = view.findViewById(R.id.home_button_descr);
		final TextView workDescr = view.findViewById(R.id.work_button_descr);
		homeDescr.setText(homePoint != null ? homePoint.getDescription() : mapActivity.getString(R.string.shared_string_add));
		workDescr.setText(workPoint != null ? workPoint.getDescription() : mapActivity.getString(R.string.shared_string_add));

		View homeButton = view.findViewById(R.id.home_button);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (homePoint == null) {
					AddPointBottomSheetDialog.showInstance(mapActivity, PointType.HOME);
				} else {
					targetPointsHelper.navigateToPoint(new LatLon(homePoint.getLatitude(), homePoint.getLongitude()),
							true, -1, homePoint.getPointDescription());
				}
			}
		});
		homeButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AddPointBottomSheetDialog.showInstance(mapActivity, PointType.HOME);
				return true;
			}
		});

		View workButton = view.findViewById(R.id.work_button);
		workButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (workPoint == null) {
					AddPointBottomSheetDialog.showInstance(mapActivity, PointType.WORK);
				} else {
					targetPointsHelper.navigateToPoint(new LatLon(workPoint.getLatitude(), workPoint.getLongitude()),
							true, -1, workPoint.getPointDescription());
				}
			}
		});
		workButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AddPointBottomSheetDialog.showInstance(mapActivity, PointType.WORK);
				return true;
			}
		});
	}
}
