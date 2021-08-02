package net.osmand.plus.track;

import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.FontCache;
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
		ImageView icon = view.findViewById(android.R.id.icon);
		TextView title = view.findViewById(android.R.id.title);

		Typeface typeface = FontCache.getRobotoRegular(app);
		title.setText(R.string.reset_to_original);
		title.setTypeface(typeface, typeface.getStyle());
		title.setTextColor(AndroidUtils.getColorFromAttr(view.getContext(), android.R.attr.textColorPrimary));
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_reset));

		reset.setOnClickListener(v -> {
			CardListener listener = getListener();
			if (listener != null) {
				listener.onCardButtonPressed(ActionsCard.this, RESET_BUTTON_INDEX);
			}
		});
		AndroidUtils.setBackground(reset, UiUtilities.getSelectableDrawable(app));
	}
}
