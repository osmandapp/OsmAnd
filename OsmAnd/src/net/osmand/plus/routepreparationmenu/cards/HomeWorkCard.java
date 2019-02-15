package net.osmand.plus.routepreparationmenu.cards;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
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
	public void update() {
		if (view != null) {
			TargetPointsHelper targetPointsHelper = mapActivity.getMyApplication().getTargetPointsHelper();
			TargetPoint homePoint = targetPointsHelper.getHomePoint();
			TargetPoint workPoint = targetPointsHelper.getWorkPoint();

			TextView homeDescr = (TextView) view.findViewById(R.id.home_button_descr);
			TextView workDescr = (TextView) view.findViewById(R.id.work_button_descr);
			homeDescr.setText(homePoint != null ? homePoint.getPointDescription(mapActivity).getName() :
					mapActivity.getString(R.string.shared_string_add));
			workDescr.setText(workPoint != null ? workPoint.getPointDescription(mapActivity).getName() :
					mapActivity.getString(R.string.shared_string_add));

			view.findViewById(R.id.home_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openAddPointDialog(mapActivity, true);
				}
			});
			view.findViewById(R.id.work_button).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openAddPointDialog(mapActivity, false);
				}
			});
		}
	}

	@Override
	protected void applyDayNightMode() {

	}

	private void openAddPointDialog(MapActivity mapActivity, boolean home) {
		Bundle args = new Bundle();
		args.putString(AddPointBottomSheetDialog.POINT_TYPE_KEY, home ? PointType.HOME.name() : PointType.WORK.name());
		AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
		fragment.setArguments(args);
		fragment.setUsedOnMap(false);
		fragment.show(mapActivity.getSupportFragmentManager(), AddPointBottomSheetDialog.TAG);
	}
}
