package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.track.TrackDrawInfo;

public class DirectionArrowsCard extends BaseSwitchCard {

	private final TrackDrawInfo trackDrawInfo;

	public DirectionArrowsCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	int getTitleId() {
		return R.string.gpx_direction_arrows;
	}

	@Override
	protected boolean getChecked() {
		return trackDrawInfo.isShowArrows();
	}

	@Override
	protected void setChecked(boolean checked) {
		trackDrawInfo.setShowArrows(checked);
	}
}