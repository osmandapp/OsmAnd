package net.osmand.plus.track.cards;

import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.track.TrackDrawInfo;

public class DirectionArrowsCard extends BaseCard {

	private final TrackDrawInfo trackDrawInfo;

	public DirectionArrowsCard(@NonNull FragmentActivity activity, @NonNull TrackDrawInfo trackDrawInfo) {
		super(activity);
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
		titleView.setText(R.string.gpx_direction_arrows);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(trackDrawInfo.isShowArrows());

		view.setOnClickListener(v -> {
			boolean checked = !compoundButton.isChecked();
			compoundButton.setChecked(checked);
			trackDrawInfo.setShowArrows(checked);
			notifyCardPressed();
		});
	}
}