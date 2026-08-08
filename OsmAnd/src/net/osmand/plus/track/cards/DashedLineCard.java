package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.track.TrackDrawInfo;

public class DashedLineCard extends BaseSwitchCard {

	private final TrackDrawInfo trackDrawInfo;

	public DashedLineCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	int getTitleId() {
		return R.string.gpx_dashed_line;
	}

	@Override
	protected boolean getChecked() {
		return trackDrawInfo.isDashedLine();
	}

	@Override
	protected void setChecked(boolean checked) {
		trackDrawInfo.setDashedLine(checked);
	}
}
