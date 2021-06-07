package net.osmand.plus.routepreparationmenu.cards;

import android.view.View;
import android.widget.TextView;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;

public class HomeWorkCard extends MapBaseCard {

	public HomeWorkCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.home_work_card;
	}

	@Override
	protected void updateContent() {
		final FavouritesDbHelper favorites = getMyApplication().getFavorites();
		final FavouritePoint homePoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.HOME);
		final FavouritePoint workPoint = favorites.getSpecialPoint(FavouritePoint.SpecialPointType.WORK);
		final TextView homeDescr = view.findViewById(R.id.home_button_descr);
		final TextView workDescr = view.findViewById(R.id.work_button_descr);
		homeDescr.setText(homePoint != null ? homePoint.getAddress() : mapActivity.getString(R.string.shared_string_add));
		workDescr.setText(workPoint != null ? workPoint.getAddress() : mapActivity.getString(R.string.shared_string_add));
		setSpecialButtonOnClickListeners(homePoint, R.id.home_button, PointType.HOME);
		setSpecialButtonOnClickListeners(workPoint, R.id.work_button, PointType.WORK);
	}

	private void setSpecialButtonOnClickListeners(FavouritePoint point, int buttonId, final PointType pointType) {
		View homeButton = view.findViewById(buttonId);
		homeButton.setOnClickListener(new SpecialButtonOnClickListener(point, pointType));
		homeButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AddPointBottomSheetDialog.showInstance(mapActivity, pointType);
				return true;
			}
		});
	}

	class SpecialButtonOnClickListener implements View.OnClickListener {
		FavouritePoint point;
		PointType pointType;

		SpecialButtonOnClickListener(FavouritePoint point, PointType pointType) {
			this.point = point;
			this.pointType = pointType;
		}

		@Override
		public void onClick(View v) {
			if (point == null) {
				AddPointBottomSheetDialog.showInstance(mapActivity, pointType);
			} else {
				mapActivity.getMyApplication().getTargetPointsHelper().navigateToPoint(
						new LatLon(point.getLatitude(), point.getLongitude()),
						true, -1, point.getPointDescription(app));
				OsmAndLocationProvider.requestFineLocationPermissionIfNeeded(mapActivity);
			}
		}
	}
}
