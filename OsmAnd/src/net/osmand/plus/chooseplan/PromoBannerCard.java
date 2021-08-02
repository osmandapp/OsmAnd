package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class PromoBannerCard extends BaseCard {

	public PromoBannerCard(@NonNull FragmentActivity activity, boolean usedOnMap) {
		super(activity, usedOnMap);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.promo_banner_card;
	}

	@Override
	protected void updateContent() {
		View btnLearnMore = view.findViewById(R.id.button_learn_more);
		UiUtilities.setupDialogButton(nightMode, btnLearnMore, DialogButtonType.PRIMARY, R.string.shared_string_learn_more);
		btnLearnMore.setOnClickListener(v -> {
			if (activity != null) {
				ChoosePlanFragment.showInstance(activity, OsmAndFeature.ADVANCED_WIDGETS);
			}
		});

		int bgResId = R.drawable.promo_banner_bg;
		int bgColor = UiUtilities.getColorWithAlpha(getActiveColor(), 0.15f);
		View background = view.findViewById(R.id.banner_background);
		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
		AndroidUtils.setBackground(background, bgDrawable);
	}
}
