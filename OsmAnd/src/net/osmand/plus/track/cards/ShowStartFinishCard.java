package net.osmand.plus.track.cards;

import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.TrackDrawInfo;

public class ShowStartFinishCard extends MapBaseCard {

	private final TrackDrawInfo trackDrawInfo;

	public ShowStartFinishCard(@NonNull MapActivity mapActivity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(mapActivity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_switch;
	}

	@Override
	protected void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.track_show_start_finish_icons);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(trackDrawInfo.isShowStartFinish());

		view.setOnClickListener(v -> {
			boolean checked = !compoundButton.isChecked();
			compoundButton.setChecked(checked);
			trackDrawInfo.setShowStartFinish(checked);
			mapActivity.refreshMap();
			notifyCardPressed();
		});
	}
}