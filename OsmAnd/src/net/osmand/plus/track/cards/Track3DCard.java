package net.osmand.plus.track.cards;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.track.TrackDrawInfo;

public class Track3DCard extends BaseSwitchCard {

	private final TrackDrawInfo trackDrawInfo;

	public Track3DCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
		this.trackDrawInfo = trackDrawInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_switch_blocked_for_free;
	}

	@Override
	int getTitleId() {
		return R.string.use_3d_track_visualization;
	}

	@Override
	protected boolean getChecked() {
		return trackDrawInfo.isUse3DTrackVisualization();
	}

	@Override
	protected void setChecked(boolean checked) {
		trackDrawInfo.setUse3DTrackVisualization(checked);
	}

	@Override
	protected void updateContent() {
		super.updateContent();

		View getBtn = view.findViewById(R.id.get_btn);
		AndroidUiHelper.updateVisibility(getBtn, isGetBtnVisible());
		getBtn.setOnClickListener((v) -> openChoosePlan());
	}

	private boolean isGetBtnVisible() {
		boolean isFullVersion = !Version.isFreeVersion(app) || InAppPurchaseUtils.isFullVersionAvailable(app, false);
		return !isFullVersion && !InAppPurchaseUtils.isSubscribedToAny(app, false);
	}

	private void openChoosePlan() {
		if (activity != null) {
			ChoosePlanFragment.showInstance(activity, OsmAndFeature.RELIEF_3D);
		}
	}

	@Override
	protected void onCardClicked() {
		if (isGetBtnVisible()) {
			openChoosePlan();
		} else {
			super.onCardClicked();
		}
	}
}