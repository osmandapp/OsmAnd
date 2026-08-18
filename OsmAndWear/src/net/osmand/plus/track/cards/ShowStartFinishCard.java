package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.track.TrackDrawInfo;

public class ShowStartFinishCard extends BaseSwitchCard {

	private final TrackDrawInfo trackDrawInfo;

	public ShowStartFinishCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	int getTitleId() {
		return R.string.track_show_start_finish_icons;
	}

	@Override
	protected boolean getChecked() {
		return trackDrawInfo.isShowStartFinish();
	}

	@Override
	protected void setChecked(boolean checked) {
		trackDrawInfo.setShowStartFinish(checked);
	}
}