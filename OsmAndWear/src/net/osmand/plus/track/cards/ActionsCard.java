package net.osmand.plus.track.cards;

import static android.graphics.Typeface.DEFAULT;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class ActionsCard extends BaseCard {

	public static final int RESET_BUTTON_INDEX = 0;

	public ActionsCard(@NonNull FragmentActivity activity) {
		super(activity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_actions_card;
	}

	@Override
	protected void updateContent() {
		View reset = view.findViewById(R.id.button_reset);
		ImageView icon = reset.findViewById(android.R.id.icon);
		TextView title = reset.findViewById(android.R.id.title);
		View button = reset.findViewById(R.id.selectable_list_item);

		title.setText(R.string.reset_to_original);
		title.setTypeface(DEFAULT);
		title.setTextColor(AndroidUtils.getColorFromAttr(view.getContext(), android.R.attr.textColorPrimary));
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_reset));

		button.setOnClickListener(v -> notifyButtonPressed(RESET_BUTTON_INDEX));
		if (button.getBackground() == null) {
			AndroidUtils.setBackground(button, UiUtilities.getSelectableDrawable(app));
		}
	}
}
