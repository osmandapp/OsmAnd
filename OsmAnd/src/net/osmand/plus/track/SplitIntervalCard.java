package net.osmand.plus.track;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class SplitIntervalCard extends BaseCard {

	private TrackDrawInfo trackDrawInfo;

	public SplitIntervalCard(@NonNull MapActivity mapActivity, TrackDrawInfo trackDrawInfo) {
		super(mapActivity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_right_descr;
	}

	@Override
	protected void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.gpx_split_interval);

		TextView descriptionView = view.findViewById(R.id.description);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SplitIntervalBottomSheet.showInstance(mapActivity.getSupportFragmentManager(), trackDrawInfo);
			}
		});
	}
}