package net.osmand.plus.quickaction;

import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.helpers.AndroidUiHelper;

public class ButtonIconsCard extends MultiStateCard {

	@Override
	public int getCardLayoutId() {
		return R.layout.map_button_icons_card;
	}

	public ButtonIconsCard(@NonNull MapActivity activity, @NonNull MapButtonIconController controller) {
		super(activity, controller.getCardController(), false);
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.button_all_icons), false);
	}

	@Override
	protected void updateCardTitle() {
		TextView title = view.findViewById(R.id.card_title);
		title.setText(R.string.shared_string_icon);
	}
}