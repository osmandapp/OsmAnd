package net.osmand.plus.track.cards;

import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public abstract class BaseSwitchCard extends BaseCard {

	public BaseSwitchCard(@NonNull FragmentActivity activity) {
		super(activity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_switch;
	}

	@Override
	protected void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);
		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(getTitleId());
		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(getChecked());
		if (getIconRes() != 0) {
			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(getContentIcon(getIconRes()));
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), true);
		}
		view.setOnClickListener(v -> onCardClicked());
	}

	protected void onCardClicked() {
		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		boolean checked = !compoundButton.isChecked();
		compoundButton.setChecked(checked);
		setChecked(checked);
		notifyCardPressed();
	}

	@StringRes
	abstract int getTitleId();

	abstract protected boolean getChecked();

	abstract protected void setChecked(boolean checked);

	@DrawableRes
	protected int getIconRes() {
		return 0;
	}
}