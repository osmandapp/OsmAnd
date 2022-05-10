package net.osmand.plus.track.cards;

import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

public class ShowStartFinishCard extends MapBaseCard {

	private OsmandPreference<Boolean> showStartFinishPreference;

	public ShowStartFinishCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		this.showStartFinishPreference = app.getSettings().SHOW_START_FINISH_ICONS;
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

		final CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		//compoundButton.setChecked(trackDrawInfo.isShowStartFinish());
		compoundButton.setChecked(showStartFinishPreference.get());

		view.setOnClickListener(v -> {
			boolean checked = !compoundButton.isChecked();
			compoundButton.setChecked(checked);
			//trackDrawInfo.setShowStartFinish(checked);
			showStartFinishPreference.set(checked);
			mapActivity.refreshMap();
			notifyCardPressed();
		});
	}
}